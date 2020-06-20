package ofs.blockimpl;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;

public class BlockFileAttributes implements BasicFileAttributes {
    private final BlockFileHead file;
    BlockFileAttributes(BlockFileHead file) {
        this.file = file;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.from(Instant.EPOCH);
    }

    @Override
    public FileTime lastAccessTime() {
        return FileTime.from(Instant.EPOCH);
    }

    @Override
    public FileTime creationTime() {
        return FileTime.from(Instant.EPOCH);
    }

    @Override
    public boolean isRegularFile() {
        return file != null && !file.isDirectory();
    }

    @Override
    public boolean isDirectory() {
        return file != null && file.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return file == null ? 0 : file.getByteCount();
    }

    @Override
    public Object fileKey() {
        return null;
    }

    public Map<String, Object> toMap() {
        return Map.of(
               "isRegularFile", isRegularFile(),
               "isDirectory", isDirectory(),
               "isSymbolicLink", false,
               "isOther", false,
               "size", size()
        );
    }
}
