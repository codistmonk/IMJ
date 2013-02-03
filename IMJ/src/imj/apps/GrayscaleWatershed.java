package imj.apps;

import static imj.IMJTools.getRegionStatistics;
import static imj.IMJTools.newImage;
import static imj.Labeling.CONNECTIVITY_4;
import static imj.Labeling.CONNECTIVITY_8;
import static imj.MorphologicalOperations.edges4;
import static imj.MorphologicalOperations.edges8;
import static imj.MorphologicalOperations.hMinima4;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools;
import imj.Image;
import imj.ImageComponent;
import imj.ImageOfBufferedImage.Feature;
import imj.ImageOfInts;
import imj.ImageWrangler;
import imj.Labeling;
import imj.MorphologicalOperations;
import imj.RegionalMinima;
import imj.TicToc;
import imj.Watershed;
import imj.IMJTools.StatisticsSelector;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-02)
 */
public class GrayscaleWatershed {
	
	private GrayscaleWatershed() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length % 2 != 0 || 8 <= commandLineArguments.length) {
			System.out.println("Arguments: file <imageId> [lod <(N|*)>] [connectivity (4|8)] [h N]");
			System.out.println("Default for lod is *");
			System.out.println("Default for connectivity is 8");
			System.out.println("Default for h is 0");
			
			return;
		}
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final int[] lods = arguments.get("lod");
		final int connectivity = arguments.get("connectivity", 8)[0];
		final int h = arguments.get("h", 0)[0];
		
		if (lods.length == 0) {
			int lod = 0;
			Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			process(image, imageId, lod, connectivity, h);
			
			while (1 < image.getRowCount() && 1 < image.getColumnCount()) {
				image = ImageWrangler.INSTANCE.load(imageId, ++lod);
				process(image, imageId, lod, connectivity, h);
			}
		} else {
			for (final int lod : lods) {
				process(ImageWrangler.INSTANCE.load(imageId, lod), imageId, lod, connectivity, h);
			}
		}
	}
	
	public static final void process(final Image image0, final String imageId, final int lod,
			final int connectivity, final int h) {
		final TicToc timer = new TicToc();
		final int[] deltas = connectivity == 4 ? CONNECTIVITY_4 : CONNECTIVITY_8;
		
		debugPrint("Processing:", "image:", imageId, "lod:", lod, "connectivity:", connectivity, "h:", h,
				"date:", new Date(timer.tic()));
		final int columnCount = image0.getColumnCount();
		final int rowCount = image0.getRowCount();
		
		debugPrint("rowCount:", rowCount, "columnCount:", columnCount);
		
		debugPrint("Converting to grayscale:", new Date(timer.tic()));
		final Image.Abstract image = new ImageOfInts(rowCount, columnCount);
		final int pixelCount = image.getPixelCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			image.setValue(pixel, Feature.MAX_RGB.getValue(image0.getValue(pixel), false));
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Extracting edges:", new Date(timer.tic()));
		final Image edges = connectivity == 4 ? edges4(image) : edges8(image);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Applying h-minima:", new Date(timer.tic()));
		final Image hMinima = connectivity == 4 ? hMinima4(edges, h) : hMinima4(image, h);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Computing markers:", new Date(timer.tic()));
		final Image initialLabels = new RegionalMinima(hMinima, deltas).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Computing watershed:", new Date(timer.tic()));
		final Image labels = new Watershed(hMinima, initialLabels, deltas).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Generating result:", new Date(timer.tic()));
		final Image result = newImage(labels, getRegionStatistics(image, labels), StatisticsSelector.MEAN);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Writing result:", new Date(timer.tic()));
		try {
			ImageIO.write(ImageComponent.awtImage(result, true,
					new BufferedImage(columnCount, rowCount, BufferedImage.TYPE_BYTE_GRAY)),
					"png", new FileOutputStream(imageId + ".lod" + lod + ".hmin" + h + ".watershed.png"));
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		} catch (final IOException exception) {
			System.err.println(exception);
		}
		
		debugPrint("Processing done:", "time:", timer.getTotalTime());
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public static final class VirtualImage extends Image.Abstract {
		
		private final Image rgbs;
		
		private final Feature feature;
		
		public VirtualImage(final Image rgbs, final Feature feature) {
			super(rgbs.getRowCount(), rgbs.getColumnCount());
			this.rgbs = rgbs;
			this.feature = feature;
		}
		
		@Override
		public final int getValue(final int index) {
			return this.feature.getValue(this.rgbs.getValue(index), false);
		}
		
		@Override
		public final int setValue(final int index, final int value) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public float getFloatValue(final int index) {
			return this.getValue(index);
		}
		
		@Override
		public final float setFloatValue(final int index, final float value) {
			throw new UnsupportedOperationException();
		}
		
	}
	
}
