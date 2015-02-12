package imj3.draft.processing;

import static imj3.draft.machinelearning.ClassDataSource.classes;
import static imj3.draft.machinelearning.Max.max;
import static imj3.draft.machinelearning.Mean.mean;
import static imj3.draft.processing.Image2DRawSource.raw;
import static java.lang.Math.rint;

import imj2.tools.BitwiseQuantizationTest.DoubleArrayComparator;

import imj3.core.Channels;
import imj3.draft.machinelearning.BufferedDataSource;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.ClassifiedDataSource;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.LinearTransform;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.MedianCutClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier;
import imj3.draft.machinelearning.NearestNeighborClustering;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;
import imj3.tools.AwtImage2D;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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
			
			Tools.debugPrint(new Histogram().add(source).getCounts().size());
			
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
	 * @author codistmonk (creation 2015-02-12)
	 */
	public static final class Histogram implements Serializable {
		
		private final Map<double[], double[]> counts = new TreeMap<>(new DoubleArrayComparator());
		
		private long totalCount;
		
		public final Map<double[], double[]> getCounts() {
			return this.counts;
		}
		
		public final long getTotalCount() {
			return this.totalCount;
		}
		
		public final Histogram reset() {
			this.getCounts().clear();
			this.totalCount = 0L;
			
			return this;
		}
		
		public final Histogram add(final double... input) {
			++this.getCounts().computeIfAbsent(input, i -> new double[1])[0];
			++this.totalCount;
			
			return this;
		}
		
		public final Histogram add(final DataSource<?, ?> inputs) {
			for (final Classification<?> classification : inputs) {
				this.add(classification.getInput().clone());
			}
			
			return this;
		}
		
		public final Histogram normalize() {
			final long n = this.getTotalCount();
			
			if (0L < n) {
				for (final double[] count : this.getCounts().values()) {
					count[0] /= n;
				}
			}
			
			return this;
		}
		
		public final Histogram denormalize() {
			final long n = this.getTotalCount();
			
			if (0L < n) {
				for (final double[] count : this.getCounts().values()) {
					count[0] = rint(count[0] * n);
				}
			}
			
			return this;
		}
		
		public final double[] pack(final int binCount) {
			final double[] result = new double[binCount];
			
			for (final Map.Entry<double[], double[]> entry : this.getCounts().entrySet()) {
				final double[] key = entry.getKey();
				
				if (key.length != 1) {
					throw new IllegalArgumentException();
				}
				
				result[(int) key[0]] = entry.getValue()[0];
			}
			
			return result;
		}
		
		private static final long serialVersionUID = -4974336898629198663L;
		
	}
	
}
