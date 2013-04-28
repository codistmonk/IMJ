package imj.apps.modules;

import static imj.apps.modules.BKSearch.bkFind;
import static imj.apps.modules.BKSearch.bkSort;
import static imj.apps.modules.BKSearch.distance;
import static imj.apps.modules.BKSearch.findClosest;
import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import imj.apps.modules.BKSearch.Distance;

import java.util.Arrays;
import java.util.Random;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-27)
 */
public final class BKSearchTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
		
		for (int n = 10; n <= 100000; n *= 4) {
			final int[] values = RadixSortTest.newRandomInts(n);
			final int[] moreValues = RadixSortTest.newRandomInts(n);
			final int[] sortedValues = values.clone();
			final int[] bkValues = values.clone();
			
			sort(sortedValues);
			
			final long[] bkDistances = bkSort(bkValues);
			
			debugPrint("n:", n);
			
			timer.tic();
			for (final int value : values) {
				assertEquals(value, findClosest(value, sortedValues));
			}
			debugPrint("time:", timer.toc());
			
			timer.tic();
			for (final int value : values) {
				assertEquals(value, bkFind(value, bkValues, bkDistances));
			}
			debugPrint("time:", timer.toc());
			
			for (final int value : moreValues) {
				final int linearClosest = findClosest(value, sortedValues);
				final int bkClosest = bkFind(value, bkValues, bkDistances);
				final long expected = distance(value, linearClosest);
				final long actual = distance(value, bkClosest);
				
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public final void test2() {
		final TicToc timer = new TicToc();
		final EuclideanDistance distance = EuclideanDistance.INSTANCE;
		
		for (int n = 10; n <= 20000; n *= 2) {
			final byte[][] values = newRandomSamples(2, n);
			final byte[][] moreValues = newRandomSamples(2, n);
//			final byte[][] values = {{65, 71}, {-59, -84}, {-31, 69}, {47, 52}, {-3, 119}, {104, -91}};
//			final byte[][] moreValues = {{107, -34}, {27, -35}, {-101, -74}, {-120, -36}, {-97, -42}, {-88, -85}};
			
			final byte[][] sortedValues = clone(values);
			final byte[][] bkValues = clone(values);
			
			sort(sortedValues, ByteArrayComparator.INSTANCE);
			
			final long[] bkDistances = bkSort(bkValues, distance, ByteArrayComparator.INSTANCE);
			
			debugPrint("n:", n);
			
			timer.tic();
			for (final byte[] value : values) {
				assertArrayEquals(value, findClosest(value, sortedValues, distance));
			}
			debugPrint("time:", timer.toc());
			
			timer.tic();
			for (final byte[] value : values) {
				assertArrayEquals(value, bkFind(value, bkValues, bkDistances, distance));
			}
			debugPrint("time:", timer.toc());
			
			for (final byte[] value : moreValues) {
				final byte[] linearClosest = findClosest(value, sortedValues, distance);
				final byte[] bkClosest = bkFind(value, bkValues, bkDistances, distance);
				final long expected = distance.getDistance(value, linearClosest);
				final long actual = distance.getDistance(value, bkClosest);
				
				if (expected != actual) {
					debugPrint(Arrays.deepToString(values));
					debugPrint(Arrays.deepToString(moreValues));
					debugPrint(Arrays.deepToString(bkValues));
					debugPrint(Arrays.toString(bkDistances));
					debugPrint(Arrays.toString(value), distance.getDistance(value, bkValues[0]));
					debugPrint(Arrays.toString(linearClosest), expected);
					debugPrint(Arrays.toString(bkClosest), actual);
				}
				
				assertEquals(expected, actual);
			}
		}
	}
	
	public static final byte[][] clone(final byte[][] array) {
		final byte[][] result = new byte[array.length][array[0].length];
		
		for (int i = 0; i < result.length; ++i) {
			result[i] = array[i].clone();
		}
		
		return result;
	}
	
	public static final byte[][] newRandomSamples(final int dimension, final int sampleCount) {
		final Random random = new Random();
		final byte[][] result = new byte[sampleCount][dimension];
		
		for (final byte[] sample : result) {
			random.nextBytes(sample);
		}
		
		return result;
	}
	
	public static final long square(final int x) {
		return (long) x * x;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class EuclideanDistance implements Distance<byte[]> {
		
		@Override
		public final long getDistance(final byte[] sample0, final byte[] sample1) {
			long result = 0L;
			final int n = sample0.length;
			
			for (int i = 0; i < n; ++i) {
				result += square(sample1[i] - sample0[i]);
			}
			
			return (long) ceil(sqrt(result));
		}
		
		public static final EuclideanDistance INSTANCE = new EuclideanDistance();
		
	}
	
}
