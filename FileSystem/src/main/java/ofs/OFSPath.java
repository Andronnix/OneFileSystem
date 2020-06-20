package ofs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

public class OFSPath implements Path {
    private final List<String> path;
    private final OFSFileSystem fs;
    private final boolean isAbsolute;

    /**
     * Constructs OFSPath from list of names. Doesn't check for path validity!
     * @param pathNames List, containing valid names.
     * @param fs OFSFileSystem instance this path is attached to.
     * @param isAbsolute Indicates whether this path is absolute or relative.
     */
    OFSPath(@NotNull List<String> pathNames, @NotNull OFSFileSystem fs, boolean isAbsolute) {
        this.path = pathNames;
        this.fs = fs;
        this.isAbsolute = isAbsolute;
    }

    /**
     * Constructs OFSPath from it's string representation. Doesn't check for path validity!
     * @param representation Path represented as as String, e.g. obtained by uri.getSchemeSpecificPart().
     * @param fs OFSFileSystem instance this path is attached to.
     */
    OFSPath(@NotNull String representation, @NotNull OFSFileSystem fs) {
        StringTokenizer st = new StringTokenizer(representation, OFSFileSystem.SEPARATOR);
        List<String> pathNames = new ArrayList<>();

        while(st.hasMoreTokens()) {
            var token = st.nextToken();
            pathNames.add(token);
        }

        if(pathNames.size() > 0 && pathNames.get(0).equals(OFSFileSystemProvider.ROOT)) {
            pathNames = pathNames.subList(1, pathNames.size());
            this.isAbsolute = true;
        } else {
            this.isAbsolute = false;
        }

        this.path = pathNames;
        this.fs = fs;
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
        if(!isAbsolute() || path.size() == 0)
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
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Path normalize() {
        return this;
    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        if(other.isAbsolute())
            return other;

        if(other.getNameCount() == 0)
            return this;

        var resultList = new ArrayList<>(this.path);
        for(int i = 0; i < other.getNameCount(); i++) {
            resultList.add(other.getName(i).toString());
        }

        return new OFSPath(resultList, fs, isAbsolute);
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        if(!(other instanceof OFSPath) || !other.startsWith(this))
            throw new IllegalArgumentException();

        var ofs = (OFSPath) other;

        return new OFSPath(ofs.path.subList(path.size(), ofs.path.size()), fs, false);
    }

    @NotNull
    @Override
    public URI toUri() {
        if(!isAbsolute())
            return this.toAbsolutePath().toUri();

        String uri = OFSFileSystemProvider.SCHEME + ":" + OFSFileSystemProvider.ROOT
                + String.join(OFSFileSystem.SEPARATOR, path);

        return URI.create(uri);
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
