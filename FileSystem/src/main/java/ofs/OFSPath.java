package ofs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;

public class OFSPath implements Path {
    private final List<String> path;
    private final OFSFileSystem fs;
    private final boolean isAbsolute;

    OFSPath(@NotNull List<String> path, @NotNull OFSFileSystem fs, boolean isAbsolute) {
        this.path = path;
        this.fs = fs;
        this.isAbsolute = isAbsolute;
    }

    @NotNull
    @Override
    public FileSystem getFileSystem() {
        return this.fs;
    }

    @Override
    public boolean isAbsolute() {
        return isAbsolute;
    }

    @Override
    public Path getRoot() {
        if(!isAbsolute())
            return null;

        return new OFSPath(List.of(), fs, true);
    }

    @Override
    public Path getFileName() {
        if(path.size() == 0)
            return null;

        return new OFSPath(List.of(path.get(path.size() - 1)), fs, false);
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return path.size();
    }

    @NotNull
    @Override
    public Path getName(int index) {
        if(index < 0 || index >= path.size())
            throw new IllegalArgumentException();

        return new OFSPath(List.of(path.get(index)), fs, false);
    }

    @NotNull
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        if(beginIndex < 0 || beginIndex >= path.size())
            throw new IllegalArgumentException("Wrong beginIndex");

        if(endIndex <= beginIndex || endIndex >= path.size())
            throw new IllegalArgumentException("Wrong endIndex");

        return new OFSPath(path.subList(beginIndex, endIndex), fs, false);
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        if(other.getFileSystem() != fs)
            return false;

        if(other.getNameCount() > path.size())
            return false;
        for(int i = 0; i < other.getNameCount(); i++) {

        }
        return false;
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        if(other.isAbsolute())
            return other;

        if(other.getNameCount() == 0)
            return this;

        return null;
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        return null;
    }

    @NotNull
    @Override
    public URI toUri() {
        return null;
    }

    @NotNull
    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @NotNull
    @Override
    public Path toRealPath(@NotNull LinkOption... options) throws IOException {
        return this.toAbsolutePath();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }
}
