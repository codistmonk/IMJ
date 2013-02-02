package imj;

import static imj.IMJTools.image;
import static imj.ImageComponent.awtImage;
import static imj.Labeling.CONNECTIVITY_8;
import static imj.MorphologicalOperations.edges8;
import static imj.MorphologicalOperations.hMinima8;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.MathTools.Statistics;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-29)
 */
public final class MultiScaleTest {
	
	@Test
	public final void test1() throws FileNotFoundException, IOException {
		final TicToc timer = new TicToc();
		final String root = "test/imj/";
		final String imageId = "12003.jpg";
//		final String root = "../Libraries/images/";
//		final String imageId = "16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(root + imageId, Feature.MAX_RGB);
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
			final Image initialLabels = new RegionalMinima(reduced, CONNECTIVITY_8).getResult();
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing watershed:", new Date(timer.tic()));
			final Image labels = new Watershed(image, initialLabels, CONNECTIVITY_8).getResult();
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Generating result:", new Date(timer.tic()));
			final Image result = IMJTools.newImage(labels, IMJTools.getRegionStatistics(image, labels), StatisticsSelector.MEAN);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Writing file:", new Date(timer.tic()));
			final String outfile = "lib/" + imageId + ".hminima_" + h + ".watershed8.mean.png";
			
			ImageIO.write(
					awtImage(result, false, new BufferedImage(image.getColumnCount(), image.getRowCount(), TYPE_3BYTE_BGR)),
					"png",
					new FileOutputStream(new File(outfile))); 
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		}
		
//		ImageComponent.showAdjusted(imageId, marker);
	}
	
//	@Test
	public final void testVarianceSegmentation1() {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
		final String imageId = "../Libraries/images/16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final Image result = new ImageOfInts(rowCount, columnCount);
		
		debugPrint("Performing adaptive variance analysis:", new Date(timer.tic()));
		final double threshold = 100.0;
		final List<Rectangle> todo = new LinkedList<Rectangle>();
		
		todo.add(new Rectangle(0, 0, columnCount, rowCount));
		
		while (!todo.isEmpty()) {
			final Rectangle r = todo.remove(0);
			
			if (r.isEmpty()) {
				continue;
			}
			
			final Statistics statistics = computeStatistics(image, r.y, r.x, r.y + r.height, r.x + r.width);
			final double variance = statistics.getVariance();
			
			if (variance <= threshold) {
				final int endY = r.y + r.height;
				final int endX = r.x + r.width;
				
				for (int y = r.y; y < endY; ++y) {
					for (int x = r.x; x < endX; ++x) {
//						result.setValue(y, x, (int) statistics.getMean());
						result.setFloatValue(y, x, (float) statistics.getVariance());
					}
				}
			}
			
			if (threshold < variance) {
				final int halfWidth = r.width / 2;
				final int halfHeight = r.height / 2;
				todo.add(new Rectangle(r.x, r.y, halfWidth, halfHeight));
				todo.add(new Rectangle(r.x + halfWidth, r.y, r.width - halfWidth, halfHeight));
				todo.add(new Rectangle(r.x, r.y + halfHeight, halfWidth, r.height - halfHeight));
				todo.add(new Rectangle(r.x + halfWidth, r.y + halfHeight, r.width - halfWidth, r.height - halfHeight));
			}
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		DensityTest.normalize(result, result, +0.0F, +255.0F);
		
		ImageComponent.showAdjusted(imageId, image, result);
	}
	
	public static final Statistics computeStatistics(final Image image, final int firstRowIndex, final int firstColumnIndex, final int endRowIndex, final int endColumnIndex) {
		final Statistics result = new Statistics();
		
		for (int rowIndex = firstRowIndex; rowIndex < endRowIndex; ++rowIndex) {
			for (int columnIndex = firstColumnIndex; columnIndex < endColumnIndex; ++columnIndex) {
				result.addValue(image.getValue(rowIndex, columnIndex));
			}
		}
		
		return result;
	}
	
}
