package imj;

import static imj.IMJTools.image;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageOfBufferedImage.Feature;

import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-29)
 */
public final class LinearFilterTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
		final String imageId = "../Libraries/images/16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Extracting edges:", new Date(timer.tic()));
		final Image edges = new LinearFilter(image, new double[] {
				-1.0, -1.0, -1.0,
				-1.0, +1.0, +1.0,
				+1.0, -1.0, -1.0,
				+1.0, +1.0, +1.0,
		}).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		ImageComponent.showAdjusted(imageId, image, edges);
	}
	
}
