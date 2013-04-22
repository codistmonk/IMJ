package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.TileDatabaseTest.Sampler.SampleProcessor;
import imj.apps.modules.ViewFilter.Channel;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest {
	
	@Test
	public final void test1() {
//		final Image image = ImageWrangler.INSTANCE.load("test/imj/12003.jpg");
		final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/16088-4.png");
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final int tileRowCount = 4;
		final int tileColumnCount = tileRowCount;
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final int tilePixelCount = tileRowCount * tileColumnCount;
		final Channel[] channels = { RED, GREEN, BLUE };
		final SparseHistogram histogram = new SparseHistogram();
		
		debugPrint(verticalTileCount, horizontalTileCount);
		
		final SampleProcessor processor = new SampleProcessor() {
			
			@Override
			public final void process(final int[] sample) {
				histogram.count(sample);
			}
			
		};
		
//		forEachPixelInEachTile(image, verticalTileCount, horizontalTileCount,
//				new LinearSampler(image, channels, tilePixelCount, processor));
		forEachPixelInEachTile(image, verticalTileCount, horizontalTileCount,
				new CompactHistogramSampler(image, channels, tilePixelCount, processor));
		
		debugPrint(histogram.getSampleCount());
		debugPrint(countBytes(histogram));
	}
	
	public static final long countBytes(final Serializable object) {
		final long[] result = { 0L };
		
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(new OutputStream() {
				
				@Override
				public final void write(final int b) throws IOException {
					++result[0];
				}
				
			});
			
			oos.writeObject(object);
			oos.flush();
			oos.close();
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
		
		return result[0];
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static abstract class Sampler implements PixelProcessor {
		
		private final Image image;
		
		private final Channel[] channels;
		
		private final int[] sample;
		
		private final SampleProcessor processor;
		
		protected Sampler(final Image image, final Channel[] channels, final int sampleSize, final SampleProcessor processor) {
			this.image = image;
			this.channels = channels;
			this.sample = new int[sampleSize];
			this.processor = processor;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
		public final Channel[] getChannels() {
			return this.channels;
		}
		
		public final int[] getSample() {
			return this.sample;
		}
		
		public final SampleProcessor getProcessor() {
			return this.processor;
		}
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 */
		public static abstract interface SampleProcessor {
			
			public abstract void process(int[] sample);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static final class LinearSampler extends Sampler {
		
		private final int tilePixelCount;
		
		private int i;
		
		public LinearSampler(final Image image, final Channel[] channels,
				final int tilePixelCount, final SampleProcessor processor) {
			super(image, channels, tilePixelCount * channels.length, processor);
			this.tilePixelCount = tilePixelCount;
		}
		
		@Override
		public final void process(final int pixel) {
			final int pixelValue = this.getImage().getValue(pixel);
			
			for (final Channel channel : this.getChannels()) {
				this.getSample()[this.i++] = channel.getValue(pixelValue);
			}
			
			if (this.tilePixelCount <= this.i) {
				this.i = 0;
				
				this.getProcessor().process(this.getSample());
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static final class CompactHistogramSampler extends Sampler {
		
		private final int tilePixelCount;
		
		private final Map<Integer, int[]> histogram;
		
		private final Integer[] indices;
		
		private final int[] values;
		
		private final int[] counts;
		
		private int i;
		
		public CompactHistogramSampler(final Image image, final Channel[] channels,
				final int tilePixelCount, final SampleProcessor processor) {
			super(image, channels, tilePixelCount * (channels.length + 1), processor);
			this.tilePixelCount = tilePixelCount;
			this.histogram = new HashMap<Integer, int[]>();
			this.indices = new Integer[tilePixelCount];
			this.values = new int[tilePixelCount];
			this.counts = new int[tilePixelCount];
		}
		
		private final void count(final int value) {
			int[] count = this.histogram.get(value);
			
			if (count == null) {
				count = new int[] { 1 };
				this.histogram.put(value, count);
			} else {
				++count[0];
			}
		}
		
		@Override
		public final void process(final int pixel) {
			this.count(computeIndex(this.getImage().getValue(pixel), this.getChannels()));
			
			if (this.tilePixelCount <= ++this.i) {
				this.i = 0;
//				final int m = max(this.histogram);
				
				for (int j = 0; j < this.indices.length; ++j) {
					this.indices[j] = j;
					this.values[j] = 0;
					this.counts[j] = 0;
				}
				
				{
					int j = 0;
					
					for (final Map.Entry<Integer, int[]> entry : this.histogram.entrySet()) {
						this.values[j] = entry.getKey();
						this.counts[j] = entry.getValue()[0];
						++j;
					}
					
					this.histogram.clear();
				}
				
				final int[] h = this.counts;
				
				sort(this.indices, new Comparator<Integer>() {
					
					@Override
					public final int compare(final Integer i1, final Integer i2) {
						return h[i2] - h[i1];
					}
					
				});
				
				for (int j = 0, k = 0; j < this.getSample().length; ++k) {
					final int index = this.indices[k];
					int value = this.values[index];
					final int count = this.counts[index];
					
					for (int channelIndex = this.getChannels().length - 1; 0 <= channelIndex; --channelIndex) {
						this.getSample()[j++] = value & 0x000000FF;
						value >>= 8;
					}
					
					this.getSample()[j++] = count;
				}
				
				this.getProcessor().process(this.getSample());
			}
		}
		
		public static final int max(final int[] values) {
			int result = Integer.MIN_VALUE;
			
			for (final int value : values) {
				if (result < value) {
					result = value;
				}
			}
			
			return result;
		}
		
		public static final int computeIndex(final int pixelValue, final Channel... channels) {
			int result = 0;
			
			for (final Channel channel : channels) {
				result = (result << 8) | channel.getValue(pixelValue);
			}
			
			return result;
		}
		
	}

	/**
	 * @author codistmonk (creation 2013-04-19)
	 */
	public static final class SparseHistogram implements Serializable {
		
		private final Map<Integer, Object> root;
		
		private int sampleCount;
		
		public SparseHistogram() {
			this.root = newMap();
		}
		
		public final void count(final int... sample) {
			final int n = sample.length;
			final int lastIndex = n - 1;
			Map<Integer, Object> node = this.root;
			
			for (int i = 0; i < lastIndex; ++i) {
				Map<Integer, Object> next = (Map<Integer, Object>) node.get(sample[i]);
				
				if (next == null) {
					next = newMap();
					node.put(sample[i], next);
				}
				
				node = next;
			}
			
			final int lastValue = sample[lastIndex];
			final Integer oldCount = (Integer) node.get(lastValue);
			
			if (oldCount == null) {
				node.put(lastValue, 1);
				++this.sampleCount;
			} else {
				node.put(lastValue, oldCount + 1);
			}
			
		}
		
		public final int getSampleCount() {
			return this.sampleCount;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 8212359447131338635L;
		
		private static final Map<Integer, Object> newMap() {
			return new TreeMap<Integer, Object>();
		}
		
	}
	
}
