package ofs.blockimpl;

import ofs.controller.OFSController;
import ofs.tree.OFSTree;
import ofs.tree.OFSTreeNode;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class BlockFileController implements OFSController {
    private final SeekableByteChannel channel;
    private final BlockManager blockManager = new BlockManager();
    private final OFSTree<BlockFileHead> fileTree;

    public BlockFileController(Path baseFile) throws IOException {
        this.channel = Files.newByteChannel(baseFile, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE));
        this.fileTree = new OFSTree<>(new BlockFileHead("", true));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        var node = fileTree.getNode(path);
        if(node != null) {
            if(node.isDirectory()) {
                throw new IllegalArgumentException("Can't create byte channel from directory");
            }
            return new BlockFileByteChannel(channel, node.getFile(), blockManager);
        }

        // File exists
        var head = new BlockFileHead(path.getFileName().toString(), false);
        if(!fileTree.addNode(path, head)) {
            throw new IllegalArgumentException();
        }

        return new BlockFileByteChannel(channel
                , head, blockManager);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        var dirNode = fileTree.getNode(dir);

        if(dirNode == null) {
            throw new NoSuchFileException(dir.toString());
        }

        if(!dirNode.isDirectory()) {
            throw new NotDirectoryException(dir.toString());
        }

        return new DirectoryStream<>() {
            private boolean invoked = false;
            @NotNull
            @Override
            public Iterator<Path> iterator() {
                if(invoked)
                    throw new IllegalStateException();

                invoked = true;
                return new Iterator<>() {
                    private final List<OFSTreeNode<BlockFileHead>> children = dirNode.getChildDirectories();
                    private int current = 0;
                    @Override
                    public boolean hasNext() {
                        return current < children.size();
                    }

                    @Override
                    public Path next() {
                        var name = children.get(current).getFile().getName();
                        current++;
                        return dir.resolve(Path.of(name));
                    }
                };
            }

            @Override
            public void close() throws IOException {}
        };
    }

    @Override
    public boolean exists(Path path) throws IOException {
        return fileTree.exists(path);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        if(fileTree.exists(dir)) {
            throw new FileAlreadyExistsException(dir.toString());
        }

        var parent = fileTree.getParentNode(dir);
        if(parent == null)
            throw new NoSuchFileException(dir.toString());

        var head = new BlockFileHead(dir.getFileName().toString(), true);
        if(!fileTree.addNode(dir, head)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        if(!fileTree.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }

        BlockFileHead h = fileTree.deleteNode(path);

        for(var block : h.getBlocks()) {
            blockManager.freeBlock(block);
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        if(!fileTree.exists(source)) {
            throw new NoSuchFileException(source.toString());
        }

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

        if(fileTree.exists(target)) {
            if(!replaceExisting) {
                throw new FileAlreadyExistsException(target.toString());
            }

            delete(target);
        }

        var targetStream = Channels.newOutputStream(newByteChannel(target, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)));
        var sourceStream = Channels.newInputStream(newByteChannel(source, Set.of(StandardOpenOption.READ)));

        sourceStream.transferTo(targetStream);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        if(!fileTree.exists(source)) {
            throw new NoSuchFileException(source.toString());
        }

        BlockFileHead head = fileTree.deleteNode(source).copyWithName(target.getFileName().toString());

        fileTree.addNode(target, head);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributeView.class)
            return (V) new BlockFileAttributeView(fileTree.getNode(path).getFile());

        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributes.class)
            return (A) new BlockFileAttributes(fileTree.getNode(path).getFile());

        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return new BlockFileAttributes(fileTree.getNode(path).getFile()).toMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    }
}
