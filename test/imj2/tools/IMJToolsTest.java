package imj2.tools;

import static imj2.core.IMJCoreTools.quantize;
import static imj2.tools.MultiThreadTools.WORKER_COUNT;
import static multij.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;

import imj2.core.ConcreteImage2D;
import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;
import imj2.core.LinearIntImage;
import imj2.core.SubsampledImage2D;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Scanner;

import javax.imageio.ImageIO;

import multij.swing.SwingTools;
import multij.tools.TicToc;

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
//		final String imageId = "E:\\SCANS\\AML\\AML_run1.svs";
//		final String imageId = "E:\\SCANS\\APG Kontrollen\\Kontrollgewebe_Tonsille_run1.svs";
//		final String imageId = "E:\\SCANS\\hBC_Pilot\\SYS15_A08_0002-001.svs";
		final Image2D image = new LociBackedImage(imageId);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		Image2DComponent.show(image);
	}
	
	@Test
	public final void testShow3() throws Exception {
		if (!ExpensiveTest.SHOW3.equals(EXPENSIVE_TEST)) {
			return;
		}
		
//		final String imageSimpleName = "SYS15_A08_0002-006_lod0";
		final String imageSimpleName = "16088_lod5";
		final Image2D image = new MultifileImage("../Libraries/images/jpg/", imageSimpleName,
				MultifileImage.DEFAULT_CONNECTION_CONFIGURATOR);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		Image2DComponent.show(image);
	}
	
	@Test
	public final void testShow4() throws Exception {
		if (!ExpensiveTest.SHOW4.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final Scanner scanner = new Scanner(new File("../Libraries/images/wsi_url.txt"));
		final URL imageDirectory = new URL(scanner.nextLine());
		
		scanner.close();
		
		final String imageSimpleName = "SYS15_A08_0002-006_lod0";
		
		System.setProperty("javax.net.ssl.trustStore", new File("../Libraries/cert/cacerts").getCanonicalPath());
		
		final Image2D image = new MultifileImage(imageDirectory.toString() + "/" + imageSimpleName, imageSimpleName + "_lod0",
				new HTTPSAuthenticationForHost(imageDirectory.getHost()));
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		Image2DComponent.show(image);
	}
	
	@Test
	public final void testShow5() throws Exception {
		if (!ExpensiveTest.SHOW5.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final Scanner scanner = new Scanner(new File("../Libraries/images/wsi_url.txt"));
		scanner.nextLine();
		
		URL.setURLStreamHandlerFactory(new SFTPStreamHandlerFactory());
		
		final URL imageDirectory = new URL(scanner.nextLine());
		
		scanner.close();
		
		final String imageName = "16088";
		final String imageLodName = imageName + "_lod0";
		
		final Image2D image = new MultifileImage(imageDirectory.toString() + "/" + imageName, imageLodName,
				new HTTPSAuthenticationForHost(imageDirectory.getHost()));
		
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
		final Image2D image = new LociBackedImage(imageId);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		debugPrint("Allocating histograms...", "date:", new Date(timer.tic()));
		
		final int colorCount = 256 * 256 * 256;
		final double[][] histograms = new double[WORKER_COUNT][colorCount];
		
		debugPrint("Allocating histograms done,", "time:", timer.toc());
		
		debugPrint("Computing histogram...", "date:", new Date(timer.tic()));
		
		new ParallelProcess2D(image) {
			
			@Override
			public final void pixel(final int x, final int y) {
				++histograms[this.getWorkerId()][this.getImage().getPixelValue(x, y) & 0x00FFFFFF];
			}
			
			/**
			 * {@value].
			 */
			private static final long serialVersionUID = -5222028164806038927L;
			
		};
		
		debugPrint("Analyzing image done,", "time:", timer.toc());
		
		for (int workerId = 1; workerId < WORKER_COUNT; ++workerId) {
			for (int color = 0; color < colorCount; ++color) {
				histograms[0][color] += histograms[workerId][color];
			}
			
			histograms[workerId] = null;
		}
		
		debugPrint("Computing histogram done", "time:", timer.toc());
		
		assertEquals(image.getPixelCount(), IMJTools.sum(histograms[0]), 0.0);
	}
	
	@Test
	public final void testLod1() {
		if (!ExpensiveTest.LOD1.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final TicToc timer = new TicToc();
		final String imageId = "../Libraries/images/svs/40267.svs";
		final Image2D image = new LociBackedImage(imageId);
		final DefaultColorModel color = new DefaultColorModel(image.getChannels());
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		debugPrint("Allocating LOD 1...", "date:", new Date(timer.tic()));
		
		final int imageLod1Width = image.getWidth() / 2;
		final int imageLod1Height = image.getHeight() / 2;
		final Image2D imageLod1 = new ConcreteImage2D(new LinearIntImage(imageId,
				(long) imageLod1Width * imageLod1Height, image.getChannels()), imageLod1Width, imageLod1Height);
		
		debugPrint("Allocating LOD 1 done,", "time:", timer.toc());
		
		debugPrint("Creating LOD 1...", "date:", new Date(timer.tic()));
		
		final Image2D[] imagesLod0 = image.newParallelViews(WORKER_COUNT);
		
		new ParallelProcess2D(imageLod1) {
			
			private Image2D imageLod0;
			
			@Override
			protected final void beforeProcessing() {
				this.imageLod0 = imagesLod0[this.getWorkerId()];
			}
			
			@Override
			public final void pixel(final int x, final int y) {
				final int rgba00 = this.imageLod0.getPixelValue(x * 2 + 0, y * 2 + 0);
				final int rgba10 = this.imageLod0.getPixelValue(x * 2 + 1, y * 2 + 0);
				final int rgba01 = this.imageLod0.getPixelValue(x * 2 + 0, y * 2 + 1);
				final int rgba11 = this.imageLod0.getPixelValue(x * 2 + 1, y * 2 + 1);
				final int red = (color.red(rgba00) + color.red(rgba10) + color.red(rgba01) + color.red(rgba11)) / 4;
				final int green = (color.green(rgba00) + color.green(rgba10) + color.green(rgba01) + color.green(rgba11)) / 4;
				final int blue = (color.blue(rgba00) + color.blue(rgba10) + color.blue(rgba01) + color.blue(rgba11)) / 4;
				final int alpha = (color.alpha(rgba00) + color.alpha(rgba10) + color.alpha(rgba01) + color.alpha(rgba11)) / 4;
				
				this.getImage().setPixelValue(x, y, DefaultColorModel.argb(red, green, blue, alpha));
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 9059308373967781139L;
			
		};
		
		debugPrint("Creating LOD 1 done,", "time:", timer.toc());
		
		Image2DComponent.show(imageLod1);
	}
	
	@Test
	public final void testLod2() {
		if (!ExpensiveTest.LOD2.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final String imageId = "../Libraries/images/svs/40267.svs";
		final Image2D imageLod0 = new LociBackedImage(imageId);
		final Image2D imageLod1 = new SubsampledImage2D(imageLod0);
		final Image2D imageLod2 = new SubsampledImage2D(imageLod1);
		
		debugPrint(imageLod1.getId());
		debugPrint(imageLod2.getId());
		
		Image2DComponent.show(imageLod2);
	}
	
	@Test
	public final void testLBP1() {
		if (!ExpensiveTest.LBP1.equals(EXPENSIVE_TEST)) {
			return;
		}
		
		final String imageId = "../Libraries/images/svs/40267.svs";
		final Image2D image = new LociBackedImage(imageId);
		
		debugPrint("imageWidth:", image.getWidth(), "imageHeight:", image.getHeight(), "channels:", image.getChannels());
		
		final Image2D lbpImage = new LocalBinaryPatternImage(image);
		
		Image2DComponent.show(lbpImage);
	}
	
	private static final ExpensiveTest EXPENSIVE_TEST = ExpensiveTest.SHOW5;
	
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
		
		SHOW1, SHOW2, SHOW3, SHOW4, SHOW5, HISTOGRAM1, LOD1, LOD2, LBP1;
		
	}
	
}
