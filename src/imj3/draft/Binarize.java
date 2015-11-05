package imj3.draft;

import static multij.tools.Tools.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-17)
 */
public final class Binarize {
	
	private Binarize() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final String outputPath = arguments.get("output", baseName(imagePath) + "_mask.png");
		final String outputFormat = arguments.get("outputFormat", outputPath.substring(outputPath.lastIndexOf(".") + 1));
		final int zero = arguments.get("zero", 0)[0];
		final BufferedImage image = ImageIO.read(new File(imagePath));
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		final int width = image.getWidth();
		final int height = image.getHeight();
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				if ((image.getRGB(x, y) & 0x00FFFFFF) != zero) {
					result.setRGB(x, y, ~0);
				}
			}
		}
		
		debugPrint("Writing", outputPath);
		ImageIO.write(result, outputFormat, new File(outputPath));
	}
	
}
