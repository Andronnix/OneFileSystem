package ofs;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public class OFSFileSystemProvider extends FileSystemProvider {
    private OFSFileSystem fileSystem;
    static final String ROOT = "]=";

    @Override
    public String getScheme() {
        return "ofs";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        if(fileSystem != null) {
            throw new FileSystemAlreadyExistsException("There can be only one OFS");
        }

        if(!uri.getScheme().equals(getScheme())) {
            throw new IllegalArgumentException(
                    String.format("Wrong uri scheme. Expected %s, got %s", getScheme(), uri.getScheme())
            );
        }

        fileSystem = new OFSFileSystem(File.createTempFile("ofs", "sfo"), this);

        return fileSystem;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        if(fileSystem == null)
            throw new FileSystemNotFoundException("The OFS was never created.");

        return fileSystem;
    }

    @NotNull
    @Override
    public Path getPath(URI uri) {
        if(!uri.getScheme().equals(getScheme())) {
            throw new IllegalArgumentException(
                    String.format("Expected %s scheme, found %s", getScheme(), uri.getScheme())
            );
        }

        if(fileSystem == null) {
            throw new FileSystemNotFoundException();
        }

        StringTokenizer st = new StringTokenizer(uri.getSchemeSpecificPart(), OFSFileSystem.SEPARATOR);
        List<String> path = new ArrayList<>();

        while(st.hasMoreTokens()) {
            var token = st.nextToken();
            path.add(token);
        }

        boolean isAbsolutePath = false;
        if(path.size() > 0 && path.get(0).equals(ROOT)) {
            path = path.subList(1, path.size());
            isAbsolutePath = true;
        }

        return new OFSPath(path, fileSystem, isAbsolutePath);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }
}
