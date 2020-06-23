package ofs.blockimpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;


public class BlockFileByteChannel implements SeekableByteChannel {
    private final BlockFileHead fileHead;
    private final BlockFileSerializer fileSerializer;
    private boolean isOpen = true;

    private int currentPosition = 0;

    BlockFileByteChannel(BlockFileHead head, BlockFileSerializer fileSerializer) {
        this.fileHead = head;
        this.fileSerializer = fileSerializer;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureIsOpen();

        var bytesRead = fileSerializer.readAt(dst, fileHead, currentPosition);

        if(bytesRead > 0)
            currentPosition += bytesRead;

        return bytesRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        ensureIsOpen();

        var bytesWritten = fileSerializer.writeAt(src, fileHead, currentPosition);
        currentPosition += bytesWritten;

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
        ensureIsOpen();

        currentPosition = fileSerializer.truncate(fileHead, currentPosition, (int) size);

        return this;
    }

    @Override
    public boolean isOpen() {
        return isOpen && fileSerializer.isOpen();
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
