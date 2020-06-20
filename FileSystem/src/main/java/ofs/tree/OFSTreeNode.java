package ofs.tree;

import ofs.controller.OFSFileHead;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OFSTreeNode <H extends OFSFileHead> {
    private final H file;
    private final ArrayList<OFSTreeNode<H>> children;

    public OFSTreeNode(@NotNull H file) {
        this.file = file;
        this.children = file.isDirectory() ? new ArrayList<>() : null;
    }

    public boolean isDirectory() {
        return children != null;
    }

    public H getFile() {
        return file;
    }

    @NotNull
    public List<OFSTreeNode<H>> getAllChildren() {
        if(children == null)
            throw new IllegalArgumentException("Can't get children of a plain file");

        return children;
    }

    @NotNull
    public List<OFSTreeNode<H>> getChildDirectories() {
        if(children == null)
            throw new IllegalArgumentException("Can't get children of a plain file");

        return children.stream().filter(OFSTreeNode::isDirectory).collect(Collectors.toList());
    }

    public boolean addChild(@NotNull OFSTreeNode<H> newChild) {
        if(children == null)
            throw new IllegalArgumentException("Can't add children to a plain file");

        for(var c : children) {
            if(c.getFile().getName().equals(newChild.file.getName())) {
                return false;
            }
        }

        children.add(newChild);
        return true;
    }
}
