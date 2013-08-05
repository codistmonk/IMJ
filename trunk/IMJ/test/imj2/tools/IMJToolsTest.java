package imj2.tools;

import static org.junit.Assert.*;
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
	public final void test() throws Exception {
		final String imageId = "test/imj/12003.jpg";
		final BufferedImage awtImage = ImageIO.read(new File(imageId));
		final ConcreteImage2D image = IMJTools.newImage(imageId, awtImage);
		
		Tools.debugPrint(image.getWidth(), image.getHeight(), image.getChannels());
		IMJTools.show(image);
	}
	
}
