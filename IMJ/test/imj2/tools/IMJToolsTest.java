package imj2.tools;

import static imj2.tools.IMJTools.quantize;
import static imj2.tools.MultiThreadTools.WORKER_COUNT;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.list;
import static org.junit.Assert.assertEquals;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
		
		debugPrint("workerCount:", WORKER_COUNT);
		
		final TicToc timer = new TicToc();
		final String imageId = "../Libraries/images/svs/40267.svs";
		final Image2D[] images = new LociBackedImage(imageId).newParallelViews(WORKER_COUNT);
		final int imageWidth = images[0].getWidth();
		final int imageHeight = images[0].getHeight();
		
		debugPrint("imageWidth:", imageWidth, "imageHeight:", imageHeight, "channels:", images[0].getChannels());
		
		debugPrint("Allocating histograms...", "date:", new Date(timer.tic()));
		
		final int colorCount = 256 * 256 * 256;
		final double[][] histograms = new double[WORKER_COUNT][colorCount];
		
		debugPrint("Allocating histograms done,", "time:", timer.toc());
		
		debugPrint("Computing histogram...", "date:", new Date(timer.tic()));
		
		final ExecutorService executor = MultiThreadTools.getExecutor();
		final Collection<Rectangle> tiles = list(IMJTools.parallelTiles(imageWidth, imageHeight, WORKER_COUNT));
		
		debugPrint("tileCount:", tiles.size());
		
		final Collection<Future<?>> tasks = new ArrayList<Future<?>>(tiles.size());
		
		for (final Rectangle tile : tiles) {
			tasks.add(executor.submit(new Runnable() {
				
				@Override
				public final void run() {
					final int workerId = MultiThreadTools.getWorkerId();
					
					images[workerId].forEachPixelInBox(tile.x, tile.y, tile.width, tile.height, new MonopatchProcess() {
						
						@Override
						public final void pixel(final int x, final int y) {
							++histograms[workerId][images[workerId].getPixelValue(x, y) & 0x00FFFFFF];
						}
						
						/**
						 * {@value}.
						 */
						private static final long serialVersionUID = -6167552483623444181L;
						
					});
				}
				
			}));
		}
		
		MultiThreadTools.wait(tasks);
		
		debugPrint("Analyzing image done,", "time:", timer.toc());
		
		for (int workerId = 1; workerId < WORKER_COUNT; ++workerId) {
			for (int color = 0; color < colorCount; ++color) {
				histograms[0][color] += histograms[workerId][color];
			}
			
			histograms[workerId] = null;
		}
		
		debugPrint("Computing histogram done", "time:", timer.toc());
		
		assertEquals(images[0].getPixelCount(), IMJTools.sum(histograms[0]), 0.0);
	}
	
	private static final ExpensiveTest EXPENSIVE_TEST = ExpensiveTest.HISTOGRAM1;
	
	@BeforeClass
	public static final void beforeClass() {
		SwingTools.useSystemLookAndFeel();
	}
	
	@AfterClass
	public static final void afterClass() {
		MultiThreadTools.shutdownExecutor();
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
