package ofs.blockimpl;

import ofs.controller.OFSFileHead;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class BlockFileHead implements OFSFileHead {
    private final ArrayList<Integer> address;
    private final String name;
    private final ArrayList<Integer> blocks;
    private final boolean isDirectory;

    private int byteCount = 0;

    public BlockFileHead copyWithName(@NotNull String newName, int newAddress) {
        var result = new BlockFileHead(newName, isDirectory, newAddress);
        for(var b : blocks) {
            result.expand(b);
        }

        result.byteCount = byteCount;

        return result;
    }

    BlockFileHead(@NotNull String name, @NotNull ArrayList<Integer> address, int byteCount, boolean isDirectory, @NotNull ArrayList<Integer> blocks) {
        if(address.size() == 0)
            throw new IllegalArgumentException();

        this.name = name;
        this.address = address;
        this.byteCount = byteCount;
        this.isDirectory = isDirectory;
        this.blocks = blocks;
    }

    public BlockFileHead(@NotNull String name, boolean isDirectory, int address) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.address = new ArrayList<>(); this.address.add(address);
        this.blocks = new ArrayList<>();
    }

    public ArrayList<Integer> getFullAddress() {
        return address;
    }

    public int getAddress(int index) {
        return address.get(index);
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
        return  isDirectory == that.isDirectory &&
                byteCount == that.byteCount &&
                name.equals(that.name) &&
                Objects.equals(address, that.address) &&
                Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, name, blocks, isDirectory, byteCount);
    }
}
