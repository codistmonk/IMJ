package imj;

import static imj.ImageComponent.showAdjusted;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class SimpleWatershedTest {
	
	@Test
	public final void test1() throws IOException {
		final TicToc timer = new TicToc();
//		final String imageId = "lib/images/42.png";
//		final String imageId = "lib/images/16088-2.png";
//		final String imageId = "lib/images/16088-4.png";
		final String imageId = "test/imj/test20.png";
//		final String imageId = "lib/images/40267-4.png";
//		final String imageId = "bsds/train/12003.jpg";
		final Image image0 = new ImageOfBufferedImage(read(new File(imageId)), Feature.MAX_RGB);
//		final String imageId = "[synthetic]";
//		final Image image = image(new int[][] {
//				{ 1, 1, 1, 1, 1 },
//				{ 1, 1, 0, 1, 1 },
//				{ 1, 1, 1, 1, 1 },
//				{ 1, 1, 1, 1, 1 },
//				{ 1, 1, 1, 1, 1 },
//				{ 1, 1, 1, 0, 1 },
//				{ 1, 0, 1, 1, 1 },
//				{ 1, 1, 1, 1, 1 },
//		});
//		final Image image = image(new int[][] {
//				{ 1, 1, 1, 1, 1, 1 },
//				{ 1, 0, 1, 1, 0, 1 },
//				{ 1, 0, 1, 1, 0, 1 },
//				{ 1, 0, 0, 0, 0, 1 },
//				{ 1, 1, 1, 1, 1, 1 },
//				{ 1, 0, 0, 0, 0, 1 },
//				{ 1, 0, 1, 1, 0, 1 },
//				{ 1, 0, 1, 1, 0, 1 },
//				{ 1, 1, 1, 1, 1, 1 },
//		});
		
		debugPrint(image0);
		
		debugPrint("Computing watershed stack", "(", "started:", new Date(timer.tic()), ")");
		final LabelingStack watersheds = LabelingStack.newInstanceFor(image0, 2, StatisticsSelector.MEAN, SimpleWatershed.class);
		debugPrint("Done:", timer.toc(), "ms");
		
//		showAdjusted(imageId, watersheds.getAllLabels());
		showAdjusted(imageId, watersheds.getAllLabels());
	}
	
}
