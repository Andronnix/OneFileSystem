package ofs.controller;

import java.util.ArrayList;

public class FileHead {
    public static int BLOCK_SIZE = 1024;
    private final ArrayList<Integer> blocks = new ArrayList<>();
    public final boolean isDirectory;

    private int byteCount = 0;

    FileHead(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public int getByteCount() {
        return byteCount;
    }

    void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    public ArrayList<Integer> getBlocks() {
        return blocks;
    }

    public void expand(Integer newBlock) {
        blocks.add(newBlock);
    }
}
