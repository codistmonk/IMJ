package imj3.draft;

import static java.lang.Math.pow;
import static multij.tools.Tools.*;

import imj2.tools.BigBitSet;

import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.AutoCloseableImageWriter;
import imj3.tools.IMJTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.Arrays;

import multij.primitivelists.LongList;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-05-03)
 */
public final class ExtractComponents {
	
	private ExtractComponents() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("file", "");
		final String labelsSuffix = arguments.get("labelsSuffix", "_mask.png");
		final String labelsPath = arguments.get("labels", baseName(imagePath) + labelsSuffix);
		final int[] excludedLabels = arguments.get("exclude", 0);
		final int lod = arguments.get("lod", 4)[0];
		final int outline = Long.decode(arguments.get("outline", "0")).intValue();
		final boolean thinOutline = arguments.get("thinOutline", 0)[0] != 0;
		final double scale = pow(2.0, -lod);
		final Image2D image = IMJTools.read(imagePath).getScaledImage(scale);
		final Image2D labels = IMJTools.read(labelsPath).getScaledImage(scale);
		final long labelMask = ~((~0) << labels.getChannels().getValueBitCount());
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int labelsWidth = labels.getWidth();
		final int labelsHeight = labels.getHeight();
		
		debugPrint(imageWidth, imageHeight, labelsWidth, labelsHeight);
		
		Arrays.sort(excludedLabels);
		
