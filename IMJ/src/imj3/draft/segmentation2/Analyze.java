package imj3.draft.segmentation2;

import static imj3.draft.segmentation2.DataSourceArrayProperty.CLASS;
import static imj3.draft.segmentation2.DataSourceArrayProperty.INPUT;
import imj3.core.Channels;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.NearestNeighborClassifier;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;
import imj3.draft.segmentation2.Analyze.LinearTransform.Transformed;
import imj3.tools.AwtImage2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
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
		final BufferedImage image = AwtImage2D.awtRead(file.getPath());
		
		SwingTools.show(image, file.getName(), false);
		
		if (false) {
			final BufferedImageMeanSource mean = new BufferedImageMeanSource(image, 2, 1, 2);
			SwingTools.show(awtImage(mean, INPUT), "Mean", false);
			SwingTools.show(awtImage(new BufferedImageMaxSource(image, 2, 1, 2), INPUT), "Max", false);
			
			SwingTools.show(awtImage(new ClassifiedImageDataSource<>(mean, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(mean)), CLASS), "Indirect (streaming)", false);
			SwingTools.show(awtImage(new ClassifiedImageDataSource<>(mean, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(mean)), CLASS), "Indirect (k-means)", false);
		}
		
		if (true) {
			final BufferedImageRawSource source = new BufferedImageRawSource(image, 4);
			final DataSource<Prototype> trainingSet = source;
			final NearestNeighborClassifier quantizer = new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(trainingSet);
			final ClassifiedImageDataSource<Prototype, Prototype> quantized = new ClassifiedImageDataSource<>(source, quantizer);
			final LinearTransform rgbRenderer = new LinearTransform(Measure.Predefined.L1_ES, newRGBRenderingMatrix(source.getPatchPixelCount()));
			final ClassifiedImageDataSource<Prototype, Transformed> rendered = new ClassifiedImageDataSource<>(quantized, rgbRenderer);
			
			SwingTools.show(awtImage(rendered, CLASS), "Quantized (streaming) -> rendered", false);
		}
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
	
	public static final BufferedImage awtImage(final ImageDataSource<?> source, final DataSourceArrayProperty property) {
		return awtImage(source, property, null);
	}
	
	public static final BufferedImage awtImage(final ImageDataSource<?> source, final DataSourceArrayProperty property, final BufferedImage result) {
		final int dimension = property.getDimension(source);
		
		if (dimension != 1 && dimension != 3) {
			throw new IllegalArgumentException();
		}
		
		final int width = source.sizeX();
		final int height = source.sizeY();
		final BufferedImage actualResult = result != null ? result : new BufferedImage(
				width, height, BufferedImage.TYPE_INT_ARGB);
		
		int pixel = 0;
		
		for (final Classification<?> c : source) {
			final int x = pixel % width;
			final int y = pixel / width;
			final double[] input = property.getArray(c);
			final int rgb = dimension == 1 ? gray(input) : argb(input);
			
			actualResult.setRGB(x, y, rgb);
			
			++pixel;
		}
		
		return actualResult;
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
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class LinearTransform implements Classifier<LinearTransform.Transformed> {
		
		private final double[][] matrix;
		
		private final ClassifierClass.Measure<Transformed> transformedMeasure;
		
		public LinearTransform(final Measure measure, double[][] matrix) {
			this.matrix = matrix;
			this.transformedMeasure = new ClassifierClass.Measure.Default<>(measure);
		}
		
		public final double[][] getMatrix() {
			return this.matrix;
		}
		
		public final int getMatrixRowCount() {
			return this.getMatrix().length;
		}
		
		public final int getMatrixColumnCount() {
			return this.getMatrix()[0].length;
		}
		
		@Override
		public final ClassifierClass.Measure<Transformed> getClassMeasure() {
			return this.transformedMeasure;
		}
		
		@Override
		public final Classification<Transformed> classify(final double... input) {
			final int n = this.getMatrixRowCount();
			final double[] datum = new double[n];
			
			for (int i = 0; i < n; ++i) {
				datum[i] = dot(this.getMatrix()[i], input);
			}
			
			return new Classification<>(input, new Transformed(datum), 0.0);
		}
		
		@Override
		public final int getClassDimension(final int inputDimension) {
			if (inputDimension == this.getMatrixColumnCount()) {
				return this.getMatrixRowCount();
			}
			
			Tools.debugPrint(inputDimension, this.getMatrixRowCount(), this.getMatrixColumnCount());
			
			throw new IllegalArgumentException();
		}
		
		private static final long serialVersionUID = -1314577713442253655L;
		
		public static final double dot(final double[] v1, final double[] v2) {
			final int n = v1.length;
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				result += v1[i] * v2[i];
			}
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2015-02-06)
		 */
		public static final class Transformed implements ClassifierClass {
			
			private final double[] datum;
			
			public Transformed(final double[] datum) {
				this.datum = datum;
			}
			
			@Override
			public final double[] toArray() {
				return this.datum;
			}
			
			private static final long serialVersionUID = 5806137995866347277L;
			
		}
		
	}
	
}
