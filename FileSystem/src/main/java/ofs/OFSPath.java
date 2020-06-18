package ofs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;

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
        if(!isAbsolute() && path.size() <= 1)
            return null;

        if(isAbsolute() && path.size() == 0)
            return null;

        return new OFSPath(path.subList(0, path.size() - 1), fs, isAbsolute);
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

        if(endIndex <= beginIndex || endIndex > path.size())
            throw new IllegalArgumentException("Wrong endIndex");

        return new OFSPath(path.subList(beginIndex, endIndex), fs, false);
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        if(other.getFileSystem() != fs || !(other instanceof OFSPath))
            return false;

        if(other.isAbsolute() && !this.isAbsolute)
            return false;

        var ofsOther = (OFSPath) other;

        if(ofsOther.getNameCount() > path.size())
            return false;

        for(int i = 0; i < other.getNameCount(); i++) {
            if(!ofsOther.path.get(i).equals(path.get(i)))
                return false;
        }

        return true;
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
        if(isAbsolute()) return this;

        return new OFSPath(path, fs, true);
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

    @Override
    public String toString() {
        var p = String.join(OFSFileSystem.SEPARATOR, path);
        if(isAbsolute())
            p = OFSFileSystemProvider.ROOT + "$" + p;

        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OFSPath path = (OFSPath) o;
        if(isAbsolute != path.isAbsolute) return false;
        if(!Objects.equals(fs, path.fs)) return false;
        if(path.getNameCount() != getNameCount()) return false;

        for(int i = 0; i < getNameCount(); i++) {
            if(!this.path.get(i).equals(path.path.get(i)))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path.size(), fs, isAbsolute);
    }
}
