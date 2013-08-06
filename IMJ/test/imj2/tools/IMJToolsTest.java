package imj2.tools;

import static org.junit.Assert.assertEquals;

import imj2.core.ConcreteImage2D;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-08-05)
 */
public final class IMJToolsTest {
	
	@Test
	public final void testAWT() throws Exception {
		final String base = "test/imj2/data/";
		final String[] ids = { "rgb24.gif", "rgb24.jpg", "rgb24.png", "rgb32.png" };
		
		for (final String id : ids) {
			final String imageId = base + id;
			final BufferedImage expectedAwtImage = ImageIO.read(new File(imageId));
			final BufferedImage actualAwtImage = IMJTools.awtImage(IMJTools.newImage(imageId, expectedAwtImage));
			final int width = expectedAwtImage.getWidth();
			final int height = expectedAwtImage.getHeight();
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					assertEquals(expectedAwtImage.getRGB(x, y), actualAwtImage.getRGB(x, y));
				}
			}
		}
	}
	
//	@Test
	public final void test() throws Exception {
		final String imageId = "test/imj/12003.jpg";
		final BufferedImage awtImage = ImageIO.read(new File(imageId));
		final ConcreteImage2D image = IMJTools.newImage(imageId, awtImage);
		
		Tools.debugPrint(image.getWidth(), image.getHeight(), image.getChannels());
		IMJTools.show(image);
	}
	
}
