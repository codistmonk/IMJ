package imj2.tools;

import static imj2.tools.IMJTools.quantize;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.assertEquals;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

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
	
//	@Test
	public final void testShow1() throws Exception {
		final String imageId = "test/imj/12003.jpg";
		final BufferedImage awtImage = ImageIO.read(new File(imageId));
		final Image2D image = new AwtBackedImage(imageId, awtImage);
		
		debugPrint(image.getWidth(), image.getHeight(), image.getChannels());
		IMJTools.show(image);
		IMJTools.show(new AwtBackedImage(imageId, awtImage));
		IMJTools.show(new LociBackedImage(imageId));
	}
	
//	@Test
	public final void testShow2() throws Exception {
		final String imageId = "../Libraries/images/svs/16088.svs";
		final Image2D image = new LociBackedImage(imageId);
		
		debugPrint(image.getWidth(), image.getHeight(), image.getChannels());
		IMJTools.show(image);
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
	
}
