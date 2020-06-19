package ofs.controller;

import ofs.OFSPath;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;
import java.util.Set;

public interface OFSController {
    SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException;

    DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException;

    boolean exists(Path path) throws IOException;

    void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException;

    void delete(Path path) throws IOException;

    void copy(Path source, Path target, CopyOption... options) throws IOException;

    void move(Path source, Path target, CopyOption... options) throws IOException;

    <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options);

    <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException;

    Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException;

    void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException;
}
