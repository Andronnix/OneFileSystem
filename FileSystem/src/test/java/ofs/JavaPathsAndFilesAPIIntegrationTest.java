package ofs;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JavaPathsAndFilesAPIIntegrationTest {
    @BeforeClass
    public static void beforeClass() throws IOException {
        FileSystems.newFileSystem(URI.create("ofs:]=$"), Map.of());
    }

    @Test
    public void getsRootPath() {
        var p = Paths.get(URI.create("ofs:]=$"));

        Assert.assertTrue(p.isAbsolute());
        Assert.assertNull(p.getRoot());
    }

    @Test
    public void createsDirectory() throws IOException {
        var dir = Files.createDirectory(Paths.get(URI.create("ofs:]=$createsDirectory")));

        Assert.assertTrue(Files.exists(dir));
        Assert.assertTrue(Files.isDirectory(dir));
    }

    @Test
    public void createsDirectories() throws IOException {
        var dir = Files.createDirectories(Paths.get(URI.create("ofs:]=$createsDirectories$b$c$d")));
        Assert.assertTrue(Files.exists(dir));
        Assert.assertTrue(Files.isDirectory(dir));
    }

    @Test
    public void createsEmptyFile() throws IOException {
        var file = Files.createFile(Paths.get(URI.create("ofs:]=$empty_file")));

        Assert.assertTrue(Files.exists(file));
        Assert.assertFalse(Files.isDirectory(file));
        Assert.assertEquals(0, Files.size(file));
    }

    @Test
    public void writesToFileAndReadsFromIt() throws IOException {
        var str = "Hello, world!";
        var file = Files.writeString(Paths.get(URI.create("ofs:]=$read_write")), str, StandardOpenOption.CREATE);

        Assert.assertEquals(str, Files.readString(file));
    }

    @Test
    public void deletesFile() throws IOException {
        var file = Files.createFile(Paths.get(URI.create("ofs:]=$file_to_delete")));
        Files.delete(file);

        Assert.assertFalse(Files.exists(file));
    }

    @Test
    public void deletesIfExistsFile() throws IOException {
        var file = Files.createFile(Paths.get(URI.create("ofs:]=$file_to_delete_if_exists")));
        Assert.assertTrue(Files.deleteIfExists(file));

        Assert.assertFalse(Files.exists(file));
    }

    @Test
    public void dontDeleteIfDoesNotExistFile() throws IOException {
        var file = Paths.get(URI.create("ofs:]=$file_that_was_newer_created"));
        Assert.assertFalse(Files.deleteIfExists(file));
        Assert.assertFalse(Files.exists(file));
    }

    @Test
    public void copiesFile() throws IOException {
        var src = Paths.get(URI.create("ofs:]=$src_file_to_copy"));
        var dest = Paths.get(URI.create("ofs:]=$dest_file_to_copy"));
        var content = "File contents";

        Files.createFile(src);
        Files.writeString(src, content);
        Files.copy(src, dest);

        Assert.assertTrue(Files.exists(src));
        Assert.assertTrue(Files.exists(dest));
        Assert.assertEquals(content, Files.readString(src));
        Assert.assertEquals(content, Files.readString(dest));
    }

    @Test
    public void copiesRealFileFromAnotherFs() throws IOException, URISyntaxException {
        var f = JavaPathsAndFilesAPIIntegrationTest.class.getResource("/jetbrains");
        var src = Path.of(f.toURI());
        var dest = Paths.get(URI.create("ofs:]=$logo.png"));

        Files.copy(src, dest);

        Assert.assertTrue(Files.exists(src));
        Assert.assertTrue(Files.exists(dest));
        Assert.assertEquals(Files.size(src), Files.size(dest));
    }

    @Test
    public void walksFileTree() throws IOException {
        var base = Paths.get(URI.create("ofs:]=$walk_file_tree$base$base1"));
        var dir1 = Files.createDirectories(base.resolve(Path.of("c", "dir1")));
        var dir2 = Files.createDirectories(base.resolve(Path.of("dir2")));

        Assert.assertTrue(Files.exists(dir1));
        Assert.assertTrue(Files.exists(dir2));

        var visitOrder = List.of("walk_file_tree", "base", "base1", "c", "dir1", "dir2");
        var realVisits = Files
                .walk(Paths.get(URI.create("ofs:]=$walk_file_tree")))
                .map(Path::getFileName)
                .filter(Objects::nonNull)
                .map(Path::toString)
                .collect(Collectors.toList());

        Assert.assertEquals(visitOrder.size(), realVisits.size());
    }
}
