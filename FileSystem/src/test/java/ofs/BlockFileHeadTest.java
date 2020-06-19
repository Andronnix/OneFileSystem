package ofs;

import ofs.blockimpl.BlockFileHead;
import org.junit.Assert;
import org.junit.Test;

public class BlockFileHeadTest {
    @Test
    public void canSerializeAndDeserialize() {
        BlockFileHead head = new BlockFileHead("test", false);

        for(int i = 99; i >= 0; i--) {
            head.expand(i);
        }

        head.setByteCount(99);

        var serialized = head.toByteBuffer(); serialized.flip();

        BlockFileHead deserialized = new BlockFileHead(serialized);

        Assert.assertFalse(deserialized.isDirectory);
        Assert.assertEquals(head.getByteCount(), deserialized.getByteCount());
        Assert.assertEquals(head.getBlocks().size(), deserialized.getBlocks().size());
        for(int i = 0; i < head.getBlocks().size(); i++) {
            Assert.assertEquals(head.getBlocks().get(i), deserialized.getBlocks().get(i));
        }
    }
}
