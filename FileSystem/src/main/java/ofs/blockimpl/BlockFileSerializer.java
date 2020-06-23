package ofs.blockimpl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;

public class BlockFileSerializer {
    private final BlockManager blockManager;
    private final SeekableByteChannel channel;

    public BlockFileSerializer(SeekableByteChannel channel, BlockManager blockManager) {
        this.blockManager = blockManager;
        this.channel = channel;
    }

    private void seekBlock(int block) throws IOException {
        channel.position(blockManager.getBlockSize() * block);
    }

    public void serializeFile(BlockFileHead fileHead) throws IOException {
        var nameBytes = fileHead.getName().getBytes();

        var serialized = ByteBuffer.allocate(
                nameBytes.length + 4 // name + it's length
                        + 4 // address
                        + 4 // byteCount
                        + 1 // isDirectory
                        + 4 // blocksSize
                        + fileHead.getBlocks().size() * 4 // actual blocks
        );

        serialized.putInt(nameBytes.length); serialized.put(nameBytes);
        serialized.putInt(fileHead.getAddress());
        serialized.putInt(fileHead.getByteCount());
        serialized.put((byte) (fileHead.isDirectory() ? 1 : 0));
        serialized.putInt(fileHead.getBlocks().size());
        for(var block : fileHead.getBlocks()) {
            serialized.putInt(block);
        }

        serialized.flip();

        seekBlock(fileHead.getAddress());
        channel.write(serialized);
    }

    public BlockFileHead deserializeFile(int block) throws IOException {
        seekBlock(block);

        var in = ByteBuffer.allocate(blockManager.getBlockSize());
        channel.read(in);
        in.flip();

        int nameLength = in.getInt();
        byte[] nameBytes = new byte[nameLength];

        in.get(nameBytes);
        var name = new String(nameBytes);
        var address = in.getInt();
        var byteCount = in.getInt();
        var isDirectory = in.get() > 0;

        var blockCount = in.getInt();
        var blocks = new ArrayList<Integer>(blockCount);
        for(int i = 0; i < blockCount; i++) {
            blocks.add(in.getInt());
        }

        return new BlockFileHead(name, address, byteCount, isDirectory, blocks);
    }
}
