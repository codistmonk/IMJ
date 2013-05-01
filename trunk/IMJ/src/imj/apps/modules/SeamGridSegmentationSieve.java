package imj.apps.modules;

import static java.lang.Math.abs;
import static java.lang.Math.max;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-05-01)
 */
public final class SeamGridSegmentationSieve extends Sieve {
	
	private RegionOfInterest segmentation;
	
	public SeamGridSegmentationSieve(final Context context) {
		super(context);
		
		this.getParameters().put("gridSize", "32");
	}
	
	@Override
	public final boolean accept(final int index, final int value) {
		return this.segmentation.get(index);
	}
	
	@Override
	public final void initialize() {
		final Image image = ViewFilter.getCurrentImage(this.getContext());
		final RegionOfInterest roi = this.getROI();
		final int rowCount = roi.getRowCount();
		final int columnCount = roi.getColumnCount();
		this.segmentation = RegionOfInterest.newInstance(rowCount, columnCount);
		final int gridSize = this.getIntParameter("gridSize");
		
		this.segmentation.reset(true);
		
		for (int y0 = gridSize; y0 < rowCount; y0 += gridSize) {
			int y = y0;
			
			for (int x = 0; x < columnCount; ++x) {
				this.segmentation.set(y, x, false);
				
				final int x1 = x + 1;
				
				if (columnCount <= x1) {
					break;
				}
				
				final long eastCost = this.getCost(image, y, x1);
				final long northEastCost = y0 < y ? this.getCost(image, y - 1, x1) : Long.MAX_VALUE;
				final long southEastCost = y + 1 < y0 + gridSize && y + 1 < rowCount ? this.getCost(image, y + 1, x1) : Long.MAX_VALUE;
				
				if (eastCost <= northEastCost && eastCost <= southEastCost) {
					// NOP
				} else if (northEastCost <= southEastCost) {
					--y;
				} else {
					++y;
				}
			}
		}
		
		for (int x0 = gridSize; x0 < columnCount; x0 += gridSize) {
			int x = x0;
			
			for (int y = 0; y < rowCount; ++y) {
				this.segmentation.set(y, x, false);
				
				final int y1 = y + 1;
				
				if (rowCount <= y1) {
					break;
				}
				
				final long southCost = this.getCost(image, y1, x);
				final long southWestCost = x0 < x ? this.getCost(image, y1, x - 1) : Long.MAX_VALUE;
				final long southEastCost = x + 1 < x0 + gridSize && x + 1 < columnCount ? this.getCost(image, y1, x + 1) : Long.MAX_VALUE;
				
				if (southCost <= southWestCost && southCost <= southEastCost) {
					// NOP
				} else if (southWestCost <= southEastCost) {
					--x;
				} else {
					++x;
				}
			}
		}
	}
	
	private final long getCost(final Image image, final int rowIndex, final int columnIndex) {
		final int lastRowIndex = image.getRowCount() - 1;
		final int lastColumnIndex = image.getColumnCount() - 1;
		final int center = channel.getValue(image.getValue(rowIndex, columnIndex));
		final int northWest = 0 < rowIndex && 0 < columnIndex ?
				abs(channel.getValue(image.getValue(rowIndex - 1, columnIndex - 1)) - center) : 0;
		final int north = 0 < rowIndex ?
				abs(channel.getValue(image.getValue(rowIndex - 1, columnIndex)) - center) : 0;
		final int northEast = 0 < rowIndex && columnIndex < lastColumnIndex ?
				abs(channel.getValue(image.getValue(rowIndex - 1, columnIndex + 1)) - center) : 0;
		final int west = 0 < rowIndex ?
				abs(channel.getValue(image.getValue(rowIndex - 1, columnIndex)) - center) : 0;
		final int east = columnIndex < lastColumnIndex ?
				abs(channel.getValue(image.getValue(rowIndex, columnIndex + 1)) - center) : 0;
		final int southWest = rowIndex < lastRowIndex && 0 < columnIndex ?
				abs(channel.getValue(image.getValue(rowIndex + 1, columnIndex - 1)) - center) : 0;
		final int south = rowIndex < lastRowIndex ?
				abs(channel.getValue(image.getValue(rowIndex + 1, columnIndex)) - center) : 0;
		final int southEast = rowIndex < lastRowIndex && columnIndex < lastColumnIndex ?
				abs(channel.getValue(image.getValue(rowIndex + 1, columnIndex + 1)) - center) : 0;
		
		return -max(max(max(northWest, north), max(northEast, west)), max(max(east, southWest), max(south, southEast)));
	}
	
	private static final Channel channel = Channel.Synthetic.BRIGHTNESS;
	
}
