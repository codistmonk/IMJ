package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;

import imj.ImageOfBufferedImage.Feature;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class IMJTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final int ceilingOfRatio(final int numerator, final int denominator) {
		return (numerator + denominator - 1) / denominator;
	}
	
	public static final Image binary(final Image image) {
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final int pixelCount = rowCount * columnCount;
		final Image result = new ImageOfInts(rowCount, columnCount);
		
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
		final int rowCount = data.length;
		final int columnCount = data[0].length;
		final Image result = new ImageOfInts(rowCount, columnCount);
		
		for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
			for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
				result.setValue(rowIndex, columnIndex, data[rowIndex][columnIndex]);
			}
		}
		
		return result;
	}
	
	public static final Image adjust(final Image image, final int newMinimum, final int newMaximum) {
		final int n = image.getRowCount() * image.getColumnCount();
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
		
		return new Image.Abstract(image.getRowCount(), image.getColumnCount()) {
			
			@Override
			public final int getValue(final int index) {
				return newMinimum + (image.getValue(index) - oldMin) * newAmplitude / oldAmpltiude;
			}
			
			@Override
			public final int setValue(final int index, final int value) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public final float getFloatValue(final int index) {
				return this.getValue(index);
			}
			
			@Override
			public final float setFloatValue(final int index, final float value) {
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
		final Image result = new ImageOfInts(rowCount, columnCount);
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
	
}
