import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        var fs = FileSystems.newFileSystem(URI.create("ofs:/"), Map.of());

        System.out.println(fs.isOpen());
    }
}
