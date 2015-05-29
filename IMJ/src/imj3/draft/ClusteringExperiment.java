package imj3.draft;

import static imj3.machinelearning.Datum.Default.datum;
import imj3.machinelearning.Classifier;
import imj3.machinelearning.DataSource;
import imj3.machinelearning.Datum;
import imj3.machinelearning.GaussianMixturePrototypeSource;
import imj3.machinelearning.GreedyAssociativeStreamingClustering;
import imj3.machinelearning.KMeansClustering;
import imj3.machinelearning.Measure;
import imj3.machinelearning.MedianCutClustering;
import imj3.machinelearning.ShuffledDataSource;
import imj3.machinelearning.StreamingClustering;
import imj3.machinelearning.Measure.Predefined;
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
		final int s = 16;
		final int d = s * s * 3;
		final int n = 10_000;
		final int k = 128;
		
		Tools.debugPrint("dimension:", d);
		
//		final DataSource<?> inputs = new RandomPrototypeSource(d, n, 0L);
//		final DataSource<?> inputs = new BufferedDataSource<>(new RandomPrototypeSource(d, n, 0L));
//		final DataSource<?> inputs = new GaussianMixturePrototypeSource(k, d, n, 0L);
//		final DataSource<?> inputs = new BufferedDataSource<>(new GaussianMixturePrototypeSource(k, d, n, 0L));
		final DataSource inputs = new ShuffledDataSource(new GaussianMixturePrototypeSource(k, d, n, 0L), 0, 0L);
		
		Tools.debugPrint(evaluate(new KMeansClustering(Measure.Predefined.L2, k).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new MedianCutClustering(Measure.Predefined.L2, k).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new StreamingClustering(Measure.Predefined.L2, k).cluster(inputs), inputs));
		Tools.debugPrint(evaluate(new GreedyAssociativeStreamingClustering(Measure.Predefined.L2, k).cluster(inputs), inputs));
	}
	
	// XXX rename this method to evaluateReconstructionError?
	public static final double evaluate(final Classifier classifier, final DataSource inputs) {
		final TicToc timer = new TicToc();
		double result = 0.0;
		final Datum tmp = datum();
		
		for (final Datum classification : inputs) {
			result += classifier.getClassMeasure().compute(
					classification.getPrototype(), classifier.classify(classification, tmp).getPrototype());
		}
		
		Tools.debugPrint("Evaluation done in", timer.toc(), "ms");
		
		return result;
	}
	
}
