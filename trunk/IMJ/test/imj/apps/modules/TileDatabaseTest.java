package imj.apps.modules;

import static imj.IMJTools.deepToString;
import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static java.util.Arrays.sort;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.assertEquals;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.TileDatabaseTest.PrefixTree.Value;
import imj.apps.modules.TileDatabaseTest.Sampler.SampleProcessor;
import imj.apps.modules.ViewFilter.Channel;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest {
	
	@Test
	public final void test1() {
		final Image image = ImageWrangler.INSTANCE.load("test/imj/12003.jpg");
//		final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/16088-4.png");
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		final int tileRowCount = 3;
		final int tileColumnCount = tileRowCount;
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final int tilePixelCount = tileRowCount * tileColumnCount;
		final Channel[] channels = { RED, GREEN, BLUE };
		final PrefixTree dictionary = new PrefixTree();
		
		debugPrint(verticalTileCount, horizontalTileCount);
		
		final SampleProcessor processor = new SampleProcessor() {
			
			@Override
			public final void process(final byte[] sample) {
				dictionary.add(sample);
			}
			
		};
		
		final int verticalTileStride = 1;
		final int horizontalTileStride = verticalTileStride;
		
		final TicToc timer = new TicToc();
		timer.tic();
//		forEachPixelInEachTile(image, verticalTileCount, horizontalTileCount,
//				new LinearSampler(image, channels, tilePixelCount, processor));
//		forEachPixelInEachTile(image, verticalTileCount, horizontalTileCount,
//				new CompactHistogramSampler(image, channels, tilePixelCount, processor));
		forEachPixelInEachTile(image, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				new CompactHistogramSampler(image, channels, tilePixelCount, processor));
		debugPrint("time:", timer.toc());
		
		debugPrint(dictionary.getValueCount());
		debugPrint(countBytes(dictionary));
		
		final int[] dictionaryEntryCount = { 0 };
		final int[] dictionarySampleCount = { 0 };
		
		for (final Map.Entry<byte[], ? extends Value> entry : dictionary) {
			assertNotNull(entry.getValue());
			++dictionaryEntryCount[0];
			dictionarySampleCount[0] += entry.getValue().getCount();
		}
		
		assertEquals(dictionary.getValueCount(), dictionaryEntryCount[0]);
		// k * verticalTileStride + tileRowCount <= imageRowCount
		// k * verticalTileStride <= imageRowCount - tileRowCount
		// k <= (imageRowCount - tileRowCount) / verticalTileStride
		// k = (imageRowCount - tileRowCount) / verticalTileStride
		assertEquals(((imageRowCount + verticalTileStride - tileRowCount) / verticalTileStride) *
				((imageColumnCount + horizontalTileStride - tileColumnCount) / horizontalTileStride), dictionarySampleCount[0]);
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
		
		private final byte[] sample;
		
		private final SampleProcessor processor;
		
		protected Sampler(final Image image, final Channel[] channels, final int sampleSize, final SampleProcessor processor) {
			this.image = image;
			this.channels = channels;
			this.sample = new byte[sampleSize];
			this.processor = processor;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
		public final Channel[] getChannels() {
			return this.channels;
		}
		
		public final byte[] getSample() {
			return this.sample;
		}
		
		public final SampleProcessor getProcessor() {
			return this.processor;
		}
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 */
		public static abstract interface SampleProcessor {
			
			public abstract void process(byte[] sample);
			
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
				this.getSample()[this.i++] = (byte) channel.getValue(pixelValue);
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
		
		private final void count(final Integer value) {
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
				
				this.postprocessHistogram();
				this.sortIndices();
				this.updateSample();
				
				this.getProcessor().process(this.getSample());
			}
		}
		
		private final void updateSample() {
			final int m = max(this.counts);
			
			for (int j = 0, k = 0; j < this.getSample().length; ++k) {
				final int index = this.indices[k];
				int value = this.values[index];
				final int count = this.counts[index];
				
				for (int channelIndex = this.getChannels().length - 1; 0 <= channelIndex; --channelIndex) {
					this.getSample()[j++] = (byte) (value & 0x000000FF);
					value >>= 8;
				}
				
				this.getSample()[j++] = (byte) (count * 255L / m);
			}
		}
		
		private final void sortIndices() {
			final int[] h = this.counts;
			
			sort(this.indices, new Comparator<Integer>() {
				
				@Override
				public final int compare(final Integer i1, final Integer i2) {
					return h[i2] - h[i1];
				}
				
			});
		}
		
		private final void postprocessHistogram() {
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
	public static final class PrefixTree implements Serializable, Iterable<Map.Entry<byte[], ? extends PrefixTree.Value>> {
		
		private final Map<Byte, Object> root;
		
		private final Class<? extends Value> valueFactory;
		
		private int sampleCount;
		
		public PrefixTree() {
			this(Value.Default.class);
		}
		
		public PrefixTree(final Class<? extends Value> valueFactory) {
			this.root = newTree();
			this.valueFactory = valueFactory;
		}
		
		@Override
		public final Iterator<Entry<byte[], ? extends Value>> iterator() {
			final int d = getDepth(this.root);
			final MutableEntry<byte[], Value> entry = new MutableEntry<byte[], Value>(new byte[d]);
			final List<Iterator<Entry<Byte, Object>>> todo = new ArrayList<Iterator<Entry<Byte, Object>>>();
			
			todo.add(this.root.entrySet().iterator());
			
			return new Iterator<Map.Entry<byte[], ? extends Value>>() {
				
				@Override
				public final boolean hasNext() {
					return !todo.isEmpty();
				}
				
				@Override
				public final Entry<byte[], ? extends Value> next() {
					Entry<Byte, Object> nodeEntry = todo.get(0).next();
					entry.getKey()[todo.size() - 1] = nodeEntry.getKey().byteValue();
					
					while (todo.size() < d) {
						Map<Byte, Object> subTree = (Map<Byte, Object>) nodeEntry.getValue();
						todo.add(0, subTree.entrySet().iterator());
						nodeEntry = todo.get(0).next();
						entry.getKey()[todo.size() - 1] = nodeEntry.getKey().byteValue();
					}
					
					entry.setValue((Value) nodeEntry.getValue());
					
					while (!todo.isEmpty() && !todo.get(0).hasNext()) {
						todo.remove(0);
					}
					
					return entry;
				}
				
				@Override
				public final void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 *
		 * @param <K>
		 * @param <V>
		 */
		private static final class MutableEntry<K, V> implements Map.Entry<K, V> {
			
			private final K key;
			
			private V value;
			
			public MutableEntry(final K key) {
				this.key = key;
			}
			
			@Override
			public final K getKey() {
				return this.key;
			}
			
			@Override
			public final V getValue() {
				return this.value;
			}
			
			@Override
			public final V setValue(final V value) {
				final V result = this.value;
				this.value = value;
				
				return result;
			}
			
			public final String toString() {
				return deepToString(this.getKey()) + "=" + this.getValue();
			}
			
		}
		
		public final void add(final byte[] key) {
			final int n = key.length;
			final int lastIndex = n - 1;
			Map<Byte, Object> node = this.root;
			
			for (int i = 0; i < lastIndex; ++i) {
				node = getOrCreateSubTree(node, key[i]);
			}
			
			final Byte lastValue = key[lastIndex];
			final Value leaf = (Value) node.get(lastValue);
			
			if (leaf == null) {
				try {
					node.put(lastValue, this.valueFactory.newInstance());
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
				
				++this.sampleCount;
			} else {
				leaf.incrementCount();
			}
		}
		
		public final int getValueCount() {
			return this.sampleCount;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 8212359447131338635L;
		
		private static final <K> Map<K, Object> newTree() {
			return new TreeMap<K, Object>();
		}
		
		public static final <K> int getDepth(final Object root) {
			final Map<K, Object> node = cast(Map.class, root);
			
			return node == null ? 0 : 1 + getDepth(node.values().iterator().next());
		}
		
		public static final <K> Map<K, Object> getOrCreateSubTree(final Map<K, Object> node, final K key) {
			Map<K, Object> result = (Map<K, Object>) node.get(key);
			
			if (result == null) {
				result = newTree();
				node.put(key, result);
			}
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 */
		public static abstract interface Value extends Serializable {
			
			public abstract int getCount();
			
			public abstract void incrementCount();
			
			/**
			 * @author codistmonk (creation 2013-04-22)
			 */
			public static final class Default implements Value {
				
				private int count = 1;
				
				@Override
				public final int getCount() {
					return this.count;
				}
				
				@Override
				public final void incrementCount() {
					++this.count;
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8585019119527978654L;
				
			}
			
		}
		
	}
	
}
