package ofs.controller;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;

public class BlockFileAttributes implements BasicFileAttributes {
    private final FileHead file;
    BlockFileAttributes(FileHead file) {
        this.file = file;
    }

    @Override
    public FileTime lastModifiedTime() {
        return null;
    }

    @Override
    public FileTime lastAccessTime() {
        return null;
    }

    @Override
    public FileTime creationTime() {
        return null;
    }

    @Override
    public boolean isRegularFile() {
        return file != null && !file.isDirectory;
    }

    @Override
    public boolean isDirectory() {
        return file != null && file.isDirectory;
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
