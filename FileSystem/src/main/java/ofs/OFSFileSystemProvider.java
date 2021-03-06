package ofs;

import ofs.controller.OFSController;
import ofs.blockimpl.BlockFileController;
import org.jetbrains.annotations.NotNull;

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
    private OFSController controller;

    static final String ROOT = "]=";
    static final String SCHEME = "ofs";

    public void close() throws IOException {
        controller.close();
    }

    public boolean isOpen() {
        return controller.isOpen();
    }

    @Override
    public String getScheme() {
        return SCHEME;
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

        var shouldDeserialize = env.containsKey("deserialize") ? (Boolean) env.get("deserialize") : false;
        if(shouldDeserialize && !env.containsKey("basePath")) {
            throw new IllegalArgumentException("Base path must be provided to deserialize fs");
        }

        var baseFile = env.containsKey("basePath") ? (Path) env.get("basePath") : Files.createTempFile("ofs", "sfo");

        controller = new BlockFileController(baseFile, shouldDeserialize);
        fileSystem = new OFSFileSystem(this);

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

        return new OFSPath(uri.getSchemeSpecificPart(), fileSystem);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        return controller.newByteChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        if(!(dir instanceof OFSPath))
            throw new IllegalArgumentException();

        return controller.newDirectoryStream(dir, filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if(!(dir instanceof OFSPath))
            throw new IllegalArgumentException();

        controller.createDirectory(dir, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        controller.delete(path);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        if(!(source instanceof OFSPath && target instanceof OFSPath))
            throw new IllegalArgumentException();

        controller.copy(source, target, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        if(!(source instanceof OFSPath && target instanceof OFSPath))
            throw new IllegalArgumentException();

        controller.move(source, target, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return path.equals(path2);
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
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        if(!controller.exists(path))
            throw new NoSuchFileException(path.toString());
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        return controller.getFileAttributeView(path, type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        return controller.readAttributes(path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        return controller.readAttributes(path, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        if(!(path instanceof OFSPath))
            throw new IllegalArgumentException();

        controller.setAttribute(path, attribute, value, options);
    }
}
