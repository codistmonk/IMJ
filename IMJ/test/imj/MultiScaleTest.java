package imj;

import static imj.IMJTools.image;
import static imj.ImageComponent.awtImage;
import static imj.Labeling.CONNECTIVITY_4;
import static imj.Labeling.CONNECTIVITY_8;
import static imj.MorphologyOperations.edges8;
import static imj.MorphologyOperations.hMinima8;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;
import imj.WatershedTest.WatershedStack;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-29)
 */
public final class MultiScaleTest {
	
	@Test
	public final void test1() throws FileNotFoundException, IOException {
		final TicToc timer = new TicToc();
		final String imageId = "test/imj/12003.jpg";
//		final String imageId = "../Libraries/images/16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Extracting edges:", new Date(timer.tic()));
		final Image edges = edges8(image);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		for (int h = 0; h < 256; ++h) {
			debugPrint("Reducing minima:", new Date(timer.tic()));
			debugPrint("h:", h);
			final Image reduced = hMinima8(edges, h);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing markers:", new Date(timer.tic()));
			final Image initialLabels = new RegionalMinima(reduced, CONNECTIVITY_4).getResult();
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing watershed:", new Date(timer.tic()));
			final Image labels = new Watershed(edges, initialLabels, CONNECTIVITY_8).getResult();
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Generating result:", new Date(timer.tic()));
			final Image result = IMJTools.newImage(labels, IMJTools.getRegionStatistics(image, labels), StatisticsSelector.MEAN);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Writing file:", new Date(timer.tic()));
			final String outfile = imageId + ".hminima_" + h + ".watershed8.mean.png";
			
			ImageIO.write(
					awtImage(result, false, new BufferedImage(image.getColumnCount(), image.getRowCount(), TYPE_3BYTE_BGR)),
					"png",
					new FileOutputStream(new File(outfile))); 
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		}
		
//		ImageComponent.showAdjusted(imageId, marker);
	}
	
}
