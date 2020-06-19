package ofs.blockimpl;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BlockFileHead {
    public static int BLOCK_SIZE = 1024;
    private final String name;
    private final ArrayList<Integer> blocks = new ArrayList<>();
    public final boolean isDirectory;

    private int byteCount = 0;

    public BlockFileHead(String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
    }

    public BlockFileHead(ByteBuffer in) {
        int nameLength = in.getInt();
        byte[] nameBytes = new byte[nameLength];

        in.get(nameBytes);
        this.name = new String(nameBytes);
        this.byteCount = in.getInt();
        this.isDirectory = in.get() > 0;

        var blockCount = in.getInt();
        for(int i = 0; i < blockCount; i++) {
            this.blocks.add(in.getInt());
        }
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
                + 4 // byteCount
                + 1 // isDirectory
                + 4 // blocksSize
                + blocks.size() * 4 // actual blocks
        );

        result.putInt(nameBytes.length); result.put(nameBytes);
        result.putInt(byteCount);
        result.put((byte) (isDirectory ? 1 : 0));
        result.putInt(blocks.size());
        for(var block : blocks) {
            result.putInt(block);
        }

        return result;
    }
}
