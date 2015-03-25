package imj3.tools;

import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.instances;

import java.io.Serializable;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2014-03-07)
 */
public final class VectorStatistics implements Serializable {
	
	private final Statistics[] statistics;
	
	public VectorStatistics(final int dimension) {
		this.statistics = instances(dimension, STATISTICS_FACTORY);
	}
	
	public final void reset() {
		for (final Statistics statistics : this.getStatistics()) {
			statistics.reset();
		}
	}
	
	public final Statistics[] getStatistics() {
		return this.statistics;
	}
	
	public final void addValues(final double... values) {
		final int n = this.getStatistics().length;
		
		for (int i = 0; i < n; ++i) {
			this.getStatistics()[i].addValue(values[i]);
		}
	}
	
	public final double[] getNormalizedValues(final double... values) {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getNormalizedValue(values[i]);
		}
		
		return result;
	}
	
	public final double[] getDenormalizedValues(final double... values) {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getDenormalizedValue(values[i]);
		}
		
		return result;
	}
	
	public final double getCount() {
		return this.getStatistics()[0].getCount();
	}
	
	public final double[] getMeans() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getMean();
		}
		
		return result;
	}
	
	public final double[] getMinima() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getMinimum();
		}
		
		return result;
	}
	
	public final double[] getMaxima() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getMaximum();
		}
		
		return result;
	}
	
	public final double[] getVariances() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getStatistics()[i].getVariance();
		}
		
		return result;
	}
	
	public final double[] getStandardDeviations() {
		final int n = this.getStatistics().length;
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = sqrt(this.getStatistics()[i].getVariance());
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8371199963847573845L;
	
	public static final DefaultFactory<Statistics> STATISTICS_FACTORY = DefaultFactory.forClass(Statistics.class);
	
}
