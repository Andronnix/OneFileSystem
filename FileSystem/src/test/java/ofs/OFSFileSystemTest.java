package ofs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Map;

public class OFSFileSystemTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        FileSystems.newFileSystem(URI.create("ofs:]=$"), Map.of());
    }

    @Test
    public void canBeClosed() throws IOException {
        var provider = new OFSFileSystemProvider();
        var fs = provider.newFileSystem(URI.create("ofs:]=$"), Map.of());

        Assert.assertTrue(fs.isOpen());
        fs.close();

        Assert.assertFalse(fs.isOpen());
    }

    @Test
    public void constructsPathFromSingleString() {
        var fs = FileSystems.getFileSystem(URI.create("ofs:]=$"));

        var path = fs.getPath("]=$a$b");
        var expected = new OFSPath(List.of("a", "b"), (OFSFileSystem) fs, true);

        Assert.assertEquals(expected, path);
    }

    @Test
    public void constructsPathFromMultipleStrings() {
        var fs = FileSystems.getFileSystem(URI.create("ofs:]=$"));

        var path = fs.getPath("]=$a$b", "c", "d$e");
        var expected = new OFSPath(List.of("a", "b", "c", "d", "e"), (OFSFileSystem) fs, true);

        Assert.assertEquals(expected, path);
    }

    @Test
    public void constructsRelativePath() {
        var fs = FileSystems.getFileSystem(URI.create("ofs:]=$"));

        var path = fs.getPath("a$b", "c", "d$e");
        var expected = new OFSPath(List.of("a", "b", "c", "d", "e"), (OFSFileSystem) fs, false);

        Assert.assertEquals(expected, path);
    }

    @Test(expected = InvalidPathException.class)
    public void doesntConstructWrongPath() {
        var fs = FileSystems.getFileSystem(URI.create("ofs:]=$"));

        fs.getPath("a$$b");
    }

    @Test
    public void thereIsOnlyOneRoot() {
        var fs = FileSystems.getFileSystem(URI.create("ofs:]=$"));

        var rootDirs = fs.getRootDirectories().iterator();
        Assert.assertTrue(rootDirs.hasNext());

        var root = rootDirs.next();
        Assert.assertFalse(rootDirs.hasNext());

        Assert.assertNull(root.getRoot());
    }
}
