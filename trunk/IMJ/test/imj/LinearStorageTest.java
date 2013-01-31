package imj;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class LinearStorageTest {
	
	@Test(expected=OutOfMemoryError.class)
	public final void test0() {
		final TicToc timer = new TicToc();
		final int n = 8192;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final Image.Abstract image = new ImageOfInts(n, n);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		final int pixelCount = image.getPixelCount();
		
		debugPrint("Writing image:", new Date(timer.tic()));
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			image.setValue(pixel, pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Reading image:", new Date(timer.tic()));
		boolean ok = true;
		for (int pixel = 0; pixel < pixelCount && ok; ++pixel) {
			ok = pixel == image.getValue(pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		assertTrue(ok);
	}
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
		final int n = 8192;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final Image.Abstract image = new LinearStorage(n, n);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		final int pixelCount = image.getPixelCount();
		
		debugPrint("Writing image:", new Date(timer.tic()));
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			image.setValue(pixel, pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Reading image:", new Date(timer.tic()));
		boolean ok = true;
		
		for (int pixel = 0; pixel < pixelCount && ok; ++pixel) {
			ok = pixel == image.getValue(pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		assertTrue(ok);
	}
	
}
