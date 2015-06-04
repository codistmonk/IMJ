package imj2.tools;

import static org.junit.Assert.*;
import imj2.tools.SplitImage.FractalZ2D;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-11-23)
 */
public final class FractalZ2DTest {
	
	@Test
	public final void test1() {
		final int x = 42;
		final int y = 33;
		final long index = FractalZ2D.getIndex(x, y);
		
		assertEquals(y, FractalZ2D.getY(index));
		assertEquals(x, FractalZ2D.getX(index));
	}
	
	@Test
	public final void test2() {
		assertEquals(0, FractalZ2D.getY(0L));
		assertEquals(0, FractalZ2D.getX(0L));
		
		assertEquals(0, FractalZ2D.getY(1L));
		assertEquals(1, FractalZ2D.getX(1L));
		
		assertEquals(1, FractalZ2D.getY(2L));
		assertEquals(0, FractalZ2D.getX(2L));
		
		assertEquals(1, FractalZ2D.getY(3L));
		assertEquals(1, FractalZ2D.getX(3L));
		
		assertEquals(0, FractalZ2D.getY(4L));
		assertEquals(2, FractalZ2D.getX(4L));
		
		assertEquals(0, FractalZ2D.getY(5L));
		assertEquals(3, FractalZ2D.getX(5L));
		
		assertEquals(1, FractalZ2D.getY(6L));
		assertEquals(2, FractalZ2D.getX(6L));
		
		assertEquals(1, FractalZ2D.getY(7L));
		assertEquals(3, FractalZ2D.getX(7L));
	}
	
}
