package imj3.draft.machinelearning;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.util.List;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * Clusters data by reading each element only once.
 * 
 * @author codistmonk (creation 2015-02-04)
 */
public final class StreamingClustering extends NearestNeighborClustering {
	
	public StreamingClustering(final Measure measure, final int clusterCount) {
		super(measure, clusterCount);
	}
	
	@Override
	public final void cluster(final DataSource<Prototype> inputs, final NearestNeighborClassifier classifier) {
		final int k = this.getClusterCount();
		final List<Prototype> prototypes = classifier.getPrototypes();
		
		for (final Classification<Prototype> classification : inputs) {
			final int n = prototypes.size();
			// XXX should the weights be considered instead of performing a normal classification?
			final Classification<Prototype> c = classifier.classify(classification.getInput());
			
			if (c == null || 0.0 != c.getScore() && n < k) {
				prototypes.add(new Prototype(c.getInput().clone()).setIndex(n));
			} else if (0.0 == c.getScore()) {
				c.getClassifierClass().updateWeight(1.0);
			} else {
				final Prototype prototype = c.getClassifierClass();
				
				mergeInto(prototype.toArray(), prototype.getWeight(), c.getInput(), 1.0);
				
				prototype.updateWeight(1.0);
			}
		}
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
