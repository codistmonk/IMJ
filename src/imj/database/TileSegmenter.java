package imj.database;

import static imj.IMJTools.forEachPixelInEachTile;

import imj.Image;
import imj.IMJTools.PixelProcessor;

/**
 * @author codistmonk (creation 2013-05-03)
 */
public final class TileSegmenter implements Segmenter {
	
	private final int tileRowCount;
	
	private final int tileColumnCount;
	
	private final int verticalTileStride;
	
	private final int horizontalTileStride;
	
	public TileSegmenter(final int tileRowCount, final int tileColumnCount,
			final int verticalTileStride, final int horizontalTileStride) {
		this.tileRowCount = tileRowCount;
		this.tileColumnCount = tileColumnCount;
		this.verticalTileStride = verticalTileStride;
		this.horizontalTileStride = horizontalTileStride;
	}
	
	@Override
	public final void process(final Image image, final PixelProcessor processor) {
		forEachPixelInEachTile(image, this.tileRowCount, this.tileColumnCount,
				this.verticalTileStride, this.horizontalTileStride, processor);
	}
	
}
