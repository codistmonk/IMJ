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
	
	private final int cellRowCount;
	
	private final int cellColumnCount;
	
	public SeamGridSegmenter(final int cellRowCount, final int cellColumnCount) {
		this.cellRowCount = cellRowCount;
		this.cellColumnCount = cellColumnCount;
	}
	
	public SeamGridSegmenter(final int cellSize) {
		this(cellSize, cellSize);
	}
	
	public final void setSeams(final Image image, final RegionOfInterest segmentation) {
		SeamGridSegmentationSieve.setSeams(image, Channel.Primitive.RGB, this.cellRowCount, this.cellColumnCount, segmentation);
	}
	
	@Override
	public final void process(final Image image, final PixelProcessor processor) {
		final RegionOfInterest segmentation = new RegionOfInterest.UsingBitSet(image.getRowCount(), image.getColumnCount());
		
		this.setSeams(image, segmentation);
		
		forEachPixelInEachComponent4(segmentation, true, processor);
	}
	
}
