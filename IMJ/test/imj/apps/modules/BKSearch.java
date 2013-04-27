package imj.apps.modules;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-04-28)
 */
public final class BKSearch {
	
	private BKSearch() {
		throw new IllegalInstantiationException();
	}
	
	public static final long[] bkSort(final int[] values) {
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
	
	public static final int bkFind(final int value, final int[] bkValues, final long[] bkDistances) {
		final int n = bkValues.length;
		final long vv0 = distance(value, bkValues[0]);
		int i0 = binarySearch(bkDistances, vv0);
		
		if (i0 < 0) {
			i0 = min(n - 1, - (i0 + 1));
		}
		
		final int i1 = i0;
		long vvi0 = distance(value, bkValues[i0]);
		
		for (int j = i0 - 1; 0 <= j; --j) {
			long vjv0 = bkDistances[j];
			long vvj = distance(value, bkValues[j]);
			
			if (vvj < vvi0) {
				i0 = j;
				vvi0 = vvj;
			}
			
			if (vjv0 < bkDistances[i0] - 2 * vvi0) {
				break;
			}
		}
		
		for (int j = i1 + 1; j < n; ++j) {
			long vjv0 = bkDistances[j];
			long vvj = distance(value, bkValues[j]);
			
			if (vvj < vvi0) {
				i0 = j;
				vvi0 = vvj;
			}
			
			if (bkDistances[i0] + 2 * vvi0 < vjv0) {
				break;
			}
		}
		
		return bkValues[i0];
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
	
	public static final int signum(final long value) {
		return value < 0L ? -1 : value == 0L ? 0 : +1;
	}
	
	public static final long distance(final int x, final int y) {
		return abs((long) y - x);
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
	
}
