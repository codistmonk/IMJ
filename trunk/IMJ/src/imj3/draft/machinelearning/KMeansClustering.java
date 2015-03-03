package imj3.draft.machinelearning;

import static imj3.draft.machinelearning.Datum.Default.datum;

import java.util.Arrays;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class KMeansClustering extends NearestNeighborClustering {
	
	private final int iterations;
	
	public KMeansClustering(final Measure measure, final int k) {
		this(measure, k, 2);
	}
	
	public KMeansClustering(final Measure measure, final int k, final int iterations) {
		super(measure, k);
		this.iterations = iterations;
	}
	
	public final int getIterations() {
		return this.iterations;
	}
	
	@Override
	protected final void cluster(final DataSource<?> inputs, final NearestNeighborClassifier classifier) {
		final int k = this.getClusterCount();
		final double[][] means = new double[k][inputs.getInputDimension()];
		
		for (int i = 0; i < k; ++i) {
			classifier.getPrototypes().add(datum(means[i]));
		}
		
		classifier.updatePrototypeIndices();
		
		final int n = inputs.size();
		final int[] clusterIndices = new int[n];
		
		for (int i = 0; i < n; ++i) {
			clusterIndices[i] = (int) ((long) k * i / n);
		}
		
		shuffle(clusterIndices);
		
		this.computeMeans(inputs, clusterIndices, means);
		
		final int iterations = this.getIterations();
		
		for (int i = 0; i < iterations; ++i) {
			this.recluster(inputs, classifier, clusterIndices);
			this.computeMeans(inputs, clusterIndices, means);
		}
	}
	
	private final void computeMeans(final DataSource<?> inputs, final int[] clusterIndices, final double[][] means) {
		for (final double[] mean : means) {
			Arrays.fill(mean, 0.0);
		}
		
		final int k = means.length;
		final int[] counts = new int[k];
		
		{
			int i = -1;
			
			for (final Datum classification : inputs) {
				final int j = clusterIndices[++i];
				final double[] mean = means[j];
				
				addTo(mean, classification.getValue());
				++counts[j];
			}
		}
		
		for (int i = 0; i < k; ++i) {
			divide(means[i], counts[i]);
		}
	}
	
	private final void recluster(final DataSource<?> inputs, final NearestNeighborClassifier classifier, final int[] clusterIndices) {
		int i = -1;
		
		final Datum tmp = datum();
		
		for (final Datum classification : inputs) {
			clusterIndices[++i] = classifier.classify(classification, tmp).getPrototype().getIndex();
		}
	}
	
	private static final long serialVersionUID = -1094744384472577208L;
	
	static final Random random = new Random(0L);
	
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
	
	public static final void shuffle(final int[] values) {
		final int n = values.length;
		
		for (int i = 0; i < n; ++i) {
			swap(values, i, random.nextInt(n));
		}
	}
	
	public static final void swap(final int[] values, final int i, final int j) {
		final int tmp = values[i];
		values[i] = values[j];
		values[j] = tmp;
	}
	
}
