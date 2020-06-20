package ofs;

import ofs.blockimpl.BlockFileController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class BlockFileControllerTest {
    private BlockFileController controller;

    @Before
    public void createController() throws IOException {
        controller = new BlockFileController(Files.createTempFile("test", "test"));
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
    public void writesToFile() throws IOException {
        var magicNumber = 12;
        var file = Path.of("file");
        var bc = controller.newByteChannel(file, Set.of(StandardOpenOption.CREATE));
        var outputStream = Channels.newOutputStream(bc);
        outputStream.write(magicNumber);
        outputStream.close();

        var copyBc = controller.newByteChannel(file, Set.of(StandardOpenOption.READ));
        var inputStream = Channels.newInputStream(copyBc);

        Assert.assertEquals(magicNumber, inputStream.read());
        Assert.assertEquals(0, inputStream.available());

        inputStream.close();
    }

    @Test
    public void writesToSparseFile() throws IOException {
        var magicBuffer  = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6});
        var magicBuffer2 = ByteBuffer.wrap(new byte[] { 9, 8, 7, 6, 5, 4});
        var veryDistantPosition = 1024 * 50;
        var file = Path.of("file");
        var bc = controller.newByteChannel(file, Set.of(StandardOpenOption.CREATE));
        bc.write(magicBuffer);
        bc.position(veryDistantPosition);
        bc.write(magicBuffer2);
        bc.close();

        var copyBc = controller.newByteChannel(file, Set.of(StandardOpenOption.READ));
        ByteBuffer dest = ByteBuffer.allocate(magicBuffer.capacity());
        copyBc.read(dest);

        dest.flip();
        magicBuffer.flip();
        Assert.assertEquals(magicBuffer, dest);

        dest.clear();
        copyBc.position(veryDistantPosition);
        copyBc.read(dest);

        dest.flip();
        magicBuffer2.flip();
        Assert.assertEquals(magicBuffer2, dest);
    }

    @Test
    public void writesLongFiles() throws IOException {
        var magicNumber = 7;
        var file = Path.of("file");
        var bc = controller.newByteChannel(file, Set.of(StandardOpenOption.CREATE));
        var outputStream = Channels.newOutputStream(bc);

        var megaByte = 1024 * 1024;
        for(int i = 0; i < megaByte; i++) { //1 megabyte
            outputStream.write(magicNumber);
        }
        outputStream.close();

        var copyBc = controller.newByteChannel(file, Set.of(StandardOpenOption.READ));
        var inputStream = Channels.newInputStream(copyBc);

        for(int i = 0; i < megaByte; i++) { //1 megabyte
            Assert.assertEquals(magicNumber, inputStream.read());
        }
        Assert.assertEquals(0, inputStream.available());

        inputStream.close();
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
        var file2 = Path.of("some_dir", "some_file2");
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
        controller.createDirectory(Path.of("src"));

        for(int i = 0; i < 100; i++) {
            controller.createDirectory(Path.of("src", Integer.toString(i)));
            created.add(i);
        }

        controller.newDirectoryStream(Path.of("src"), p -> true)
                .forEach(p -> Assert.assertTrue( // Ensure we don't have extra dirs we didn't create
                        created.remove(Integer.parseInt(p.getFileName().toString()))
                ));

        Assert.assertTrue(created.isEmpty()); // Ensure we seen all dirs we have created
    }
}
