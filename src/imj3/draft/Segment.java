package imj3.draft;

import static imj3.tools.IMJTools.read;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;

import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2016-06-03)
 */
public final class Segment {
	
	private Segment() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>unused
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final Image2D image = read(imagePath);
		final String outputPath = arguments.get("out", baseName(imagePath) + "_segments.png");
		final int idMask = arguments.get("idMask", 0x80000000)[0];
		final BufferedImage segments = process(image, idMask);
		
		try {
			debugPrint("Writing", outputPath + "...");
			ImageIO.write(segments, "png", new File(outputPath));
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final BufferedImage process(final Image2D image, final int idMask) {
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		ExtractComponents.forEachPixelInEachComponent4(image, new Pixel2DProcessor() {
			
			private int id;
			
			@Override
			public final boolean pixel(final int x, final int y) {
				result.setRGB(x, y, idMask | this.id);
				
				return true;
			}
			
			@Override
			public final void endOfPatch() {
				++this.id;
			}
			
			private static final long serialVersionUID = -7088195004570881462L;
			
		});
		
		return result;
	}

}
