package imj2.tools;

import static imj2.tools.IMJTools.quantize;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.SystemProperties;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-08-05)
 */
public final class IMJToolsTest {
	
	@Test
	public final void testAwt() throws Exception {
		final String base = "test/imj2/data/";
		final String[] ids = { "rgb24.gif", "rgb24.jpg", "rgb24.png", "rgb32.png" };
		
		for (final String id : ids) {
			final String imageId = base + id;
			final BufferedImage expectedAwtImage = ImageIO.read(new File(imageId));
			final Image2D image = new AwtBackedImage(imageId, expectedAwtImage);
			final DefaultColorModel color = new DefaultColorModel(image.getChannels());
			final BufferedImage actualAwtImage = IMJTools.awtImage(image);
			
			Image2D.Traversing.ALL.forEachPixelIn(image, new MonopatchProcess() {
				
				@Override
				public final void pixel(final int x, final int y) {
					assertEquals(expectedAwtImage.getRGB(x, y), actualAwtImage.getRGB(x, y));
				}
				
				/**
				 * @{value}.
				 */
				private static final long serialVersionUID = 6154461314835897640L;
				
			});
			
			assertRGB(color, 255, 0, 0, image.getPixelValue(0, 0));
			assertRGB(color, 0, 255, 0, image.getPixelValue(1, 0));
			assertRGB(color, 0, 0, 255, image.getPixelValue(0, 1));
		}
	}
	
	@Test
	public final void testLoci1() throws Exception {
		final String base = "test/imj2/data/";
		final String[] ids = { "rgb24.jpg", "rgb24.png", "rgb24.ppm", "rgb24.bmp", "rgb24.gif", "rgb32.png" };
		
		for (final String id : ids) {
			final String imageId = base + id;
			final Image2D image = new LociBackedImage(imageId);
			final DefaultColorModel color = new DefaultColorModel(image.getChannels());
			
			assertRGB(color, 255, 0, 0, image.getPixelValue(0, 0));
			assertRGB(color, 0, 255, 0, image.getPixelValue(1, 0));
			assertRGB(color, 0, 0, 255, image.getPixelValue(0, 1));
		}
	}
	
	@Test
	public final void testLoci2() throws Exception {
		final String base = "test/imj2/data/";
		final String[] ids = { "gray8.png", "gray8.pgm" };
		
		for (final String id : ids) {
			final String imageId = base + id;
			final Image2D image = new LociBackedImage(imageId);
			final DefaultColorModel color = new DefaultColorModel(image.getChannels());
			
			assertGray(color, 0, image.getPixelValue(0, 0));
			assertGray(color, 84, image.getPixelValue(1, 0));
			assertGray(color, 171, image.getPixelValue(0, 1));
			assertGray(color, 255, image.getPixelValue(1, 1));
		}
	}
	
	@Test
	public final void testLoci3() throws Exception {
		final String base = "test/imj2/data/";
		final String[] ids = { "bw1.pbm" };
		
		for (final String id : ids) {
			final String imageId = base + id;
			final Image2D image = new LociBackedImage(imageId);
			final DefaultColorModel color = new DefaultColorModel(image.getChannels());
			
			debugPrint(image.getWidth(), image.getHeight(), image.getChannels());
			
//			assertEquals(PredefinedChannels.C1_U1, image.getChannels());
			assertBinary(color, 0, image.getPixelValue(0, 0));
			assertBinary(color, 1, image.getPixelValue(1, 0));
			assertBinary(color, 1, image.getPixelValue(0, 1));
			assertBinary(color, 0, image.getPixelValue(1, 1));
		}
	}
	
