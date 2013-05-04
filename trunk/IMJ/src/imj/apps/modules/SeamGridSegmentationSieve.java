package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachComponent4;
import static imj.apps.modules.ViewFilter.getCurrentImage;
import static imj.apps.modules.ViewFilter.parseChannel;
import static java.lang.Math.max;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.ImageOfInts;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Date;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-05-01)
 */
public final class SeamGridSegmentationSieve extends Sieve {
	
	private RegionOfInterest segmentation;
	
	public SeamGridSegmentationSieve(final Context context) {
		super(context);
		
		this.getParameters().put("channel", "rgb");
		this.getParameters().put("cellSize", "32");
	}
	
	@Override
	public final boolean accept(final int index, final int value) {
		return this.segmentation.get(index);
	}
	
	@Override
	public final void initialize() {
		final Channel channel = parseChannel(this.getParameters().get("channel").toUpperCase(Locale.ENGLISH));
		final Image image = getCurrentImage(this.getContext());
		final RegionOfInterest roi = this.getROI();
		final int rowCount = roi.getRowCount();
		final int columnCount = roi.getColumnCount();
		this.segmentation = RegionOfInterest.newInstance(rowCount, columnCount);
		final int cellSize = this.getIntParameter("cellSize");
		
		this.segmentation.reset(true);
		
		setSeams(image, channel, cellSize, this.segmentation);
		
		debugPrint("segmentCount:", countSegments4(this.segmentation));
	}
	
	public static final void setSeams(final Image image, final Channel channel, final int cellSize, final RegionOfInterest segmentation) {
		final TicToc timer = new TicToc();
		
		debugPrint("Setting horizontal band seams...", new Date(timer.tic()));
		
		setHorizontalBandSeams(image, channel, cellSize, segmentation);
		
		debugPrint("Setting horizontal band seams done", "time:", timer.toc());
		
		debugPrint("Setting vertical band seams...", new Date(timer.tic()));
		
		setHorizontalBandSeams(new Transpose(image), channel, cellSize, new Transpose(segmentation));
		
		debugPrint("Setting vertical band seams done", "time:", timer.toc());
	}
	
	public static final void copyHorizontalBand(final Image source, final int rowIndex0, final Image destination) {
		final int sourceRowCount = source.getRowCount();
		final int rowIndex1 = rowIndex0 + destination.getRowCount();
		final int columnIndex0 = 0;
		final int columnIndex1 = columnIndex0 + destination.getColumnCount();
		
		for (int rowIndex = rowIndex0, r = rowIndex, i = 0; rowIndex < rowIndex1; ++rowIndex) {
			for (int columnIndex = columnIndex0; columnIndex < columnIndex1; ++columnIndex, ++i) {
				destination.setValue(i, source.getValue(r, columnIndex));
			}
			
			if (r + 1 < sourceRowCount) {
				++r;
			}
		}
	}
	
	public static void setHorizontalBandSeams(final Image image, final Channel channel,
			final int cellSize, final Image segmentation) {
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final Image band = new ImageOfInts(cellSize, columnCount, 1);
		
		for (int y0 = cellSize; y0 < rowCount; y0 += cellSize) {
			System.out.print(y0 + "/" + rowCount + "\r");
			
			copyHorizontalBand(image, y0, band);
			
			int y = y0;
			
			for (int x = 0; x < columnCount; ++x) {
				segmentation.setValue(y, x, 0);
				
				final int x1 = x + 1;
				
				if (columnCount <= x1) {
					break;
				}
				
				final long northEastCost = y0 < y ?
						getCost(band, channel, y - y0 - 1, x1) : Long.MAX_VALUE;
				final long eastCost = getCost(band, channel, y - y0, x1);
				final long southEastCost = y + 1 < y0 + cellSize && y + 1 < rowCount ?
						getCost(band, channel, y - y0 + 1, x1) : Long.MAX_VALUE;
				
				if (eastCost <= northEastCost && eastCost <= southEastCost) {
					// NOP
				} else if (northEastCost <= southEastCost) {
					--y;
				} else {
					++y;
				}
			}
		}
	}
	
	public static final int countSegments4(final Image segmentation) {
		final int[] segmentCount = { 0 };
		
		forEachPixelInEachComponent4(segmentation, new PixelProcessor() {
			
			@Override
			public final void process(final int pixel) {
				// NOP
			}
			
			@Override
			public final void finishPatch() {
				++segmentCount[0];
			}
			
		});
		
		return segmentCount[0];
	}
	
	public static final int getCost(final Image image, final Channel channel, final int rowIndex, final int columnIndex) {
		final int lastRowIndex = image.getRowCount() - 1;
		final int lastColumnIndex = image.getColumnCount() - 1;
		final int center = channel.getValue(image.getValue(rowIndex, columnIndex));
		final int northWest = 0 < rowIndex && 0 < columnIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex - 1, columnIndex - 1)), center) : 0;
		final int north = 0 < rowIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex - 1, columnIndex)), center) : 0;
		final int northEast = 0 < rowIndex && columnIndex < lastColumnIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex - 1, columnIndex + 1)), center) : 0;
		final int west = 0 < rowIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex - 1, columnIndex)), center) : 0;
		final int east = columnIndex < lastColumnIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex, columnIndex + 1)), center) : 0;
		final int southWest = rowIndex < lastRowIndex && 0 < columnIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex + 1, columnIndex - 1)), center) : 0;
		final int south = rowIndex < lastRowIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex + 1, columnIndex)), center) : 0;
		final int southEast = rowIndex < lastRowIndex && columnIndex < lastColumnIndex ?
				channel.getDistance(channel.getValue(image.getValue(rowIndex + 1, columnIndex + 1)), center) : 0;
		
		return -max(max(max(northWest, north), max(northEast, west)), max(max(east, southWest), max(south, southEast)));
	}
	
}
