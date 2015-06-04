package imj.clustering;

import static java.lang.Math.abs;
import static multij.tools.Tools.cast;
import static multij.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import multij.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-06-07)
 */
public final class IncrementalClustererTest {
	
	@Test
	public final void test1() {
		final int clusterCount = 2;
		final double epsilon = 1.0E-6;
		final IncrementalClusterer<Double> clusterer = new IncrementalClusterer<Double>(
				clusterCount, DoubleOperations.INSTANCE);
		
		assertEquals(clusterCount, clusterer.getClusterCount());
		assertEquals(0, clusterer.getNodeCount());
		
		clusterer.add(0.0);
		
		assertEquals(1, clusterer.getNodeCount());
		assertEquals(0.0, clusterer.getNode(0), epsilon);
		
		clusterer.add(0.0);
		
		assertEquals(1, clusterer.getNodeCount());
		assertEquals(0.0, clusterer.getNode(0), epsilon);
		
		clusterer.add(1.0);
		
		assertEquals(2, clusterer.getNodeCount());
		assertEquals(0.0, clusterer.getNode(0), epsilon);
		assertEquals(1.0, clusterer.getNode(1), epsilon);
		
		clusterer.add(3.0);
		
		assertEquals(2, clusterer.getNodeCount());
		assertEquals(0.333333, clusterer.getNode(0), epsilon);
		assertEquals(3.0, clusterer.getNode(1), epsilon);
	}
	
	/**
	 * @author codistmonk (creation 2013-06-07)
	 */
	public static final class DoubleOperations implements IncrementalClusterer.Operations<Double> {
		
		@Override
		public final double getDistance(final Double node0, final Double node1) {
			return abs(node1 - node0);
		}
		
		@Override
		public final Double merge(final Double node0, final long weight0, final Double node1, final long weight1) {
			return (node0 * weight0 + node1 * weight1) / (weight0 + weight1);
		}
		
		public static final DoubleOperations INSTANCE = new DoubleOperations();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-06-07)
	 */
	public static final class IncrementalClusterer<T> {
		
		private final int clusterCount;
		
		private final Operations<T> operations;
		
		private final List<T> nodes;
		
		private final long[] weights;
		
		private final Collection<Pair>[] pairs;
		
		private final PriorityQueue<Pair> queue;
		
		public IncrementalClusterer(final int clusterCount, final Operations<T> operations) {
			if (clusterCount <= 0) {
				throw new IllegalArgumentException();
			}
			
			this.clusterCount = clusterCount;
			this.operations = operations;
			this.nodes = new ArrayList<T>(clusterCount);
			this.weights = new long[clusterCount];
			this.pairs = new Collection[clusterCount];
			this.queue = new PriorityQueue<Pair>();
			
			for (int i = 0; i < clusterCount; ++i) {
				this.pairs[i] = new ArrayList<Pair>(clusterCount);
			}
		}
		
		public final int getClusterCount() {
			return this.clusterCount;
		}
		
		public final Operations<T> getOperations() {
			return this.operations;
		}
		
