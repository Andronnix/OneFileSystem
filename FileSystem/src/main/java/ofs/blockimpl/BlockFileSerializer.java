package ofs.blockimpl;

import ofs.OFSPath;
import ofs.tree.OFSTreeNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

public class BlockFileSerializer {
    private final BlockManager blockManager;
    private final SeekableByteChannel channel;
    private final int EMPTY = -1;

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
        ensureHeadHasEnoughBlocks(fileHead);

        var serialized = ByteBuffer.allocate(blockManager.getBlockSize());

        var nameBytes = fileHead.getName().getBytes();
        serialized.putInt(nameBytes.length); serialized.put(nameBytes);
        serialized.putInt(fileHead.getFullAddress().size() == 1 ? EMPTY : fileHead.getFullAddress().get(1));
        serialized.putInt(fileHead.getByteCount());
        serialized.put((byte) (fileHead.isDirectory() ? 1 : 0));

        var blocks = fileHead.getBlocks();
        serialized.putInt(blocks.size());
        int block = 0;
        while(block < blocks.size() && serialized.remaining() >= 4) {
            serialized.putInt(blocks.get(block));
            block++;
        }

        seekBlock(fileHead.getAddress(0)); serialized.flip();
        channel.write(serialized);

        int currentHeadBlock = 1;
        while(block < blocks.size()) {
            serialized.clear();
            var lastBlock = currentHeadBlock + 1 == fileHead.getFullAddress().size();
            serialized.putInt(lastBlock ? EMPTY : fileHead.getAddress(currentHeadBlock + 1));
            while(block < blocks.size() && serialized.remaining() > 4) {
                serialized.putInt(blocks.get(block));
                block++;
            }

            seekBlock(fileHead.getFullAddress().get(currentHeadBlock));
            channel.write(serialized);
        }
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
        var address = new ArrayList<Integer>(); address.add(block);
        var nextAddress = in.getInt();
        var byteCount = in.getInt();
        var isDirectory = in.get() > 0;

        var blockCount = in.getInt();
        var blocks = new ArrayList<Integer>();

        while(in.remaining() >= 4 && blocks.size() < blockCount) {
            blocks.add(in.getInt());
        }

        while(blocks.size() < blockCount) {
            in.clear();
            if(nextAddress == EMPTY)
                throw new IllegalArgumentException("Wrong format, not enough header blocks");
            address.add(nextAddress);

            seekBlock(nextAddress);
            channel.read(in); in.flip();

            nextAddress = in.getInt();
            while(blocks.size() < blockCount && in.remaining() >= 4) {
                blocks.add(in.getInt());
            }
        }

        return new BlockFileHead(name, address, byteCount, isDirectory, blocks);
    }

    private void ensureHeadHasEnoughBlocks(@NotNull BlockFileHead file) throws IOException {
        var headerSize =
                4 + //name length
                OFSPath.MAX_NAME_LENGTH + // nameBytes
                4 + // next Address
                4 + // content byte count
                1 + // isDirectory
                4 + // content blocks count
                4 * file.getBlocks().size(); // content blocks themselves

        var blockSize = blockManager.getBlockSize();

        if(headerSize <= blockSize)
            return;

        var additionalBlocks = (int) Math.ceil((headerSize - blockSize) / (1.0 * blockSize - 4.0));

        var fullAddress = file.getFullAddress();
        while(fullAddress.size() > additionalBlocks) {
            var last = fullAddress.remove(fullAddress.size() - 1);
            blockManager.freeBlock(last);
        }

        var tail = blockManager.allocateBlocks(additionalBlocks - (fullAddress.size() - 1));
        if(tail.isEmpty())
            throw new IOException("Couldn't allocate file header. Not enough space");

        fullAddress.addAll(tail.get());
    }

    private void ensureFileHasEnoughBlocks(@NotNull BlockFileHead file, int requiredCapacity) throws IOException {
        var neededBlocks = (int) Math.ceil(requiredCapacity / (1.0 * blockManager.getBlockSize()));
        var blocksToAllocate = neededBlocks - file.getBlocks().size();
        if(blocksToAllocate <= 0)
            return;

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

        var children = serializeDirectoryChildrenList(dir);
        writeAt(children, dir.getFile(), 0);

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
            buffer.putInt(c.getFile().getAddress(0));
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
