package ofs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Map;

public class OFSFileSystemProviderTest {
    @Test
    public void createsOFSFileSystem() throws IOException {
        var provider = new OFSFileSystemProvider();

        Assert.assertNotNull(provider.newFileSystem(URI.create("ofs:/"), Map.of()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesToCreateNonOFSFileSystem() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.newFileSystem(URI.create("nonofs:/"), Map.of());
    }

    @Test(expected = FileSystemAlreadyExistsException.class)
    public void createsFileSystemExactlyOnce() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.newFileSystem(URI.create("ofs:/"), Map.of());
        provider.newFileSystem(URI.create("ofs:/"), Map.of());
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void createsFileSystemOnlyWhenRequested() throws IOException {
        var provider = new OFSFileSystemProvider();

        provider.getFileSystem(URI.create("ofs:/"));
    }

    @Test
    public void returnsTheSameFileSystemWhenGet() throws IOException {
        var provider = new OFSFileSystemProvider();

        var fs0 = provider.newFileSystem(URI.create("ofs:/"), Map.of());
        var fs1 = provider.getFileSystem(URI.create("ofs:/somepath"));
        var fs2 = provider.getFileSystem(URI.create("ofs:/differentpath"));

        Assert.assertEquals(fs0, fs1);
        Assert.assertEquals(fs1, fs2);
    }
}
