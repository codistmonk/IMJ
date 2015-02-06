package imj3.draft.segmentation2;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static imj3.draft.segmentation2.Analyze.DataSourceArrayProperty.CLASS;
import static imj3.draft.segmentation2.Analyze.DataSourceArrayProperty.INPUT;
import static java.lang.Math.max;
import imj3.core.Channels;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;
import imj3.tools.AwtImage2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

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
		final Mean mean = new Mean(image, 2, 1, 2);
//		SwingTools.show(convert(mean, INPUT), "Mean", false);
//		SwingTools.show(convert(new Max(image, 2, 1, 2), INPUT), "Max", false);
		
		SwingTools.show(convert(new ClassifiedImageDataSource<>(mean, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(mean)), CLASS), "Indirect (streaming)", false);
		SwingTools.show(convert(new ClassifiedImageDataSource<>(mean, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(mean)), CLASS), "Indirect (k-means)", false);
	}
	
	public static final BufferedImage convert(final ImageDataSource<Prototype> source, final DataSourceArrayProperty property) {
		return convert(source, property, null);
	}
	
	public static final BufferedImage convert(final ImageDataSource<Prototype> source, final DataSourceArrayProperty property, final BufferedImage result) {
		final int dimension = property.getDimension(source);
		
		if (dimension != 1 && dimension != 3) {
			throw new IllegalArgumentException();
		}
		
		final int width = source.sizeX();
		final int height = source.sizeY();
		final BufferedImage actualResult = result != null ? result : new BufferedImage(
				width, height, BufferedImage.TYPE_INT_ARGB);
		
		int pixel = 0;
		
		for (final Classification<Prototype> c : source) {
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
	public static enum DataSourceArrayProperty {
		
		INPUT {
			
			@Override
			public final int getDimension(final DataSource<?> source) {
				return source.getInputDimension();
			}
			
			@Override
			public final double[] getArray(final Classification<?> classification) {
				return classification.getInput();
			}
			
		}, CLASS {
			
			@Override
			public final int getDimension(final DataSource<?> source) {
				return source.getClassDimension();
			}
			
			@Override
			public final double[] getArray(final Classification<?> classification) {
				return classification.getClassifierClass().toArray();
			}
			
		};
		
		public abstract int getDimension(DataSource<?> source);
		
		public abstract double[] getArray(Classification<?> classification);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class ClassifiedImageDataSource<In extends ClassifierClass, Out extends ClassifierClass> extends ImageDataSource<Out> {
		
		private final ImageDataSource<In> source;
		
		private final Classifier<Out> classifier;
		
		public ClassifiedImageDataSource(final ImageDataSource<In> source, final Classifier<Out> classifier) {
			super(source.getPatchSize(), source.getPatchSparsity(), source.getStride());
			this.source = source;
			this.classifier = classifier;
		}
		
		public final ImageDataSource<In> getSource() {
			return this.source;
		}
		
		public final Classifier<Out> getClassifier() {
			return this.classifier;
		}
		
		@Override
		public final int getInputDimension() {
			return this.getSource().getClassDimension();
		}
		
		@Override
		public final int getClassDimension() {
			return this.getClassifier().getClassDimension(this.getInputDimension());
		}
		
		@Override
		public final Iterator<Classification<Out>> iterator() {
			return new Iterator<Classification<Out>>() {
				
				private final Iterator<Classification<In>> inputs = ClassifiedImageDataSource.this.getSource().iterator();
				
				@Override
				public final boolean hasNext() {
					return this.inputs.hasNext();
				}
				
				@Override
				public final Classification<Out> next() {
					final Classification<Out> result = ClassifiedImageDataSource.this.getClassifier().classify(
							this.inputs.next().getClassifierClass().toArray());
					
					return result;
				}
				
			};
		}
		
		@Override
		public int getImageWidth() {
			return this.getSource().getImageWidth();
		}
		
		@Override
		public final int getImageHeight() {
			return this.getSource().getImageHeight();
		}
		
		private static final long serialVersionUID = -4480722678689454042L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static abstract class ImageDataSource<C extends ClassifierClass> implements DataSource<C> {
		
		private final int patchSize;
		
		private final int patchSparsity;
		
		private final int stride;
		
		public ImageDataSource(final int patchSize) {
			this(patchSize, 1, 1);
		}
		
		public ImageDataSource(final int patchSize, final int patchSparsity, final int stride) {
			this.patchSize = patchSize;
			this.patchSparsity = patchSparsity;
			this.stride = stride;
		}
		
		public final int getPatchSize() {
			return this.patchSize;
		}
		
		public final int getPatchSparsity() {
			return this.patchSparsity;
		}
		
		public final int getStride() {
			return this.stride;
		}
		
		@Override
		public final int size() {
			return this.sizeX() * this.sizeY();
		}
		
		public final int sizeX() {
			final int stride = this.getStride();
			final int offset = stride / 2;
			
			return 1 + (this.getImageWidth() - offset) / stride;
		}
		
		public final int sizeY() {
			final int stride = this.getStride();
			final int offset = stride / 2;
			
			return 1 + (this.getImageHeight() - offset) / stride;
		}
		
		public final int getPatchPixelCount() {
			final int n = this.getPatchSize() / this.getPatchSparsity();
			
			return n * n;
		}
		
		public abstract int getImageWidth();
		
		public abstract int getImageHeight();
		
		private static final long serialVersionUID = -5424770105639516510L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static abstract class BufferedImageDataSource<C extends ClassifierClass> extends ImageDataSource<C> {
		
		private final BufferedImage image;
		
		public BufferedImageDataSource(final BufferedImage image, final int patchSize) {
			this(image, patchSize, 1, 1);
		}
		
		public BufferedImageDataSource(final BufferedImage image, final int patchSize,
				final int patchSparsity, final int stride) {
			super(patchSize, patchSparsity, stride);
			this.image = image;
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		@Override
		public final int getImageWidth() {
			return this.getImage().getWidth();
		}
		
		@Override
		public final int getImageHeight() {
			return this.getImage().getHeight();
		}
		
		@Override
		public final Iterator<Classification<C>> iterator() {
			final int imageWidth = this.getImageWidth();
			final int imageHeight = this.getImageHeight();
			final int stride = this.getStride();
			final int offset = stride / 2;
			
			return new Iterator<Classification<C>>() {
				
				private int x = offset;
				
				private int y = this.x;
				
				private final int[] patchData = new int[BufferedImageDataSource.this.getPatchPixelCount()];
				
				private final Object context = BufferedImageDataSource.this.newContext();
				
				@Override
				public final boolean hasNext() {
					return this.y < imageHeight && this.x < imageWidth;
				}
				
				@Override
				public final Classification<C> next() {
					BufferedImageDataSource.this.extractPatchValues(this.x, this.y, this.patchData);
					
					final Classification<C> result = BufferedImageDataSource.this.convert(this.x, this.y, this.patchData, this.context);
					
					if (imageWidth <= (this.x += stride)) {
						this.x = offset;
						this.y += stride;
					}
					
					return result;
				}
				
			};
		}
		
		public final void extractPatchValues(final int x, final int y, final int[] result) {
			Arrays.fill(result, 0);
			final int s = this.getPatchSize();
			final int half = s / 2;
			final int x0 = x - half;
			final int x1 = x0 + s;
			final int y0 = y - half;
			final int y1 = y0 + s;
			final int step = this.getPatchSparsity();
			final int bufferWidth = s / step;
			final BufferedImage image = this.getImage();
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			
			for (int yy = y0, i = 0; yy < y1; yy += step) {
				if (0 <= yy && yy < imageHeight) {
					for (int xx = x0; xx < x1; xx += step, ++i) {
						if (0 <= xx && xx < imageWidth) {
							result[i] = image.getRGB(xx, yy);
						}
					}
				} else {
					i += bufferWidth;
				}
			}
		}
		
		protected abstract Object newContext();
		
		protected abstract Classification<C> convert(int x, int y, int[] patchValues, Object context);
		
		private static final long serialVersionUID = -774979627942684978L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static abstract class PrototypeSource extends BufferedImageDataSource<Prototype> {
		
		public PrototypeSource(final BufferedImage image, final int patchSize) {
			super(image, patchSize);
		}
		
		public PrototypeSource(final BufferedImage image, final int patchSize,
				final int patchSparsity, final int stride) {
			super(image, patchSize, patchSparsity, stride);
		}
		
		@Override
		public final int getClassDimension() {
			return this.getInputDimension();
		}
		
		@Override
		protected final Context newContext() {
			return this.new Context();
		}
		
		@Override
		protected final Classification<Prototype> convert(final int x, final int y, final int[] patchValues, final Object context) {
			final Context c = (Context) context;
			
			this.convert(x, y, patchValues, c.getDatum());
			
			return c.getClassification();
		}
		
		protected abstract void convert(int x, int y, int[] patchValues, double[] result);
		
		private static final long serialVersionUID = 1904026018619304970L;
		
		/**
		 * @author codistmonk (creation 2015-02-06)
		 */
		public final class Context implements Serializable {
			
			private final double[] datum;
			
			private final Classification<Prototype> classification;
			
			public Context() {
				this.datum = new double[PrototypeSource.this.getInputDimension()];
				this.classification = new Classification<>(this.datum, new Prototype(this.datum), 0.0);
			}
			
			public final double[] getDatum() {
				return this.datum;
			}
			
			public final Classification<Prototype> getClassification() {
				return this.classification;
			}
			
			private static final long serialVersionUID = -7200337284453859382L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Raw extends PrototypeSource {
		
		public Raw(final BufferedImage image, final int patchSize) {
			super(image, patchSize);
		}
		
		public Raw(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
			super(image, patchSize, patchSparsity, stride);
		}
		
		@Override
		public final int getInputDimension() {
			return 3 * this.getPatchPixelCount();
		}
		
		@Override
		protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
			int i = -1;
			
			for (final int value : patchValues) {
				result[++i] = red8(value);
				result[++i] = green8(value);
				result[++i] = blue8(value);
			}
		}
		
		private static final long serialVersionUID = 3938160512172714562L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Mean extends PrototypeSource {
		
		public Mean(final BufferedImage image, final int patchSize) {
			super(image, patchSize);
		}
		
		public Mean(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
			super(image, patchSize, patchSparsity, stride);
		}
		
		@Override
		public final int getInputDimension() {
			return 3;
		}
		
		@Override
		protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
			Arrays.fill(result, 0.0);
			
			for (final int value : patchValues) {
				result[0] += red8(value);
				result[1] += green8(value);
				result[2] += blue8(value);
			}
			
			KMeansClustering.divide(result, patchValues.length);
		}
		
		private static final long serialVersionUID = 3938160512172714562L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Max extends PrototypeSource {
		
		public Max(final BufferedImage image, final int patchSize) {
			super(image, patchSize);
		}
		
		public Max(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
			super(image, patchSize, patchSparsity, stride);
		}
		
		@Override
		public final int getInputDimension() {
			return 1;
		}
		
		@Override
		protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
			double max = 0.0;
			
			for (final int value : patchValues) {
				max = max(max, max(red8(value), max(green8(value), blue8(value))));
			}
			
			result[0] = max;
		}
		
		private static final long serialVersionUID = -3551891713169505385L;
		
	}
	
}
