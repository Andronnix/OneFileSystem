package ofs.controller;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BaseFileController implements OFSController {
    private final SeekableByteChannel channel;
    private final Map<String, FileHead> files = new HashMap<>();
    private final FileBlockManager blockManager = new FileBlockManager();

    public BaseFileController(Path baseFile) throws IOException {
        this.channel = Files.newByteChannel(baseFile, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        var strPath = path.toString();

        if(!files.containsKey(strPath)) {
            var head = new FileHead();
            files.put(strPath, head);
        }

        return new BlockFileByteChannel(channel, files.get(strPath), blockManager);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public boolean exists(Path path) throws IOException {
        return files.containsKey(path.toString());
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {
        var strPath = path.toString();
        if(!files.containsKey(strPath)) {
            throw new NoSuchFileException(strPath);
        }

        FileHead h = files.remove(strPath);

        for(var block : h.getBlocks()) {
            blockManager.freeBlock(block);
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        var sourcePath = source.toString();
        if(!files.containsKey(sourcePath)) {
            throw new NoSuchFileException(sourcePath);
        }

        FileHead h = files.remove(sourcePath);
        files.put(target.toString(), h);

        for(var block : h.getBlocks()) {
            blockManager.freeBlock(block);
        }
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
