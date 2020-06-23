package ofs.blockimpl;

import ofs.tree.OFSTreeNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

public class BlockFileSerializer {
    private final BlockManager blockManager;
    private final SeekableByteChannel channel;

    public BlockFileSerializer(@NotNull SeekableByteChannel channel, @NotNull BlockManager blockManager) {
        this.blockManager = blockManager;
        this.channel = channel;
    }

    private void seekBlock(int block) throws IOException {
        channel.position(blockManager.getBlockSize() * block);
    }

    private void seekPositionInFile(@NotNull BlockFileHead fileHead, int position) throws IOException {
        var offset = position % blockManager.getBlockSize();
        var currentBlock = position / blockManager.getBlockSize();

        channel.position(fileHead.getBlocks().get(currentBlock) * blockManager.getBlockSize() + offset);
    }

    public void serializeFileHead(@NotNull BlockFileHead fileHead) throws IOException {
        var nameBytes = fileHead.getName().getBytes();

        var serialized = ByteBuffer.allocate(
                nameBytes.length + 4 // name + it's length
                        + 4 // address
                        + 4 // byteCount
                        + 1 // isDirectory
                        + 4 // blocksSize
                        + fileHead.getBlocks().size() * 4 // actual blocks
        );

        serialized.putInt(nameBytes.length); serialized.put(nameBytes);
        serialized.putInt(fileHead.getAddress());
        serialized.putInt(fileHead.getByteCount());
        serialized.put((byte) (fileHead.isDirectory() ? 1 : 0));
        serialized.putInt(fileHead.getBlocks().size());
        for(var block : fileHead.getBlocks()) {
            serialized.putInt(block);
        }

        serialized.flip();

        seekBlock(fileHead.getAddress());
        channel.write(serialized);
    }

    public BlockFileHead deserializeFileHead(int block) throws IOException {
        seekBlock(block);

        var in = ByteBuffer.allocate(blockManager.getBlockSize());
        channel.read(in);
        in.flip();

        int nameLength = in.getInt();
        byte[] nameBytes = new byte[nameLength];

        in.get(nameBytes);
        var name = new String(nameBytes);
        var address = in.getInt();
        var byteCount = in.getInt();
        var isDirectory = in.get() > 0;

        var blockCount = in.getInt();
        var blocks = new ArrayList<Integer>(blockCount);
        for(int i = 0; i < blockCount; i++) {
            blocks.add(in.getInt());
        }

        return new BlockFileHead(name, address, byteCount, isDirectory, blocks);
    }

    private void ensureFileHasEnoughBlocks(@NotNull BlockFileHead file, int requiredCapacity) throws IOException {
        var neededBlocks = (int) Math.ceil(requiredCapacity / (1.0 * blockManager.getBlockSize()));
        var blocksToAllocate = neededBlocks - file.getBlocks().size();
        if(blocksToAllocate <= 0)
            return;

        if(neededBlocks > BlockFileHead.getMaxContentBlockCount(blockManager.getBlockSize())) {
            throw new IOException("File is too large.");
        }

        var blocks = blockManager.allocateBlocks(blocksToAllocate);
        if(blocks.isEmpty()) {
            throw new IOException("Couldn't allocate enough space.");
        }

        for(var block : blocks.get()) {
            file.expand(block);
        }
    }

    public int writeAt(@NotNull ByteBuffer src, @NotNull BlockFileHead file, int positionInFile) throws IOException {
        var bytesWritten = 0;
        var startingPosition = positionInFile;

        ensureFileHasEnoughBlocks(file, positionInFile + src.remaining());

        ByteBuffer buffer = ByteBuffer.allocate(blockManager.getBlockSize());
        while(src.hasRemaining()) {
            var remainingBytesInCurrentBLock = blockManager.getBlockSize() - positionInFile % blockManager.getBlockSize();

            seekPositionInFile(file, positionInFile);

            var bytesToWrite = Math.min(src.remaining(), remainingBytesInCurrentBLock);
            for(int i = 0; i < bytesToWrite; i++) {
                buffer.put(src.get());
            }
            buffer.flip();
            channel.write(buffer);

            positionInFile += bytesToWrite;
            bytesWritten += bytesToWrite;

            buffer.clear();
        }

        file.setByteCount(Math.max(startingPosition + bytesWritten, file.getByteCount()));

        serializeFileHead(file);

        return bytesWritten;
    }

