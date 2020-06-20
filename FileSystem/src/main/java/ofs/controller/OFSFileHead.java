package ofs.controller;

import java.nio.channels.SeekableByteChannel;

public interface OFSFileHead {
    String getName();
    boolean isDirectory();
}
