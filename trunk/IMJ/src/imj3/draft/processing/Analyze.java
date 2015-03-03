package imj3.draft.processing;

import static imj3.draft.machinelearning.BufferedDataSource.buffer;
import static imj3.draft.machinelearning.ClassDataSource.classes;
import static imj3.draft.machinelearning.Max.max;
import static imj3.draft.machinelearning.Mean.mean;
import static imj3.draft.processing.Image2DRawSource.raw;

import imj3.core.Channels;
import imj3.draft.machinelearning.ClassifiedDataSource;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.Datum;
import imj3.draft.machinelearning.Histogram;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.LinearTransform;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.MedianCutClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier;
import imj3.draft.machinelearning.NearestNeighborClustering;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.tools.AwtImage2D;

import java.io.File;
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
			final Image2DSource raw = raw(image, 2, 1, 2);
			
			SwingTools.show(image(classes(mean(raw, 3))).getSource(), "Mean", false);
			SwingTools.show(image(classes(max(raw))).getSource(), "Max", false);
			
			SwingTools.show(image(classes(mean(classes(classify(raw, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(raw))), 3))).getSource(), "Indirect (streaming)", false);
			SwingTools.show(image(classes(mean(classes(classify(raw, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(raw))), 3))).getSource(), "Indirect (k-means)", false);
		}
		
		if (true) {
			final DataSource source = buffer(raw(image, 8, 1, 8));
			
			Tools.debugPrint(new Histogram().add(source).getCounts().size());
			
			final DataSource trainingSet = source;
			
//			final NearestNeighborClustering clustering = new KMeansClustering(Measure.Predefined.L2_ES, 256, 8);
			final NearestNeighborClustering clustering = new MedianCutClustering(Measure.Predefined.L2_ES, 256);
//			final NearestNeighborClustering clustering = new StreamingClustering(Measure.Predefined.L1_ES, 3);
			final NearestNeighborClassifier quantizer = clustering.cluster(trainingSet);
			final DataSource quantized = classify(source, quantizer);
			final LinearTransform rgbRenderer = new LinearTransform(Measure.Predefined.L2_ES, newRGBRenderingMatrix(source.findSource(Patch2DSource.class).getPatchPixelCount()));
			final DataSource rendered = classify(classes(quantized), rgbRenderer);
			
//			SwingTools.show(image(classes(mean(classes(quantized), 3))).getSource(), clustering.getClass().getSimpleName() + " -> rendered", false);
			SwingTools.show(image(classes(rendered)).getSource(), clustering.getClass().getSimpleName() + " -> rendered", false);
		}
	}
	
	public static final ClassifiedDataSource classify(final DataSource source, final Classifier classifier) {
		return new ClassifiedDataSource(source, classifier);
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
	
	public static final AwtImage2D image(final DataSource source) {
		final Patch2DSource patches = source.findSource(Patch2DSource.class);
		
		return image(source, new AwtImage2D(Long.toHexString(new Random().nextLong()),
				patches.sizeX(), patches.sizeY()));
	}
	
	public static final AwtImage2D image(final DataSource source,
			final AwtImage2D result) {
		final TicToc timer = new TicToc();
		final int dimension = source.getInputDimension();
		
		if (dimension != 1 && dimension != 3) {
			Tools.debugError(dimension);
			throw new IllegalArgumentException();
		}
		
		final int width = source.findSource(Patch2DSource.class).sizeX();
		
		int pixel = 0;
		
		for (final Datum c : source) {
			final int x = pixel % width;
			final int y = pixel / width;
			final double[] input = c.getValue();
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
	
	public static final int gray(final double[] value) {
		final int gray = int8(value[0]);
		
		return Channels.Predefined.a8r8g8b8(0xFF, gray, gray, gray);
	}
	
	public static final int labelColor(final double[] value) {
		final int label = (int) value[0];
		int result = 0xFF000000;
		
		for (int i = 0, m = 1; i < 3; ++i, m <<= 3) {
			result |= (label & (m << 0)) << (5 + 8 * 0 + i);
			result |= (label & (m << 1)) << (5 + 8 * 1 + i);
			result |= (label & (m << 2)) << (5 + 8 * 2 + i);
		}
		
		return result;
	}
	
	public static final int int8(final double value0255) {
		return ((int) value0255) & 0xFF;
	}
	
}
