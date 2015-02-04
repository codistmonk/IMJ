package imj3.draft.segmentation2;

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
	
}
