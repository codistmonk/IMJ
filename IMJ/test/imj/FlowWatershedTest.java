package imj;

import static imj.ImageComponent.showAdjusted;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class FlowWatershedTest {
	
	@Test
	public final void reverseEngineerArticleGraph() {
		final int[][] absoluteDifferences =  {
				{ 0, 1, 2 }, { 0, 4, 1 },
				{ 1, 2, 6 }, { 1, 5, 5 },
				{ 2, 3, 6 }, { 2, 6, 5 },
				{ 3, 7, 4 },
				{ 4, 5, 2 }, { 4, 8, 2 },
				{ 5, 6, 6 }, { 5, 9, 6 },
				{ 6, 7, 5 }, { 6, 10, 7 },
				{ 7, 11, 5 },
				{ 8, 9, 5 },
		};
		final int[] nodes = new int[12];
		
//		final int[][] absoluteDifferences =  {
//				{  0,  1, 2 }, {  0,  4, 1 },
//				{  1,  2, 6 }, {  1,  5, 5 },
//				{  2,  3, 6 }, {  2,  6, 5 },
//				{  3,  7, 4 },
//				{  4,  5, 2 }, {  4,  8, 2 },
//				{  5,  6, 6 }, {  5,  9, 6 },
//				{  6,  7, 5 }, {  6, 10, 7 },
//				{  7, 11, 5 },
//				{  8,  9, 5 }, {  8, 12, 8 },
//				{  9, 10, 7 }, {  9, 13, 7 },
//				{ 10, 11, 3 }, { 10, 14, 3 },
//				{ 11, 15, 3 },
//				{ 12, 13, 6 },
//				{ 13, 14, 5 },
//				{ 14, 15, 3 },
//		};
//		final int[] nodes = new int[16];
//		
//		assertEquals(24, absoluteDifferences.length);
		
		for (int signBits = 0; signBits <= 0x00FFFFFF; ++signBits) {
			Arrays.fill(nodes, Integer.MIN_VALUE);
			nodes[0] = 0;
			int signMask = 0x00000001;
			
			for (final int[] absoluteDifference : absoluteDifferences) {
				final int i = absoluteDifference[0];
				final int j = absoluteDifference[1];
				final int d = absoluteDifference[2] * ((signBits & signMask) == 0 ? 1 : -1);
				
				if (nodes[j] == Integer.MIN_VALUE) {
					nodes[j] = nodes[i] + d;
				} else if (nodes[i] + d != nodes[j]) {
					break;
				}
				
				signMask <<= 1;
			}
			
			if (signMask == (1 << absoluteDifferences.length)) {
				debugPrint(Arrays.toString(nodes));
				break;
			}
		}
	}
	
	@Test
	public final void test1() throws IOException {
		final TicToc timer = new TicToc();
		final String imageId = "test/imj/12003.jpg";
//		final String imageId = "lib/images/16088-2.png";
//		final String imageId = "lib/images/42.png";
		final Image image0 = new ImageOfBufferedImage(read(new File(imageId)), Feature.MAX_RGB);
		
//		final String imageId = "[synthetic]";
//		final Image image0 = image(new int[][] {
//				{ 0, 1, 1, 0 },
//		});
		
//		debugPrint(image0);
//		
//		showAdjusted(imageId, image0, new FlowWatershed(image0).getLabels());
		
		debugPrint("Computing watershed stack", "(", "started:", new Date(timer.tic()), ")");
		final LabelingStack watersheds = LabelingStack.newInstanceFor(image0, 4, StatisticsSelector.MEAN, FlowWatershed.class);
		debugPrint("Done:", timer.toc(), "ms");
		
		showAdjusted(imageId, watersheds.getAllImages());
	}
	
}
