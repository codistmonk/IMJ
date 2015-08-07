package imj3.draft;

import static imj3.core.Channels.Predefined.C1_U1;
import static imj3.draft.ExtractComponents.forEachPixelInEachComponent4;
import static imj3.tools.IMJTools.read;
import static multij.tools.Tools.*;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.imageio.ImageIO;

import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.PackedImage2D;

import multij.primitivelists.LongList;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-08-07)
 */
public final class EraseSmallComponents {
	
	private EraseSmallComponents() {
		throw new IllegalInstantiationException();
	}
	
	public static final long INT_MASK = ~((~0L) << Integer.SIZE);
	
	/**
	 * @param commandLineArguments
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final int threshold = arguments.get("threshold", 0)[0];
		final Image2D image = read(imagePath);
		final String outputPath = arguments.get("output", baseName(imagePath) + "_filtered.png");
		
		debugPrint("image:", imagePath);
		debugPrint("Locating components below", threshold + "...");
		
		final Image2D mask = locateSmallComponents(image, threshold, newMask(image, "_mask"));
		
		debugPrint("Locating borders...");
		
		final Collection<Long> borderPixels = locateBorderPixels(mask);
		
		shrinkBorders(image, mask, borderPixels);
		
		try {
			debugPrint("Writing", outputPath + "...");
			ImageIO.write((RenderedImage) image.toAwt(), "png", new File(outputPath));
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final void shrinkBorders(final Image2D image, final Image2D mask, final Collection<Long> borderPixels) {
		final int lastX = image.getWidth() - 1;
		final int lastY = image.getHeight() - 1;
		long maximum = image.getPixelCount();
		
		while (!borderPixels.isEmpty()) {
			if (--maximum < 0L) {
				throw new IllegalStateException();
			}
			
			final long pixel = take(borderPixels);
			final int x = image.getX(pixel);
			final int y = image.getY(pixel);
			
			if (0 < y && mask.getPixelValue(x, y - 1) == 0L) {
				image.setPixelValue(x, y, image.getPixelValue(x, y - 1));
				mask.setPixelValue(x, y, 0L);
			} else if (0 < x && mask.getPixelValue(x - 1, y) == 0L) {
				image.setPixelValue(x, y, image.getPixelValue(x - 1, y));
				mask.setPixelValue(x, y, 0L);
			} else if (x < lastX && mask.getPixelValue(x + 1, y) == 0L) {
				image.setPixelValue(x, y, image.getPixelValue(x + 1, y));
				mask.setPixelValue(x, y, 0L);
			} else if (y < lastY && mask.getPixelValue(x, y + 1) == 0L) {
				image.setPixelValue(x, y, image.getPixelValue(x, y + 1));
				mask.setPixelValue(x, y, 0L);
			}
			
			if (0 < y && mask.getPixelValue(x, y - 1) != 0L) {
				borderPixels.add(image.getPixel(x, y - 1));
			}
			
			if (0 < x && mask.getPixelValue(x - 1, y) != 0L) {
				borderPixels.add(image.getPixel(x - 1, y));
			}
			
			if (x < lastX && mask.getPixelValue(x + 1, y) != 0L) {
				borderPixels.add(image.getPixel(x + 1, y));
			}
			
			if (y < lastY && mask.getPixelValue(x, y + 1) != 0L) {
				borderPixels.add(image.getPixel(x, y + 1));
			}
		}
	}
	
	public static final <E> E take(final Iterable<E> iterable) {
		final Iterator<E> i = iterable.iterator();
		final E result = i.next();
		i.remove();
		return result;
	}
	
	public static final Collection<Long> locateBorderPixels(final Image2D mask) {
		final Collection<Long> result = new LinkedHashSet<>();
		final int lastX = mask.getWidth() - 1;
		final int lastY = mask.getHeight() - 1;
		
		mask.forEachPixel((x, y) -> {
			final long pixelValue = mask.getPixelValue(x, y);
			
			if (0L != pixelValue && (0 < y && pixelValue != mask.getPixelValue(x, y - 1)
					|| 0 < x && pixelValue != mask.getPixelValue(x - 1, y)
					|| x < lastX && pixelValue != mask.getPixelValue(x + 1, y)
					|| y < lastY && pixelValue != mask.getPixelValue(x, y + 1))) {
				result.add(mask.getPixel(x, y));
			}
			
			return true;
		});
		
		return result;
	}
	
	public static Image2D locateBorders(final Image2D mask, final Image2D result) {
		final int lastX = mask.getWidth() - 1;
		final int lastY = mask.getHeight() - 1;
		
		mask.forEachPixel((x, y) -> {
			final long pixelValue = mask.getPixelValue(x, y);
			
			if (0L != pixelValue && (y == 0 || pixelValue != mask.getPixelValue(x, y - 1)
					|| x == 0 ||  pixelValue != mask.getPixelValue(x - 1, y)
					|| x == lastX ||  pixelValue != mask.getPixelValue(x + 1, y)
					|| y == lastY ||  pixelValue != mask.getPixelValue(x, y + 1))) {
				result.setPixelValue(x, y, ~0L);
			}
			
			return true;
		});
		
		return result;
	}
	
	public static Image2D locateSmallComponents(final Image2D image, final int threshold, final Image2D result) {
		forEachPixelInEachComponent4(image, new Pixel2DProcessor() {
			
			private final LongList pixels = new LongList();
			
			@Override
			public final boolean pixel(final int x, final int y) {
				this.pixels.add(image.getPixel(x, y));
				
				return true;
			}
			
			@Override
			public final void endOfPatch() {
				if (this.pixels.size() <= threshold) {
					this.pixels.forEach(p -> {
						result.setPixelValue(p, ~0L);
						return true;
					});
				}
				
				this.pixels.clear();
			}
			
			private static final long serialVersionUID = -1592687244886135633L;
			
		});
		
		return result;
	}
	
	public static Image2D newMask(final Image2D image, final String idSuffix) {
		return new PackedImage2D(image.getId() + idSuffix, image.getWidth(), image.getHeight(), C1_U1);
	}
	
}
