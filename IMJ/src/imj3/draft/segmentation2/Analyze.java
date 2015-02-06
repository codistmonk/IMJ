package imj3.draft.segmentation2;

import static imj3.draft.segmentation2.DataSourceArrayProperty.CLASS;
import static imj3.draft.segmentation2.DataSourceArrayProperty.INPUT;
import imj3.core.Channels;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.KMeansClustering;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.StreamingClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;
import imj3.tools.AwtImage2D;

import java.awt.image.BufferedImage;
import java.io.File;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
			SwingTools.show(convert(mean, INPUT), "Mean", false);
			SwingTools.show(convert(new BufferedImageMaxSource(image, 2, 1, 2), INPUT), "Max", false);
			
			SwingTools.show(convert(new ClassifiedImageDataSource<>(mean, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(mean)), CLASS), "Indirect (streaming)", false);
			SwingTools.show(convert(new ClassifiedImageDataSource<>(mean, new KMeansClustering(Measure.Predefined.L1_ES, 8, 6).cluster(mean)), CLASS), "Indirect (k-means)", false);
		}
		
		if (true) {
			final BufferedImageRawSource source = new BufferedImageRawSource(image, 1);
			
			SwingTools.show(convert(new ClassifiedImageDataSource<>(source, new StreamingClustering(Measure.Predefined.L1_ES, 8).cluster(source)), CLASS), "Indirect (streaming)", false);
		}
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
	
}
