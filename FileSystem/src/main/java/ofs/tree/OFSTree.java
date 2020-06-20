package ofs.tree;

import ofs.controller.OFSFileHead;
import org.jetbrains.annotations.NotNull;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Path;

public class OFSTree <H extends OFSFileHead> {
    private final OFSTreeNode<H> root;

    public OFSTree(@NotNull H root) {
        this.root = new OFSTreeNode<>(root);
    }

    /**
     * Returns a node, corresponding to the given path, null otherwise.
     */
    public OFSTreeNode<H> getNode(@NotNull Path path) {
        if(path.getNameCount() == 0)
            return root;

        var current = root;
        var pathPos = 0;
        while(pathPos < path.getNameCount()) {
            var nextName = path.getName(pathPos).toString();

            if(!current.isDirectory()) {
                return null;
            }

            var found = false;
            for(var child : current.getAllChildren()) {
                if(child.getFile().getName().equals(nextName)) {
                    found = true;
                    current = child;
                    break;
                }
            }

            if(!found)
                return null;

            pathPos++;
        }

        return current;
    }

    public boolean exists(@NotNull Path path) {
        return getNode(path) != null;
    }

    /**
     * Returns a parent of a node, corresponding to the given path, null otherwise.
     */
    public OFSTreeNode<H> getParentNode(@NotNull Path path) {
        if(path.getNameCount() == 0)
            return null;

        var current = root;
        var pathPos = 0;
        var depth = path.getNameCount() - 1;
        while(pathPos < depth) {
            var nextName = path.getName(pathPos).toString();

            if(!current.isDirectory()) {
                return null;
            }

            var found = false;
            for(var child : current.getAllChildren()) {
                if(child.getFile().getName().equals(nextName)) {
                    found = true;
                    current = child;
                    break;
                }
            }

            if(!found)
                return null;

            pathPos++;
        }

        return current;
    }

    /**
     * Deletes a node, corresponding to the given path.
     * @throws IllegalArgumentException if node doesn't exist or given path is a root path.
     * @throws DirectoryNotEmptyException if node is a directory and has children.
     * @return FileHead, corresponding to the deleted file
     */
    public H deleteNode(@NotNull Path path) throws DirectoryNotEmptyException {
        if (path.getNameCount() == 0)
            throw new IllegalArgumentException();

        var parent = getParentNode(path);
        if (parent == null || !parent.isDirectory()) {
            throw new IllegalArgumentException();
        }

        var fileName = path.getFileName().toString();
        for (var child : parent.getAllChildren()) {
            if (child.getFile().getName().equals(fileName)) {
                if(child.isDirectory() && child.getAllChildren().size() > 0)
                    throw new DirectoryNotEmptyException(path.toString());

                parent.getAllChildren().remove(child);
                return child.getFile();
            }
        }

        throw new IllegalStateException("File tree is broken.");
    }

    /**
     * Creates a new node, corresponding to the given path.
     * @param path location of the new file.
     * @param file file header object.
     * @throws IllegalArgumentException when attempting to recreate root.
     * @return true if file was created, false otherwise.
     */
    public boolean addNode(@NotNull Path path, @NotNull H file) {
        if(path.getNameCount() == 0)
            throw new IllegalArgumentException("Can't create root again");

        var parent = getParentNode(path);
        if(parent == null)
            return false;

        return parent.addChild(new OFSTreeNode<>(file));
    }
}
