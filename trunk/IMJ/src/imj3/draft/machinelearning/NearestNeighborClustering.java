package imj3.draft.machinelearning;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class NearestNeighborClustering implements Clustering<Prototype> {
	
	private final Measure measure;
	
	private final int clusterCount;
	
	public NearestNeighborClustering(final Measure measure, final int clusterCount) {
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
		
		this.cluster(inputs, result);
		
		return result;
	}
	
	protected abstract void cluster(DataSource<Prototype> inputs, NearestNeighborClassifier classifier);
	
	private static final long serialVersionUID = 2918812158797378496L;
	
}