package ofs;

import ofs.controller.TempFileController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class TempFileControllerTest {
    private TempFileController controller;

    @Before
    public void createController() throws IOException {
        controller = new TempFileController();
    }

    @Test
    public void nonExistingFilesDontExist() throws IOException {
        Assert.assertFalse(controller.exists(Path.of("does_not_exist")));
        Assert.assertFalse(controller.exists(Path.of("does_not_exist", "also_does_not_exist")));
    }

    @Test(expected = NoSuchFileException.class)
    public void deletingNonExistingFileCausesException() throws IOException {
        controller.delete(Path.of("does_not_exist"));
    }

    @Test
    public void createsNewFile() throws IOException {
        var path = Path.of("plain_file");
        controller.newByteChannel(path, Set.of(StandardOpenOption.CREATE));
        Assert.assertTrue(controller.exists(path));

        var attrs = controller.readAttributes(path, BasicFileAttributes.class);
        Assert.assertFalse(attrs.isDirectory());
    }

    @Test
    public void deletesExistingFile() throws IOException {
        var path = Path.of("plain_file");
        controller.newByteChannel(path, Set.of(StandardOpenOption.CREATE));
        Assert.assertTrue(controller.exists(path));

        controller.delete(path);
        Assert.assertFalse(controller.exists(path));
    }

    @Test
    public void createsNewDirectory() throws IOException {
        var path = Path.of("dir");
        controller.createDirectory(path);
        Assert.assertTrue(controller.exists(path));

        var attrs = controller.readAttributes(path, BasicFileAttributes.class);
        Assert.assertTrue(attrs.isDirectory());
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void doesntDeleteNonEmptyDir() throws IOException {
        var dir = Path.of("some_dir");
        var file = Path.of("some_dir", "some_file");
        var file2 = Path.of("some_dir", "some_file");
        controller.createDirectory(dir);
        controller.newByteChannel(file, Set.of(StandardOpenOption.CREATE));
        controller.newByteChannel(file2, Set.of(StandardOpenOption.CREATE));

        controller.delete(dir);
    }

    @Test
    public void createsMultipleFiles() throws IOException {
        var file = Path.of("some_file");
        var file2 = Path.of("another_file");

        controller.newByteChannel(file, Set.of(StandardOpenOption.CREATE));
        controller.newByteChannel(file2, Set.of(StandardOpenOption.CREATE));

        Assert.assertTrue(controller.exists(file));
        Assert.assertTrue(controller.exists(file2));
    }

    @Test
    public void createsDirInsideDir() throws IOException {
        var dir = Path.of("dir");
        var inner = Path.of("dir", "inner_dir");

        controller.createDirectory(dir);
        controller.createDirectory(inner);

        Assert.assertTrue(controller.exists(dir));
        Assert.assertTrue(controller.exists(inner));
    }

    @Test
    public void createsFileInsideDir() throws IOException {
        var dir = Path.of("dir");
        var inner = Path.of("dir", "inner_file");

        controller.createDirectory(dir);
        controller.newByteChannel(inner, Set.of(StandardOpenOption.CREATE));

        Assert.assertTrue(controller.exists(dir));
        Assert.assertTrue(controller.exists(inner));
    }

    @Test
    public void copiesFiles() throws IOException {
        var dir = Path.of("dir");
        controller.createDirectory(dir);

        var src = Path.of("dir", "src");
        var target = Path.of("dir", "target");

        controller.newByteChannel(src, Set.of(StandardOpenOption.CREATE));

        controller.copy(src, target);

        Assert.assertTrue(controller.exists(src));
        Assert.assertTrue(controller.exists(target));
    }

    @Test
    public void movesFiles() throws IOException {
        var dir = Path.of("dir");
        controller.createDirectory(dir);

        var src = Path.of("dir", "src");
        var target = Path.of("dir", "target");

        controller.newByteChannel(src, Set.of(StandardOpenOption.CREATE));

        controller.move(src, target);

        Assert.assertFalse(controller.exists(src));
        Assert.assertTrue(controller.exists(target));
    }

    @Test
    public void copiesFileContents() throws IOException {
        var magicNumber = 12;
        var dir = Path.of("dir");
        controller.createDirectory(dir);

        var src = Path.of("dir", "src");
        var target = Path.of("dir", "target");

        var bc = controller.newByteChannel(src, Set.of(StandardOpenOption.CREATE));
        var outputStream = Channels.newOutputStream(bc);
        outputStream.write(magicNumber);
        outputStream.close();

        controller.copy(src, target);

        var copyBc = controller.newByteChannel(target, Set.of(StandardOpenOption.CREATE));
        var inputStream = Channels.newInputStream(copyBc);

        Assert.assertEquals(magicNumber, inputStream.read());
        Assert.assertEquals(0, inputStream.available());

        inputStream.close();
    }

    @Test
    public void movesDirectoriesToAnotherLevel() throws IOException {
        var dir = Path.of("dir");
        controller.createDirectory(dir);

        var src = Path.of("src");
        controller.createDirectory(src);

        var target = Path.of("dir", "target");

        controller.move(src, target);

        Assert.assertFalse(controller.exists(src));
        Assert.assertTrue(controller.exists(target));
    }

    @Test
    public void providesCorrectDirectoryStream() throws IOException {
        Set<Integer> created = new HashSet<>();
        for(int i = 0; i < 100; i++) {
            controller.createDirectory(Path.of("src", Integer.toString(i)));
            created.add(i);
        }

        controller.newDirectoryStream(Path.of("src"), p -> true)
                .forEach(p -> Assert.assertTrue( // Ensure we don't have extra dirs we didn't create
                        created.remove(Integer.parseInt(p.getName(0).toString()))
                ));

        Assert.assertTrue(created.isEmpty()); // Ensure we seen all dirs we have created
    }
}
