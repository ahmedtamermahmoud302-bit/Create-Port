import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Bakes modern paletted_permutations into sprites usable by the 1.12 atlas. */
public final class PaletteTextures {
    public static void main(String[] args) throws Exception {
        if (args.length % 4 != 0) throw new IllegalArgumentException("Expected base/key/palette/output jobs");
        for (int i = 0; i < args.length; i += 4) {
            BufferedImage base = ImageIO.read(new File(args[i]));
            BufferedImage key = ImageIO.read(new File(args[i + 1]));
            BufferedImage palette = ImageIO.read(new File(args[i + 2]));
            if (key.getWidth() != palette.getWidth() || key.getHeight() != palette.getHeight())
                throw new IllegalArgumentException("Incompatible trim palettes");
            Map<Integer, Integer> colors = new HashMap<>();
            for (int y = 0; y < key.getHeight(); y++) for (int x = 0; x < key.getWidth(); x++)
                colors.put(key.getRGB(x, y) & 0xffffff, palette.getRGB(x, y));
            BufferedImage output = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < base.getHeight(); y++) for (int x = 0; x < base.getWidth(); x++) {
                int pixel = base.getRGB(x, y);
                int mapped = colors.getOrDefault(pixel & 0xffffff, pixel);
                int alpha = (pixel >>> 24) * (mapped >>> 24) / 255;
                output.setRGB(x, y, (mapped & 0xffffff) | (alpha << 24));
            }
            ImageIO.write(output, "PNG", new File(args[i + 3]));
        }
        System.out.println("Baked " + args.length / 4 + " palette sprites");
    }
}
