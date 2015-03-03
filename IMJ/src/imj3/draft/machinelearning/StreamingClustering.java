package imj3.draft.machinelearning;

import static imj3.draft.machinelearning.Datum.Default.datum;

import java.util.List;

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
	public final void cluster(final DataSource inputs, final NearestNeighborClassifier classifier) {
		final int k = this.getClusterCount();
		final List<Datum> prototypes = classifier.getPrototypes();
		final Datum tmp = new Datum.Default();
		
		for (final Datum classification : inputs) {
			final int n = prototypes.size();
			// XXX should the weights be considered instead of performing a normal classification?
			final Datum c = classifier.classify(classification, tmp);
			
			if (c == null) {
				throw new NullPointerException();
			}
			
			if (0.0 != c.getScore() && n < k) {
				prototypes.add(datum(c.getValue().clone()).setIndex(n));
			} else if (0.0 == c.getScore()) {
				c.getPrototype().updateWeight(1.0);
			} else {
				final Datum prototype = c.getPrototype();
				
				mergeInto(prototype.getValue(), prototype.getWeight(), c.getValue(), 1.0);
				
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
