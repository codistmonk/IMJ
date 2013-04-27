package imj.apps.modules;

import static java.util.Collections.shuffle;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-27)
 */
public final class RadixSortTest {
	
	@Test
	public final void test1() {
		testSortInts(100);
	}
	
	@Test
	public final void test2() {
		testSortInts(1000000);
	}
	
	public static final void testSortInts(final int n) {
		final TicToc timer = new TicToc();
		final int[] values = newRandomInts(n);
		
		final int[] expecteds = values.clone();
		timer.tic();
		Arrays.sort(expecteds);
		debugPrint("time:", timer.toc());
		
		final int[] actuals = values.clone();
		timer.tic();
		RadixSort.sort(actuals);
		debugPrint("time:", timer.toc());
		
		assertArrayEquals(expecteds, actuals);
	}
	
	public static final int[] newRandomInts(final int n) {
		final Random random = new Random();
		final int[] result = new int[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = random.nextInt();
		}
		
		return result;
	}
	
	public static final int[] newRandomizedRange(final int n) {
		final int[] result = new int[n];
		final List<Integer> values = new ArrayList<Integer>(n);
		
		for (int i = 0; i < n; ++i) {
			values.add(i);
		}
		
		shuffle(values);
		
		for (int i = 0; i < n; ++i) {
			result[i] = values.get(i);
		}
		
		return result;
	}
	
}
