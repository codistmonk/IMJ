package imj.clustering;

import static imj.clustering.HierarchicalClusterer.NodeProcessor.Traversor.BREADTH_FIRST;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.util.Arrays.binarySearch;
import static java.util.Collections.sort;
import static multij.tools.Tools.cast;
import static multij.tools.Tools.debugPrint;
import imj.clustering.FunTools.Filter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeSet;

import multij.tools.MathTools.Statistics;
import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2013-04-03)
 */
public final class HierarchicalClusterer {
	
	private final Distance distance;
	
	private final List<Node> nodes;
	
	private int sampleCount;
	
	private final Statistics diameterStatistics;
	
	public HierarchicalClusterer(final Distance distance) {
		this.distance = distance;
		this.nodes = new ArrayList<Node>();
		this.diameterStatistics = new Statistics();
	}
	
	public final Statistics getDiameterStatistics() {
		return this.diameterStatistics;
	}
	
	public final int getSampleCount() {
		return this.sampleCount;
	}
	
	public final Distance getDistance() {
		return this.distance;
	}
	
	public final Node getLastNode() {
		return this.getNodes().get(this.getNodes().size() - 1);
	}
	
	public final List<Node> getNodes() {
		return this.nodes;
	}
	
	public final void addSample(final double... sample) {
		this.getNodes().add(new Leaf(this.sampleCount++, sample));
	}
	
	public static final int countPairs(final int n) {
		return n * (n - 1) / 2;
	}
	
	public static final int[] range(final int start, final int end) {
		final int n = end - start;
		final int[] result = new int[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = start + i;
		}
		
		return result;
	}
	
	public final void finish2() {
		final int n = this.getSampleCount();
		
		if (n != this.getNodes().size()) {
			throw new IllegalStateException();
		}
		
		final int edgeCount = countPairs(n);
		final List<Edge> edges = new ArrayList<Edge>(edgeCount);
		
		for (int i = 0; i < n; ++i) {
			for (int j = i + 1; j < n; ++j) {
				edges.add(this.new Edge(i, j));
			}
		}
		
		sort(edges);
		
		final int[] clusters = range(0, n);
		
		for (final Edge edge : edges) {
			final Node edgeNode0 = edge.getNode0();
			final Node edgeNode1 = edge.getNode1();
			
			final int clusterId0 = clusters[edgeNode0.getId()];
			final int clusterId1 = clusters[edgeNode1.getId()];
			
			if (clusterId0 != clusterId1) {
				final Node clusterNode0 = this.getNodes().get(clusterId0);
				final Node clusterNode1 = this.getNodes().get(clusterId1);
				final Node newNode = new InterNode(this.getNodes().size(), clusterNode0, clusterNode1,
						this.getDistance().getDistance(clusterNode0.getSample(), clusterNode1.getSample()));
				
				this.getNodes().add(newNode);
				BREADTH_FIRST.forEachNode(newNode, new NodeProcessor() {
					
					@Override
					public final boolean process(final Node node) {
						if (!(node instanceof Leaf)) {
							return true;
						}
						
						clusters[node.getId()] = newNode.getId();
						
						return false;
					}
					
				});
			}
		}
	}
	
	private static final Filter<Edge> EDGE_OBSOLETE = new Filter<Edge>() {
		
		@Override
		public final boolean accept(final Edge edge) {
			return edge.getNode0().getParent() != null || edge.getNode1().getParent() != null;
		}
		
	};
	
	public final void finish() {
		if (this.getSampleCount() != this.getNodes().size()) {
			throw new IllegalStateException();
		}
		
		this.getDiameterStatistics().reset();
		
		final PriorityQueue<Edge> edges = new PriorityQueue<Edge>(countPairs(this.getSampleCount()));
		
		{
			final int n = this.getNodes().size();
			
			for (int i = 0; i < n; ++i) {
				for (int j = i + 1; j < n; ++j) {
					edges.add(this.new Edge(i, j));
				}
			}
		}
		
		final TicToc timer1 = new TicToc();
		final TicToc timer2 = new TicToc();
		int iterationCount = 0;
		int maximumEdgeCount = edges.size();
		
		while (!edges.isEmpty()) {
			++iterationCount;
			
			if (maximumEdgeCount < edges.size()) {
				maximumEdgeCount = edges.size();
			}
			
			timer1.tic();
			final Edge edge0 = pollWhile(EDGE_OBSOLETE, edges);
			timer1.toc();
			
			if (edge0 == null) {
				break;
			}
			
			final int newNodeIndex = this.getNodes().size();
			final InterNode newNode = new InterNode(newNodeIndex, edge0.getNode0(), edge0.getNode1(), edge0.getDistance());
			this.getDiameterStatistics().addValue(newNode.getDiameter());
			
			timer2.tic();
			this.getNodes().add(newNode);
			
			for (int i = 0; i < newNodeIndex; ++i) {
				if (this.getNodes().get(i).getParent() == null) {
					edges.add(this.new Edge(i, newNodeIndex));
				}
			}
			timer2.toc();
		}
		
		debugPrint(iterationCount, this.getSampleCount(), maximumEdgeCount);
		debugPrint(timer1.getTotalTime(), timer2.getTotalTime());
	}
	
