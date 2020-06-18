package ofs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
        Assert.assertEquals(path, path.getRoot());
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

    @Test(expected = IllegalArgumentException.class)
    public void constructsChildRelativePaths() {
        var path1 = Path.of(URI.create("ofs:]=$a$b$c"));
        var path2 = Path.of(URI.create("ofs:]=$a$b$c$d$e"));

        var relative = path2.relativize(path1);

        Assert.assertFalse(relative.isAbsolute());
        Assert.assertEquals("d", relative.getName(0).toString());
        Assert.assertEquals("e", relative.getName(1).toString());
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

        Assert.assertEquals("b", subpath.getName(0).toString());
        Assert.assertEquals("c", subpath.getName(1).toString());
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
        Assert.assertEquals("b", abs.getName(0).toString());
        Assert.assertEquals("c", abs.getName(1).toString());
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
}
