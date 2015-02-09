package imj3.draft.segmentation2;

import imj3.core.Channels;
import imj3.core.Image2D;
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
import imj3.tools.IMJTools;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
		
//		new BufferedImage(null, new WritableRaster
		
		SwingTools.show(image.getSource(), file.getName(), false);
		
		if (false) {
			final Image2DMeanSource mean = new Image2DMeanSource(image, 2, 1, 2);
			SwingTools.show(image(mean).getSource(), "Mean", false);
			SwingTools.show(image(new Image2DMaxSource(image, 2, 1, 2)).getSource(), "Max", false);
			
			SwingTools.show(image(classes(classify(mean, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(mean)))).getSource(), "Indirect (streaming)", false);
			SwingTools.show(image(classes(classify(mean, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(mean)))).getSource(), "Indirect (k-means)", false);
		}
		
		if (true) {
			final DataSource<? extends ImageDataSource.Metadata, Prototype> source = new Image2DRawSource(image, 8);
			final DataSource<? extends ImageDataSource.Metadata, Prototype> trainingSet = source;
			
			final NearestNeighborClustering clustering = new KMeansClustering(Measure.Predefined.L1_ES, 3);
//			final NearestNeighborClustering clustering = new StreamingClustering(Measure.Predefined.L1_ES, 3);
			final NearestNeighborClassifier quantizer = clustering.cluster(trainingSet);
			final DataSource<? extends ImageDataSource.Metadata, Prototype> quantized = classify(source, quantizer);
			final LinearTransform rgbRenderer = new LinearTransform(Measure.Predefined.L1_ES, newRGBRenderingMatrix(source.getMetadata().getPatchPixelCount()));
			final DataSource<? extends ImageDataSource.Metadata, ?> rendered = classify(quantized, rgbRenderer);
			
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
	
	public static final AwtImage2D image(final DataSource<? extends ImageDataSource.Metadata, ?> source) {
		return image(source, new AwtImage2D(Long.toHexString(new Random().nextLong()),
				source.getMetadata().sizeX(), source.getMetadata().sizeY()));
	}
	
	public static final AwtImage2D image(final DataSource<? extends ImageDataSource.Metadata, ?> source, final AwtImage2D result) {
		final TicToc timer = new TicToc();
		final int dimension = source.getInputDimension();
		
		if (dimension != 1 && dimension != 3) {
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
	
}