	public static final <E> E pollWhile(final Filter<E> filter, final Queue<E> queue) {
		E result = queue.poll();
		
		while (result != null && filter.accept(result)) {
			result = queue.poll();
		}
		
		return result;
	}
	
	public final int[] getClusters(final double maximumDistance) {
		final int[] result = new int[this.getSampleCount()];
		
		BREADTH_FIRST.forEachNode(this.getLastNode(), new NodeProcessor() {
			
			@Override
			public final boolean process(final Node node) {
				if (maximumDistance < node.getDiameter()) {
					return true;
				}
				
				label(node, result);
				
				return false;
			}
			
		});
		
		return result;
	}
	
	public static final void label(final Node root, final int[] clusters) {
		BREADTH_FIRST.forEachNode(root, new NodeProcessor() {
			
			@Override
			public final boolean process(final Node node) {
				if (!(node instanceof Leaf)) {
					return true;
				}
				
				clusters[node.getId()] = root.getId();
				
				return false;
			}
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2013-04-03)
	 */
	public static interface NodeProcessor {
		
		public abstract boolean process(Node node);
		
		/**
		 * @author codistmonk (creation 2013-04-03)
		 */
		public static enum Traversor {
			
			BREADTH_FIRST {
				
				@Override
				protected final void scheduleChildren(final InterNode parent, final List<Node> todo) {
					todo.add(parent.getChild0());
					todo.add(parent.getChild1());
				}
				
			}, DEPTH_FIRST {
				
				@Override
				protected final void scheduleChildren(final InterNode parent, final List<Node> todo) {
					todo.add(0, parent.getChild1());
					todo.add(0, parent.getChild0());
				}
				
			};
			
			public final void forEachNode(final Node root, final NodeProcessor processor) {
				final List<Node> todo = new ArrayList<Node>();
				
				todo.add(root);
				
				while (!todo.isEmpty()) {
					final Node n = todo.remove(0);
					
					if (processor.process(n)) {
						final InterNode interNode = cast(InterNode.class, n);
						
						if (interNode != null) {
							this.scheduleChildren(interNode, todo);
						}
					}
				}
			}
			
			protected abstract void scheduleChildren(InterNode parent, List<Node> todo);
			
		}
		
	}
	
	public static final int relabel(final int[] clusters) {
		final Collection<Integer> values = new TreeSet<Integer>();
		
		for (final int i : clusters) {
			values.add(i);
		}
		
		final int result = values.size();
		final Integer[] sortedValues = values.toArray(new Integer[result]);
		final int n = clusters.length;
		
		for (int i = 0; i < n; ++i) {
			clusters[i] = binarySearch(sortedValues, clusters[i]);
		}
		
		return result;
	}
	
	public static final BufferedImage makeImage(final HierarchicalClusterer clusterer, final int scale, final int[] clusters) {
		int width = 0;
		int height = 0;
		final int sampleCount = clusterer.getSampleCount();
		int lastClusterId = 0;
		
		for (int i = 0; i < sampleCount; ++i) {
			final double[] sample = clusterer.getNodes().get(i).getSample();
			final int w = 1 + (int) sample[0];
			final int h = 1 + (int) sample[1];
			
			if (width < w) {
				width = w;
			}
			
			if (height < h) {
				height = h;
			}
			
			final int clusterId = clusters[i];
			
			if (lastClusterId < clusterId) {
				lastClusterId = clusterId;
			}
		}
		
		final BufferedImage result = new BufferedImage(width * scale, height * scale, TYPE_3BYTE_BGR);
		
		for (int i = 0; i < sampleCount; ++i) {
			final double[] sample = clusterer.getNodes().get(i).getSample();
			final int x = (int) sample[0];
			final int y = height - 1 - (int) sample[1];
			final int clusterId = clusters[i];
			
			for (int yy = y * scale; yy < (y + 1) * scale; ++yy) {
				for (int xx = x * scale; xx < (x + 1) * scale; ++xx) {
					result.setRGB(xx, yy, 0x00FFFFFF * (1 + clusterId) / (1 + lastClusterId));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-03)
	 */
	public final class Edge implements Comparable<Edge> {
		
		private final int node0Index;
		
		private final int node1Index;
		
		private final double distance;
		
		public Edge(final int node0Index, final int node1Index) {
			this.node0Index = node0Index;
			this.node1Index = node1Index;
			this.distance = HierarchicalClusterer.this.getDistance().getDistance(
					this.getNode0().getSample(), this.getNode1().getSample());
		}
		
		public final Node getNode0() {
			return HierarchicalClusterer.this.getNodes().get(this.node0Index);
		}
		
		public final Node getNode1() {
			return HierarchicalClusterer.this.getNodes().get(this.node1Index);
		}
		
		public final double getDistance() {
			return this.distance;
		}
		
		@Override
		public final int compareTo(final Edge that) {
			int result = Double.compare(this.getDistance(), that.getDistance());
			
			if (result != 0) {
				return result;
			}
			
			result = this.node0Index - that.node0Index;
			
			if (result != 0) {
				return result;
			}
			
			return this.node1Index - that.node1Index;
		}
		
		@Override
		public final String toString() {
			return this.node0Index + "_" + this.node1Index + ":" + this.getDistance();
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-03)
	 */
	public static abstract interface Node {
		
		public abstract int getId();
		
		public abstract InterNode getParent();
		
		public abstract void setParent(InterNode parent);
		
		public abstract double[] getSample();
		
		public abstract double getWeight();
		
		public abstract double getDiameter();
		
		/**
		 * @author codistmonk (creation 2013-04-03)
		 */
		public static abstract class Abstract implements Node {
			
			private final int id;
			
			private InterNode parent;
			
			protected Abstract(final int id) {
				this.id = id;
			}
			
			@Override
			public final InterNode getParent() {
				return this.parent;
			}
			
			public final int getId() {
				return this.id;
			}
			
			@Override
			public final void setParent(final InterNode parent) {
				assert null == this.getParent();
				
				this.parent = parent;
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-03)
	 */
	public static final class Leaf extends Node.Abstract {
		
		private final double[] sample;
		
		private final double weight;
		
		public Leaf(final int id, final double[] sample) {
			super(id);
			this.sample = sample;
			this.weight = +1.0;
		}
		
		@Override
		public final double[] getSample() {
			return this.sample;
		}
		
		@Override
		public final double getWeight() {
			return this.weight;
		}
		
		@Override
		public final double getDiameter() {
			return 0.0;
		}
		
		@Override
		public final String toString() {
			return "" + this.getId() + ":" + Arrays.toString(this.getSample());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-03)
	 */
	public static final class InterNode extends Node.Abstract {
		
		private final Node child0;
		
		private final Node child1;
		
		private final double[] sample;
		
		private final double weight;
		
		private final double diameter;
		
		public InterNode(final int id, final Node child0, final Node child1, final double diameter) {
			super(id);
			this.child0 = child0;
			this.child1 = child1;
			final double[] sample0 = child0.getSample();
			final double[] sample1 = child1.getSample();
			final double weight0 = child0.getWeight();
			final double weight1 = child1.getWeight();
			final int n = sample0.length;
			this.sample = new double[n];
			this.weight = weight0 + weight1;
			this.diameter = diameter;
			
			for (int i = 0; i < n; ++i) {
				this.sample[i] = (sample0[i] * weight0 + sample1[i] * weight1) / this.weight;
			}
			
			child0.setParent(this);
			child1.setParent(this);
		}
		
		public final Node getChild0() {
			return this.child0;
		}
		
		public final Node getChild1() {
			return this.child1;
		}
		
		@Override
		public final double[] getSample() {
			return this.sample;
		}
		
		@Override
		public final double getWeight() {
			return this.weight;
		}
		
		public final double getDiameter() {
			return this.diameter;
		}
		
		@Override
		public final String toString() {
			return "(" + this.getChild0() + " " + this.getChild1() + ")";
		}
		
	}
	
}
