package imj;

import static imj.IMJTools.binary;
import static imj.IMJTools.image;
import static imj.Labeling.CONNECTIVITY_4;
import static imj.Labeling.CONNECTIVITY_8;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageOfBufferedImage.Feature;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public final class RegionalExtremaTest {
	
	@Test
	public final void test1() {
		final Image image = image(new int[][] {
				{ 0 }
		});
		final Image expected = image(new int[][] {
				{ 1 }
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test2() {
		final Image image = image(new int[][] {
				{ 0, 0 }
		});
		final Image expected = image(new int[][] {
				{ 1, 1 }
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test3() {
		final Image image = image(new int[][] {
				{ 0, 0 },
				{ 0, 0 }
		});
		final Image expected = image(new int[][] {
				{ 1, 1 },
				{ 1, 1 }
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test4() {
		final Image image = image(new int[][] {
				{ 0, 1 },
		});
		final Image expected = image(new int[][] {
				{ 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test5() {
		final Image image = image(new int[][] {
				{ 1, 0 },
		});
		final Image expected = image(new int[][] {
				{ 0, 1 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test6() {
		final Image image = image(new int[][] {
				{ 0, 1, 0 },
		});
		final Image expected = image(new int[][] {
				{ 1, 0, 1 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test7() {
		final Image image = image(new int[][] {
				{ 1, 0, 1 },
		});
		final Image expected = image(new int[][] {
				{ 0, 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test8() {
		final Image image = image(new int[][] {
				{ 1, 0, 0, 1 },
		});
		final Image expected = image(new int[][] {
				{ 0, 1, 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test9() {
		final Image image = image(new int[][] {
				{ 2, 1, 0, 1 },
		});
		final Image expected = image(new int[][] {
				{ 0, 0, 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test10() {
		final Image image = image(new int[][] {
				{ 1, 1 },
				{ 0, 2 },
		});
		final Image expected = image(new int[][] {
				{ 0, 0 },
				{ 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test11() {
		final Image image = image(new int[][] {
				{ 2, 3, 1 },
				{ 1, 0, 0 },
				{ 2, 0, 1 },
		});
		final Image expected = image(new int[][] {
				{ 0, 0, 0 },
				{ 0, 1, 1 },
				{ 0, 1, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test12() {
		final Image image = image(new int[][] {
				{ 3, 2, 3, 1 },
				{ 2, 1, 2, 0 },
				{ 3, 1, 1, 1 },
		});
		final Image expected = image(new int[][] {
				{ 0, 0, 0, 0 },
				{ 0, 0, 0, 1 },
				{ 0, 0, 0, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test13() {
		final Image image = image(new int[][] {
				{ 174, 168, 174 },
				{ 161, 168, 173 },
				{ 159, 166, 171 },
		});
		final Image expected = image(new int[][] {
				{ 0, 0, 0 },
				{ 0, 0, 0 },
				{ 1, 0, 0 },
		});
		final Image minima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
		
		assertImageEquals(expected, minima);
	}
	
	@Test
	public final void test14() throws IOException {
		final TicToc timer = new TicToc();
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image("test/imj/12003.jpg", Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image expectedMaxRGB = image("test/imj/12003_max_rgb.png", Feature.TO_UINT_8);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		assertImageEquals(expectedMaxRGB, image);
		
		{
			debugPrint("Loading image:", new Date(timer.tic()));
			final Image expectedMinima = image("test/imj/12003_minima_4.png", Feature.TO_UINT_8);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing minima:", new Date(timer.tic()));
			final Image actualMinima = binary(new RegionalMinima(image, CONNECTIVITY_4).getResult());
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			assertImageEquals(expectedMinima, actualMinima);
		}
		
		{
			debugPrint("Loading image:", new Date(timer.tic()));
			final Image expectedMinima = image("test/imj/12003_minima_8.png", Feature.TO_UINT_8);
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			debugPrint("Computing minima:", new Date(timer.tic()));
			final Image actualMinima = binary(new RegionalMinima(image, CONNECTIVITY_8).getResult());
			debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
			
			assertImageEquals(expectedMinima, actualMinima);
		}
	}
	
	public static final String toString(final Image image) {
		final StringBuilder resultBuilder = new StringBuilder();
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		
		for (int y = 0; y < rowCount; ++y) {
			for (int x = 0; x < columnCount; ++x) {
				resultBuilder.append(image.getValue(y, x)).append(' ');
			}
			
			resultBuilder.append('\n');
		}
		
		return resultBuilder.toString();
	}
	
	public static final void assertImageEquals(final Image expected, final Image actual) {
		final int pixelCount = expected.getRowCount() * expected.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int expectedPixelValue = expected.getValue(pixel);
			final int actualPixelValue = actual.getValue(pixel);
			
			if (expectedPixelValue != actualPixelValue) {
				throw new AssertionError("Mismatching pixel " + pixel + " (expected: " + expectedPixelValue + " actual: " + actualPixelValue + ")");
			}
		}
	}
	
}
