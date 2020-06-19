package ofs.controller;


import java.util.Optional;

public class FileBlockManager {
    private int lastBlock = 0;

    public void freeBlock(int index) {
        //
    }

    /**
     * Request for a new block.
     * @return Address of the allocated block if possible or empty optional if there is not enough space.
     */
    public Optional<Integer> allocateBlock() {
        return Optional.of(lastBlock++);
    }
}
