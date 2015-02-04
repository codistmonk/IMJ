package imj3.draft.segmentation2;

import imj3.draft.segmentation2.NearestNeighborClassifier.Prototype;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class ClusteringExperiment {
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final int d = 2;
		final int n = 10;
		final int k = 2;
		final DataSource<Prototype> inputs = new RandomPrototypeSource(d, n, 0L);
		
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, k).cluster(inputs), inputs));
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class RandomPrototypeSource implements DataSource<Prototype> {
		
		private final int dimension;
		
		private final int size;
		
		private final long seed;
		
		public RandomPrototypeSource(final int dimension, final int size, final long seed) {
			this.dimension = dimension;
			this.size = size;
			this.seed = seed;
		}
		
		public final long getSeed() {
			return this.seed;
		}
		
		@Override
		public final Iterator<Classification<Prototype>> iterator() {
			final int d = this.getDimension();
			
			return new Iterator<Classification<Prototype>>() {
				
				private final Random random = new Random(RandomPrototypeSource.this.getSeed());
				
				private final double[] datum = new double[d];
				
				private final Classification<Prototype> result = new Classification<>(
						this.datum, new Prototype(this.datum), 0.0);
				
				private int i = 0;
				
				@Override
				public final boolean hasNext() {
					return this.i < RandomPrototypeSource.this.size();
				}
				
				@Override
				public final Classification<Prototype> next() {
					++this.i;
					
					for (int i = 0; i < d; ++i) {
						this.datum[i] = this.random.nextDouble();
					}
					
					return this.result;
				}
				
			};
		}
		
		@Override
		public final int getDimension() {
			return this.dimension;
		}
		
		@Override
		public final int size() {
			return this.size;
		}
		
		private static final long serialVersionUID = -5303911125576388280L;
		
	}
	
	// XXX rename this method to evaluateReconstructionError?
	public static final <C extends ClassifierClass> double evaluate(final Classifier<C> classifier, final DataSource<C> inputs) {
		double result = 0.0;
		
		for (final Classification<C> classification : inputs) {
			result += classifier.getClassMeasure().compute(classification.getClassifierClass(), classifier.classify(classification.getInput()).getClassifierClass());
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static abstract interface Clustering<C extends ClassifierClass> extends Serializable {
		
		public abstract Classifier<C> cluster(DataSource<C> inputs);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class KMeansClustering implements Clustering<NearestNeighborClassifier.Prototype> {
		
		private final Measure measure;
		
		private final int k;
		
		public KMeansClustering(final Measure measure, final int k) {
			this.measure = measure;
			this.k = k;
		}
		
		public final Measure getMeasure() {
			return this.measure;
		}
		
		public final int getK() {
			return this.k;
		}
		
		@Override
		public final NearestNeighborClassifier cluster(final DataSource<Prototype> inputs) {
			final NearestNeighborClassifier result = new NearestNeighborClassifier(this.getMeasure());
			final int k = this.getK();
			final double[][] means = new double[k][inputs.getDimension()];
			
			for (int i = 0; i < k; ++i) {
				result.getPrototypes().add(new Prototype(means[i]));
			}
			
			result.updatePrototypeIndices();
			
			final int n = inputs.size();
			final int[] clusterIndices = new int[n];
			
			for (int i = 0; i < n; ++i) {
				clusterIndices[i] = k * i / n;
			}
			
			for (int i = 0; i < 8; ++i) {
				this.computeMeans(inputs, clusterIndices, means);
				this.recluster(inputs, result, clusterIndices);
			}
			
			return result;
		}
		
		private final void computeMeans(final DataSource<Prototype> inputs, final int[] clusterIndices, final double[][] means) {
			for (final double[] mean : means) {
				Arrays.fill(mean, 0.0);
			}
			
			final int k = means.length;
			final int[] counts = new int[k];
			
			{
				int i = -1;
				
				for (final Classification<Prototype> classification : inputs) {
					final int j = clusterIndices[++i];
					final double[] mean = means[j];
					
					addTo(mean, classification.getInput());
					++counts[j];
				}
			}
			
			for (int i = 0; i < k; ++i) {
				divide(means[i], counts[i]);
			}
		}
		
		private final void recluster(final DataSource<Prototype> inputs, final NearestNeighborClassifier classifier, final int[] clusterIndices) {
			int i = -1;
			
			for (final Classification<Prototype> classification : inputs) {
				clusterIndices[++i] = classifier.classify(classification.getInput()).getClassifierClass().getIndex();
			}
		}
		
		private static final long serialVersionUID = -1094744384472577208L;
		
		public static final void addTo(final double[] v1, final double[] v2) {
			final int n = v1.length;
			
			for (int i = 0; i < n; ++i) {
				v1[i] += v2[i];
			}
		}
		
		public static final void divide(final double[] v, final double divisor) {
			if (divisor == 0.0) {
				return;
			}
			
			final int n = v.length;
			
			for (int i = 0; i < n; ++i) {
				v[i] /= divisor;
			}
		}
		
	}
	
}
