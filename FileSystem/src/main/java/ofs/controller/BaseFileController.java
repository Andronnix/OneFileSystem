package ofs.controller;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
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
            var head = new FileHead(false);
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
        var dirStr = dir.toString();
        if(files.containsKey(dirStr)) {
            throw new FileAlreadyExistsException(dirStr);
        }

        var head = new FileHead(true);
        files.put(dirStr, head);
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
        var sourcePath = source.toString();
        if(!files.containsKey(sourcePath)) {
            throw new NoSuchFileException(sourcePath);
        }
        var targetPath = target.toString();

        var replaceExisting = false;
        var copyAttributes = false;
        for(var opt : options) {
            if(opt == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
            }
            if(opt == StandardCopyOption.COPY_ATTRIBUTES) {
                copyAttributes = true;
            }
        }

        if(files.containsKey(targetPath)) {
            if(!replaceExisting) {
                throw new FileAlreadyExistsException(targetPath);
            }

            delete(target);
        }

        var targetStream = Channels.newOutputStream(newByteChannel(target, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)));
        var sourceStream = Channels.newInputStream(newByteChannel(source, Set.of(StandardOpenOption.READ)));

        sourceStream.transferTo(targetStream);
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
        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributeView.class)
            return (V) new BlockFileAttributeView(files.get(path));

        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributes.class)
            return (A) new BlockFileAttributes(files.get(path.toString()));

        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return new BlockFileAttributes(files.get(path.toString())).toMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    }
}
