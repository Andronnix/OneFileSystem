package ofs;

import ofs.blockimpl.BlockManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class BlockManagerTest {
    @Test
    public void allowsToAllocateMaxBlocksInBulk() {
        var maxBlocks = 10;
        var mgr = new BlockManager(maxBlocks);

        var blocks = mgr.allocateBlocks(maxBlocks);
        Assert.assertTrue(blocks.isPresent());
        Assert.assertEquals(maxBlocks, blocks.get().size());
        Assert.assertEquals(maxBlocks, Set.copyOf(blocks.get()).size());
        Assert.assertTrue(mgr.allocateBlock().isEmpty());
    }

    @Test
    public void allowsToAllocateMaxBlocksOneByOne() {
        var maxBlocks = 10;
        var mgr = new BlockManager(maxBlocks);

        var allocated = new HashSet<Integer>();
        for(int i = 0; i < maxBlocks; i++) {
            var b = mgr.allocateBlock();
            Assert.assertTrue(b.isPresent());
            allocated.add(b.get());
        }
        Assert.assertTrue(mgr.allocateBlock().isEmpty());

        Assert.assertEquals(maxBlocks, allocated.size());
    }

    @Test
    public void allowsToAllocateChessBoard() {
        var maxBlocks = 10;
        var mgr = new BlockManager(maxBlocks);

        var blocksOptional = mgr.allocateBlocks(maxBlocks);
        Assert.assertTrue(blocksOptional.isPresent());
        Assert.assertTrue(mgr.allocateBlock().isEmpty());

        var blocks = blocksOptional.get();

        var blocksToRemove = new HashSet<Integer>();
        int freed = 0;
        for(int i = 0; i < blocks.size(); i += 2) {
            var toRemove = blocks.get(i);
            mgr.freeBlock(toRemove);
            blocksToRemove.add(toRemove);
            freed++;
        }
        blocks.removeAll(blocksToRemove);

        var blocksOptional2 = mgr.allocateBlocks(freed);
        Assert.assertTrue(blocksOptional2.isPresent());

        var blocks2 = blocksOptional2.get();
        Assert.assertEquals(freed, blocks2.size());

        for(var b2 : blocks2) {
            Assert.assertFalse(blocks.contains(b2));
        }
    }
}
