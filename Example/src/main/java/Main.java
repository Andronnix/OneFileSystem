import java.net.URI;
import java.nio.file.FileSystems;

public class Main {
    public static void main(String[] args) {
        var fs = FileSystems.getFileSystem(URI.create("ofs:/"));

        System.out.println("Hello world!");
    }
}
