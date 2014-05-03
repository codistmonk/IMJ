package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj2.core.Image.Monochannel;
import imj2.core.LinearPackedGrayImage;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

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
					"../Libraries/images/jpg/SYS08_A10_7414-005", "SYS08_A10_7414-005_lod0", null).getLODImage(5);
			
			debugPrint("Loading image done in", timer.toc(), "ms");
			debugPrint(image.getWidth(), image.getHeight(), image.getPixelCount());
			
			debugPrint("Analyzing image started...", new Date(timer.tic()));
			
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int s = 1282;
			
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
			final LinearPackedGrayImage markers = new LinearPackedGrayImage(""
					, horizontalMarkers + verticalMarkers, new Monochannel(Long.SIZE - Long.numberOfLeadingZeros(s - 1L)));
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
						final int row = ceiling(y - s / 2, s);
						
						for (int x = s / 2 + s * ceiling(x0 - s / 2, s); x < x1; x += s) {
							final int column = ceiling(x - s / 2, s);
							
							debugPrint("segment:", x, y, row, column);
							
							{
								if (y == s / 2) {
									// TODO set top left horizontal marker
									markersDone.incrementAndGet();
									
									if (w <= x + s) {
										// TODO set top right horizontal marker
										markersDone.incrementAndGet();
									}
								}
								
								// TODO set left horizontal marker
								markersDone.incrementAndGet();
								
								if (w <= x + s) {
									// TODO set right horizontal marker
									markersDone.incrementAndGet();
								}
								
								if (h <= y + s) {
									// TODO set bottom left horizontal marker
									markersDone.incrementAndGet();
									
									if (w <= x + s) {
										// TODO set bottom right horizontal marker
										markersDone.incrementAndGet();
									}
								}
							}
							
							{
								if (x == s / 2) {
									// TODO set top left vertical marker
									markersDone.incrementAndGet();
									
									if (h <= y + s) {
										// TODO set bottom left vertical marker
										markersDone.incrementAndGet();
									}
								}
								
								// TODO set top vertical marker
								markersDone.incrementAndGet();
								
								if (h <= y + s) {
									// TODO set bottom vertical marker
									markersDone.incrementAndGet();
								}
								
								if (w <= x + s) {
									// TODO set top right vertical marker
									markersDone.incrementAndGet();
									
									if (w <= x + s) {
										// TODO set bottom right vertical marker
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
				System.err.println(debug(DEBUG_STACK_OFFSET
						, "expectedMarkers:", markers.getPixelCount(), "actualMarkers:", markersDone.get()));
			}
		}
	}
	
	public static final int ceiling(final int a, final int b) {
		return (a + b - 1) / b;
	}
	
}
