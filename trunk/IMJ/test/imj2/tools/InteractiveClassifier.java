package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.debugError;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj2.core.Image.Channels;
import imj2.core.Image.Monochannel;
import imj2.core.Image2D;
import imj2.core.LinearPackedGrayImage;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-05-03)
 */
public final class InteractiveClassifier {
	
	private InteractiveClassifier() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		{
			final TicToc timer = new TicToc();
			
			debugPrint("Loading image started...", new Date(timer.tic()));
			
//			final TiledImage2D image = new LociBackedImage("../Libraries/images/svs/SYS08_A10_7414-005.svs");
			final TiledImage2D image = (TiledImage2D) new MultifileImage(
					"../Libraries/images/jpg/SYS08_A10_7414-005", "SYS08_A10_7414-005_lod0", null).getLODImage(6);
			
			debugPrint("Loading image done in", timer.toc(), "ms");
			debugPrint(image.getWidth(), image.getHeight(), image.getPixelCount());
			
			debugPrint("Analyzing image started...", new Date(timer.tic()));
			
			final MarkersBuilder markersBuilder = new MarkersBuilder(image, 32);
			
			markersBuilder.buildMarkersFor(image);
			
			debugPrint("Analizing image done in", timer.toc(), "ms");
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-05-03)
	 */
	public static final class MarkersBuilder implements Serializable {
		
		private final Image2D image;

		private final int w;

		private final int h;
		
		private final int s;

		private final int markersPerRow;

		private final int rows;

		private final long horizontalMarkers;

		private final int markersPerColumn;

		private final int columns;

		private final long verticalMarkers;

		private final long allMarkers;
		
		private final AtomicLong markersDone;
		
		public MarkersBuilder(final Image2D image, final int s) {
			this.image = image;
			
			this.w = image.getWidth();
			this.h = image.getHeight();
			this.s = s;
			
			/*
			 * <-------------------w------------------->
			 * <---s/2--><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0---*
			 * 
			 * <-----------------------w----------------------->
			 * <---s/2--><-----s-----><-----s-----><-----s----->
			 * *---------0---*--------0------*-----0-----------*
			 * 
			 * numberOfMarkers = 1+ceiling((w-s/2)/s)
			 */
			
			this.markersPerRow = 1 + ceiling(this.w - s / 2, s);
			this.rows = 2 + ceiling(this.h - s / 2, s);
			this.horizontalMarkers = (long) this.markersPerRow * this.rows;
			this.markersPerColumn = 1 + ceiling(this.h - s / 2, s);
			this.columns = 2 + ceiling(this.w - s / 2, s);
			this.verticalMarkers = (long) this.markersPerColumn * this.columns;
			this.allMarkers = this.horizontalMarkers + this.verticalMarkers;
			final LinearPackedGrayImage markers = this.newEmptyMarkers();
			this.markersDone = new AtomicLong();
			
			debugPrint("markersPerRow:", this.markersPerRow, "rows:", this.rows
					, "markersPerColumn:", this.markersPerColumn, "columns:", this.columns
					, "markers:", markers.getPixelCount());
		}
		
