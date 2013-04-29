package imj.database;

import static imj.database.BKSearch.bkFind;
import static imj.database.BKSearch.bkSort;
import static imj.database.BKSearch.distance;
import static imj.database.BKSearch.findClosest;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import imj.apps.modules.RadixSortTest;
import imj.database.BKSearch.BKDatabase;
import imj.database.IMJDatabaseTools.EuclideanMetric;

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
		for (int n = 10; n <= 20000; n *= 2) {
			testGenericBKSearch(2, n);
		}
	}
	
	@Test
	public final void test3() {
		for (int n = 10; n <= 20000; n *= 2) {
			testGenericBKSearch(10, n);
		}
	}
	
	public static final void testGenericBKSearch(final int dimension, int n) {
		final TicToc timer = new TicToc();
		final EuclideanMetric distance = EuclideanMetric.INSTANCE;
		final byte[][] values = newRandomSamples(dimension, n);
		final byte[][] moreValues = newRandomSamples(dimension, n);
		final byte[][] sortedValues = clone(values);
		final BKDatabase<byte[]> database = new BKDatabase<byte[]>(clone(values), distance, ByteArrayComparator.INSTANCE);
		
		sort(sortedValues, ByteArrayComparator.INSTANCE);
		
		debugPrint("n:", n);
		
		timer.tic();
		for (final byte[] value : values) {
			assertArrayEquals(value, findClosest(value, sortedValues, distance));
		}
		debugPrint("time:", timer.toc());
		
		timer.tic();
		for (final byte[] value : values) {
			assertArrayEquals(value, database.findClosest(value));
		}
		debugPrint("time:", timer.toc());
		
		for (final byte[] value : moreValues) {
			final byte[] linearClosest = findClosest(value, sortedValues, distance);
			final byte[] bkClosest = database.findClosest(value);
			final long expected = distance.getDistance(value, linearClosest);
			final long actual = distance.getDistance(value, bkClosest);
			
			if (expected != actual) {
				debugPrint(Arrays.deepToString(values));
				debugPrint(Arrays.deepToString(moreValues));
				debugPrint(Arrays.toString(value), distance.getDistance(value, database.getValues()[0]));
				debugPrint(Arrays.toString(linearClosest), expected);
				debugPrint(Arrays.toString(bkClosest), actual);
			}
			
			assertEquals(expected, actual);
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
	
}
