package ofs;

import ofs.blockimpl.BlockFileHead;
import ofs.blockimpl.BlockFileSerializer;
import ofs.blockimpl.BlockManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class BlockFileSerializerTest {
    @Test
    public void canSerializeAndDeserialize() throws IOException {
        var temp = Files.createTempFile("test_serialize", null);
        var bc = Files.newByteChannel(temp, StandardOpenOption.READ, StandardOpenOption.WRITE);
        var bm = new BlockManager(1024, 4096);

        var serializer = new BlockFileSerializer(bc, bm);
        BlockFileHead head = new BlockFileHead("test", false, 0);
        for(int i = 99; i >= 0; i--) {
            head.expand(i);
        }
        head.setByteCount(99);

        serializer.serializeFileHead(head);
        var deserialized = serializer.deserializeFileHead(0);

        Assert.assertEquals(head, deserialized);
    }
}
