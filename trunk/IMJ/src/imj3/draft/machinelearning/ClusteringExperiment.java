package imj3.draft.machinelearning;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

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
		final int s = 4;
		final int d = s * s * 3;
		final int n = 1_000_000;
		final int k = 16;
		
		Tools.debugPrint("dimension:", d);
		
		final DataSource<Prototype> inputs = new RandomPrototypeSource(d, n, 0L);
//		final DataSource<Prototype> inputs = new BufferedDataSource<>(new RandomPrototypeSource(d, n, 0L));
//		final DataSource<Prototype> inputs = new GaussianMixturePrototypeSource(k, d, n, 0L);
//		final DataSource<Prototype> inputs = new BufferedDataSource<>(new GaussianMixturePrototypeSource(k, d, n, 0L));
		
//		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L1, k).cluster(inputs), inputs));
//		Tools.debugPrint(evaluate(new StreamingClustering(Measure.Predefined.L1, n).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new StreamingClustering(Measure.Predefined.L1, k).cluster(inputs), inputs));
	}
	
	// XXX rename this method to evaluateReconstructionError?
	public static final <C extends ClassifierClass> double evaluate(final Classifier<C> classifier, final DataSource<C> inputs) {
		final TicToc timer = new TicToc();
		double result = 0.0;
		
		for (final Classification<C> classification : inputs) {
			result += classifier.getClassMeasure().compute(
					classification.getClassifierClass(), classifier.classify(classification.getInput()).getClassifierClass());
		}
		
		Tools.debugPrint("Evaluation done in", timer.toc(), "ms");
		
		return result;
	}
	
}
