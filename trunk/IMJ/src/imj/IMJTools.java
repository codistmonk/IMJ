package imj;

import static imj.IMJTools.forEachPixel;
import static imj.ImageOfBufferedImage.Feature.MAX_RGB;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.ImageOfBufferedImage.Feature;
import imj.apps.modules.FilteredImage;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class IMJTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final int ALPHA_MASK = 0xFF000000;
	
	/**
	 * {@value}.
	 */
	public static final int RED_MASK = 0x00FF0000;
	
	/**
	 * {@value}.
	 */
	public static final int GREEN_MASK = 0x0000FF00;
	
	/**
	 * {@value}.
	 */
	public static final int BLUE_MASK = 0x000000FF;
	
	public static final int acyclicDistance(final int a, final int b) {
		return abs(b - a);
	}
	
	public static final int cyclicDistance(final int a, final int b, final int n) {
		if (b < a) {
			return cyclicDistance(b, a, n);
		}
		
		return min(b - a, n + a - b);
	}
	
	public static final void updateHistograms(final Image image, final RegionOfInterest roi, final Channel[] channels,
			final int[][] histograms) {
		forEachPixel(image, roi, new PixelProcessor() {
			
			@Override
			public final void process(final int pixel) {
				if (roi == null || roi.get(pixel)) {
					for (final Channel channel : channels) {
						++histograms[channel.getChannelIndex()][channel.getValue(image.getValue(pixel))];
					}
				}
			}
			
		});
	}
	
	public static final void forEachPixel(final Image image, final RegionOfInterest roi, final PixelProcessor processor) {
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final FilteredImage f = cast(FilteredImage.class, image);
		
		if (f != null) {
			final int tileRowCount = FilteredImage.DEFAULT_CACHE_ROW_COUNT;
			final int tileColumnCount = FilteredImage.DEFAULT_CACHE_COLUMN_COUNT;
			final int lastTileRowIndex = imageRowCount / tileRowCount;
			final int lastTileColumnIndex = imageColumnCount / tileColumnCount;
			
			for (int tileRowIndex = 0; tileRowIndex <= lastTileRowIndex; ++tileRowIndex) {
				System.out.print(tileRowIndex + "/" + lastTileRowIndex + "\r");
				
				for (int tileColumnIndex = 0; tileColumnIndex <= lastTileColumnIndex; ++tileColumnIndex) {
					for (int rowIndexInTile = 0, rowIndex = tileRowIndex * tileRowCount; rowIndexInTile < tileRowCount && rowIndex < imageRowCount; ++rowIndexInTile, ++rowIndex) {
						for (int columnIndexInTile = 0, columnIndex = tileColumnIndex * tileColumnCount; columnIndexInTile < tileColumnCount && columnIndex < imageColumnCount; ++columnIndexInTile, ++columnIndex) {
							if (roi == null || roi.get(rowIndex, columnIndex)) {
								processor.process(rowIndex * imageColumnCount + columnIndex);
							}
						}
					}
				}
			}
		} else {
			final int pixelCount = image.getPixelCount();
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				if (pixel % imageColumnCount == 0) {
					System.out.print(pixel + "/" + pixelCount + "\r");
				}
				
				if (roi == null || roi.get(pixel)) {
					processor.process(pixel);
				}
			}
		}
	}
	
	public static final void forEachPixelInEachTile(final Image image,
			final int tileRowCount, final int tileColumnCount, final int verticalTileStride, final int horizontalTileStride,
			final PixelProcessor processor) {
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		
		for (int tileRowIndex = 0; tileRowIndex + tileRowCount <= imageRowCount; tileRowIndex += verticalTileStride) {
			System.out.print(tileRowIndex + "/" + imageRowCount + "\r");
			
			for (int tileColumnIndex = 0; tileColumnIndex + tileColumnCount <= imageColumnCount; tileColumnIndex += horizontalTileStride) {
				for (int rowIndex = tileRowIndex; rowIndex < tileRowIndex + tileRowCount; ++rowIndex) {
					for (int columnIndex = tileColumnIndex; columnIndex < tileColumnIndex + tileColumnCount; ++columnIndex) {
						processor.process(rowIndex * imageColumnCount + columnIndex);
					}
				}
			}
		}
	}
	
	public static final void forEachPixelInEachTile(final Image image, final int verticalTileCount, final int horizontalTileCount, final PixelProcessor processor) {
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final int tileRowCount = imageRowCount / verticalTileCount;
		final int tileColumnCount = imageColumnCount / horizontalTileCount;
		
		for (int tileRowIndex = 0; tileRowIndex < verticalTileCount; ++tileRowIndex) {
			System.out.print(tileRowIndex + "/" + verticalTileCount + "\r");
			
			for (int tileColumnIndex = 0; tileColumnIndex < horizontalTileCount; ++tileColumnIndex) {
				for (int rowIndex = tileRowIndex * tileRowCount, endRowIndex = rowIndex + tileRowCount; rowIndex < endRowIndex; ++rowIndex) {
					for (int columnIndex = tileColumnIndex * tileColumnCount, endColumnIndex = columnIndex + tileColumnCount; columnIndex < endColumnIndex; ++columnIndex) {
						processor.process(rowIndex * imageColumnCount + columnIndex);
					}
				}
			}
		}
	}
	
	public static final String deepToString(final Object object) {
		if (object == null) {
			return "null";
		}
		
		if (object.getClass().isArray()) {
			if (!object.getClass().getComponentType().isPrimitive()) {
				return Arrays.deepToString((Object[]) object);
			}
			
			if (boolean[].class.equals(object.getClass())) {
				return Arrays.toString((boolean[]) object);
			}
			
			if (byte[].class.equals(object.getClass())) {
				return Arrays.toString((byte[]) object);
			}
			
			if (char[].class.equals(object.getClass())) {
				return Arrays.toString((char[]) object);
			}
			
			if (int[].class.equals(object.getClass())) {
				return Arrays.toString((int[]) object);
			}
			
			if (long[].class.equals(object.getClass())) {
				return Arrays.toString((long[]) object);
			}
			
			if (float[].class.equals(object.getClass())) {
				return Arrays.toString((float[]) object);
			}
			
			if (double[].class.equals(object.getClass())) {
				return Arrays.toString((double[]) object);
			}
		}
		
		return object.toString();
	}
	
	public static final void writePPM(final Image image, final OutputStream output) {
		try {
			final int columnCount = image.getColumnCount();
			final int rowCount = image.getRowCount();
			
			output.write("P6".getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			output.write(("" + columnCount).getBytes("ASCII"));
			output.write(" ".getBytes("ASCII"));
			output.write(("" + rowCount).getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			output.write("255".getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			
			final byte[] buffer = new byte[columnCount * 3];
			
			for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
					final int rgb = image.getValue(rowIndex, columnIndex);
					buffer[3 * columnIndex + 0] = (byte) red(rgb);
					buffer[3 * columnIndex + 1] = (byte) green(rgb);
					buffer[3 * columnIndex + 2] = (byte) blue(rgb);
				}
				
				output.write(buffer);
			}
			
			output.close();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void writePGM(final Image image, final OutputStream output) {
		try {
			final int columnCount = image.getColumnCount();
			final int rowCount = image.getRowCount();
			
			output.write("P5".getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			output.write(("" + columnCount).getBytes("ASCII"));
			output.write(" ".getBytes("ASCII"));
			output.write(("" + rowCount).getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			output.write("255".getBytes("ASCII"));
			output.write("\n".getBytes("ASCII"));
			
			final byte[] buffer = new byte[columnCount];
			
			for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
					buffer[columnIndex] = (byte) MAX_RGB.getValue(image.getValue(rowIndex, columnIndex), false);
				}
				
				output.write(buffer);
			}
			
			output.close();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final int unsigned(final byte value) {
		return value & 0x000000FF;
	}
	
	public static final int argb(final int alpha, final int red, final int green, final int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	public static final int alpha(final int argb) {
		return channelValue(argb, 3);
	}
	
	public static final int red(final int argb) {
		return channelValue(argb, 2);
	}
	
	public static final int green(final int argb) {
		return channelValue(argb, 1);
	}
	
	public static final int blue(final int argb) {
		return channelValue(argb, 0);
	}
	
	public static final int hue(final int argb) {
		final int red = red(argb);
		final int green = green(argb);
		final int blue = blue(argb);
		int brightness = red < green ? green : red;
		int darkness = red < green ? red : green;
		
		if (brightness < blue) {
			brightness = blue;
		} else if (blue < darkness) {
			darkness = blue;
		}
		
		final int amplitude = brightness - darkness;
		final int saturation = brightness == 0 ? 0 : amplitude * 255 / brightness;
		
		if (saturation == 0) {
			return 0;
		}
		
		final float redc = (float) (brightness - red) / amplitude;
		final float greenc = (float) (brightness - green) / amplitude;
		final float bluec = (float) (brightness - blue) / amplitude;
		float hue;
		
	    if (red == brightness) {
	    	hue = bluec - greenc;
	    } else if (green == brightness) {
	    	hue = 2F + redc - bluec;
	    } else {
	    	hue = 4F + greenc - redc;
	    }
	    
	    hue /= 6F;
	    
	    return (int) ((hue < 0F ? hue + 1F : hue) * 255F);
	}
	
	public static final int saturation(final int argb) {
		final int red = red(argb);
		final int green = green(argb);
		final int blue = blue(argb);
		int brightness = red < green ? green : red;
		int darkness = red < green ? red : green;
		
		if (brightness < blue) {
			brightness = blue;
		} else if (blue < darkness) {
			darkness = blue;
		}
		
		return brightness == 0 ? 0 : (brightness - darkness) * 255 / brightness;
	}
	
	public static final int brightness(final int argb) {
		final int red = red(argb);
		final int green = green(argb);
		final int blue = blue(argb);
		int result = red < green ? green : red;
		
		if (result < blue) {
			result = blue;
		}
		
		return result;
	}
	
	public static final int darkness(final int argb) {
		final int red = red(argb);
		final int green = green(argb);
		final int blue = blue(argb);
		int result = red > green ? green : red;
		
		if (result > blue) {
			result = blue;
		}
		
		return result;
	}
	
	public static final int channelValue(final int argb, final int channelIndex) {
		return (argb >> (channelIndex << 3)) & 0x000000FF;
	}
	
	public static final int square(final int x) {
		return x * x;
	}
	
	public static final int ceilingOfRatio(final int numerator, final int denominator) {
		return (numerator + denominator - 1) / denominator;
	}
	
	public static final Image binary(final Image image) {
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final int pixelCount = rowCount * columnCount;
		final Image result = new ImageOfInts(rowCount, columnCount, 1);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result.setValue(pixel, image.getValue(pixel) == 0 ? 0 : 1);
		}
		
		return result;
	}
	
	public static final Image image(final String id, final Feature feature) {
		try {
			return new ImageOfBufferedImage(read(new File(id)), Feature.MAX_RGB);
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Image image(final int[][] data) {
		return image(1, data);
	}
	
	public static final Image image(final int channelCount, final int[][] data) {
		final int rowCount = data.length;
		final int columnCount = data[0].length;
		final Image result = new ImageOfInts(rowCount, columnCount, channelCount);
		
		for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
			for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
				result.setValue(rowIndex, columnIndex, data[rowIndex][columnIndex]);
			}
		}
		
		return result;
	}
	
	public static final Image adjust(final Image image, final int newMinimum, final int newMaximum) {
		final int n = image.getPixelCount();
		int oldMinimum = Integer.MAX_VALUE;
		int oldMaximum = Integer.MIN_VALUE;
		
		for (int i = 0; i < n; ++i) {
			final int value = image.getValue(i);
			
			if (value < oldMinimum) {
				oldMinimum = value;
			}
			
			if (oldMaximum < value) {
				oldMaximum = value;
			}
		}
		
		final int oldMin = oldMinimum;
		final int oldAmpltiude = oldMaximum - oldMinimum;
		final int newAmplitude = newMaximum - newMinimum;
		
		return new Image.Abstract(image.getRowCount(), image.getColumnCount(), 1) {
			
			@Override
			public final int getValue(final int index) {
				return newMinimum + (image.getValue(index) - oldMin) * newAmplitude / oldAmpltiude;
			}
			
			@Override
			public final int setValue(final int index, final int value) {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	public static final int getMinimum(final Image image) {
		int result = Integer.MAX_VALUE;
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result = min(result, image.getValue(pixel));
		}
		
		return result;
	}
	
	public static final int getMaximum(final Image image) {
		int result = Integer.MIN_VALUE;
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result = max(result, image.getValue(pixel));
		}
		
		return result;
	}
	
	public static final int[] getMinimumAndMaximum(final Image image) {
		final int[] result = { Integer.MAX_VALUE, Integer.MIN_VALUE };
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int value = image.getValue(pixel);
			result[0] = min(result[0], value);
			result[1] = max(result[1], value);
		}
		
		return result;
	}
	
	public static final Statistics[] getRegionStatistics(final Image image, final Image labels) {
		final int lastLabel = getMinimumAndMaximum(labels)[1];
		final int labelCount = lastLabel + 1;
		final Statistics[] result = new Statistics[labelCount];
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			getOrCreate(result, labels.getValue(pixel)).addValue(image.getValue(pixel));
		}
		
		return result;
	}
	
	public static final Statistics getOrCreate(final Statistics[] statistics, final int index) {
		final Statistics maybeResult = statistics[index];
		
		return maybeResult != null ? maybeResult : (statistics[index] = new Statistics());
	}
	
	public static final Image newImage(final Image labels, final Statistics[] statistics, final StatisticsSelector feature) {
		final int rowCount = labels.getRowCount();
		final int columnCount = labels.getColumnCount();
		final Image result = new ImageOfInts(rowCount, columnCount, 1);
		final int pixelCount = rowCount * columnCount;
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result.setValue(pixel, (int) feature.getValue(statistics[labels.getValue(pixel)]));
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-01-25)
	 */
	public static enum StatisticsSelector {
		
		MEAN {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getMean();
			}
			
		}, MINIMUM {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getMinimum();
			}
			
		}, MAXIMUM {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getMaximum();
			}
			
		}, AMPLITUDE {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getAmplitude();
			}
			
		}, SUM {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getSum();
			}
			
		}, SUM_OF_SQUARES {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getSumOfSquares();
			}
			
		}, VARIANCE {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getVariance();
			}
			
		}, COUNT {
			
			@Override
			public final double getValue(final Statistics statistics) {
				return statistics.getCount();
			}
			
		};
		
		public abstract double getValue(Statistics statistics);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-08)
	 */
	public static abstract interface PixelProcessor {
		
		public abstract void process(int pixel);
		
	}
	
}
