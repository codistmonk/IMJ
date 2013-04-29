package imj.database;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.sort;

import java.util.Comparator;

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
		final IntDistant[] vds = new IntDistant[n];
		final int v0 = values[0];
		
		for (int i = 0; i < n; ++i) {
			final int vi = values[i];
			vds[i] = new IntDistant(vi, distance(v0, vi));
		}
		
		sort(vds);
		
		final long[] result = new long[n];
		
		for (int i = 0; i < n; ++i) {
			final IntDistant vd = vds[i];
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
	
	public static final <T> long[] bkSort(final T[] values, final Metric<T> distance, final Comparator<T> comparator) {
		final int n = values.length;
		@SuppressWarnings("unchecked")
		final GenericDistant<T>[] vds = new GenericDistant[n];
		final T v0 = values[0];
		
		for (int i = 0; i < n; ++i) {
			final T vi = values[i];
			vds[i] = new GenericDistant<T>(vi, distance.getDistance(v0, vi), comparator);
		}
		
		sort(vds);
		
		final long[] result = new long[n];
		
		for (int i = 0; i < n; ++i) {
			final GenericDistant<T> vd = vds[i];
			values[i] = vd.getValue();
			result[i] = vd.getDistance();
		}
		
		return result;
	}
	
	public static final <T> T bkFind(final T value, final T[] bkValues, final long[] bkDistances, final Metric<T> distance) {
		final int n = bkValues.length;
		final long vv0 = distance.getDistance(value, bkValues[0]);
		int i0 = binarySearch(bkDistances, vv0);
		
		if (i0 < 0) {
			i0 = min(n - 1, - (i0 + 1));
		}
		
		final int i1 = i0;
		long vvi0 = distance.getDistance(value, bkValues[i0]);
		
		for (int j = i0 - 1; 0 <= j; --j) {
			long vjv0 = bkDistances[j];
			long vvj = distance.getDistance(value, bkValues[j]);
			
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
			long vvj = distance.getDistance(value, bkValues[j]);
			
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
			final long d = distance(value, v);
			
			if (d < closestDistance) {
				closestDistance = d;
				result = v;
			}
		}
		
		return result;
	}
	
	public static final <T> T findClosest(final T value, final T[] values, final Metric<T> distance) {
		long closestDistance = Long.MAX_VALUE;
		T result = null;
		
		for (final T v : values) {
			final long d = distance.getDistance(value, v);
			
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
	private static final class IntDistant implements Comparable<IntDistant> {
		
		private final int value;
		
		private final long distance;
		
		public IntDistant(final int value, final long distance) {
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
		public final int compareTo(final IntDistant that) {
			int result = signum(this.getDistance() - that.getDistance());
			
			if (result == 0) {
				return this.getValue() - that.getValue();
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	private static final class GenericDistant<T> implements Comparable<GenericDistant<T>> {
		
		private final T value;
		
		private final long distance;
		
		private final Comparator<T> comparator;
		
		public GenericDistant(final T value, final long distance, final Comparator<T> comparator) {
			this.value = value;
			this.distance = distance;
			this.comparator = comparator;
		}
		
		public final T getValue() {
			return this.value;
		}
		
		public final long getDistance() {
			return this.distance;
		}
		
		@Override
		public final int compareTo(final GenericDistant<T> that) {
			int result = signum(this.getDistance() - that.getDistance());
			
			if (result == 0) {
				return this.comparator.compare(this.getValue(), that.getValue());
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 *
	 * @param <T>
	 */
	public static abstract interface Metric<T> {
		
		public abstract long getDistance(T object0, T object1);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 *
	 * @param <T>
	 */
	public static final class BKDatabase<T> {
		
		private final T[] values;
		
		private final long[] distances;
		
		private final Metric<T> metric;
		
		public BKDatabase(final T[] values, final Metric<T> distance, final Comparator<T> comparator) {
			this.values = values;
			this.distances = bkSort(values, distance, comparator);
			this.metric = distance;
		}
		
		public final T[] getValues() {
			return this.values;
		}
		
		public final long[] getDistances() {
			return this.distances;
		}
		
		public final Metric<T> getMetric() {
			return this.metric;
		}
		
		public final T findClosest(final T value) {
			return bkFind(value, this.getValues(), this.getDistances(), this.getMetric());
		}
		
	}
	
}
