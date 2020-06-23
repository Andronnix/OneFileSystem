package ofs.blockimpl;

import ofs.OFSPath;
import ofs.controller.OFSFileHead;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlockFileHead implements OFSFileHead {
    private final int address;
    private final String name;
    private final ArrayList<Integer> blocks;
    private final boolean isDirectory;

    private int byteCount = 0;

    public BlockFileHead copyWithName(@NotNull String newName, int newAddress) {
        var result = new BlockFileHead(newName, isDirectory, newAddress);
        for(var b : blocks) {
            result.expand(b);
        }

        return result;
    }

    public static int getMaxContentBlockCount(int blockSize) {
        var spaceAvailableForBlocksList = blockSize;
        spaceAvailableForBlocksList -= 4; // name size;
        spaceAvailableForBlocksList -= OFSPath.MAX_NAME_LENGTH; // name length;
        spaceAvailableForBlocksList -= 4; // self block address;
        spaceAvailableForBlocksList -= 4; // self content byte count;
        spaceAvailableForBlocksList -= 1; // is directory;
        spaceAvailableForBlocksList -= 4; // blocks count;

        return spaceAvailableForBlocksList / 4;
    }

    BlockFileHead(String name, int address, int byteCount, boolean isDirectory, ArrayList<Integer> blocks) {
        this.name = name;
        this.address = address;
        this.byteCount = byteCount;
        this.isDirectory = isDirectory;
        this.blocks = blocks;
    }

    public BlockFileHead(String name, boolean isDirectory, int address) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.address = address;
        this.blocks = new ArrayList<>();
    }

    public int getAddress() {
        return address;
    }

    public int getByteCount() {
        return byteCount;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    public ArrayList<Integer> getBlocks() {
        return blocks;
    }

    public void expand(Integer newBlock) {
        blocks.add(newBlock);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockFileHead that = (BlockFileHead) o;
        return address == that.address &&
                isDirectory == that.isDirectory &&
                byteCount == that.byteCount &&
                name.equals(that.name) &&
                Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, name, blocks, isDirectory, byteCount);
    }
}
