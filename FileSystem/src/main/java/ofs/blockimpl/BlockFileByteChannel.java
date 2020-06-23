package ofs.blockimpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


public class BlockFileByteChannel implements SeekableByteChannel {
    private final SeekableByteChannel channel;
    private final BlockFileHead fileHead;
    private final BlockManager blockManager;
    private final BlockFileSerializer fileSerializer;
    private boolean isOpen = true;

    private int currentPosition = 0;

    BlockFileByteChannel(SeekableByteChannel channel, BlockFileHead head, BlockFileSerializer fileSerializer, BlockManager blockManager) {
        this.channel = channel;
        this.fileHead = head;
        this.blockManager = blockManager;
        this.fileSerializer = fileSerializer;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureIsOpen();

        var fileSize = fileHead.getByteCount();
        if(currentPosition >= fileSize)
            return -1;

        int count = 0;
        ByteBuffer block = ByteBuffer.allocate(blockManager.getBlockSize());
        while(dst.hasRemaining() && currentPosition < fileSize) {
            int offset = currentPosition % blockManager.getBlockSize();
            int currentBlock = currentPosition / blockManager.getBlockSize();
            int blockBeginPosition = fileHead.getBlocks().get(currentBlock) * blockManager.getBlockSize();
            channel.position(blockBeginPosition);

            int readBytes = channel.read(block) - offset;
            block.flip();
            block.position(offset);

            if(dst.remaining() >= readBytes && currentPosition + readBytes < fileSize) {
                dst.put(block);

                count += readBytes;
                currentPosition += readBytes;
            } else {
                while(dst.hasRemaining() && currentPosition < fileSize) {
                    dst.put(block.get());
                    count++;
                    currentPosition++;
                }
            }

            block.clear();
        }

        return count;
    }

    private void ensureFileHasEnoughBlocks(int writeBytes) throws IOException {
        ensureIsOpen();

        var neededCapacity = writeBytes + currentPosition;
        var neededBlocks = (int) Math.ceil(neededCapacity / (1.0 * blockManager.getBlockSize()));
        var blocksToAllocate = neededBlocks - fileHead.getBlocks().size();
        if(blocksToAllocate <= 0)
            return;

        if(neededBlocks > BlockFileHead.getMaxContentBlockCount(blockManager.getBlockSize())) {
            throw new IOException("Too large file");
        }

        var blocks = blockManager.allocateBlocks(blocksToAllocate);
        if(blocks.isEmpty()) {
            throw new IOException("Couldn't allocate enough space");
        }

        for(var block : blocks.get()) {
            fileHead.expand(block);
        }
    }

    private void ensureUnderlyingChannelPosition() throws IOException {
        var offset = currentPosition % blockManager.getBlockSize();
        var currentBlock = currentPosition / blockManager.getBlockSize();

        channel.position(fileHead.getBlocks().get(currentBlock) * blockManager.getBlockSize() + offset);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ensureIsOpen();

        var bytesWritten = 0;
        var startingPosition = currentPosition;

        ensureFileHasEnoughBlocks(src.remaining());

        ByteBuffer buffer = ByteBuffer.allocate(blockManager.getBlockSize());
        while(src.hasRemaining()) {
            var remainingBytesInCurrentBLock = blockManager.getBlockSize() - currentPosition % blockManager.getBlockSize();

            ensureUnderlyingChannelPosition();

            var bytesToWrite = Math.min(src.remaining(), remainingBytesInCurrentBLock);
            for(int i = 0; i < bytesToWrite; i++) {
                buffer.put(src.get());
            }
            buffer.flip();
            channel.write(buffer);

            currentPosition += bytesToWrite;
            bytesWritten += bytesToWrite;

            buffer.clear();
        }

        fileHead.setByteCount(Math.max(startingPosition + bytesWritten, fileHead.getByteCount()));

        fileSerializer.serializeFile(fileHead);

        return bytesWritten;
    }

    @Override
    public long position() throws IOException {
        ensureIsOpen();

        return currentPosition;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        ensureIsOpen();

        if(newPosition > Integer.MAX_VALUE)
            currentPosition = Integer.MAX_VALUE;
        else
            currentPosition = (int) newPosition;

        return this;
    }

    @Override
    public long size() throws IOException {
        ensureIsOpen();

        return fileHead.getByteCount();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        if(size < 0)
            throw new IllegalArgumentException("Size must be positive.");

        var newNeededBlocks = (int) Math.ceil(size / (1.0 * blockManager.getBlockSize()));
        var oldBlocksCount = fileHead.getBlocks().size();

        currentPosition = Math.min(currentPosition, (int) size);
        if(newNeededBlocks < oldBlocksCount) {
            var fileBlocks = fileHead.getBlocks();
            for (int i = oldBlocksCount - 1; i >= newNeededBlocks; i--) {
                var last = fileBlocks.get(i);
                blockManager.freeBlock(last);

                fileBlocks.remove(i);
            }
        }

        return this;
    }

    @Override
    public boolean isOpen() {
        return isOpen && channel.isOpen();
    }

    @Override
    public void close() {
        this.isOpen = false;
    }

    private void ensureIsOpen() throws IOException {
        if(!isOpen())
            throw new IOException("Channel is closed");
    }
}
