package ofs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Set;

public class OFSFileSystem extends FileSystem {
    static final String SEPARATOR = "$";

    private final OFSFileSystemProvider provider;

    OFSFileSystem(OFSFileSystemProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public void close() throws IOException {
        provider.close();
    }

    @Override
    public boolean isOpen() {
        return provider.isOpen();
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
        return List.of(new OFSPath(List.of(), this, true));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of("basic");
    }

    @NotNull
    @Override
    public Path getPath(@NotNull String first, @NotNull String... more) {
        String repr = first + OFSFileSystem.SEPARATOR + String.join(OFSFileSystem.SEPARATOR, more);

        return new OFSPath(repr, this);
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