    public int readAt(@NotNull ByteBuffer dst, @NotNull BlockFileHead file, int positionInFile) throws IOException {
        var fileSize = file.getByteCount();
        if(positionInFile >= fileSize)
            return -1;

        int count = 0;
        ByteBuffer block = ByteBuffer.allocate(blockManager.getBlockSize());
        while(dst.hasRemaining() && positionInFile < fileSize) {
            int offset = positionInFile % blockManager.getBlockSize();
            int currentBlock = positionInFile / blockManager.getBlockSize();
            int blockBeginPosition = file.getBlocks().get(currentBlock) * blockManager.getBlockSize();
            channel.position(blockBeginPosition);

            int readBytes = channel.read(block) - offset;
            block.flip();
            block.position(offset);

            if(dst.remaining() >= readBytes && positionInFile + readBytes < fileSize) {
                dst.put(block);

                count += readBytes;
                positionInFile += readBytes;
            } else {
                while(dst.hasRemaining() && positionInFile < fileSize) {
                    dst.put(block.get());
                    count++;
                    positionInFile++;
                }
            }

            block.clear();
        }

        return count;
    }

    public int truncate(@NotNull BlockFileHead file, int currentPosition, int desiredSize) {
        if(desiredSize < 0)
            throw new IllegalArgumentException("Size must be positive.");

        var newNeededBlocks = (int) Math.ceil(desiredSize / (1.0 * blockManager.getBlockSize()));
        var oldBlocksCount = file.getBlocks().size();

        var newPosition = Math.min(currentPosition, desiredSize);
        if(newNeededBlocks < oldBlocksCount) {
            var fileBlocks = file.getBlocks();
            for (int i = oldBlocksCount - 1; i >= newNeededBlocks; i--) {
                var last = fileBlocks.get(i);
                blockManager.freeBlock(last);

                fileBlocks.remove(i);
            }
        }

        return newPosition;
    }

    public void serializeDirectory(@NotNull OFSTreeNode<BlockFileHead> dir) throws IOException {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var contentBuffer = serializeDirectoryChildrenList(dir);
        writeAt(contentBuffer, dir.getFile(), 0);

        serializeFileHead(dir.getFile());
    }

    private ByteBuffer serializeDirectoryChildrenList(@NotNull OFSTreeNode<BlockFileHead> dir) {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var children = dir.getAllChildren();

        var buffer = ByteBuffer.allocate(
                4 + // Children.size()
                        children.size() * 4 // Block address of each child
        );

        buffer.putInt(children.size());
        for(var c : children) {
            buffer.putInt(c.getFile().getAddress());
        }

        buffer.flip();

        return buffer;
    }

    void deserializeDirectory(@NotNull OFSTreeNode<BlockFileHead> dir) throws IOException {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var countBuffer = ByteBuffer.allocate(4);
        readAt(countBuffer, dir.getFile(), 0); countBuffer.flip();
        var childrenCount = countBuffer.getInt();

        var childrenBuffer = ByteBuffer.allocate(childrenCount * 4);
        readAt(childrenBuffer, dir.getFile(), 4); childrenBuffer.flip();

        for(int i = 0; i < childrenCount; i++) {
            var childBlock = childrenBuffer.getInt();
            var childHead = deserializeFileHead(childBlock);

            dir.addChild(new OFSTreeNode<>(childHead));
        }

        for(var childDir : dir.getChildDirectories()) {
            deserializeDirectory(childDir);
        }
    }

    public boolean isOpen() {
        return channel.isOpen();
    }
}
