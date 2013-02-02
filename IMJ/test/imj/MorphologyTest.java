package imj;

import static imj.IMJTools.image;
import static imj.Labeling.CONNECTIVITY_4;
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
		final Image expectedResult = image(new int[][] {
				{ 0 }
		});
		
		assertImageEquals(expectedResult, MorphologicalOperations.erode4(image));
	}
	
	@Test
	public final void testErosion2() {
		final Image image = image(new int[][] {
				{ 1 }
		});
		final Image expectedResult = image(new int[][] {
				{ 1 }
		});
		
		assertImageEquals(expectedResult, MorphologicalOperations.erode4(image));
	}
	
	@Test
	public final void testErosion3() {
		final Image image = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 2, 1 },
				{ 1, 1, 1 },
		});
		final Image expectedResult = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 1, 1 },
				{ 1, 1, 1 },
		});
		
		assertImageEquals(expectedResult, MorphologicalOperations.erode4(image));
	}
	
	@Test
	public final void testDilation1() {
		final Image image = image(new int[][] {
				{ 1, 1, 1 },
				{ 1, 2, 1 },
				{ 1, 1, 1 },
		});
		final Image expectedResult = image(new int[][] {
				{ 1, 2, 1 },
				{ 2, 2, 2 },
				{ 1, 2, 1 },
		});
		
		assertImageEquals(expectedResult, MorphologicalOperations.dilate4(image));
	}
	
	@Test
	public final void testHMinima1() {
		final Image image = image(new int[][] {
				{ 10,  7, 10,  3,  2,  5,  0 }
		});
		final int h = 4;
		final Image expectedResult = image(new int[][] {
				{ 10, 10, 10,  5,  5,  5,  4 }
		});
		final Image hMinima = MorphologicalOperations.hMinima(image, h, CONNECTIVITY_4);
		
		assertImageEquals(expectedResult, hMinima);
	}
	
	@Test
	public final void testHMinima2() {
		final Image image = image(new int[][] {
				{ 10, 10, 10, 10, 10 },
				{ 10,  7,  7, 10, 10 },
				{ 10,  7, 10,  2, 10 },
				{ 10, 10,  2,  2, 10 },
				{ 10, 10, 10, 10, 10 },
		});
		final int h = 4;
		final Image expectedResult = image(new int[][] {
				{ 10, 10, 10, 10, 10 },
				{ 10, 10, 10, 10, 10 },
				{ 10, 10, 10,  6, 10 },
				{ 10, 10,  6,  6, 10 },
				{ 10, 10, 10, 10, 10 },
		});
		final Image hMinima = MorphologicalOperations.hMinima(image, h, CONNECTIVITY_4);
		
		assertImageEquals(expectedResult, hMinima);
	}
	
	@Test
	public final void testHMaxima1() {
		final Image image = image(new int[][] {
				{ 1, 2, 1 },
		});
		final int h = 1;
		final Image expectedResult = image(new int[][] {
				{ 1, 1, 1 },
		});
		final Image hMaxima = MorphologicalOperations.hMaxima(image, h, CONNECTIVITY_4);
		
		assertImageEquals(expectedResult, hMaxima);
	}
	
	@Test
	public final void testHMaxima2() {
		final Image image = image(new int[][] {
				{ 1, 1, 1, 1, 1 },
				{ 1, 2, 2, 1, 3 },
				{ 1, 2, 1, 1, 1 },
				{ 1, 2, 2, 2, 1 },
		});
		final int h = 1;
		final Image expectedResult = image(new int[][] {
				{ 1, 1, 1, 1, 1 },
				{ 1, 1, 1, 1, 2 },
				{ 1, 1, 1, 1, 1 },
				{ 1, 1, 1, 1, 1 },
		});
		final Image hMaxima = MorphologicalOperations.hMaxima(image, h, CONNECTIVITY_4);
		
		assertImageEquals(expectedResult, hMaxima);
	}
	
}
