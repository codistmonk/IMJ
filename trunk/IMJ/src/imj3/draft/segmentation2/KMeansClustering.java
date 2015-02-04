package imj3.draft.segmentation2;

import imj3.draft.segmentation2.NearestNeighborClassifier.Prototype;

import java.util.Arrays;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class KMeansClustering implements Clustering<NearestNeighborClassifier.Prototype> {
	
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
		final TicToc timer = new TicToc();
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
			clusterIndices[i] = (int) ((long) k * i / n);
		}
		
		for (int i = 0; i < 8; ++i) {
			this.computeMeans(inputs, clusterIndices, means);
			this.recluster(inputs, result, clusterIndices);
		}
		
		Tools.debugPrint("Clustering done in", timer.toc(), "ms");
		
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