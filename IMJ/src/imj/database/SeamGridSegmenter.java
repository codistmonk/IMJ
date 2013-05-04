package imj.database;

import static imj.IMJTools.forEachPixelInEachComponent4;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.SeamGridSegmentationSieve;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-05-03)
 */
public final class SeamGridSegmenter implements Segmenter {
	
	private final int cellSize;
	
	public SeamGridSegmenter(final int cellSize) {
		this.cellSize = cellSize;
	}
	
	@Override
	public final void process(final Image image, final PixelProcessor processor) {
		final RegionOfInterest segmentation = new RegionOfInterest.UsingBitSet(image.getRowCount(), image.getColumnCount());
		
		SeamGridSegmentationSieve.setSeams(image, Channel.Primitive.RGB, this.cellSize, segmentation);
		
		forEachPixelInEachComponent4(segmentation, true, processor);
	}
	
}
