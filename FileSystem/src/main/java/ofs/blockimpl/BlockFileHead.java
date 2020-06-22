package ofs.blockimpl;

import ofs.OFSPath;
import ofs.controller.OFSFileHead;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BlockFileHead implements OFSFileHead {
    private final int address;
    private final String name;
    private final ArrayList<Integer> blocks = new ArrayList<>();
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

    public BlockFileHead(String name, boolean isDirectory, int address) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.address = address;
    }

    public BlockFileHead(ByteBuffer in) {
        int nameLength = in.getInt();
        byte[] nameBytes = new byte[nameLength];

        in.get(nameBytes);
        this.name = new String(nameBytes);
        this.address = in.getInt();
        this.byteCount = in.getInt();
        this.isDirectory = in.get() > 0;

        var blockCount = in.getInt();
        for(int i = 0; i < blockCount; i++) {
            this.blocks.add(in.getInt());
        }
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

    public ByteBuffer toByteBuffer() {
        var nameBytes = name.getBytes();

        var result = ByteBuffer.allocate(
                nameBytes.length + 4 // name + it's length
                + 4 // address
                + 4 // byteCount
                + 1 // isDirectory
                + 4 // blocksSize
                + blocks.size() * 4 // actual blocks
        );

        result.putInt(nameBytes.length); result.put(nameBytes);
        result.putInt(address);
        result.putInt(byteCount);
        result.put((byte) (isDirectory ? 1 : 0));
        result.putInt(blocks.size());
        for(var block : blocks) {
            result.putInt(block);
        }

        result.flip();

        return result;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }
}
