package imj3.draft.machinelearning;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.util.List;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class OnlineClustering implements Clustering<Prototype> {
	
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
		final TicToc timer = new TicToc();
		final NearestNeighborClassifier result = new NearestNeighborClassifier(this.getMeasure());
		final int k = this.getClusterCount();
		final List<Prototype> prototypes = result.getPrototypes();
		final double[] weights = new double[k];
		
		for (final Classification<Prototype> classification : inputs) {
			final int n = prototypes.size();
			// XXX should the weights be considered instead of performing a normal classification?
			final Classification<Prototype> c = result.classify(classification.getInput());
			
			if (c == null || 0.0 != c.getScore() && n < k) {
				++weights[n];
				prototypes.add(new Prototype(c.getInput().clone()).setIndex(n));
			} else if (0.0 == c.getScore()) {
				++weights[c.getClassifierClass().getIndex()];
			} else {
				final Prototype prototype = c.getClassifierClass();
				
				mergeInto(prototype.getDatum(), weights[prototype.getIndex()], c.getInput(), 1.0);
			}
		}
		
		Tools.debugPrint("Clustering done in", timer.toc(), "ms");
		
		return result;
	}
	
	private static final long serialVersionUID = 1208345425946241729L;
	
	public static final void mergeInto(final double[] v1, final double w1, final double[] v2, final double w2) {
		final int n = v1.length;
		final double w = w1 + w2;
		
		for (int i = 0; i < n; ++i) {
			v1[i] = (v1[i] * w1 + v2[i] * w2) / w;
		}
	}
	
}
