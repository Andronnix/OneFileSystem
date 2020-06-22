package ofs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;

public class OFSFileSystemProviderTest {
    @Test
    public void createsOFSFileSystem() throws IOException {
        var provider = new OFSFileSystemProvider();

        Assert.assertNotNull(provider.newFileSystem(URI.create("ofs:]=$"), Map.of()));
    }

    /**
     * This test creates new fs in a baseFile, creates a file in this fs and writes into it. Then fs is closed and
     * a new fs using the same baseFile is created within a new provider.
     * Test verifies that file exists and has the same content.
     */
    @Test
    public void writesFileSystemToFile() throws IOException {
        var basePath = Files.createTempFile("test", "test");

        var provider = new OFSFileSystemProvider();
        var fs = provider.newFileSystem(URI.create("ofs:]=$"), Map.of("basePath", basePath));

        provider.createDirectory(fs.getPath("dir"));
        var file = fs.getPath("dir", "some_file");
        var bc = provider.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));

        var outputStream = Channels.newOutputStream(bc);
        for(byte b = 0; b < 10; b++)
            outputStream.write(b);
        outputStream.close();

        fs.close();
        provider = null;

        Assert.assertTrue(Files.size(basePath) >= 10);

        var newProvider = new OFSFileSystemProvider();
        var newFs = newProvider.newFileSystem(URI.create("ofs:]=$"), Map.of("basePath", basePath));
        var newFile = newFs.getPath("dir", "some_file");
        var newBc = newProvider.newByteChannel(newFile, Set.of(StandardOpenOption.READ));

        var inputStream = Channels.newInputStream(newBc);
        for(byte b = 0; b < 10; b++) {
            Assert.assertEquals(b, inputStream.read());
        }

        Assert.assertEquals(0, inputStream.available());
        inputStream.close();
    }

    @Test
    public void writesFileSystemWithComplexDirStructureToFile() throws IOException {
        var basePath = Files.createTempFile("test", "test");

        var provider = new OFSFileSystemProvider();
        var fs = provider.newFileSystem(URI.create("ofs:]=$"), Map.of("basePath", basePath));

        provider.createDirectory(fs.getPath("dir"));
        provider.createDirectory(fs.getPath("dir", "inner"));
        provider.createDirectory(fs.getPath("dir", "inner2"));
        provider.createDirectory(fs.getPath("dir", "inner2", "deep1"));
        provider.createDirectory(fs.getPath("dir", "inner2", "deep2"));
        provider.createDirectory(fs.getPath("dir", "inner3"));
        provider.createDirectory(fs.getPath("another_dir"));
        provider.createDirectory(fs.getPath("another_dir", "inner"));

        var file = fs.getPath("dir", "inner2", "deep2", "some_file");
        provider.newByteChannel(file, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));

        var file2 = fs.getPath("another_dir", "inner", "some_file2");
        provider.newByteChannel(file2, Set.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));

        fs.close();
        provider = null;

        var newProvider = new OFSFileSystemProvider();
        var newFs = newProvider.newFileSystem(URI.create("ofs:]=$"), Map.of("basePath", basePath));

        var root = newFs.getPath("]=");
        newProvider.checkAccess(root.resolve("dir"));
        newProvider.checkAccess(root.resolve("dir").resolve("inner"));
        newProvider.checkAccess(root.resolve("dir").resolve("inner2"));
        newProvider.checkAccess(root.resolve("dir").resolve("inner2").resolve("deep2"));
        newProvider.checkAccess(root.resolve("dir").resolve("inner2").resolve("deep2").resolve("some_file"));
        newProvider.checkAccess(root.resolve("dir").resolve("inner3"));
        newProvider.checkAccess(root.resolve("another_dir"));
        newProvider.checkAccess(root.resolve("another_dir").resolve("inner"));
        newProvider.checkAccess(root.resolve("another_dir").resolve("inner").resolve("some_file2"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void refusesToCreateNonOFSFileSystem() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.newFileSystem(URI.create("nonofs:/"), Map.of());
    }

    @Test(expected = FileSystemAlreadyExistsException.class)
    public void createsFileSystemExactlyOnce() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.newFileSystem(URI.create("ofs:]=$"), Map.of());
        provider.newFileSystem(URI.create("ofs:]=$"), Map.of());
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void createsFileSystemOnlyWhenRequested() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.getFileSystem(URI.create("ofs:]=$"));
    }

    @Test
    public void returnsTheSameFileSystemWhenGet() throws IOException {
        var provider = new OFSFileSystemProvider();

        var fs0 = provider.newFileSystem(URI.create("ofs:]=$"), Map.of());
        var fs1 = provider.getFileSystem(URI.create("ofs:]=$simple_path"));
        var fs2 = provider.getFileSystem(URI.create("ofs:]=$differentpath"));

        Assert.assertEquals(fs0, fs1);
        Assert.assertEquals(fs1, fs2);
    }

    @Test
    public void createsOFSPathWithFileSystems() throws IOException {
        FileSystems.newFileSystem(URI.create("ofs:]=$"), Map.of());

        Path p = Path.of(URI.create("ofs:]=$simple_path"));

        Assert.assertEquals(p.getClass(), OFSPath.class);
    }
}
