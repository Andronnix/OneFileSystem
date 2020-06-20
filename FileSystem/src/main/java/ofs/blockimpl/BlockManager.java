package ofs.blockimpl;


import java.util.ArrayList;
import java.util.Optional;

public class BlockManager {
    private int afterLastBlock = 0;

    public void freeBlock(int index) {
        //
    }

    /**
     * Request for a new block.
     * @return Address of the allocated block if possible or empty optional if there is not enough space.
     */
    public Optional<Integer> allocateBlock() {
        return Optional.of(afterLastBlock++);
    }

    /**
     * Request for a given number of new blocks.
     * @return List of addresses of the allocated blocks if possible or empty optional if there is not enough space.
     */
    public Optional<ArrayList<Integer>> allocateBlocks(int number) {
        var result = new ArrayList<Integer>();
        for(int i = 0; i < number; i++) {
            result.add(afterLastBlock++);
        }

        return Optional.of(result);
    }
}
