package imj3.draft.machinelearning;

import static java.lang.Math.rint;

import imj2.tools.BitwiseQuantizationTest.DoubleArrayComparator;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author codistmonk (creation 2015-02-12)
 */
public final class Histogram implements Serializable {
	
	private final Map<double[], double[]> counts = new TreeMap<>(new DoubleArrayComparator());
	
	private long totalCount;
	
	public final Map<double[], double[]> getCounts() {
		return this.counts;
	}
	
	public final long getTotalCount() {
		return this.totalCount;
	}
	
	public final Histogram reset() {
		this.getCounts().clear();
		this.totalCount = 0L;
		
		return this;
	}
	
	public final Histogram add(final double... input) {
		++this.getCounts().computeIfAbsent(input, i -> new double[1])[0];
		++this.totalCount;
		
		return this;
	}
	
	public final Histogram add(final DataSource inputs) {
		for (final Datum classification : inputs) {
			this.add(classification.getValue().clone());
		}
		
		return this;
	}
	
	public final Histogram normalize() {
		final long n = this.getTotalCount();
		
		if (0L < n) {
			for (final double[] count : this.getCounts().values()) {
				count[0] /= n;
			}
		}
		
		return this;
	}
	
	public final Histogram denormalize() {
		final long n = this.getTotalCount();
		
		if (0L < n) {
			for (final double[] count : this.getCounts().values()) {
				count[0] = rint(count[0] * n);
			}
		}
		
		return this;
	}
	
	public final double[] pack(final int binCount) {
		final double[] result = new double[binCount];
		
		for (final Map.Entry<double[], double[]> entry : this.getCounts().entrySet()) {
			final double[] key = entry.getKey();
			
			if (key.length != 1) {
				throw new IllegalArgumentException();
			}
			
			result[(int) key[0]] = entry.getValue()[0];
		}
		
		return result;
	}
	
	private static final long serialVersionUID = -4974336898629198663L;
	
}