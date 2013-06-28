package imj.apps.modules;

import static imj.MathOperations.compute;
import static imj.MathOperations.BinaryOperator.Predefined.MASKED_BY;
import static imj.apps.modules.ImageComponent.awtImage;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.Image;
import imj.ImageOfInts;
import imj.ImageWrangler;
import imj.database.SeamGridSegmenter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-06-16)
 */
public final class SeamSegmentationTest {
	
	@Test
	public final void test() throws IOException {
		final String imageKey = "45660_lod0_sample1";
		final Image image = ImageWrangler.INSTANCE.load("test/imj/" + imageKey + ".png");
		
		debugPrint(image.getRowCount(), image.getColumnCount());
		
		final int n = 2;
		final int cellSize = image.getRowCount() / n;
		
		for (int i = 0; i < n; ++i) {
			{
				final Image band = copyOf(image, i * cellSize, 0, cellSize, image.getColumnCount());
				final BufferedImage awtBand = new BufferedImage(band.getColumnCount(), band.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
				final RegionOfInterest segmentation = new RegionOfInterest.UsingBitSet(band.getRowCount(), band.getColumnCount());
				
				new SeamGridSegmenter(cellSize, 0).setSeams(band, segmentation);
				
				ImageIO.write(awtImage(band, false, awtBand), "png", new File(imageKey + "_horizontal_band" + cellSize + "_" + (i + 1) + ".png"));
				ImageIO.write(awtImage(segmentation, false, awtBand), "png", new File(imageKey + "_horizontal_band" + cellSize + "_" + (i + 1) + "_segmentation.png"));
			}
			{
				final Image band = copyOf(image, 0, i * cellSize, image.getRowCount(), cellSize);
				final BufferedImage awtBand = new BufferedImage(band.getColumnCount(), band.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
				final RegionOfInterest segmentation = new RegionOfInterest.UsingBitSet(band.getRowCount(), band.getColumnCount());
				
				new SeamGridSegmenter(0, cellSize).setSeams(band, segmentation);
				
				ImageIO.write(awtImage(band, false, awtBand), "png", new File(imageKey + "_vertical_band" + cellSize + "_" + (i + 1) + ".png"));
				ImageIO.write(awtImage(segmentation, false, awtBand), "png", new File(imageKey + "_vertical_band" + cellSize + "_" + (i + 1) + "_segmentation.png"));
			}
		}
		
		{
			final RegionOfInterest segmentation = new RegionOfInterest.UsingBitSet(image.getRowCount(), image.getColumnCount());
			final BufferedImage awtImage = new BufferedImage(image.getColumnCount(), image.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
			
			new SeamGridSegmenter(cellSize, cellSize).setSeams(image, segmentation);
			
			ImageIO.write(awtImage(segmentation, false, awtImage), "png", new File(imageKey + "_segmentation" + cellSize + ".png"));
			ImageIO.write(awtImage(compute(image, MASKED_BY, segmentation), false, awtImage), "png", new File(imageKey + "_segmented" + cellSize + ".png"));
		}
	}
	
	public static final ImageOfInts copyOf(final Image image, final int rowIndex, final int columnIndex, final int rowCount, final int columnCount) {
		final ImageOfInts result = new ImageOfInts(rowCount, columnCount, image.getChannelCount());
		final int rowEnd = rowIndex + rowCount;
		final int columnEnd = columnIndex + columnCount;
		
		for (int row = rowIndex, i = 0; row < rowEnd; ++row) {
			for (int column = columnIndex; column < columnEnd; ++column, ++i) {
				result.setValue(i, image.getValue(row, column));
			}
		}
		
		return result;
	}
	
}
