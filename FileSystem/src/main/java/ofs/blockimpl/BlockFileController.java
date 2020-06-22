package ofs.blockimpl;

import ofs.controller.OFSController;
import ofs.tree.OFSTree;
import ofs.tree.OFSTreeNode;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class BlockFileController implements OFSController {
    public static int BLOCK_SIZE = 1024;
    public static int MAX_SPACE = 1024 * 1024 * 1024; // Gigabyte
    private final SeekableByteChannel channel;
    private final BlockManager blockManager = new BlockManager(BLOCK_SIZE, MAX_SPACE);
    private final OFSTree<BlockFileHead> fileTree;

    public BlockFileController(@NotNull Path baseFile, boolean shouldDeserialize) throws IOException {
        this.channel = Files.newByteChannel(baseFile, Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE));

        if(shouldDeserialize) {
            this.fileTree = deserializeTree();
        } else {
            var rootBlock = blockManager.allocateBlock();
            if (rootBlock.isEmpty())
                throw new IOException();

            var rootHead = new BlockFileHead("", true, rootBlock.get());
            this.fileTree = new OFSTree<>(rootHead);

            serializeDirectory(this.fileTree.getRoot());
        }
    }

    private OFSTree<BlockFileHead> deserializeTree() throws IOException {
        var buffer = ByteBuffer.allocate(BLOCK_SIZE);
        channel.position(0);
        channel.read(buffer);

        buffer.flip();
        var root = new BlockFileHead(buffer);

        var fileTree = new OFSTree<>(root);

        deserializeDirectory(fileTree.getRoot());

        return fileTree;
    }

    private BlockFileHead allocateHead(@NotNull String name, boolean isDirectory) throws IOException {
        var headBlock = blockManager.allocateBlock();
        if(headBlock.isEmpty())
            throw new IOException("Couldn't create new file, not enough space.");

        var fileHead = new BlockFileHead(name, isDirectory, headBlock.get());
        if(isDirectory) {
            var block = blockManager.allocateBlock();
            if(block.isEmpty())
                throw new IOException("Couldn't create new file, not enough space.");

            fileHead.expand(block.get());
            fileHead.setByteCount(4); // One int for contents length
        }

        return fileHead;
    }

    private void serializeDirectory(@NotNull OFSTreeNode<BlockFileHead> dir) throws IOException {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var contentBuffer = serializeDirectoryChildrenList(dir);
        var bc = new BlockFileByteChannel(channel, dir.getFile(), blockManager);
        bc.write(contentBuffer);

        var headBuffer = dir.getFile().toByteBuffer();
        channel.position(dir.getFile().getAddress() * BLOCK_SIZE);
        channel.write(headBuffer);
    }

    private ByteBuffer serializeDirectoryChildrenList(OFSTreeNode<BlockFileHead> dir) {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var children = dir.getAllChildren();

        var buffer = ByteBuffer.allocate(
                4 + // Children.size()
                children.size() * 4 // Block address of each child
        );

        buffer.putInt(children.size());
        for(var c : children) {
            buffer.putInt(c.getFile().getAddress());
        }

        buffer.flip();

        return buffer;
    }

    private void deserializeDirectory(OFSTreeNode<BlockFileHead> dir) throws IOException {
        if(!dir.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var countBuffer = ByteBuffer.allocate(4);
        var bc = new BlockFileByteChannel(channel, dir.getFile(), blockManager);
        bc.read(countBuffer);
        countBuffer.flip();
        var childrenCount = countBuffer.getInt();

        var childrenBuffer = ByteBuffer.allocate(childrenCount * 4);
        bc.read(childrenBuffer); childrenBuffer.flip();
        bc.close();

        var headBuffer = ByteBuffer.allocate(BLOCK_SIZE);
        for(int i = 0; i < childrenCount; i++) {
            var childBlock = childrenBuffer.getInt();
            channel.position(childBlock * BLOCK_SIZE);
            channel.read(headBuffer); headBuffer.flip();

            var childHead = new BlockFileHead(headBuffer);

            dir.addChild(new OFSTreeNode<>(childHead));
            headBuffer.clear();
        }

        for(var childDir : dir.getChildDirectories()) {
            deserializeDirectory(childDir);
        }
    }

    private void updateParentDirectory(@NotNull Path child) throws IOException {
        var parent = fileTree.getParentNode(child);
        if(parent == null)
            throw new IllegalArgumentException();

        serializeDirectory(parent);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        ensureBaseFileIsOpen();

        var node = fileTree.getNode(path);
        if(node != null && node.isDirectory()) {
            throw new IllegalArgumentException("Can't create byte channel from directory");
        }

        if(node != null && options.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException(path.toString());
        }

        BlockFileHead head;
        if(node == null) {
            head = allocateHead(path.getFileName().toString(), false);
            if(!fileTree.addNode(path, head)) {
                throw new IllegalArgumentException();
            }

            channel.position(head.getAddress() * BLOCK_SIZE);
            channel.write(head.toByteBuffer());

            updateParentDirectory(path);
        } else {
            head = node.getFile();
        }

        SeekableByteChannel bc = new BlockFileByteChannel(channel, head, blockManager);
        if(options.contains(StandardOpenOption.APPEND)) {
            bc = bc.position(bc.size());
        }

        if(options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
            bc = bc.position(0);
        }

        return bc;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        ensureBaseFileIsOpen();

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
        ensureBaseFileIsOpen();

        return fileTree.exists(path);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        ensureBaseFileIsOpen();

        if(fileTree.exists(dir)) {
            throw new FileAlreadyExistsException(dir.toString());
        }

        var parent = fileTree.getParentNode(dir);
        if(parent == null)
            throw new NoSuchFileException(dir.toString());

        var head = allocateHead(dir.getFileName().toString(), true);
        if(!fileTree.addNode(dir, head)) {
            throw new IllegalArgumentException();
        }

        channel.position(head.getAddress() * BLOCK_SIZE);
        channel.write(head.toByteBuffer());
        updateParentDirectory(dir);
    }

    @Override
    public void delete(Path path) throws IOException {
        ensureBaseFileIsOpen();

        if(!fileTree.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }

        BlockFileHead h = fileTree.deleteNode(path);

        for(var block : h.getBlocks()) {
            blockManager.freeBlock(block);
        }

        updateParentDirectory(path);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        ensureBaseFileIsOpen();

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

        updateParentDirectory(target);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        ensureBaseFileIsOpen();

        if(!fileTree.exists(source)) {
            throw new NoSuchFileException(source.toString());
        }

        var newHeadBlock = blockManager.allocateBlock();
        if(newHeadBlock.isEmpty())
            throw new IOException("Couldn't create new file, not enough space.");
        BlockFileHead head = fileTree.deleteNode(source).copyWithName(target.getFileName().toString(), newHeadBlock.get());

        fileTree.addNode(target, head);

        updateParentDirectory(source);
        updateParentDirectory(target);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if(!isOpen())
            return null;

        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributeView.class)
            return (V) new BlockFileAttributeView(fileTree.getNode(path).getFile());

        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        ensureBaseFileIsOpen();

        if (type == null)
            throw new NullPointerException();

        if (type == BasicFileAttributes.class)
            return (A) new BlockFileAttributes(fileTree.getNode(path).getFile());

        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        ensureBaseFileIsOpen();

        return new BlockFileAttributes(fileTree.getNode(path).getFile()).toMap();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        ensureBaseFileIsOpen();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    void ensureBaseFileIsOpen() throws IOException {
        if(!channel.isOpen())
            throw new IOException("Base file channel is not open");
    }
}
