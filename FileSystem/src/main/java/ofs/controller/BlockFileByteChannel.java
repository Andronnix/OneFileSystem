package ofs.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


public class BlockFileByteChannel implements SeekableByteChannel {
    private final SeekableByteChannel channel;
    private final FileHead fileHead;
    private final FileBlockManager blockManager;
    private boolean isOpen = true;

    private int currentPosition = 0;

    BlockFileByteChannel(SeekableByteChannel channel, FileHead head, FileBlockManager blockManager) {
        this.channel = channel;
        this.fileHead = head;
        this.blockManager = blockManager;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        var fileSize = fileHead.getByteCount();
        if(currentPosition >= fileSize)
            return -1;

        int count = 0;
        ByteBuffer block = ByteBuffer.allocate(FileHead.BLOCK_SIZE);
        while(dst.hasRemaining() && currentPosition < fileSize) {
            int currentBlock = currentPosition / FileHead.BLOCK_SIZE;
            int blockBeginPosition = fileHead.getBlocks().get(currentBlock);
            channel.position(blockBeginPosition);

            int readBytes = channel.read(block);
            block.flip();

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

    @Override
    public int write(ByteBuffer src) throws IOException {
        var bytesWritten = 0;
        var oldFileSize = fileHead.getByteCount();
        var startingPosition = currentPosition;

        ByteBuffer buffer = ByteBuffer.allocate(FileHead.BLOCK_SIZE);
        while(src.hasRemaining()) {
            var bytesLeftInCurrentBlock = FileHead.BLOCK_SIZE - currentPosition % FileHead.BLOCK_SIZE;

            if(bytesLeftInCurrentBlock == FileHead.BLOCK_SIZE && currentPosition >= oldFileSize) {
                var blockPtr = blockManager.allocateBlock();
                if(blockPtr.isEmpty()) {
                    fileHead.setByteCount(startingPosition + bytesWritten);
                    return bytesWritten;
                }

                fileHead.expand(blockPtr.get());
            }

            var bytesToWrite = Math.min(src.remaining(), bytesLeftInCurrentBlock);

            for(int i = 0; i < bytesToWrite; i++) {
                buffer.put(src.get());
            }
            buffer.flip();
            channel.write(buffer);

            currentPosition += bytesToWrite;
            bytesWritten += bytesToWrite;

            buffer.rewind();
        }

        fileHead.setByteCount(startingPosition + bytesWritten);

        return bytesWritten;
    }

    @Override
    public long position() throws IOException {
        return currentPosition;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if(newPosition > Integer.MAX_VALUE)
            currentPosition = Integer.MAX_VALUE;
        else
            currentPosition = (int) newPosition;

        return this;
    }

    @Override
    public long size() throws IOException {
        return fileHead.getByteCount();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return null;
    }

    @Override
    public boolean isOpen() {
        return isOpen && channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.isOpen = false;
    }
}
