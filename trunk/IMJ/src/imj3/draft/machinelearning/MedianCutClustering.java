package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.Tools.intRange;
import static net.sourceforge.aprog.tools.Tools.sort;

import imj2.tools.VectorStatistics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import net.sourceforge.aprog.tools.IntComparator;

/**
 * Differences with "original" median-cut:<ul>
 *  <li> dimension selection criterion is variance instead of amplitude;
 * 	<li> generated classifier is 1-NN instead of octree (may change in the future).
 * </ul>
 * 
 * @author codistmonk (creation 2015-02-10)
 */
public final class MedianCutClustering extends NearestNeighborClustering {
	
	public MedianCutClustering(final Measure measure, final int clusterCount) {
		super(measure, clusterCount);
	}
	
	@Override
	protected final void cluster(final DataSource inputs, final NearestNeighborClassifier classifier) {
		final Queue<MedianCutClustering.Chunk> chunks = new PriorityQueue<>(new Comparator<MedianCutClustering.Chunk>() {
			
			@Override
			public final int compare(final MedianCutClustering.Chunk chunk1, final MedianCutClustering.Chunk chunk2) {
				return Double.compare(chunk2.getScore(), chunk1.getScore());
			}
			
		});
		
		final MedianCutClustering.Chunk chunk0 = new Chunk(inputs, true).analyze();
		final int inputCount = (int) chunk0.getStatistics().getCount();
		final int n = Math.min(inputCount, this.getClusterCount());
		
		chunks.add(chunk0);
		
		while (chunks.size() < n) {
			chunks.addAll(Arrays.asList(chunks.remove().cut()));
		}
		
		for (final MedianCutClustering.Chunk chunk : chunks) {
			classifier.getPrototypes().add(new Datum.Default().setValue(chunk.getStatistics().getMeans()));
		}
	}
	
	private static final long serialVersionUID = 5051949087551037706L;
	
	/**
	 * @author codistmonk (creation 2015-02-10)
	 */
	public static final class Chunk implements Serializable {
		
		private final DataSource inputs;
		
		private final BitSet subset;
		
		private final VectorStatistics statistics;
		
		private int dimensionIndex;
		
		private double score;
		
		public Chunk(final DataSource inputs, final boolean initial) {
			this.inputs = inputs;
			this.subset = initial ? null : new BitSet();
			this.statistics = new VectorStatistics(inputs.getInputDimension());
		}
		
		public final DataSource getInputs() {
			return this.inputs;
		}
		
		public final BitSet getSubset() {
			return this.subset;
		}
		
		public final VectorStatistics getStatistics() {
			return this.statistics;
		}
		
		public final int getDimensionIndex() {
			return this.dimensionIndex;
		}
		
		public final double getScore() {
			return this.score;
		}
		
		public final MedianCutClustering.Chunk analyze() {
			final VectorStatistics statistics = this.getStatistics();
			final int d = statistics.getStatistics().length;
			int i = -1;
			
			for (final Datum classification : this.getInputs()) {
				if (this.contains(++i)) {
					statistics.addValues(classification.getValue());
				}
			}
			
			for (i = 0; i < d; ++i) {
				// Original median-cut would use amplitude as score, but I believe variance makes more sense for general clustering
				final double score = this.getStatistics().getStatistics()[i].getVariance();
				
				if (this.getScore() < score) {
					this.score = score;
					this.dimensionIndex = i;
				}
			}
			
			return this;
		}
		
		public final MedianCutClustering.Chunk[] cut() {
			final MedianCutClustering.Chunk[] result = { new Chunk(this.getInputs(), false), new Chunk(this.getInputs(), false) };
			final int n = (int) this.getStatistics().getCount();
			final int[] indexIndices = intRange(n);
			final int[] indices = new int[n];
			final double[] values = new double[n];
			final int j = this.getDimensionIndex();
			int i = -1;
			int k = -1;
			
			for (final Datum classification : this.getInputs()) {
				if (this.contains(++i)) {
					indices[++k] = i;
					values[k] = classification.getValue()[j];
				}
			}
			
			sort(indexIndices, 0, n, new IntComparator() {
				
				@Override
				public final int compare(final int index1, final int index2) {
					return Double.compare(values[index1], values[index2]);
				}
				
				private static final long serialVersionUID = -1853523891974367332L;
				
			});
			
			for (i = 0; i < n / 2; ++i) {
				result[0].getSubset().set(indices[indexIndices[i]]);
			}
			
			for (; i < n; ++i) {
				result[1].getSubset().set(indices[indexIndices[i]]);
			}
			
			Arrays.stream(result).forEach(Chunk::analyze);
			
			return result;
		}
		
		public final boolean contains(final int index) {
			return this.getSubset() == null || this.getSubset().get(index);
		}
		
		private static final long serialVersionUID = 8016924428574511333L;
		
		public static final int toInt(final boolean value) {
			return value ? 1 : 0;
		}
		
	}
	
}