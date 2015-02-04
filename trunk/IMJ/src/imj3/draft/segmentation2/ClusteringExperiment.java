package imj3.draft.segmentation2;

import static net.sourceforge.aprog.tools.MathTools.square;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import imj3.draft.segmentation2.NearestNeighborClassifier.Prototype;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class ClusteringExperiment {
	
	private ClusteringExperiment() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final int s = 32;
		final int d = s * s * 3;
		final int n = 10_000;
		final int k = 16;
//		final DataSource<Prototype> inputs = new RandomPrototypeSource(d, n, 0L);
//		final DataSource<Prototype> inputs = new BufferedDataSource<>(new RandomPrototypeSource(d, n, 0L));
//		final DataSource<Prototype> inputs = new GaussianMixturePrototypeSource(k, d, n, 0L);
		final DataSource<Prototype> inputs = new BufferedDataSource<>(new GaussianMixturePrototypeSource(k, d, n, 0L));
		
//		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, k, 1).cluster(inputs), inputs));
//		Tools.debugPrint(evaluate(new OnlineClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new OnlineClustering(Measure.Predefined.L1, k).cluster(inputs), inputs));
	}
	
	// XXX rename this method to evaluateReconstructionError?
	public static final <C extends ClassifierClass> double evaluate(final Classifier<C> classifier, final DataSource<C> inputs) {
		final TicToc timer = new TicToc();
		double result = 0.0;
		
		for (final Classification<C> classification : inputs) {
			result += classifier.getClassMeasure().compute(classification.getClassifierClass(), classifier.classify(classification.getInput()).getClassifierClass());
		}
		
		Tools.debugPrint("Evaluation done in", timer.toc(), "ms");
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class GaussianMixturePrototypeSource implements DataSource<Prototype> {
		
		private final int dimension;
		
		private final int size;
		
		private final List<Gaussian> gaussians;
		
		private final long seed;
		
		public GaussianMixturePrototypeSource(final int n, final int dimension, final int size, final long seed) {
			this.dimension = dimension;
			this.size = size;
			this.gaussians = new ArrayList<>(n);
			
			final Random random = new Random(seed);
			
			for (int i = 0; i < n; ++i) {
				this.gaussians.add(new Gaussian(random.doubles(dimension).toArray(), random.nextDouble()));
			}
			
			this.seed = random.nextLong();
		}
		
		public final int getSize() {
			return this.size;
		}
		
		public final List<Gaussian> getGaussians() {
			return this.gaussians;
		}
		
		public final long getSeed() {
			return this.seed;
		}
		
		@Override
		public final Iterator<Classification<Prototype>> iterator() {
			final int d = this.getDimension();
			
			return new Iterator<Classification<Prototype>>() {
				
				private final Random random = new Random(GaussianMixturePrototypeSource.this.getSeed());
				
				private final double[] datum = new double[d];
				
				private final Classification<Prototype> result = new Classification<>(
						this.datum, new Prototype(this.datum), 0.0);
				
				private int i = 0;
				
				@Override
				public final boolean hasNext() {
					return this.i < GaussianMixturePrototypeSource.this.size();
				}
				
				@Override
				public final Classification<Prototype> next() {
					++this.i;
					
					final List<Gaussian> gaussians = GaussianMixturePrototypeSource.this.getGaussians();
					final Gaussian gaussian = gaussians.get(this.random.nextInt(gaussians.size()));
					final double sigma2 = square(gaussian.getSigma());
					
					for (int i = 0; i < d; ++i) {
						this.datum[i] = gaussian.getCenter()[i] + this.random.nextGaussian() * sigma2;
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
		
		private static final long serialVersionUID = 3121457385669815840L;
		
		/**
		 * @author codistmonk (creation 2015-02-04)
		 */
		public static final class Gaussian implements Serializable {
			
			private final double[] center;
			
			private final double sigma;
			
			public Gaussian(final double[] center, final double sigma) {
				this.center = center;
				this.sigma = sigma;
			}
			
			public final double[] getCenter() {
				return this.center;
			}
			
			public final double getSigma() {
				return this.sigma;
			}
			
			private static final long serialVersionUID = 7955299362209802947L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 * @param <C>
	 */
	public static final class BufferedDataSource<C extends ClassifierClass> implements DataSource<C> {
		
		private final int dimension;
		
		private final List<Classification<C>> dataset;
		
		@SuppressWarnings("unchecked")
		public BufferedDataSource(final DataSource<C> source) {
			this.dimension = source.getDimension();
			this.dataset = new ArrayList<>();
			
			Tools.debugPrint("Buffering...");
			
			for (final Classification<C> classification : source) {
				final Prototype prototype = Tools.cast(Prototype.class, classification.getClassifierClass());
				
				if (prototype != null && classification.getInput() == prototype.getDatum()) {
					final double[] input = classification.getInput().clone();
					
					this.dataset.add(new Classification<>(input,
							(C) new Prototype(input), classification.getScore()));
				} else {
					this.dataset.add(new Classification<>(classification.getInput().clone(),
							classification.getClassifierClass(), classification.getScore()));
				}
			}
			
			Tools.debugPrint("Buffering", this.dataset.size(), "elements done");
		}
		
		public final List<Classification<C>> getDataset() {
			return this.dataset;
		}
		
		@Override
		public final Iterator<Classification<C>> iterator() {
			return this.getDataset().iterator();
		}
		
		@Override
		public final int getDimension() {
			return this.dimension;
		}
		
		private static final long serialVersionUID = -3379089397400242050L;
		
	}
	
}
