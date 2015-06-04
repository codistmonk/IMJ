package imj3.machinelearning;

import static imj3.machinelearning.Datum.Default.datum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class GreedyAssociativeStreamingClustering extends NearestNeighborClustering {
	
	public GreedyAssociativeStreamingClustering(final Measure measure, final int clusterCount) {
		super(measure, clusterCount);
	}
	
	@Override
	protected final void cluster(final DataSource inputs, final NearestNeighborClassifier classifier) {
		final int k = this.getClusterCount();
		final List<GreedyAssociativeStreamingClustering.EndPoint<Datum>> sources = new ArrayList<>(k);
		final List<GreedyAssociativeStreamingClustering.EndPoint<Datum>> targets = new ArrayList<>(k);
		final Iterator<Datum> i = inputs.iterator();
		
		for (int j = 0; j < k && i.hasNext(); ++j) {
			sources.add(new GreedyAssociativeStreamingClustering.EndPoint<>(datum(i.next().getValue().clone())));
		}
		
		final List<GreedyAssociativeStreamingClustering.Association<Datum>> associations = new ArrayList<>(k * k);
		
		while (i.hasNext()) {
			for (int j = 0; j < k && i.hasNext(); ++j) {
				targets.add(new GreedyAssociativeStreamingClustering.EndPoint<>(datum(i.next().getValue().clone())));
			}
			
			for (final GreedyAssociativeStreamingClustering.EndPoint<Datum> source : sources) {
				for (final GreedyAssociativeStreamingClustering.EndPoint<Datum> target: targets) {
					final double cost = Measure.Predefined.L1.compute(source.getObject().getValue(), target.getObject().getValue());
					
					associations.add(new GreedyAssociativeStreamingClustering.Association<>(source, target, cost));
				}
			}
			
			for (final GreedyAssociativeStreamingClustering.Association<Datum> association : associations) {
				if (!association.isLocked()) {
					final GreedyAssociativeStreamingClustering.EndPoint<Datum> source = association.getSource();
					final GreedyAssociativeStreamingClustering.EndPoint<Datum> target = association.getTarget();
					
					source.setLocked(true);
					target.setLocked(true);
					
					final Datum sourcePrototype = source.getObject();
					final Datum targetPrototype = target.getObject();
					
					StreamingClustering.mergeInto(sourcePrototype.getValue(), sourcePrototype.getWeight(),
							targetPrototype.getValue(), targetPrototype.getWeight());
				}
			}
			
			targets.clear();
			associations.clear();
		}
		
		for (final GreedyAssociativeStreamingClustering.EndPoint<Datum> source : sources) {
			classifier.getPrototypes().add(source.getObject());
		}
	}
	
	private static final long serialVersionUID = -5507345094506094377L;
	
	/**
	 * @author codistmonk (creation 2015-02-05)
	 */
	public static final class Association<T> implements Serializable, Comparable<GreedyAssociativeStreamingClustering.Association<T>> {
		
		private final GreedyAssociativeStreamingClustering.EndPoint<T> source, target;
		
		private final double cost;
		
		public Association(final GreedyAssociativeStreamingClustering.EndPoint<T> source, final GreedyAssociativeStreamingClustering.EndPoint<T> target, final double cost) {
			this.source = source;
			this.target = target;
			this.cost = cost;
		}
		
		public final GreedyAssociativeStreamingClustering.EndPoint<T> getSource() {
			return this.source;
		}
		
		public final GreedyAssociativeStreamingClustering.EndPoint<T> getTarget() {
			return this.target;
		}
		
		public final double getCost() {
			return this.cost;
		}
		
		public final boolean isLocked() {
			return this.getSource().isLocked() || this.getTarget().isLocked();
		}
		
		private static final long serialVersionUID = 7836354402558855202L;
		
		@Override
		public final int compareTo(final GreedyAssociativeStreamingClustering.Association<T> that) {
			return Double.compare(this.getCost(), that.getCost());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-05)
	 */
	public static final class EndPoint<T> implements Serializable {
		
		private final T object;
		
		private boolean locked;
		
		public EndPoint(final T object) {
			this.object = object;
		}
		
		public final boolean isLocked() {
			return this.locked;
		}
		
		public final void setLocked(final boolean locked) {
			this.locked = locked;
		}
		
		public final T getObject() {
			return this.object;
		}
		
		private static final long serialVersionUID = 1490439068402247668L;
		
	}
	
}
