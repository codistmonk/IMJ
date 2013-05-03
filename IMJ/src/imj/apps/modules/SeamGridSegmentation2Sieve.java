package imj.apps.modules;

import static imj.apps.modules.SeamGridSegmentationSieve.countSegments4;
import static imj.apps.modules.SeamGridSegmentationSieve.getCost;
import static imj.apps.modules.ViewFilter.getCurrentImage;
import static imj.apps.modules.ViewFilter.parseChannel;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.Image;
import imj.ImageOfInts;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Date;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-05-02)
 */
public final class SeamGridSegmentation2Sieve extends Sieve {
	
	private RegionOfInterest segmentation;
	
	public SeamGridSegmentation2Sieve(final Context context) {
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
		final int cellSize = this.getIntParameter("cellSize");
		final Image image = getCurrentImage(this.getContext());
		final RegionOfInterest roi = this.getROI();
		this.segmentation = RegionOfInterest.newInstance(roi.getRowCount(), roi.getColumnCount());
		this.segmentation.reset(true);
		final TicToc timer = new TicToc();
		
		debugPrint("Setting horizontal band seams...", new Date(timer.tic()));
		setHorizontalBandSeams(image, channel, cellSize, this.segmentation);
		debugPrint("Setting horizontal band seams done", "time:", timer.toc());
		
		debugPrint("Setting vertical band seams...", new Date(timer.tic()));
		setHorizontalBandSeams(new Transpose(image), channel, cellSize, new Transpose(this.segmentation));
		debugPrint("Setting vertical band seams done", "time:", timer.toc());
		
		debugPrint("segmentCount:", countSegments4(this.segmentation));
	}
	
	/**
	 * @author codistmonk (creation 2013-05-02)
	 */
	public static final class Transpose implements Image {
		
		private final Image source;
		
		public Transpose(final Image source) {
			this.source = source;
		}
		
		public final Image getSource() {
			return this.source;
		}
		
		@Override
		public final int getPixelCount() {
			return this.getSource().getPixelCount();
		}
		
		@Override
		public final int getChannelCount() {
			return this.getSource().getChannelCount();
		}
		
		@Override
		public final int getDimensionCount() {
			return this.getSource().getDimensionCount();
		}
		
		@Override
		public final int getDimension(final int dimensionIndex) {
			return this.getSource().getDimension(dimensionIndex <= 1 ? dimensionIndex ^ 1 : dimensionIndex);
		}
		
		@Override
		public final int getRowCount() {
			return this.getSource().getColumnCount();
		}
		
		@Override
		public final int getColumnCount() {
			return this.getSource().getRowCount();
		}
		
		@Override
		public final int getValue(final int rowIndex, final int columnIndex) {
			return this.getSource().getValue(columnIndex, rowIndex);
		}
		
		@Override
		public final int setValue(final int rowIndex, final int columnIndex, final int value) {
			return this.getSource().setValue(columnIndex, rowIndex, value);
		}
		
		@Override
		public final int getValue(final int index) {
			final int sourceColumnCount = this.getSource().getColumnCount();
			final int rowIndexInSource = index / sourceColumnCount;
			final int columnIndexInSource = index % sourceColumnCount;
			
			return this.getValue(columnIndexInSource, rowIndexInSource);
		}
		
		@Override
		public final int setValue(int index, int value) {
			final int sourceColumnCount = this.getSource().getColumnCount();
			final int rowIndexInSource = index / sourceColumnCount;
			final int columnIndexInSource = index % sourceColumnCount;
			
			return this.setValue(columnIndexInSource, rowIndexInSource, value);
		}
		
	}
	
	public static final void setHorizontalBandSeams(final Image image, final Channel channel,
			final int cellSize, final Image segmentation) {
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final Image band = new ImageOfInts(cellSize, columnCount, 1);
		
		for (int rowIndex0 = cellSize; rowIndex0 + cellSize < rowCount; rowIndex0 += cellSize) {
			System.out.print(rowIndex0 + "/" + rowCount + "\r");
			
			for (int rowIndex = rowIndex0; rowIndex < rowIndex0 + cellSize; ++rowIndex) {
				band.setValue(rowIndex - rowIndex0, 0, getCost(image, channel, rowIndex, 0));
			}
			
			for (int columnIndex = 1; columnIndex < columnCount; ++columnIndex) {
				for (int rowIndex = rowIndex0; rowIndex < rowIndex0 + cellSize; ++rowIndex) {
					final int northWest = rowIndex0 < rowIndex ?
							getCost(image, channel, rowIndex - 1, columnIndex - 1) : Integer.MAX_VALUE;
					final int west = getCost(image, channel, rowIndex, columnIndex - 1);
					final int southWest = rowIndex + 1 < rowIndex0 + cellSize ?
							getCost(image, channel, rowIndex + 1, columnIndex - 1) : Integer.MAX_VALUE;
					final int min = min(northWest, min(west, southWest));
					
					band.setValue(rowIndex - rowIndex0, columnIndex,
							clampedSum(min, getCost(image, channel, rowIndex, columnIndex)));
				}
			}
			
			final int lastColumnIndex = columnCount - 1;
			int rowIndexOfMin = rowIndex0;
			int min = band.getValue(rowIndexOfMin - rowIndex0, lastColumnIndex);
			
			for (int rowIndex = rowIndex0; rowIndex < rowIndex0 + cellSize; ++rowIndex) {
				final int value = band.getValue(rowIndex - rowIndex0, lastColumnIndex);
				
				if (value < min) {
					min = value;
					rowIndexOfMin = rowIndex;
				}
			}
			
			for (int columnIndex = lastColumnIndex, rowIndex = rowIndexOfMin; 0 <= columnIndex; --columnIndex) {
				segmentation.setValue(rowIndex, columnIndex, 0);
				
				if (columnIndex == 0) {
					break;
				}
				
				final int northWest = rowIndex0 < rowIndex ?
						band.getValue(rowIndex - rowIndex0 - 1, columnIndex - 1) : Integer.MAX_VALUE;
				final int west = band.getValue(rowIndex - rowIndex0, columnIndex - 1);
				final int southWest = rowIndex + 1 < rowIndex0 + cellSize ?
						band.getValue(rowIndex - rowIndex0 + 1, columnIndex - 1) : Integer.MAX_VALUE;
				
				if (west <= northWest && west <= southWest) {
					// NOP
				} else if (northWest <= southWest) {
					--rowIndex;
				} else {
					++rowIndex;
				}
			}
		}
	}
	
	public static final int clampedSum(final int a, final int b) {
		return (int) min(Integer.MAX_VALUE, (long) a + b);
	}
	
}
