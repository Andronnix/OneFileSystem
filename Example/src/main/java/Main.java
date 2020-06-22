import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        var m = new Main();
        m.copyImage();
        m.showImage();
    }

    void copyImage() throws IOException, URISyntaxException {
        FileSystems.newFileSystem(URI.create("ofs:]=$"), Map.of());

        var resourcePath = Path.of(Main.class.getResource("/inner/jetbrains.png").toURI());
        var dir = Files.createDirectory(Path.of(URI.create("ofs:]=$inner_dir")));
        var targetPath = dir.resolve("logo.png");

        Files.copy(resourcePath, targetPath);
    }

    void showImage() throws IOException {
        var is = Files.newInputStream(Path.of(URI.create("ofs:]=$inner_dir$logo.png")));

        BufferedImage img = ImageIO.read(is);
        ImageIcon icon = new ImageIcon(img);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(600,600);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