		forEachPixelInEachComponent4(labels, new ComponentPixelsProcessor() {
			
			private final Rectangle bounds = new Rectangle();
			
			private long label;
			
			private int id;
			
			@Override
			protected final boolean accept(final int x, final int y) {
				this.label = labels.getPixelValue(x, y);
				this.bounds.add(x, y);
				
				return true;
			}
			
			@Override
			protected final void protectedEndOfPatch() {
				++this.id;
				
				if (Arrays.binarySearch(excludedLabels, (int) (this.label & labelMask)) < 0 && 128 <= this.bounds.width * this.bounds.height) {
					debugPrint(this.bounds);
					
					final long[] pixels = this.getPixels().toArray();
					
					Arrays.sort(pixels);
					
					final int left = this.bounds.x * imageWidth / labelsWidth;
					final int top = this.bounds.y * imageHeight / labelsHeight;
					final int w = (this.bounds.width + 1) * imageWidth / labelsWidth;
					final int h = (this.bounds.height + 1) * imageHeight / labelsHeight;
					final String extractPath = baseName(imagePath) + "_lod" + lod + "_id" + this.id + "_label" + Long.toHexString(this.label & labelMask)
							+ "_x" + left + "_y" + top
							+ "_w" + w + "_h" + h + ".jpg";
					final BufferedImage extract = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
					final Channels channels = image.getChannels();
					final int n = channels.getChannelCount();
					
					for (int yOut = 0, yIn = top; yOut < h; ++yOut, ++yIn) {
						for (int xOut = 0, xIn = left; xOut < w; ++xOut, ++xIn) {
							final int labelX = xIn * labelsWidth / imageWidth;
							final int labelY = yIn * labelsHeight / imageHeight;
							int rgb = 0;
							final boolean pixelInSegment = 0 <= Arrays.binarySearch(pixels, getPixel(labelX, labelY));
							
							{
								final long pixelValue = image.getPixelValue(xIn, yIn);
								
								for (int i = n - 1; 0 <= i; --i) {
									rgb = (rgb << Byte.SIZE) | ((int) channels.getChannelValue(pixelValue, i) & 0xFF);
								}
							}
							
							if (outline != 0) {
								final int northY = thinOutline ? (yIn - 1) * labelsHeight / imageHeight : labelY - 1;
								final int westX = thinOutline ? (xIn - 1) * labelsWidth / imageWidth : labelX - 1;
								final int eastX = thinOutline ? (xIn + 1) * labelsWidth / imageWidth : labelX + 1;
								final int southY = thinOutline ? (yIn + 1) * labelsHeight / imageHeight : labelY + 1;
								final boolean northInSegment = 0 <= Arrays.binarySearch(pixels, getPixel(labelX, northY));
								final boolean westInSegment = 0 <= Arrays.binarySearch(pixels, getPixel(westX, labelY));
								final boolean eastInSegment = 0 <= Arrays.binarySearch(pixels, getPixel(eastX, labelY));
								final boolean southInSegment = 0 <= Arrays.binarySearch(pixels, getPixel(labelX, southY));
								
								if (pixelInSegment) {
									if (northInSegment && eastInSegment && westInSegment && southInSegment) {
										extract.setRGB(xOut, yOut, rgb);
									} else {
										extract.setRGB(xOut, yOut, outline);
									}
								} else {
									if (thinOutline && (northInSegment || eastInSegment || westInSegment || southInSegment)) {
										extract.setRGB(xOut, yOut, 0);
									} else {
										extract.setRGB(xOut, yOut, rgb);
									}
								}
							} else if (pixelInSegment) {
								extract.setRGB(xOut, yOut, rgb);
							}
						}
					}
					
					try (final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter("jpg")) {
						imageWriter.setCompressionQuality(0.9F).setOutput(new FileOutputStream(extractPath)).write(extract);
					} catch (final Exception exception) {
						throw unchecked(exception);
					}
				}
				
				this.bounds.setSize(-1, -1);
			}
			
			private static final long serialVersionUID = -6555019874711337589L;
			
		});
	}
	
	public static final void forEachPixelInEachComponent4(final Image2D image, final Pixel2DProcessor process) {
		forEachPixelInEachComponent4(image, process, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
	}
	
	public static final void forEachPixelInEachComponent4(final Image2D image, final Pixel2DProcessor process, final Rectangle bounds) {
		final LongList todo = new LongList();
		final long pixelCount = image.getPixelCount();
		final BigBitSet done = new BigBitSet(pixelCount);
		
		for (long pixel = 0L; pixel < pixelCount; ++pixel) {
			processComponent4(image, process, todo, done, pixel, bounds);
		}
	}
	
	public static final void forEachPixelInComponent4(final Image2D image, final long componentPixel, final Pixel2DProcessor process) {
		forEachPixelInComponent4(image, componentPixel, process, new Rectangle(0, 0, image.getWidth(), image.getHeight()));
	}
	
	public static final void forEachPixelInComponent4(final Image2D image, final long componentPixel, final Pixel2DProcessor process, final Rectangle bounds) {
		processComponent4(image, process, new LongList(), new BigBitSet(image.getPixelCount()), componentPixel, bounds);
	}
	
	public static final void processComponent4(final Image2D image, final Pixel2DProcessor process, final LongList todo,
			final BigBitSet done, final long componentPixel, final Rectangle bounds) {
		final int width = image.getWidth();
		
		if (!done.get(componentPixel)) {
			schedule(componentPixel, todo, done);
			
			boolean processing = true;
			
			while (!todo.isEmpty()) {
				final long p = todo.remove(0);
				final long value = image.getPixelValue(p);
				final int x = (int) (p % width);
				final int y = (int) (p / width);
				
				if (processing && !process.pixel(x, y)) {
					processing = false;
				}
				
				if (bounds.y < y) {
					maybeSchedule(image, p - width, value, todo, done);
				}
				if (bounds.x < x) {
					maybeSchedule(image, p - 1, value, todo, done);
				}
				if (x + 1 < bounds.x + bounds.width) {
					maybeSchedule(image, p + 1, value, todo, done);
				}
				if (y + 1 < bounds.y + bounds.height) {
					maybeSchedule(image, p + width, value, todo, done);
				}
			}
			
			if (processing) {
				process.endOfPatch();
			}
		}
	}
	
	public static final void schedule(final long pixel, final LongList todo, final BigBitSet done) {
		done.set(pixel, true);
		todo.add(pixel);
	}
	
	private static final void maybeSchedule(final Image2D image, final long pixel,
			final long value, final LongList todo, final BigBitSet done) {
		if (!done.get(pixel) && value == image.getPixelValue(pixel)) {
			schedule(pixel, todo, done);
		}
	}
	
	/**
	 * @author codistmonk (creation 2015-05-10)
	 */
	public static abstract class ComponentPixelsProcessor implements Pixel2DProcessor {
		
		private final LongList pixels = new LongList();
		
		public final LongList getPixels() {
			return this.pixels;
		}
		
		@Override
		public final boolean pixel(final int x, final int y) {
			if (this.accept(x, y)) {
				this.pixels.add(getPixel(x, y));
				
				return true;
			}
			
			this.getPixels().clear();
			
			return false;
		}
		
		@Override
		public final void endOfPatch() {
			this.protectedEndOfPatch();
			this.getPixels().clear();
		}
		
		protected boolean accept(final int x, final int y) {
			ignore(x);
			ignore(y);
			
			return true;
		}
		
		protected void protectedEndOfPatch() {
			// NOP
		}
		
		private static final long serialVersionUID = -7576924788386611226L;
		
		public static final long INT_MASK = (1L << Integer.SIZE) - 1;
		
		public static final long getPixel(final int x, final int y) {
			return ((long) x << Integer.SIZE) | (y & INT_MASK);
		}
		
		public static final int getX(final long pixel) {
			return (int) ((pixel >> Integer.SIZE) & INT_MASK);
		}
		
		public static final int getY(final long pixel) {
			return (int) (pixel & INT_MASK);
		}
		
	}
	
}
