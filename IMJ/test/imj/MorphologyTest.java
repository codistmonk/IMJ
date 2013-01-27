package imj;

import static imj.IMJTools.image;
import static imj.RegionalExtremaTest.assertImageEquals;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class MorphologyTest {
	
	@Test
	public final void testErosion1() {
		final Image image = image(new int[][] {
				{ 0 }
		});
		final Image expected = image(new int[][] {
				{ 0 }
		});
		
		assertImageEquals(expected, MorphologyOperations.erode4(image));
	}
	
	@Test
	public final void testErosion2() {
		final Image image = image(new int[][] {
				{ 1 }
		});
		final Image expected = image(new int[][] {
				{ 1 }
		});
		
		assertImageEquals(expected, MorphologyOperations.erode4(image));
	}
	
	@Test
	public final void testErosion3() {
		final Image image = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 2, 1 },
				{ 1, 1, 1 },
		});
		final Image expected = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 1, 1 },
				{ 1, 1, 1 },
		});
		
		assertImageEquals(expected, MorphologyOperations.erode4(image));
	}
	
	@Test
	public final void testDilation1() {
		final Image image = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 2, 1 },
				{ 1, 1, 1 },
		});
		final Image expected = image(new int[][] {
				{ 1, 2, 1 },
				{ 2, 2, 2 },
				{ 1, 2, 1 },
		});
		
		assertImageEquals(expected, MorphologyOperations.dilate4(image));
	}
	
}
