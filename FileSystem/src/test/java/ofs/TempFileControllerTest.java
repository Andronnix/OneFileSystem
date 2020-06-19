package ofs;

import org.junit.Test;

public class TempFileControllerTest {


    @Test
    public void nonExistingFilesDontExist() {

    }

    /*
    SeekableByteChannel newByteChannel(OFSPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException;

    DirectoryStream<Path> newDirectoryStream(OFSPath dir, DirectoryStream.Filter<? super Path> filter) throws IOException;

    boolean exists(OFSPath path) throws IOException;

    void createDirectory(OFSPath dir, FileAttribute<?>... attrs) throws IOException;

    void delete(OFSPath path) throws IOException;

    void copy(OFSPath source, OFSPath target, CopyOption... options) throws IOException;

    void move(OFSPath source, OFSPath target, CopyOption... options) throws IOException;

    <V extends FileAttributeView> V getFileAttributeView(OFSPath path, Class<V> type, LinkOption... options);

    <A extends BasicFileAttributes> A readAttributes(OFSPath path, Class<A> type, LinkOption... options) throws IOException;

    Map<String, Object> readAttributes(OFSPath path, String attributes, LinkOption... options) throws IOException;

    void setAttribute(OFSPath path, String attribute, Object value, LinkOption... options) throws IOException;
    */
}
