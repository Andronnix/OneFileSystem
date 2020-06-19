package ofs.blockimpl;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class BlockFileAttributeView implements BasicFileAttributeView {
    private final BlockFileHead file;

    BlockFileAttributeView(BlockFileHead file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "basic";
    }

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return new BlockFileAttributes(file);
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
    }
}
