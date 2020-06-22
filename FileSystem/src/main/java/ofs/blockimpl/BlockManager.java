package ofs.blockimpl;


import java.util.ArrayList;
import java.util.Optional;

public class BlockManager {
    private final int maxBlocks;
    private final int blockSize;
    private final boolean[] occupied;

    private int occupiedCount = 0;
    private int lastBlock = 0;

    public BlockManager(int blockSize, int maxBytes) {
        this.blockSize = blockSize;
        this.maxBlocks = maxBytes / blockSize;
        this.occupied = new boolean[maxBlocks];
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void freeBlock(int index) {
        if(occupied[index]) {
            occupiedCount--;
            occupied[index] = false;
        }
    }

    private Integer allocateNextBlock() {
        while(occupied[lastBlock]) {
            lastBlock = (lastBlock + 1) % maxBlocks;
        }

        occupied[lastBlock] = true;
        occupiedCount++;
        return lastBlock;
    }

    /**
     * Request for a new block.
     * @return Address of the allocated block if possible or empty optional if there is not enough space.
     */
    public Optional<Integer> allocateBlock() {
        if(occupiedCount == maxBlocks) {
            return Optional.empty();
        }

        return Optional.of(allocateNextBlock());
    }

    /**
     * Request for a given number of new blocks.
     * @return List of addresses of the allocated blocks if possible or empty optional if there is not enough space.
     */
    public Optional<ArrayList<Integer>> allocateBlocks(int number) {
        if(occupiedCount + number > maxBlocks) {
            return Optional.empty();
        }

        var result = new ArrayList<Integer>();
        for(int i = 0; i < number; i++) {
            result.add(allocateNextBlock());
        }

        return Optional.of(result);
    }
}
