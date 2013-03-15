package imj;

import static imj.IMJTools.image;
import static imj.RegionalExtremaTest.assertImageEquals;
import imj.MathOperations.BinaryOperator;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-02-10)
 */
public final class MathOperationsTest {
	
	@Test
	public final void testUnaryOperation1() {
		final Image image = image(new int[][] {
				{ 0 }
		});
		final Image expectedResult = image(new int[][] {
				{ 1 }
		});
		
		assertImageEquals(expectedResult, MathOperations.compute(BinaryOperator.Predefined.PLUS.bindLeft(0, 1), image));
	}
	
	@Test
	public final void testUnaryOperation2() {
		final Image image = image(3, new int[][] {
				{ 0xFF00FF7F }
		});
		final Image expectedResult = image(3, new int[][] {
				{ 0xFF01FF80 }
		});
		
		assertImageEquals(expectedResult, MathOperations.compute(BinaryOperator.Predefined.PLUS.bindLeft(0, 1), image));
	}
	
}
