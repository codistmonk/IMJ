package imj3.draft.segmentation2;

import imj3.core.Channels;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.ClassifiedDataSource;
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

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
		final BufferedImage image = AwtImage2D.awtRead(file.getPath());
		
		SwingTools.show(image, file.getName(), false);
		
		if (false) {
			final BufferedImageMeanSource mean = new BufferedImageMeanSource(image, 2, 1, 2);
			SwingTools.show(awtImage(mean), "Mean", false);
			SwingTools.show(awtImage(new BufferedImageMaxSource(image, 2, 1, 2)), "Max", false);
			
			SwingTools.show(awtImage(new ClassDataSource<>(new ClassifiedDataSource<>(mean, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(mean)))), "Indirect (streaming)", false);
			SwingTools.show(awtImage(new ClassDataSource<>(new ClassifiedDataSource<>(mean, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(mean)))), "Indirect (k-means)", false);
		}
		
		if (true) {
			final DataSource<? extends ImageDataSource.Metadata, Prototype> source = new BufferedImageRawSource(image, 8);
			final DataSource<? extends ImageDataSource.Metadata, Prototype> trainingSet = source;
			
			final NearestNeighborClustering clustering = new KMeansClustering(Measure.Predefined.L1_ES, 3);
//			final NearestNeighborClustering clustering = new StreamingClustering(Measure.Predefined.L1_ES, 3);
			final NearestNeighborClassifier quantizer = clustering.cluster(trainingSet);
			final DataSource<? extends ImageDataSource.Metadata, Prototype> quantized = new ClassifiedDataSource<>(source, quantizer);
			final LinearTransform rgbRenderer = new LinearTransform(Measure.Predefined.L1_ES, newRGBRenderingMatrix(source.getMetadata().getPatchPixelCount()));
			final DataSource<? extends ImageDataSource.Metadata, ?> rendered = new ClassifiedDataSource<>(quantized, rgbRenderer);
			
			SwingTools.show(awtImage(new ClassDataSource<>(rendered)), clustering.getClass().getSimpleName() + " -> rendered", false);
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
	
	public static final BufferedImage awtImage(final DataSource<? extends ImageDataSource.Metadata, ?> source) {
		return awtImage(source, null);
	}
	
	public static final BufferedImage awtImage(final DataSource<? extends ImageDataSource.Metadata, ?> source, final BufferedImage result) {
		final TicToc timer = new TicToc();
		final int dimension = source.getInputDimension();
		
		if (dimension != 1 && dimension != 3) {
			throw new IllegalArgumentException();
		}
		
		final int width = source.getMetadata().sizeX();
		final int height = source.getMetadata().sizeY();
		final BufferedImage actualResult = result != null ? result : new BufferedImage(
				width, height, BufferedImage.TYPE_INT_ARGB);
		
		int pixel = 0;
		
		for (final Classification<?> c : source) {
			final int x = pixel % width;
			final int y = pixel / width;
			final double[] input = c.getInput();
			final int rgb = dimension == 1 ? gray(input) : argb(input);
			
			actualResult.setRGB(x, y, rgb);
			
			++pixel;
		}
		
		Tools.debugPrint("Awt image created in", timer.toc(), "ms");
		
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
	 * @author codistmonk (creation 2015-02-08)
	 * 
	 * @param <M>
	 * @param <C>
	 */
	public static final class ClassDataSource<M extends DataSource.Metadata> extends DataSource.Abstract<M, ClassifierClass> {
		
		private final DataSource<M, ClassifierClass> source;
		
		@SuppressWarnings("unchecked")
		public ClassDataSource(final DataSource<M, ?> source) {
			super(source.getMetadata());
			this.source = (DataSource<M, ClassifierClass>) source;
		}
		
		@Override
		public final int getInputDimension() {
			return this.getSource().getClassDimension();
		}
		
		@Override
		public final int getClassDimension() {
			return this.getInputDimension();
		}
		
		@Override
		public final Iterator<Classification<ClassifierClass>> iterator() {
			return new Iterator<Classification<ClassifierClass>>() {
				
				private final Iterator<Classification<ClassifierClass>> i = ClassDataSource.this.getSource().iterator();
				
				private final Classification<MutableClassifierClass> result = new Classification<>(null, new MutableClassifierClass(), 0.0);
				
				@Override
				public final boolean hasNext() {
					return this.i.hasNext();
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public final Classification<ClassifierClass> next() {
					return (Classification) this.result.setInput(
							this.result.getClassifierClass().setArray(
									this.i.next().getClassifierClass().toArray()).toArray());
				}
				
			};
		}
		
		final DataSource<M, ClassifierClass> getSource() {
			return this.source;
		}
		
		private static final long serialVersionUID = -4926248507282321872L;
		
		/**
		 * @author codistmonk (creation 2015-02-08)
		 */
		public static final class MutableClassifierClass implements ClassifierClass {
			
			private double[] array;
			
			public final MutableClassifierClass setArray(final double[] array) {
				this.array = array;
				
				return this;
			}
			
			@Override
			public final double[] toArray() {
				return this.array;
			}
			
			private static final long serialVersionUID = 8563042210828121994L;
			
		}
		
	}
	
}
