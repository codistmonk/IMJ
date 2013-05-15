package imj;

import static imj.IMJTools.forEachPixelInEachComponent4b;
import static java.lang.Long.bitCount;
import static java.lang.Long.toBinaryString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import imj.IMJTools.PixelProcessor;
import imj.apps.modules.RegionOfInterest;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-05-15)
 */
public class IMJToolsTest {
	
	@Test
	public final void testComponentIteration1() {
		final Image roi = roi(3,
				1, 0, 1,
				0, 0, 0,
				1, 0, 1);
		
		if (true) {
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4b(roi, false, counter);
			
			assertArrayEquals(new int[] { 1, 1, 1, 1 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4b(roi, true, counter);
			
			assertArrayEquals(new int[] { 4, 2, 2, 1 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration2() {
		final Image roi = roi(3,
				1, 0, 1,
				1, 1, 0,
				1, 0, 1);
		
		if (false) {
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4b(roi, false, counter);
			
			assertArrayEquals(new int[] { 4, 1, 1 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4b(roi, true, counter);
			
			assertArrayEquals(new int[] { 6, 2, 1 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration3() {
		for (int n = 1; n <= 4; ++n) {
			testAllBinarySquares(n);
		}
	}
	
	public static final void testAllBinarySquares(final int n) {
		for (long i = 0L; i < ~((~0L) << (n * n)); ++i) {
			final RegionOfInterest roi = new RegionOfInterest.UsingBitSet(n, n, false);
			
			for (int j = 0; j < n * n; ++j) {
				if (0L != ((i >> (n * n - 1L - j)) & 1L)) {
					roi.set(j);
				}
			}
			
			{
				final PixelCounter counter = new PixelCounter();
				
				forEachPixelInEachComponent4b(roi, false, counter);
				
				assertEquals("n: " + n + ", i: " + toBinaryString(i), bitCount(i), counter.getPixelCount());
			}
			
			{
				final PixelCounter counter = new PixelCounter();
				
				forEachPixelInEachComponent4b(roi, true, counter);
				
				assertEquals("n: " + n + ", i:" + toBinaryString(i), roi.getPixelCount(), counter.getPixelCount());
			}
		}
	}
	
	public static final RegionOfInterest.UsingBitSet roi(final int columnCount, final int... values) {
		final int pixelCount = values.length;
		final int rowCount = pixelCount / columnCount;
		
		if (pixelCount != rowCount * columnCount) {
			throw new IllegalArgumentException();
		}
		
		final RegionOfInterest.UsingBitSet result = new RegionOfInterest.UsingBitSet(rowCount, columnCount);
		int i = 0;
		
		for (final int value : values) {
			result.set(i++, value != 0);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-05-15)
	 */
	public static final class PixelCounter implements PixelProcessor {
		
		private final IntList counts;
		
		private int pixelCount;
		
		private int componentIndex;
		
		public PixelCounter() {
			this.counts = new IntList();
		}
		
		public final int getPixelCount() {
			return this.pixelCount;
		}
		
		public final void setPixelCount(int pixelCount) {
			this.pixelCount = pixelCount;
		}

		public final IntList getCounts() {
			return this.counts;
		}
		
		@Override
		public final void process(final int pixel) {
			++this.pixelCount;
			
			while (this.getCounts().size() <= this.componentIndex) {
				this.getCounts().add(0);
			}
			
			this.getCounts().set(this.componentIndex, this.getCounts().get(this.componentIndex) + 1);
		}
		
		@Override
		public final void finishPatch() {
			++this.componentIndex;
		}
		
	}
	
}
