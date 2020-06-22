package ofs.blockimpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


public class BlockFileByteChannel implements SeekableByteChannel {
    private final SeekableByteChannel channel;
    private final BlockFileHead fileHead;
    private final BlockManager blockManager;
    private boolean isOpen = true;

    private int currentPosition = 0;

    BlockFileByteChannel(SeekableByteChannel channel, BlockFileHead head, BlockManager blockManager) {
        this.channel = channel;
        this.fileHead = head;
        this.blockManager = blockManager;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureIsOpen();

        var fileSize = fileHead.getByteCount();
        if(currentPosition >= fileSize)
            return -1;

        int count = 0;
        ByteBuffer block = ByteBuffer.allocate(BlockFileHead.BLOCK_SIZE);
        while(dst.hasRemaining() && currentPosition < fileSize) {
            int offset = currentPosition % BlockFileHead.BLOCK_SIZE;
            int currentBlock = currentPosition / BlockFileHead.BLOCK_SIZE;
            int blockBeginPosition = fileHead.getBlocks().get(currentBlock) * BlockFileHead.BLOCK_SIZE;
            channel.position(blockBeginPosition);

            int readBytes = channel.read(block) - offset;
            block.flip();
            block.position(offset);

            if(dst.remaining() >= readBytes) {
                dst.put(block);

                count += readBytes;
                currentPosition += readBytes;
            } else {
                while(dst.hasRemaining()) {
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
        var neededBlocks = (int) Math.ceil(neededCapacity / (1.0 * BlockFileHead.BLOCK_SIZE));
        var blocksToAllocate = neededBlocks - fileHead.getBlocks().size();
        if(blocksToAllocate <= 0)
            return;

        var blocks = blockManager.allocateBlocks(blocksToAllocate);
        if(blocks.isEmpty()) {
            throw new IOException("Couldn't allocate enough space");
        }

        for(var block : blocks.get()) {
            fileHead.expand(block);
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ensureIsOpen();

        var bytesWritten = 0;
        var startingPosition = currentPosition;

        ensureFileHasEnoughBlocks(src.remaining());

        ByteBuffer buffer = ByteBuffer.allocate(BlockFileHead.BLOCK_SIZE);
        while(src.hasRemaining()) {
            var bytesLeftInCurrentBlock = BlockFileHead.BLOCK_SIZE - currentPosition % BlockFileHead.BLOCK_SIZE;

            if(bytesLeftInCurrentBlock == BlockFileHead.BLOCK_SIZE) {
                var currentBlock = currentPosition / BlockFileHead.BLOCK_SIZE;
                channel.position(fileHead.getBlocks().get(currentBlock) * BlockFileHead.BLOCK_SIZE);
            }

            var bytesToWrite = Math.min(src.remaining(), bytesLeftInCurrentBlock);
            for(int i = 0; i < bytesToWrite; i++) {
                buffer.put(src.get());
            }
            buffer.flip();
            channel.write(buffer);

            currentPosition += bytesToWrite;
            bytesWritten += bytesToWrite;

            buffer.clear();
        }

        fileHead.setByteCount(startingPosition + bytesWritten);

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
        throw new UnsupportedOperationException("Not implemented");
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
