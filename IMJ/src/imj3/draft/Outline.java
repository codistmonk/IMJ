package imj3.draft;

import static imj3.tools.IMJTools.read;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Image2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-14)
 */
public final class Outline {
	
	private Outline() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("file", "");
		final int lod = arguments.get("lod", 0)[0];
		final String maskPath = arguments.get("mask", baseName(imagePath) + "_classification.png");
		final Image2D image = read(imagePath, lod);
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Image2D labels = read(maskPath, lod);
		final int labelsWidth = labels.getWidth();
		final int labelsHeight = labels.getHeight();
		final int[] colors = arguments.get("colors");
		final BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_BGR);
		final boolean binaryLabels = labels.getChannels().getValueBitCount() == 1;
		
		for (int y = 0; y < imageHeight; ++y) {
			final int labelY = y * labelsHeight / imageHeight;
			final int northLabelY = 0 < y ? (y - 1) * labelsHeight / imageHeight : labelY;
			final int southLabelY = y + 1 < imageHeight ? (y + 1) * labelsHeight / imageHeight : labelY;
			
			for (int x = 0; x < imageWidth; ++x) {
				final int labelX = x * labelsWidth / imageWidth;
				final int westLabelX = 0 < x ? (x - 1) * labelsWidth / imageWidth : labelX;
				final int eastLabelX = x + 1 < imageWidth ? (x + 1) * labelsWidth / imageWidth : labelX;
				final long label = labels.getPixelValue(labelX, labelY);
				final long northLabel = northLabelY != labelY ? labels.getPixelValue(labelX, northLabelY) : label;
				final long westLabel = westLabelX != labelX ? labels.getPixelValue(westLabelX, labelY) : label;
				final long eastLabel = eastLabelX != labelX ? labels.getPixelValue(eastLabelX, labelY) : label;
				final long southLabel = southLabelY != labelY ? labels.getPixelValue(labelX, southLabelY) : label;
				
				if (label != northLabel || label != westLabel || label != eastLabel || label != southLabel) {
					result.setRGB(x, y, 0 < colors.length ? colors[binaryLabels ? ((label & 0x00FFFFFF) == 0 ? 0 : 1) : (int) label] : (int) label);
				} else {
					result.setRGB(x, y, (int) image.getPixelValue(x, y));
				}
			}
		}
		
		
		{
			final File resultFile = new File(baseName(imagePath) + "_outlined.jpg");
			
			debugPrint("Writing: ", resultFile);
			
			try {
				ImageIO.write(result, "jpg", resultFile);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}
	}
	
}
