package ofs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class OFSPathTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        FileSystems.newFileSystem(URI.create("ofs:]=$"), Map.of());
    }

    @Test
    public void rootPathBehavesProperly() {
        var path = Path.of(URI.create("ofs:]=$"));

        Assert.assertTrue(path.isAbsolute());
        Assert.assertNull(path.getRoot());
        Assert.assertEquals(0, path.getNameCount());
    }

    @Test
    public void lengthIsCorrect() {
        String uri = "ofs:]=$";

        for(int i = 0; i < 255; i++) {
            var path = Path.of(URI.create(uri));
            Assert.assertEquals(i, path.getNameCount());
            uri = uri + "$" + i;
        }
    }

    @Test
    public void trailingSeparatorDoesntChangeLength() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$c$d"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$"));

        Assert.assertTrue(path1.getNameCount() == path2.getNameCount());
    }

    @Test
    public void constructsSubpath() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        var expected = new OFSPath(List.of("b", "c", "d"), (OFSFileSystem) path.getFileSystem(), false);

        Assert.assertEquals(expected, path.subpath(1, 4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesntConstructsTooLongSubpath() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        path.subpath(1, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesntAcceptWrongRightSubpathBound() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        path.subpath(2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesntAcceptNegativesSubpathBound() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        path.subpath(-1, 1);
    }

    @Test
    public void pathPartsAreProperlyParsed() {
        StringBuilder uri = new StringBuilder("ofs:]=$");

        for(int i = 0; i < 255; i++) {
            uri.append("$");
            uri.append(i);
        }

        var path = Path.of(URI.create(uri.toString()));

        for(int i = 0; i < 255; i++) {
            Assert.assertEquals(Integer.toString(i), path.getName(i).toString());
        }
    }

    @Test
    public void constructsChildRelativePaths() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$c"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        var relative = path1.relativize(path2);

        Assert.assertFalse(relative.isAbsolute());
        Assert.assertEquals("d$e", relative.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesntConstructNonChildRelativePaths() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$c"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$Z$d$e"));

        path2.relativize(path1);
    }

    @Test
    public void checksForEmptyPrefix() {
        var path1 = Path.of(URI.create("ofs:]=$"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        Assert.assertTrue(path2.startsWith(path1));
    }

    @Test
    public void checksForPrefix() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$c"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        Assert.assertTrue(path2.startsWith(path1));
    }

    @Test
    public void checksForWrongPrefix() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$z"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        Assert.assertFalse(path2.startsWith(path1));
    }

    @Test
    public void parentIsPrefix() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        Assert.assertTrue(path.startsWith(path.getParent()));
    }

    @Test
    public void checksRelativeForPrefix() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d$e"));
        var subpath = path.subpath(1, 4);

        var prefix = new OFSPath(List.of("b", "c"), (OFSFileSystem) path.getFileSystem(), false);

        Assert.assertTrue(subpath.startsWith(prefix));
    }

    @Test
    public void checksRelativeDoesntHaveAbsolutePrefix() {
        var path = Path.of(URI.create("ofs:]=$a$b"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        var subpath = path2.subpath(0, 3);

        Assert.assertFalse(subpath.startsWith(path));
    }

    @Test
    public void checksRelativeForWrongPrefix() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$z")).subpath(1, 3);
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e")).subpath(1, 4);

        Assert.assertFalse(path2.startsWith(path1));
    }

    @Test
    public void parentOfRelativeIsPrefix() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d$e")).subpath(1, 5);

        Assert.assertTrue(path.startsWith(path.getParent()));
    }

    @Test
    public void returnsProperFileSystem() {
        var uri = URI.create("ofs:]=$a$b$c");
        var path = Path.of(uri);
        var fs = FileSystems.getFileSystem(uri);

        Assert.assertEquals(fs, path.getFileSystem());
    }

    @Test
    public void returnsCorrectFileName() {
        var path = Path.of(URI.create("ofs:]=$a$b$c"));

        Assert.assertEquals("c", path.getFileName().toString());
    }

    @Test
    public void returnsCorrectFileNameWhenEmpty() {
        var path = Path.of(URI.create("ofs:]=$"));

        Assert.assertNull(path.getFileName());
    }

    @Test
    public void returnsCorrectRoot() {
        var path = Path.of(URI.create("ofs:]=$a$b$"));
        var root = Path.of(URI.create("ofs:]=$"));

        Assert.assertEquals(root, path.getRoot());
    }

    @Test
    public void returnsCorrectRootWhenNoRoot() {
        var path = Path.of(URI.create("ofs:]=$a$b$"));
        var subpath = path.subpath(1, 2);

        Assert.assertNull(subpath.getRoot());
    }

    @Test
    public void returnsCorrectSubpath() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));
        var subpath = path.subpath(1, 3);

        Assert.assertEquals("b$c", subpath.toString());
    }

    @Test
    public void absolutePathStaysTheSame() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        Assert.assertEquals(path, path.toAbsolutePath());
    }

    @Test
    public void relativePathConvertsToAbsolute() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));
        var subpath = path.subpath(1, 3);
        var abs = subpath.toAbsolutePath();

        Assert.assertTrue(abs.isAbsolute());
        Assert.assertEquals("]=$b$c", abs.toString());
    }

    @Test
    public void subPathIsRelative() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));
        var subpath = path.subpath(1, 3);

        Assert.assertFalse(subpath.isAbsolute());
    }

    @Test
    public void returnsCorrectStringForAbsolutePath() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d"));

        Assert.assertEquals("]=$a$b$c$d", path.toString());
    }

    @Test
    public void returnsCorrectStringForEmptyPath() {
        var path = Path.of(URI.create("ofs:]=$"));

        Assert.assertEquals("]=$", path.toString());
    }

    @Test
    public void returnsCorrectStringForSubPath() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d$e"));
        var subpath = path.subpath(2, 4);

        Assert.assertEquals("c$d", subpath.toString());
    }

    @Test
    public void constructsParent() {
        var path = Path.of(URI.create("ofs:]=$a$b$c$d$e"));
        var expected = Path.of(URI.create("ofs:]=$a$b$c$d"));

        Assert.assertEquals(expected, path.getParent());
    }

    @Test
    public void constructsNullParentOfEmptyPath() {
        var path = Path.of(URI.create("ofs:]=$"));

        Assert.assertNull(path.getParent());
    }

    @Test
    public void constructsNullParentOfEmptyRelativePath() {
        var path = new OFSPath(List.of(), (OFSFileSystem) FileSystems.getFileSystem(URI.create("ofs:]=$")), false);

        Assert.assertNull(path.getParent());
    }

    @Test(expected = InvalidPathException.class)
    public void doesntConstructTooLongPath() {
        var path = Path.of(URI.create("ofs:]=$" + "a".repeat(10000)));
    }
}
