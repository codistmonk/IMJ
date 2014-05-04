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
					"../Libraries/images/jpg/SYS08_A10_7414-005", "SYS08_A10_7414-005_lod0", null).getLODImage(0);
			
			debugPrint("Loading image done in", timer.toc(), "ms");
			debugPrint(image.getWidth(), image.getHeight(), image.getPixelCount());
			
			debugPrint("Analyzing image started...", new Date(timer.tic()));
			
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int s = 32;
			
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
			
			final int markersPerRow = 1 + ceiling(w - s / 2, s);
			final int rows = 2 + ceiling(h - s / 2, s);
			final long horizontalMarkers = (long) markersPerRow * rows;
			final int markersPerColumn = 1 + ceiling(h - s / 2, s);
			final int columns = 2 + ceiling(w - s / 2, s);
			final long verticalMarkers = (long) markersPerColumn * columns;
			final long allMarkers = horizontalMarkers + verticalMarkers;
			final LinearPackedGrayImage markers = new LinearPackedGrayImage(""
					, allMarkers, new Monochannel(Long.SIZE - Long.numberOfLeadingZeros(s - 1L)));
			final AtomicLong markersDone = new AtomicLong();
			
			debugPrint("markersPerRow:", markersPerRow, "rows:", rows
					, "markersPerColumn:", markersPerColumn, "columns:", columns
					, "markers:", markers.getPixelCount());
			
			IMJTools.forEachTileIn(image, new TileProcessor() {
				
				@Override
				public final void pixel(final Info info) {
					final int x0 = info.getTileX() + info.getPixelXInTile();
					final int y0 = info.getTileY() + info.getPixelYInTile();
					final int x1 = info.getTileX() + info.getActualTileWidth() - 1;
					final int y1 = info.getTileY() + info.getActualTileHeight() - 1;
					
					debugPrint("tile:", info.getTileX(), info.getTileY(), info.getActualTileWidth(), info.getActualTileHeight());
					
					/*
					 * y0 <= s/2 + k s
					 * <- y0 - s/2 <= k s
					 * <- (y0 - s/2)/s <= k
					 * <- k = ceil(y0-s/2,s)
					 */
					
					for (int y = s / 2 + s * ceiling(y0 - s / 2, s); y < y1; y += s) {
						final int row = 1 + ceiling(y - s / 2, s);
						
						for (int x = s / 2 + s * ceiling(x0 - s / 2, s); x < x1; x += s) {
							final int column = 1 + ceiling(x - s / 2, s);
							
//							debugPrint("segment:", x, y, row, column);
							
							{
								if (y == s / 2) {
									{
										final long markerIndex = (row - 1L) * markersPerRow + column - 1L;
										
										if (column == 1) {
											markers.setPixelValue(markerIndex, 0);
										} else {
											markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(image, x - s, 0, s));
										}
										
										markersDone.incrementAndGet();
									}
									
									if (w <= x + s) {
										final long markerIndex = (row - 1L) * markersPerRow + column;
										markers.setPixelValue(markerIndex, w - 1 - x);
										markersDone.incrementAndGet();
									}
								}
								
								{
									final long markerIndex = row * markersPerRow + column - 1L;
									
									if (column == 1) {
										markers.setPixelValue(markerIndex, 0);
									} else {
										markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(image, x - s, y, s));
									}
									
									markersDone.incrementAndGet();
								}
								
								if (w <= x + s) {
									final long markerIndex = row * markersPerRow + column;
									markers.setPixelValue(markerIndex, w - 1 - x);
									markersDone.incrementAndGet();
								}
								
								if (h <= y + s) {
									{
										final long markerIndex = (row + 1L) * markersPerRow + column - 1L;
										
										if (column == 1) {
											markers.setPixelValue(markerIndex, 0);
										} else {
											markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(image, x - s, h - 1, s));
										}
										
										markersDone.incrementAndGet();
									}
									
									if (w <= x + s) {
										final long markerIndex = (row + 1L) * markersPerRow + column;
										markers.setPixelValue(markerIndex, w - 1 - x);
										markersDone.incrementAndGet();
									}
								}
							}
							
							{
								if (x == s / 2) {
									{
										final long markerIndex = horizontalMarkers + (column - 1L) * markersPerColumn + row - 1L;
										
										if (row == 1) {
											markers.setPixelValue(markerIndex, 0);
										} else {
											markers.setPixelValue(markerIndex, getHorizontalOffsetOfLargestGradient(image, 0, y - s, s));
										}
										
										markersDone.incrementAndGet();
									}
									
									if (h <= y + s) {
										final long markerIndex = horizontalMarkers + (column - 1L) * markersPerColumn + row;
										markers.setPixelValue(markerIndex, h - 1 - y);
										markersDone.incrementAndGet();
									}
								}
								
								{
									final long markerIndex = horizontalMarkers + column * markersPerColumn + row - 1L;
									
									if (row == 1) {
										markers.setPixelValue(markerIndex, 0);
									} else {
										markers.setPixelValue(markerIndex, getVerticalOffsetOfLargestGradient(image, x, y - s, s));
									}
									
									markersDone.incrementAndGet();
								}
								
								if (h <= y + s) {
									final long markerIndex = horizontalMarkers + column * markersPerColumn + row;
									markers.setPixelValue(markerIndex, h - 1 - y);
									markersDone.incrementAndGet();
								}
								
								if (w <= x + s) {
									{
										final long markerIndex = horizontalMarkers + (column + 1L) * markersPerColumn + row - 1L;
										
										if (row == 1) {
											markers.setPixelValue(markerIndex, 0);
										} else {
											markers.setPixelValue(markerIndex, getVerticalOffsetOfLargestGradient(image, w - 1, y - s, s));
										}
										
										markersDone.incrementAndGet();
									}
									
									if (h <= y + s) {
										final long markerIndex = horizontalMarkers + (column + 1L) * markersPerColumn + row;
										markers.setPixelValue(markerIndex, h - 1 - y);
										markersDone.incrementAndGet();
									}
								}
							}
						}
					}
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
			
			debugPrint("Analyzing image done in", timer.toc(), "ms");
			
			if (markersDone.get() != markers.getPixelCount()) {
				debugError("expectedMarkers:", markers.getPixelCount(), "actualMarkers:", markersDone.get());
			}
			
			{
				long errors = 0L;
				
				for (long i = 0L; i < horizontalMarkers; ++i) {
					final int markerValue = markers.getPixelValue(i);
					
					if (i % markersPerRow == 0) {
						if (markerValue != 0) {
							++errors;
						}
					} else if (markerValue == 0) {
						++errors;
					}
				}
				
				for (long i = horizontalMarkers; i < allMarkers; ++i) {
					final int markerValue = markers.getPixelValue(i);
					
					if ((i - horizontalMarkers) % markersPerColumn == 0) {
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
	}
	
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