		public final void add(final T node) {
			final int nodeCount = this.getNodeCount();
			
			if (nodeCount < this.getClusterCount()) {
				final Collection<Pair> nodePairs = new ArrayList<Pair>(nodeCount);
				
				for (int i = 0; i < nodeCount; ++i) {
					final Pair pair = this.new Pair(i, node, nodeCount);
					
					if (0.0 == pair.getDistance()) {
						++this.weights[i];
						
						return;
					}
					
					nodePairs.add(pair);
				}
				
				this.nodes.add(node);
				
				for (final Pair p : nodePairs) {
					this.pairs[p.getFirstIndex()].add(p);
				}
				
				this.pairs[nodeCount] = nodePairs;
				this.weights[nodeCount] = 1L;
				this.queue.addAll(nodePairs);
			} else {
				int obsoleteNodeIndex = -1;
				double shortestDistance = this.queue.peek().getDistance();
				
				for (int i = 0; i < nodeCount; ++i) {
					final double distance = this.getOperations().getDistance(this.nodes.get(i), node);
					
					if (distance < shortestDistance) {
						obsoleteNodeIndex = i;
						shortestDistance = distance;
					}
				}
				
				if (obsoleteNodeIndex < 0) {
					final Pair p = this.queue.peek();
					
					this.removeAll(p.getFirstPairs());
					this.removeAll(p.getSecondPairs());
					
					this.nodes.set(p.getFirstIndex(), this.getOperations().merge(
							p.getFirst(), this.weights[p.getFirstIndex()], p.getSecond(), this.weights[p.getSecondIndex()]));
					this.weights[p.getFirstIndex()] += this.weights[p.getSecondIndex()];
					this.nodes.set(p.getSecondIndex(), node);
					this.weights[p.getSecondIndex()] = 1L;
					
					final Collection<Pair> pairs = new HashSet<Pair>(p.getFirstPairs());
					
					pairs.addAll(p.getSecondPairs());
					
					this.updateAndAddAll(pairs);
				} else {
					this.removeAll(this.pairs[obsoleteNodeIndex]);
					
					this.nodes.set(obsoleteNodeIndex, this.getOperations().merge(
							this.nodes.get(obsoleteNodeIndex), this.weights[obsoleteNodeIndex], node, 1L));
					++this.weights[obsoleteNodeIndex];
					
					this.updateAndAddAll(this.pairs[obsoleteNodeIndex]);
				}
			}
			
			assert this.getNodeCount() * (this.getNodeCount() - 1) / 2 == this.queue.size();
		}
		
		public final int getNodeCount() {
			return this.nodes.size();
		}
		
		public final T getNode(final int index) {
			return this.nodes.get(index);
		}
		
		final Collection<Pair> getPairs(final int index) {
			return this.pairs[index];
		}
		
		private final void removeAll(final Iterable<Pair> pairs) {
			for (final Pair p : pairs) {
				this.queue.remove(p);
			}
		}
		
		private final void updateAndAddAll(final Iterable<Pair> pairs) {
			for (final Pair p : pairs) {
				p.updateDistance();
				this.queue.add(p);
			}
		}
		
		/**
		 * @author codistmonk (creation 2013-06-07)
		 */
		public final class Pair implements Comparable<Pair> {
			
			private final int firstIndex;
			
			private int secondIndex;
			
			private double distance;
			
			public Pair(final int firstIndex, final T second, final int secondIndex) {
				this.firstIndex = firstIndex;
				this.secondIndex = secondIndex;
				this.distance = IncrementalClusterer.this.getOperations().getDistance(this.getFirst(), second);
			}
			
			public final int getFirstIndex() {
				return this.firstIndex;
			}
			
			public final int getSecondIndex() {
				return this.secondIndex;
			}
			
			public final void setSecondIndex(final int secondIndex) {
				this.secondIndex = secondIndex;
			}
			
			public final T getFirst() {
				return IncrementalClusterer.this.getNode(this.getFirstIndex());
			}
			
			public final T getSecond() {
				return IncrementalClusterer.this.getNode(this.getSecondIndex());
			}
			
			public final Collection<Pair> getFirstPairs() {
				return IncrementalClusterer.this.getPairs(this.getFirstIndex());
			}
			
			public final Collection<Pair> getSecondPairs() {
				return IncrementalClusterer.this.getPairs(this.getSecondIndex());
			}
			
			public final double getDistance() {
				return this.distance;
			}
			
			public final void updateDistance() {
				this.distance = IncrementalClusterer.this.getOperations().getDistance(this.getFirst(), this.getSecond());
			}
			
			@Override
			public final int compareTo(final Pair that) {
				return Double.compare(this.getDistance(), that.getDistance());
			}
			
			@Override
			public final int hashCode() {
				return this.getFirstIndex() + this.getSecondIndex();
			}
			
			@Override
			public final boolean equals(final Object object) {
				final Pair that = cast(this.getClass(), object);
				
				return that != null && this.getFirstIndex() == that.getFirstIndex() &&
						this.getSecondIndex() == that.getSecondIndex();
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-06-07)
		 */
		public static abstract interface Operations<T> {
			
			public abstract double getDistance(T node0, T node1);
			
			public abstract T merge(T node0, long weight0, T node1, long weight1);
			
		}
		
	}
	
}
