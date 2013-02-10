package imj;

import static imj.LinearStorage.DATUM_SIZE;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

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
		final Image.Abstract image = new ImageOfInts(n, n, 1);
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
		final Image.Abstract image = new LinearStorage(n, n, 1);
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
	
	@Test(expected=OutOfMemoryError.class)
	public final void testDirectByteBuffer() {
		final TicToc timer = new TicToc();
		final int n = 8192;
		final int pixelCount = n * n;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final ByteBuffer buffer = ByteBuffer.allocateDirect((int) (n * n * LinearStorage.DATUM_SIZE));
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Writing image:", new Date(timer.tic()));
		for (int pixel = 0; pixel < n * n; ++pixel) {
			buffer.putInt(pixel, pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Reading image:", new Date(timer.tic()));
		boolean ok = true;
		
		for (int pixel = 0; pixel < pixelCount && ok; ++pixel) {
			ok = pixel == buffer.getInt(pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		assertTrue(ok);
	}
	
	@Test
	public final void testChannel() throws IOException {
		final TicToc timer = new TicToc();
		final int n = 8192;
		final int pixelCount = n * n;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final File file = File.createTempFile("image", ".raw");
		file.deleteOnExit();
		debugPrint(file);
		final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		final MappedByteBuffer map = raf.getChannel().map(MapMode.READ_WRITE, 0L, pixelCount * DATUM_SIZE);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Writing image:", new Date(timer.tic()));
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			map.putInt((int) (pixel * DATUM_SIZE), pixel);
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint(map.limit(), map.position(), map.capacity());
		
		debugPrint("Reading image:", new Date(timer.tic()));
		boolean ok = true;
		
		for (int pixel = 0; pixel < pixelCount && ok; ++pixel) {
			ok = pixel == map.getInt((int) (pixel * DATUM_SIZE));
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		raf.close();
		
		assertTrue(ok);
	}
	
}
