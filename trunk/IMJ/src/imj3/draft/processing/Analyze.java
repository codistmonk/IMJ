package imj3.draft.processing;

import static imj3.draft.machinelearning.Max.max;
import static imj3.draft.machinelearning.Mean.mean;
import static imj3.draft.processing.Image2DRawSource.raw;
import static net.sourceforge.aprog.tools.Tools.intRange;

import imj2.pixel3d.OrthographicRenderer;
import imj2.pixel3d.OrthographicRenderer.IntComparator;
import imj2.tools.VectorStatistics;

import imj3.core.Channels;
import imj3.draft.machinelearning.BufferedDataSource;
import imj3.draft.machinelearning.ClassDataSource;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.ClassifiedDataSource;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.LinearTransform;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.NearestNeighborClassifier;
import imj3.draft.machinelearning.NearestNeighborClustering;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;
import imj3.tools.AwtImage2D;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Analyze {
	
	private Analyze() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File file = new File(arguments.get("file", ""));
		final AwtImage2D image = new AwtImage2D(file.getPath());
		
		SwingTools.show(image.getSource(), file.getName(), false);
		
		if (false) {
			final DataSource<Image2DSource.Metadata, ?> raw = raw(image, 2, 1, 2);
			
			SwingTools.show(image(classes(mean(raw, 3))).getSource(), "Mean", false);
			SwingTools.show(image(classes(max(raw))).getSource(), "Max", false);
			
			SwingTools.show(image(classes(mean(classes(classify(raw, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(raw))), 3))).getSource(), "Indirect (streaming)", false);
			SwingTools.show(image(classes(mean(classes(classify(raw, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(raw))), 3))).getSource(), "Indirect (k-means)", false);
		}
		
		if (true) {
			final DataSource<? extends Patch2DSource.Metadata, ?> source = new BufferedDataSource<>(raw(image, 1, 1, 1));
			final DataSource<? extends Patch2DSource.Metadata, ?> trainingSet = source;
			
//			final NearestNeighborClustering clustering = new KMeansClustering(Measure.Predefined.L2_ES, 256, 8);
			final NearestNeighborClustering clustering = new MedianCutClustering(Measure.Predefined.L2_ES, 256);
//			final NearestNeighborClustering clustering = new StreamingClustering(Measure.Predefined.L1_ES, 3);
			final NearestNeighborClassifier quantizer = clustering.cluster(trainingSet);
			final DataSource<? extends Patch2DSource.Metadata, Prototype> quantized = classify(source, quantizer);
			final LinearTransform rgbRenderer = new LinearTransform(Measure.Predefined.L2_ES, newRGBRenderingMatrix(source.getMetadata().getPatchPixelCount()));
			final DataSource<? extends Patch2DSource.Metadata, ?> rendered = classify(classes(quantized), rgbRenderer);
			
//			SwingTools.show(image(classes(mean(classes(quantized), 3))).getSource(), clustering.getClass().getSimpleName() + " -> rendered", false);
			SwingTools.show(image(classes(rendered)).getSource(), clustering.getClass().getSimpleName() + " -> rendered", false);
		}
	}
	
	public static final <M extends DataSource.Metadata, In extends ClassifierClass, Out extends ClassifierClass>
	ClassifiedDataSource<M, In, Out> classify(final DataSource<M, In> quantized, final Classifier<Out> rgbRenderer) {
		return new ClassifiedDataSource<>(quantized, rgbRenderer);
	}
	
	public static final <M extends DataSource.Metadata> ClassDataSource<M> classes(final DataSource<M, ?> inputs) {
		return new ClassDataSource<>(inputs);
	}
	
	public static final double[][] newRGBRenderingMatrix(final int inputPatchPixelCount) {
		final double[][] result = new double[3][];
		final int n = 3 * inputPatchPixelCount;
		
		for (int i = 0; i < 3; ++i) {
			final double[] row = new double[n];
			
			for (int j = i; j < n; j += 3) {
				row[j] = 1.0 / inputPatchPixelCount;
			}
			
			result[i] = row;
		}
		
		return result;
	}
	
	public static final AwtImage2D image(final DataSource<? extends Patch2DSource.Metadata, ?> source) {
		return image(source, new AwtImage2D(Long.toHexString(new Random().nextLong()),
				source.getMetadata().sizeX(), source.getMetadata().sizeY()));
	}
	
	public static final AwtImage2D image(final DataSource<? extends Patch2DSource.Metadata, ?> source, final AwtImage2D result) {
		final TicToc timer = new TicToc();
		final int dimension = source.getInputDimension();
		
		if (dimension != 1 && dimension != 3) {
			Tools.debugError(dimension);
			throw new IllegalArgumentException();
		}
		
		final int width = source.getMetadata().sizeX();
		
		int pixel = 0;
		
		for (final Classification<?> c : source) {
			final int x = pixel % width;
			final int y = pixel / width;
			final double[] input = c.getInput();
			final int rgb = dimension == 1 ? gray(input) : argb(input);
			
			result.setPixelValue(x, y, rgb);
			
			++pixel;
		}
		
		Tools.debugPrint("Awt image created in", timer.toc(), "ms");
		
		return result;
	}
	
	public static final int argb(final double[] rgb) {
		return Channels.Predefined.a8r8g8b8(0xFF, int8(rgb[0]), int8(rgb[1]), int8(rgb[2]));
	}
	
	public static final int gray(final double[] rgb) {
		final int gray = int8(rgb[0]);
		
		return Channels.Predefined.a8r8g8b8(0xFF, gray, gray, gray);
	}
	
	public static final int int8(final double value0255) {
		return ((int) value0255) & 0xFF;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-10)
	 */
	//TODO make specific classifier
	public static final class MedianCutClustering extends NearestNeighborClustering {
		
		public MedianCutClustering(final Measure measure, final int clusterCount) {
			super(measure, clusterCount);
		}
		
		@Override
		protected final void cluster(final DataSource<?, ?> inputs, final NearestNeighborClassifier classifier) {
			final Queue<Chunk> chunks = new PriorityQueue<>(new Comparator<Chunk>() {
				
				@Override
				public final int compare(final Chunk chunk1, final Chunk chunk2) {
					return Double.compare(chunk2.getScore(), chunk1.getScore());
				}
				
			});
			
			// TODO handle case inputs.size() < this.getClusterCount()
			
			chunks.add(new Chunk(inputs, true).analyze());
			
			while (chunks.size() < this.getClusterCount()) {
				chunks.addAll(Arrays.asList(chunks.remove().cut()));
			}
			
			for (final Chunk chunk : chunks) {
				classifier.getPrototypes().add(new Prototype(chunk.getStatistics().getMeans()));
			}
		}
		
		private static final long serialVersionUID = 5051949087551037706L;
		
		/**
		 * @author codistmonk (creation 2015-02-10)
		 */
		public static final class Chunk implements Serializable {
			
			private final DataSource<?, ?> inputs;
			
			private final BitSet subset;
			
			private final VectorStatistics statistics;
			
			private int dimensionIndex;
			
			private double score;
			
			public Chunk(final DataSource<?, ?> inputs, final boolean initial) {
				this.inputs = inputs;
				this.subset = initial ? null : new BitSet();
				this.statistics = new VectorStatistics(inputs.getInputDimension());
			}
			
			public final DataSource<?, ?> getInputs() {
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
			
			public final Chunk analyze() {
				final VectorStatistics statistics = this.getStatistics();
				final int d = statistics.getStatistics().length;
				int i = -1;
				
				for (final Classification<?> classification : this.getInputs()) {
					if (this.contains(++i)) {
						statistics.addValues(classification.getInput());
					}
				}
				
				final double n = statistics.getCount();
				
				for (i = 0; i < d; ++i) {
					final double score = this.getStatistics().getStatistics()[this.getDimensionIndex()].getVariance() * n;
					
					if (this.getScore() < score) {
						this.score = score;
						this.dimensionIndex = i;
					}
				}
				
				return this;
			}
			
			public final Chunk[] cut() {
				final Chunk[] result = { new Chunk(this.getInputs(), false), new Chunk(this.getInputs(), false) };
				final int n = (int) this.getStatistics().getCount();
				final int[] indexIndices = intRange(n);
				final int[] indices = new int[n];
				final double[] values = new double[n];
				final int j = this.getDimensionIndex();
				int i = -1;
				int k = -1;
				
				for (final Classification<?> classification : this.getInputs()) {
					if (this.contains(++i)) {
						indices[++k] = i;
						values[k] = classification.getInput()[j];
					}
				}
				
				OrthographicRenderer.dualPivotQuicksort(indexIndices, 0, n, new IntComparator() {
					
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
	
}
