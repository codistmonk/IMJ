package imj.apps.modules;

import static imj.apps.modules.RadixSortTest.newRandomInts;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.util.Arrays;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-27)
 */
public final class TriangulationTreeTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
		
		for (int n = 10; n <= 100000; n *= 4) {
			final int[] values = RadixSortTest.newRandomInts(n);
			final int[] sortedValues = values.clone();
			final int[] triangulationTree = values.clone();
			
			sort(sortedValues);
			makeTriangulationTree(triangulationTree);
			final long[] distances = sortByDistance(triangulationTree);
			
			debugPrint("n:", n);
			
			timer.tic();
			for (final int value : values) {
				assertEquals(value, findClosest(value, sortedValues));
			}
			debugPrint("time:", timer.toc());
			
			timer.tic();
			for (final int value : values) {
//				assertEquals(value, findClosestInTriangulationTree(value, triangulationTree));
				assertEquals(value, bkFind(value, triangulationTree, distances));
			}
			debugPrint("time:", timer.toc());
		}
	}
	
	public static final long[] sortByDistance(final int[] values) {
		final int n = values.length;
		final ValueDistance[] vds = new ValueDistance[n];
		final int v0 = values[0];
		
		for (int i = 0; i < n; ++i) {
			final int vi = values[i];
			vds[i] = new ValueDistance(vi, distance(v0, vi));
		}
		
		sort(vds);
		
		final long[] result = new long[n];
		
		for (int i = 0; i < n; ++i) {
			final ValueDistance vd = vds[i];
			values[i] = vd.getValue();
			result[i] = vd.getDistance();
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	private static final class ValueDistance implements Comparable<ValueDistance> {
		
		private final int value;
		
		private final long distance;
		
		public ValueDistance(final int value, final long distance) {
			this.value = value;
			this.distance = distance;
		}
		
		public final int getValue() {
			return this.value;
		}
		
		public final long getDistance() {
			return this.distance;
		}
		
		@Override
		public final int compareTo(final ValueDistance that) {
			int result = signum(this.getDistance() - that.getDistance());
			
			if (result == 0) {
				return this.getValue() - that.getValue();
			}
			
			return result;
		}
		
	}
	
	public static final int signum(final long value) {
		return value < 0L ? -1 : value == 0L ? 0 : +1;
	}
	
	public static final int bkFind(final int value, final int[] values, final long[] distances) {
		final int n = values.length;
		final long vv0 = distance(value, values[0]);
		int i0 = binarySearch(distances, vv0);
		int i1 = i0;
		
		if (i0 < 0) {
			i0 = min(n - 1, - (i0 + 1));
		}
		
		long vvi0 = distance(value, values[i0]);
		
		for (int j = i0 - 1; 0 <= j; --j) {
			long vjv0 = distances[j];
			long vvj = distance(value, values[j]);
			
			if (vvj < vvi0) {
				i0 = j;
				vvi0 = vvj;
			}
			
			if (vjv0 < distances[i0] - vvi0) {
				break;
			}
		}
		
		for (int j = i1 + 1; j < n; ++j) {
			long vjv0 = distances[j];
			long vvj = distance(value, values[j]);
			
			if (vvj < vvi0) {
				i0 = j;
				vvi0 = vvj;
			}
			
			if (distances[i0] + vvi0 < vjv0) {
				break;
			}
		}
		
		return values[i0];
	}
	
	public static final int findClosestInTriangulationTree(final int value, final int[] triangulationTree) {
		int end = triangulationTree.length;
		
//		debugPrint(Arrays.toString(triangulationTree));
//		debugPrint(value);
		
		if (true) return findClosestInTriangulationTree(value, triangulationTree, 0, end);
		
		int start = 0;
		int middle = (start + end) / 2;
		
		while (start < middle) {
			final long startValue = triangulationTree[start];
			
			if (value == startValue) {
				return value;
			}
			
			long d = abs(value - startValue) - abs(triangulationTree[middle] - startValue);
//			debugPrint(value, startValue, triangulationTree[middle], d);
			
			if (d < 0) {
				end = middle;
			} else {
				start = middle;
			}
			
			middle = (start + end) / 2;
		}
		
		return triangulationTree[middle];
	}
	
	private static final int findClosestInTriangulationTree(final int value, final int[] triangulationTree,
			final int start, final int end) {
		final int middle = (start + end) / 2;
//		debugPrint(start, middle, end);
		final int startValue = triangulationTree[start];
		
		if (startValue == value) {
			return value;
		}
		
		final int middleValue = triangulationTree[middle];
		
		if (value == middleValue || middle <= start) {
			return middleValue;
		}
		
		final long d = distance(startValue, value) - distance(startValue, middleValue);
		
//		debugPrint(d);
		
		if (d < 0) {
			return findClosestInTriangulationTree(value, triangulationTree, start, middle);
		}
		
		if (0 < d) {
			return findClosestInTriangulationTree(value, triangulationTree, middle, end);
		}
		
		final int left = findClosestInTriangulationTree(value, triangulationTree, start, middle);
		final int right = findClosestInTriangulationTree(value, triangulationTree, middle, end);
		final long dLeft = distance(left, value);
		final long dRight = distance(value, right);
		
//		debugPrint(left, right);
		
		return dLeft < dRight ? left : right;
	}
	
	public static final void makeTriangulationTree(final int[] values) {
		final int n = values.length;
		final IntDistant[] distants = new IntDistant[n];
		
		for (int i = 0; i < n; ++i) {
			distants[i] = new IntDistant(values, i);
		}
		
		makeTriangulationTree(distants, 0, n);
	}
	
	public static final long distance(final int x, final int y) {
		return abs((long) y - x);
	}
	
	public static final long[] distances(final int[] values) {
		final int n = values.length;
		final long[] result = new long[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = distance(values[0], values[i]);
		}
		
		return result;
	}
	
	private static final void makeTriangulationTree(final IntDistant[] distants, final int start, final int end) {
		for (int i = start; i < end; ++i) {
			distants[i].setSource(start);
		}
		
		sort(distants, start, end);
		
		for (int i = start; i < end; ++i) {
			distants[i].update(i);
		}
		
//		debugPrint(Arrays.toString(distants));
//		debugPrint(start, end);
		
		final int middle = (start + end) / 2;
		final int quarter = (start + middle) / 2;
		
		if (1 < middle - quarter) {
			makeTriangulationTree(distants, quarter, middle);
		}
		
		if (1 < end - middle) {
			makeTriangulationTree(distants, middle, end);
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	public static abstract interface Distant extends Comparable<Distant> {
		
		public abstract void setSource(int sourceIndex);
		
		public abstract long getDistance();
		
		public abstract void update(int index);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	public static final class IntDistant implements Distant {
		
		private final int[] values;
		
		private final int value;
		
		private long distance;
		
		public IntDistant(final int[] values, final int index) {
			this.values = values;
			this.value = values[index];
		}
		
		@Override
		public final void setSource(final int sourceIndex) {
			this.distance = abs((long) this.value - this.values[sourceIndex]);
		}
		
		@Override
		public final long getDistance() {
			return this.distance;
		}
		
		@Override
		public final int compareTo(final Distant that) {
			final long d = this.getDistance() - that.getDistance();
			
			return d < 0 ? -1 : d == 0 ? 0 : +1;
		}
		
		@Override
		public final void update(final int index) {
			this.values[index] = this.value;
		}
		
		public final String toString() {
			return "(" + this.getDistance() + ":" + this.value + ")";
		}
		
	}
	
	public static final int findClosest(final int value, final int[] values) {
		long closestDistance = Long.MAX_VALUE;
		int result = 0;
		
		for (final int v : values) {
			final long d = abs((long) value - v);
			
			if (d < closestDistance) {
				closestDistance = d;
				result = v;
			}
		}
		
		return result;
	}
	
}
