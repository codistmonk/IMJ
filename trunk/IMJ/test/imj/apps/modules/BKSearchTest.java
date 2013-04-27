package imj.apps.modules;

import static imj.apps.modules.BKSearch.bkFind;
import static imj.apps.modules.BKSearch.bkSort;
import static imj.apps.modules.BKSearch.distance;
import static imj.apps.modules.BKSearch.findClosest;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;

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
			
			final long[] distances = bkSort(bkValues);
			
			debugPrint("n:", n);
			
			timer.tic();
			for (final int value : values) {
				assertEquals(value, findClosest(value, sortedValues));
			}
			debugPrint("time:", timer.toc());
			
			timer.tic();
			for (final int value : values) {
				assertEquals(value, bkFind(value, bkValues, distances));
			}
			debugPrint("time:", timer.toc());
			
			for (final int value : moreValues) {
				final int linearClosest = findClosest(value, sortedValues);
				final int bkClosest = bkFind(value, bkValues, distances);
				final long expected = distance(value, linearClosest);
				final long actual = distance(value, bkClosest);
				
				assertEquals(expected, actual);
			}
		}
	}
	
}
