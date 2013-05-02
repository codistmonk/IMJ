package imj.apps.modules;

import static imj.IMJTools.forEachPixel;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageOfInts;
import imj.ImageWrangler;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public final class HistogramProjectionTest {
	
	@Test
	public final void test1() {
		final int histogramProjectionRowCount = 128;
		final int histogramProjectionColumnCount = histogramProjectionRowCount;
		final Image histogramProjection = new ImageOfInts(histogramProjectionRowCount, histogramProjectionColumnCount, 1);
		final Image image = ImageWrangler.INSTANCE.load("test/imj/12003.jpg");
		final Channel[] channels = { RED, GREEN, BLUE };
		final int[] histogram = histogram(image, channels, null);
		
		assertEquals(image.getPixelCount(), sum(histogram));
		
		final int[][] axes = generateProjectionBasis(1, 1, 1);
		
		debugPrint(Arrays.deepToString(axes));
		
		project(image, channels, axes, histogram, histogramProjection);
		
		ImageComponent.showAdjusted("test1", histogramProjection);
	}
	
	public static final void project(final Image image, final Channel[] channels, final int[][] axes,
			final int[] histogram, final Image histogramProjection) {
		final int histogramProjectionRowCount = histogramProjection.getRowCount();
		final int histogramProjectionColumnCount = histogramProjection.getChannelCount();
		final int axisCount = axes.length;
		final int[] minima = new int[axisCount];
		final int[] amplitudes = new int[axisCount];
		
		fill(minima, Integer.MAX_VALUE);
		fill(amplitudes, Integer.MIN_VALUE);
		
		forEachPixel(image, null, new ProjectionProcessor(image, channels, axes) {
			
			@Override
			public final void process(final int pixel, final int[] projections) {
				for (int i = 0; i < axisCount; ++i) {
					final int projection = projections[i];
					
					if (projection < minima[i]) {
						minima[i] = projection;
					}
					
					if (amplitudes[i] < projection) {
						amplitudes[i] = projection;
					}
				}
			}
			
			@Override
			public final void finishPatch() {
				// NOP
			}
			
		});
		
		for (int i = 0; i < axisCount; ++i) {
			amplitudes[i] -= minima[i];
		}
		
		debugPrint(Arrays.toString(minima));
		debugPrint(Arrays.toString(amplitudes));
		
		forEachPixel(image, null, new ProjectionProcessor(image, channels, axes) {
			
			@Override
			public final void process(final int pixel, final int[] projections) {
				assert minima[0] <= projections[0];
				assert minima[1] <= projections[1];
				
				final int p0 = adjust(projections[0], minima[0], amplitudes[0], 0, histogramProjectionRowCount - 1);
				final int p1 = adjust(projections[1], minima[1], amplitudes[1], 0, histogramProjectionColumnCount - 1);
				final int projectionIndex = p0 * histogramProjectionColumnCount + p1;
				
				histogramProjection.setValue(projectionIndex,
						histogramProjection.getValue(projectionIndex) + histogram[valueIndex(image, channels, pixel)]);
			}
			
			@Override
			public final void finishPatch() {
				// NOP
			}
			
		});
	}
	
	public static final int[] histogram(final Image image, final Channel[] channels, final int[] histogram) {
		final int[] result = histogram != null ? histogram : new int[1 << (BITS_PER_CHANNEL * channels.length)];
		
		forEachPixel(image, null, new PixelProcessor() {
			
			@Override
			public final void process(final int pixel) {
				++result[valueIndex(image, channels, pixel)];
			}
			
			@Override
			public final void finishPatch() {
				// NOP
			}
			
		});
		
		return result;
	}
	
	public static final int valueIndex(final Image image, final Channel[] channels, final int pixel) {
		final int pixelValue = image.getValue(pixel);
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << BITS_PER_CHANNEL) | channel.getValue(pixelValue);
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	public static final int BITS_PER_CHANNEL = 8;
	
	public static final int adjust(final int value,
			final int inputMinimum, final int inputAmplitude, final int outputMinimum, final int outputAmplitude) {
		return outputMinimum + (value - inputMinimum) * outputAmplitude / inputAmplitude;
	}
	
	public static final int[][] generateProjectionBasis(final int... axis) {
		final int n = axis.length;
		final int[][] result = new int[n - 1][n];
		
		for (int i = 0, k = 0; i < n; ++i) {
			if (axis[i] != 0) {
				for (int j = 0; j < n; ++j) {
					if (i != j) {
						result[k][i] = axis[j];
						result[k][j] = -axis[i];
						
						assert 0 == dot(result[k], axis);
						
						++k;
					}
				}
				
				break;
			}
		}
		
		return result;
	}
	
	public static final int dot(final int[] x, final int[] y) {
		final int n = x.length;
		
		if (n != y.length) {
			throw new IllegalArgumentException();
		}
		
		int result = 0;
		
		for (int i = 0; i < n; ++i) {
			result += x[i] * y[i];
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-19)
	 */
	public static abstract class ProjectionProcessor implements PixelProcessor {
		
		private final Image image;
		
		private final Channel[] channels;
		
		private final int[][] axes;
		
		private final int[] channelValues;
		
		private final int[] projections;
		
		protected ProjectionProcessor(final Image image, final Channel[] channels, final int[]... axes) {
			if (channels == null || axes == null) {
				throw new NullPointerException();
			}
			
			for (final int[] axis : axes) {
				if (axis == null || axis.length != channels.length) {
					throw new IllegalArgumentException();
				}
			}
			
			this.image = image;
			this.channels = channels;
			this.axes = axes;
			this.channelValues = new int[channels.length];
			this.projections = new int[axes.length];
		}
		
		@Override
		public final void process(final int pixel) {
			final int axisCount = this.axes.length;
			final int channelCount = this.channels.length;
			
			for (int i = 0; i < channelCount; ++i) {
				this.channelValues[i] = this.channels[i].getValue(this.image.getValue(pixel));
			}
			
			for (int i = 0; i < axisCount; ++i) {
				this.projections[i] = 0;
				final int[] axis = this.axes[i];
				
				for (int j = 0; j < channelCount; ++j) {
					this.projections[i] += this.channelValues[j] * axis[j];
				}
			}
			
			this.process(pixel, this.projections);
		}
		
		public abstract void process(int pixel, int[] projections);
		
		public static final Channel[] channels(final Channel... channels) {
			return channels;
		}
		
		public static final int[] axis(final int... values) {
			return values;
		}
		
	}
	
	public static final int sum(final int[] values) {
		int result = 0;
		
		for (final int value : values) {
			result += value;
		}
		
		return result;
	}
	
}
