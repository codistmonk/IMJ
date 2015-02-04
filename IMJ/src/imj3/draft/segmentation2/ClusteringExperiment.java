package imj3.draft.segmentation2;

import java.util.List;

import imj3.draft.segmentation2.NearestNeighborClassifier.Prototype;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
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
		final int d = 2;
		final int n = 10;
		final int k = 2;
		final DataSource<Prototype> inputs = new RandomPrototypeSource(d, n, 0L);
		
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, k).cluster(inputs), inputs));
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
	public static final class OnlineClustering implements Clustering<Prototype> {
		
		private final Measure measure;
		
		private final int clusterCount;
		
		public OnlineClustering(final Measure measure, final int clusterCount) {
			this.measure = measure;
			this.clusterCount = clusterCount;
		}
		
		public final Measure getMeasure() {
			return this.measure;
		}
		
		public final int getClusterCount() {
			return this.clusterCount;
		}
		
		@Override
		public final NearestNeighborClassifier cluster(final DataSource<Prototype> inputs) {
			final NearestNeighborClassifier result = new NearestNeighborClassifier(this.getMeasure());
			final int k = this.getClusterCount();
			final int d = inputs.getDimension();
			final List<Prototype> prototypes = result.getPrototypes();
			final double[] weights = new double[k];
			final int[] prototypeNearestNeighbors = new int[k];
			final double[] prototypeNearestScores = new double[k];
			
			for (int i = 0; i < k; ++i) {
				prototypeNearestNeighbors[i] = i;
				prototypeNearestScores[i] = Double.POSITIVE_INFINITY;
			}
			
			for (final Classification<Prototype> classification : inputs) {
				final int n = prototypes.size();
				final Classification<Prototype> c = result.classify(classification.getInput());
				
				if (c == null || 0.0 != c.getScore() && n < k) {
					++weights[n];
					prototypes.add(new Prototype(c.getInput().clone()));
					
					for (int i = 0; i < n; ++i) {
						final double score = this.getMeasure().compute(c.getInput(), prototypes.get(i).getDatum(), Double.POSITIVE_INFINITY);
						
						if (score < prototypeNearestScores[i]) {
							prototypeNearestNeighbors[i] = n;
							prototypeNearestScores[i] = score;
						}
						
						if (score < prototypeNearestScores[n]) {
							prototypeNearestNeighbors[n] = i;
							prototypeNearestScores[n] = score;
						}
					}
				} else if (0.0 == c.getScore()) {
					++weights[n];
				} else {
					// TODO
				}
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 1208345425946241729L;
		
	}
	
}
