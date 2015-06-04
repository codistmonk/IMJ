package imj;

import static imj.IMJTools.forEachPixelInEachComponent4;
import static java.lang.Long.bitCount;
import static java.lang.Long.toBinaryString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import imj.IMJTools.PixelProcessor;
import imj.apps.modules.RegionOfInterest;

import multij.primitivelists.IntList;

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
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, false, counter);
			
			assertArrayEquals(new int[] { 1, 1, 1, 1 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, true, counter);
			
			assertArrayEquals(new int[] { 4, 2, 2, 1 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration2() {
		final Image roi = roi(3,
				1, 0, 1,
				1, 1, 0,
				1, 0, 1);
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, false, counter);
			
			assertArrayEquals(new int[] { 4, 1, 1 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, true, counter);
			
			assertArrayEquals(new int[] { 6, 2, 1 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration3() {
		final Image roi = roi(3,
				1, 0, 0,
				1, 0, 1,
				1, 1, 1);
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, false, counter);
			
			assertArrayEquals(new int[] { 6 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, true, counter);
			
			assertArrayEquals(new int[] { 9 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration4() {
		final Image roi = roi(4,
				1, 0, 0, 1,
				0, 1, 0, 1,
				0, 0, 1, 1,
				0, 0, 0, 0);
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, false, counter);
			
			assertArrayEquals(new int[] { 1, 4, 1 }, counter.getCounts().toArray());
		}
		
		{
			final PixelCounter counter = new PixelCounter();
			
			forEachPixelInEachComponent4(roi, true, counter);
			
			assertArrayEquals(new int[] { 6, 6, 4 }, counter.getCounts().toArray());
		}
	}
	
	@Test
	public final void testComponentIteration5() {
		for (int n = 1; n <= 4; ++n) {
			testAllBinarySquares(n);
		}
	}
	
	public static final void testAllBinarySquares(final int n) {
		for (long i = 0L; i < ~((~0L) << (n * n)); ++i) {
			final RegionOfInterest roi = newROI(n, i);
			final int componentCount;
			
			{
				final PixelCounter counter = new PixelCounter();
				
				forEachPixelInEachComponent4(roi, false, counter);
				
				assertEquals("n: " + n + ", i: " + toBinaryString(i), bitCount(i), counter.getPixelCount());
				
				componentCount = counter.getCounts().size();
			}
			
			{
				final PixelCounter counter = new PixelCounter();
				
				forEachPixelInEachComponent4(roi, true, counter);
				
				assertEquals("n: " + n + ", i:" + toBinaryString(i), roi.getPixelCount(), counter.getPixelCount());
				
				if (0L != (i >> (n * n - 1L))) {
					assertEquals("n: " + n + ", i:" + toBinaryString(i), componentCount, counter.getCounts().size());
				} else {
					assertEquals("n: " + n + ", i:" + toBinaryString(i), componentCount + 1, counter.getCounts().size());
				}
			}
		}
	}
	
	public static final RegionOfInterest newROI(final int n, long bits) {
		final RegionOfInterest result = new RegionOfInterest.UsingBitSet(n, n, false);
		
		for (int i = 0; i < n * n; ++i) {
			if (0L != ((bits >> (n * n - 1L - i)) & 1L)) {
				result.set(i);
			}
		}
		
		return result;
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
