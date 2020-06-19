package ofs.controller;

import ofs.OFSPath;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;
import java.util.Set;

public class TempFileController implements OFSController {
    private final File baseFile;

    public TempFileController() throws IOException {
        baseFile = Files.createTempFile("ofs", "sfo").toFile();
    }

    @Override
    public SeekableByteChannel newByteChannel(OFSPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(OFSPath dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public boolean exists(OFSPath path) throws IOException {
        return false;
    }

    @Override
    public void createDirectory(OFSPath dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(OFSPath path) throws IOException {

    }

    @Override
    public void copy(OFSPath source, OFSPath target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(OFSPath source, OFSPath target, CopyOption... options) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(OFSPath path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(OFSPath path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(OFSPath path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(OFSPath path, String attribute, Object value, LinkOption... options) throws IOException {

    }
}