	@Test
	public final void testShow1() throws Exception {
		if (!ExpensiveTest.SHOW1.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final String imageId = "test/imj/12003.jpg";
		final BufferedImage awtImage = ImageIO.read(new File(imageId));
		final Image2D image = new AwtBackedImage(imageId, awtImage);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		Image2DComponent.show(image);
		Image2DComponent.show(new AwtBackedImage(imageId, awtImage));
		Image2DComponent.show(new LociBackedImage(imageId));
	}
	
	@Test
	public final void testShow2() throws Exception {
		if (!ExpensiveTest.SHOW2.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final String imageId = "../Libraries/images/svs/45657.svs";
		final Image2D image = new LociBackedImage(imageId);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		Image2DComponent.show(image);
	}
	
	@Test
	public final void testHistogram1() {
		if (!ExpensiveTest.HISTOGRAM1.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final TicToc timer = new TicToc();
		final String imageId = "../Libraries/images/svs/40267.svs";
		final int workerCount = SystemProperties.getAvailableProcessorCount();
		final Image2D[] images = new Image2D[workerCount];
		
		debugPrint("workerCount:", workerCount);
		
		for (int i = 0; i < workerCount; ++i) {
			images[i] = new LociBackedImage(imageId);
		}
		
		final int imageWidth = images[0].getWidth();
		final int imageHeight = images[0].getHeight();
		
		debugPrint("imageWidth:", imageWidth, "imageHeight:", imageHeight, "channels:", images[0].getChannels());
		
		debugPrint("Allocating histograms...", "date:", new Date(timer.tic()));
		
		final int n = 256 * 256 * 256;
		final double[][] histograms = new double[workerCount][n];
		
		debugPrint("Allocating histograms done,", "time:", timer.toc());
		
		debugPrint("Computing histogram...", "date:", new Date(timer.tic()));
		
		final ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		
		try {
			final int verticalOptimalTileCount = (int) sqrt(workerCount);
			final int horizontalOptimalTileCount = workerCount / verticalOptimalTileCount;
			final Collection<Rectangle> tiles = new ArrayList<Rectangle>();
			final int optimalTileWidth = imageWidth / horizontalOptimalTileCount;
			final int optimalTileHeight = imageHeight / verticalOptimalTileCount;
			
			for (int tileY = 0, nextTileY = optimalTileHeight; tileY < imageHeight;
					tileY = nextTileY, nextTileY = min(imageHeight, nextTileY + optimalTileHeight)) {
				for (int tileX = 0, nextTileX = optimalTileWidth; tileX < imageWidth;
						tileX = nextTileX, nextTileX = min(imageWidth, nextTileX + optimalTileWidth)) {
					tiles.add(new Rectangle(tileX, tileY, nextTileX - tileX, nextTileY - tileY));
				}
			}
			
			debugPrint("tileCount:", tiles.size());
			
			final Collection<Future<?>> tasks = new ArrayList<Future<?>>(tiles.size());
			final Map<Thread, Integer> workerIds = new HashMap<Thread, Integer>(workerCount);
			
			for (final Rectangle tile : tiles) {
				tasks.add(executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						final int workerId = getOrCreateId(workerIds, Thread.currentThread());
						
						images[workerId].forEachPixelInBox(tile.x, tile.y, tile.width, tile.height, new MonopatchProcess() {
							
							@Override
							public final void pixel(final int x, final int y) {
								++histograms[workerId][images[workerId].getPixelValue(x, y) & 0x00FFFFFF];
							}
							
						});
					}
					
				}));
			}
			
			for (final Future<?> task : tasks) {
				task.get();
			}
		} catch (final Exception exception) {
			throw Tools.unchecked(exception);
		} finally {
			executor.shutdown();
		}
		
		debugPrint("Analyzing image done,", "time:", timer.toc());
		
		for (int i = 1; i < workerCount; ++i) {
			for (int j = 0; j < n; ++j) {
				histograms[0][j] += histograms[i][j];
			}
			
			histograms[i] = null;
		}
		
		debugPrint("Computing histogram done", "time:", timer.toc());
		
		assertEquals(images[0].getPixelCount(), IMJTools.sum(histograms[0]), 0.0);
		
	}
	
	private static final ExpensiveTest EXPENSIVE_TEST = ExpensiveTest.HISTOGRAM1;
	
	static {
		SwingTools.useSystemLookAndFeel();
	}
	
	public static final <K> int getOrCreateId(final Map<K, Integer> ids, final K key) {
		synchronized (ids) {
			Integer result = ids.get(key);
			
			if (result == null) {
				result = ids.size();
				ids.put(key, result);
			}
			
			return result;
		}
	}
	
	public static final void assertRGB(final DefaultColorModel color,
			final int expectedRed, final int expectedGreen, final int expectedBlue, final int actualPixelValue) {
		assertEquals(quantize(expectedRed, 2), quantize(color.red(actualPixelValue), 2));
		assertEquals(quantize(expectedGreen, 2), quantize(color.green(actualPixelValue), 2));
		assertEquals(quantize(expectedBlue, 2), quantize(color.blue(actualPixelValue), 2));
	}
	
	public static final void assertGray(final DefaultColorModel color,
			final int expectedGray, final int actualPixelValue) {
		assertEquals(quantize(expectedGray, 2), quantize(color.gray(actualPixelValue), 2));
	}
	
	public static final void assertBinary(final DefaultColorModel color,
			final int expectedBinary, final int actualPixelValue) {
		assertEquals(expectedBinary, color.binary(actualPixelValue));
	}
	
	/**
	 * @author codistmonk (creation 2013-08-14)
	 */
	private static enum ExpensiveTest {
		
		SHOW1, SHOW2, HISTOGRAM1;
		
	}
	
}