		public final LinearPackedGrayImage buildMarkersFor(final TiledImage2D image) {
			final LinearPackedGrayImage result = this.newEmptyMarkers();
			
			IMJTools.forEachTileIn(image, new TileProcessor() {
				
				@Override
				public final void pixel(final Info info) {
					debugPrint("tile:", info.getTileX(), info.getTileY(), info.getActualTileWidth(), info.getActualTileHeight());
					
					final int xStart = info.getTileX() + info.getPixelXInTile();
					final int yStart = info.getTileY() + info.getPixelYInTile();
					final int xEnd = info.getTileX() + info.getActualTileWidth();
					final int yEnd = info.getTileY() + info.getActualTileHeight();
					
					MarkersBuilder.this.setMarkers(result, xStart, yStart, xEnd, yEnd);
				}
				
				@Override
				public final void endOfTile() {
					// NOP
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 8075249791734409997L;
				
			});
			
			this.check(result);
			
			return result;
		}
		
		public final LinearPackedGrayImage newEmptyMarkers() {
			return new LinearPackedGrayImage(""
					, this.allMarkers, new Monochannel(Long.SIZE - Long.numberOfLeadingZeros(this.s - 1L)));
		}
		
		public final void setMarkers(final LinearPackedGrayImage markers, final int xStart, final int yStart, final int xEnd, final int yEnd) {
			/*
			 * y0 <= s/2 + k s
			 * <- y0 - s/2 <= k s
			 * <- (y0 - s/2)/s <= k
			 * <- k = ceil(y0-s/2,s)
			 */
			
			for (int y = this.s / 2 + this.s * ceiling(yStart - this.s / 2, this.s); y < yEnd; y += this.s) {
				final int row = 1 + ceiling(y - this.s / 2, this.s);
				
				for (int x = this.s / 2 + this.s * ceiling(xStart - this.s / 2, this.s); x < xEnd; x += this.s) {
					final int column = 1 + ceiling(x - this.s / 2, this.s);
					
//					debugPrint("segment:", x, y, row, column);
					
					{
						if (y == this.s / 2) {
							{
								final long markerIndex = (row - 1L) * this.markersPerRow + column - 1L;
								
								if (column == 1) {
									markers.setPixelValue(markerIndex, 0);
								} else {
									markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(this.image, x - this.s, 0, this.s));
								}
								
								this.markersDone.incrementAndGet();
							}
							
							if (this.w <= x + this.s) {
								final long markerIndex = (row - 1L) * this.markersPerRow + column;
								markers.setPixelValue(markerIndex, this.w - 1 - x);
								this.markersDone.incrementAndGet();
							}
						}
						
						{
							final long markerIndex = row * this.markersPerRow + column - 1L;
							
							if (column == 1) {
								markers.setPixelValue(markerIndex, 0);
							} else {
								markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(this.image, x - this.s, y, this.s));
							}
							
							this.markersDone.incrementAndGet();
						}
						
						if (this.w <= x + this.s) {
							final long markerIndex = row * this.markersPerRow + column;
							markers.setPixelValue(markerIndex, this.w - 1 - x);
							this.markersDone.incrementAndGet();
						}
						
						if (this.h <= y + this.s) {
							{
								final long markerIndex = (row + 1L) * this.markersPerRow + column - 1L;
								
								if (column == 1) {
									markers.setPixelValue(markerIndex, 0);
								} else {
									markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(this.image, x - this.s, this.h - 1, this.s));
								}
								
								this.markersDone.incrementAndGet();
							}
							
							if (this.w <= x + this.s) {
								final long markerIndex = (row + 1L) * this.markersPerRow + column;
								markers.setPixelValue(markerIndex, this.w - 1 - x);
								this.markersDone.incrementAndGet();
							}
						}
					}
					
					{
						if (x == this.s / 2) {
							{
								final long markerIndex = this.horizontalMarkers + (column - 1L) * this.markersPerColumn + row - 1L;
								
								if (row == 1) {
									markers.setPixelValue(markerIndex, 0);
								} else {
									markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(this.image, 0, y - this.s, this.s));
								}
								
								this.markersDone.incrementAndGet();
							}
							
							if (this.h <= y + this.s) {
								final long markerIndex = this.horizontalMarkers + (column - 1L) * this.markersPerColumn + row;
								markers.setPixelValue(markerIndex, this.h - 1 - y);
								this.markersDone.incrementAndGet();
							}
						}
						
						{
							final long markerIndex = this.horizontalMarkers + column * this.markersPerColumn + row - 1L;
							
							if (row == 1) {
								markers.setPixelValue(markerIndex, 0);
							} else {
								markers.setPixelValue(markerIndex, getVerticalOffsetOfLargestGradient(this.image, x, y - this.s, this.s));
							}
							
							this.markersDone.incrementAndGet();
						}
						
						if (this.h <= y + this.s) {
							final long markerIndex = this.horizontalMarkers + column * this.markersPerColumn + row;
							markers.setPixelValue(markerIndex, this.h - 1 - y);
							this.markersDone.incrementAndGet();
						}
						
						if (this.w <= x + this.s) {
							{
								final long markerIndex = this.horizontalMarkers + (column + 1L) * this.markersPerColumn + row - 1L;
								
								if (row == 1) {
									markers.setPixelValue(markerIndex, 0);
								} else {
									markers.setPixelValue(markerIndex, getVerticalOffsetOfLargestGradient(this.image, this.w - 1, y - this.s, this.s));
								}
								
								this.markersDone.incrementAndGet();
							}
							
							if (this.h <= y + this.s) {
								final long markerIndex = this.horizontalMarkers + (column + 1L) * this.markersPerColumn + row;
								markers.setPixelValue(markerIndex, this.h - 1 - y);
								this.markersDone.incrementAndGet();
							}
						}
					}
				}
			}
		}
		
		public final void check(final LinearPackedGrayImage markers) {
			if (this.markersDone.get() != markers.getPixelCount()) {
				debugError("expectedMarkers:", markers.getPixelCount(), "actualMarkers:", this.markersDone.get());
			}
			
			{
				long errors = 0L;
				
				for (long i = 0L; i < this.horizontalMarkers; ++i) {
					final int markerValue = markers.getPixelValue(i);
					
					if (i % this.markersPerRow == 0) {
						if (markerValue != 0) {
							++errors;
						}
					} else if (markerValue == 0) {
						++errors;
					}
				}
				
				for (long i = this.horizontalMarkers; i < this.allMarkers; ++i) {
					final int markerValue = markers.getPixelValue(i);
					
					if ((i - this.horizontalMarkers) % this.markersPerColumn == 0) {
						if (markerValue != 0) {
							++errors;
						}
					} else if (markerValue == 0) {
						++errors;
					}
				}
				
				if (0 < errors) {
					debugError("erroneousMarkers:", errors);
				}
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2680059399437526583L;
		
		public static final int ceiling(final int a, final int b) {
			return (a + b - 1) / b;
		}
		
		public static final int getHorizontalOffsetOfLargestGradient(final Image2D image, final int x, final int y, final int s) {
			final int w = image.getWidth();
			final int xStart = x + 1;
			final int xEnd = min(w, x + s);
			int largestGradient = getGradient(image, xStart, y);
			int result = 1;
			
			for (int xx = xStart; xx < xEnd; ++xx) {
				final int gradient = getGradient(image, xx, y);
				
				if (largestGradient < gradient) {
					largestGradient = gradient;
					result = xx - xStart;
				}
			}
			
			return result;
		}
		
		public static final int getVerticalOffsetOfLargestGradient(final Image2D image, final int x, final int y, final int s) {
			final int h = image.getHeight();
			final int yStart = y + 1;
			final int yEnd = min(h, y + s);
			int largestGradient = getGradient(image, x, yStart);
			int result = 1;
			
			for (int yy = yStart; yy < yEnd; ++yy) {
				final int gradient = getGradient(image, x, yy);
				
				if (largestGradient < gradient) {
					largestGradient = gradient;
					result = yy - yStart;
				}
			}
			
			return result;
		}
		
		public static final int getGradient(final Image2D image, final int x, final int y) {
			final int pixelValue = image.getPixelValue(x, y);
			final Channels channels = image.getChannels();
			final int channelCount = channels.getChannelCount();
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int xStart = max(0, x - 1);
			final int xEnd = min(w, x + 1);
			final int yStart = max(0, y - 1);
			final int yEnd = min(h, y + 1);
			int result = 0;
			
			for (int yi = yStart; yi < yEnd; ++yi) {
				for (int xi = xStart; xi < xEnd; ++xi) {
					if (xi != x || yi != y) {
						final int neighborValue = image.getPixelValue(xi, yi);
						
						for (int channel = 0; channel < channelCount; ++channel) {
							result += abs(channels.getChannelValue(pixelValue, channel)
									- channels.getChannelValue(neighborValue, channel));
						}
					}
				}
			}
			
			return result;
		}
		
	}
	
}
