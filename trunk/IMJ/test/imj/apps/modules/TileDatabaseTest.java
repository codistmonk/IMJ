package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.ViewFilter.Channel;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
		
		forEachPixelInEachTile(image, verticalTileCount, horizontalTileCount, new PixelProcessor() {
			
			private final int[] sample = new int[tilePixelCount * channels.length];
			
			private int i;
			
			@Override
			public final void process(final int pixel) {
				final int pixelValue = image.getValue(pixel);
				
				for (final Channel channel : channels) {
					this.sample[this.i++] = channel.getValue(pixelValue);
				}
				
				if (this.sample.length <= this.i) {
					this.i = 0;
					
					histogram.count(this.sample);
				}
			}
			
		});
		
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
	 * @author codistmonk (creation 2013-04-19)
	 */
	public static final class SparseHistogram implements Serializable {
		
		private final Map<Integer, Object> data;
		
		private int sampleCount;
		
		public SparseHistogram() {
			this.data = newMap();
		}
		
		public final void count(final int... sample) {
			final int n = sample.length;
			final int lastIndex = n - 1;
			Map<Integer, Object> node = this.data;
			
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
