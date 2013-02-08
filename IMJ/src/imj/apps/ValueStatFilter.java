package imj.apps;

import static imj.apps.GrayscaleWatershed.message;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import imj.IMJTools.StatisticsSelector;
import imj.Image;
import imj.ImageComponent;
import imj.ImageWrangler;
import imj.Labeling;
import imj.Labeling.MemoryManagementStrategy;
import imj.MorphologicalOperations.StructuringElement;
import imj.ValueStatisticsFilter;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-02-08)
 */
public final class ValueStatFilter {
	
	private ValueStatFilter() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final int lod = arguments.get("lod", 0)[0];
		final String prioritizeWhat = arguments.get("prioritize", "speed").toLowerCase(Locale.ENGLISH);
		
		if ("memory".equals(prioritizeWhat)) {
			Labeling.setMemoryManagementStrategy(MemoryManagementStrategy.PRIORITIZE_MEMORY);
		}
		
		final StatisticsSelector selector = StatisticsSelector.MEAN;
		final int[] structuringElement = StructuringElement.SIMPLE_CONNECTIVITY_8;
		final TicToc timer = new TicToc();
		
		message("Loading image", imageId, "lod", lod);
		timer.tic();
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		message("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		message("Applying statistical filter", selector, "on values");
		timer.tic();
		final Image result = new ValueStatisticsFilter(image, selector, structuringElement).getResult();
		message("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		try {
			message("Writing result:", new Date(timer.tic()));
			
			final int columnCount = image.getColumnCount();
			final int rowCount = image.getRowCount();
			
			ImageIO.write(ImageComponent.awtImage(result, true,
					new BufferedImage(columnCount, rowCount, BufferedImage.TYPE_BYTE_GRAY)),
					"png", new FileOutputStream(imageId + ".lod" + lod + ".values." + selector +
							".se" + (structuringElement.length / 2 - 1) + ".png"));
			
			message("Done:", "time:", timer.toc(), "memory:", usedMemory());
		} catch (final IOException exception) {
			System.err.println(exception);
		}
		
	}
	
}
