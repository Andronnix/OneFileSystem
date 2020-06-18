package ofs;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class OFSFileSystem extends FileSystem {
    static final String SEPARATOR = "$";

    private File baseFile;
    private OFSFileSystemProvider provider;

    OFSFileSystem(File baseFile, OFSFileSystemProvider provider) {
        if(!baseFile.exists())
            throw new IllegalArgumentException("Base file doesn't exist.");

        if(!baseFile.isFile())
            throw new IllegalArgumentException("Base file is not a file.");

        if(!baseFile.canRead() || !baseFile.canWrite())
            throw new IllegalArgumentException("Base file must be readable and writable.");

        this.baseFile = baseFile;
        this.provider = provider;
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Path getPath(String first, String... more) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("UserPrincipalLookupService for main.java.ofs doesn't exist.");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("WatchService for main.java.ofs doesn't exist.");
    }
}
