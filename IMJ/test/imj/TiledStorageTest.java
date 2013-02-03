package imj;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class TiledStorageTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
		final int n = 2048;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final TiledStorage image = new TiledStorage(n, n);
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
	public final void test2() {
		final TicToc timer = new TicToc();
		final int n = 8192;
		
		debugPrint("Allocating image:", new Date(timer.tic()));
		final TiledStorage image = new TiledStorage(n, n);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		final int rowTileCount = image.getRowTileCount();
		final int columnTileCount = image.getColumnTileCount();
		
		debugPrint("Writing image:", new Date(timer.tic()));
		for (int rowTileIndex = 0; rowTileIndex < rowTileCount; ++rowTileIndex) {
			final int firstRowIndex = rowTileIndex * image.getOptimalTileRowCount();
			final int endRowIndex = (rowTileIndex + 1) * image.getOptimalTileRowCount();
			
			for (int columnTileIndex = 0; columnTileIndex < columnTileCount; ++columnTileIndex) {
				final int firstColumnIndex = columnTileIndex * image.getOptimalTileColumnCount();
				final int endColumnIndex = (columnTileIndex + 1) * image.getOptimalTileColumnCount();
				
				for (int rowIndex = firstRowIndex; rowIndex < endRowIndex; ++rowIndex) {
					for (int columnIndex = firstColumnIndex; columnIndex < endColumnIndex; ++columnIndex) {
						final int pixel = image.getIndex(rowIndex, columnIndex);
						
						image.setValue(pixel, pixel);
					}
				}
			}
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Reading image:", new Date(timer.tic()));
		boolean ok = true;
		
		for (int rowTileIndex = 0; rowTileIndex < rowTileCount && ok; ++rowTileIndex) {
			final int firstRowIndex = rowTileIndex * image.getOptimalTileRowCount();
			final int endRowIndex = (rowTileIndex + 1) * image.getOptimalTileRowCount();
			
			for (int columnTileIndex = 0; columnTileIndex < columnTileCount && ok; ++columnTileIndex) {
				final int firstColumnIndex = columnTileIndex * image.getOptimalTileColumnCount();
				final int endColumnIndex = (columnTileIndex + 1) * image.getOptimalTileColumnCount();
				
				for (int rowIndex = firstRowIndex; rowIndex < endRowIndex; ++rowIndex) {
					for (int columnIndex = firstColumnIndex; columnIndex < endColumnIndex; ++columnIndex) {
						final int pixel = image.getIndex(rowIndex, columnIndex);
						
						ok = pixel == image.getValue(pixel);
					}
				}
			}
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		assertTrue(ok);
	}
	
}
