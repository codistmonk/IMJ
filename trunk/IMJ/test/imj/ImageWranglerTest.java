package imj;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.ProgressMonitor;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class ImageWranglerTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
//		final String imageId = "../Libraries/images/16088-4.png";
		final String imageId = "../Libraries/images/42628.svs";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = ImageWrangler.INSTANCE.load(imageId);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		ImageComponent.show(imageId, image);
	}
	
}

/**
 * @author codistmonk (creation 2013-01-31)
 */
final class ImageWrangler {
	
	private final Preferences preferences;
	
	private ImageWrangler() {
		this.preferences = Preferences.userNodeForPackage(this.getClass());
	}
	
	public final Image load(final String imageId) {
		final String fileId = this.preferences.get("image:" + imageId, null);
		final File maybeExistsingFile = new File(fileId == null ? "" : fileId);
		
		if (fileId == null || !(maybeExistsingFile.exists() && maybeExistsingFile.isFile())) {
			final IFormatReader reader = new ImageReader();
			
			try {
				reader.setId(imageId);
				
				final int rowCount = reader.getSizeY();
				final int columnCount = reader.getSizeX();
				debugPrint("Allocating");
				final LinearStorage result = new LinearStorage(rowCount, columnCount, false);
				debugPrint("Allocated in file:", result.getFile());
				final int optimalTileRowCount = reader.getOptimalTileHeight();
				final int optimalTileColumnCount = reader.getOptimalTileWidth();
				final int channelCount = reader.getSizeC();
				final int bufferRowCount;
				final int bufferColumnCount;
				final ProgressMonitor progressMonitor = new ProgressMonitor(null, "Loading", null, 0, rowCount - 1);
				
				result.getMetadata().put("channelCount", channelCount);
				
				debugPrint("rowCount:", rowCount, "columnCount:", columnCount);
				debugPrint("channelCount:", channelCount);
				debugPrint("optimalTileRowCount:", optimalTileRowCount, "optimalTileColumnCount:", optimalTileColumnCount);
				
				if (optimalTileRowCount < rowCount || optimalTileColumnCount < columnCount) {
					bufferRowCount = optimalTileRowCount;
					bufferColumnCount = optimalTileColumnCount;
				} else {
					bufferRowCount = 4;
					bufferColumnCount = columnCount;
				}
				
				final byte[] buffer = new byte[bufferRowCount * bufferColumnCount * channelCount];
					
				for (int y = 0; y < rowCount && !progressMonitor.isCanceled(); y += bufferRowCount) {
					final int h = y + bufferRowCount <= rowCount ? bufferRowCount : rowCount - y;
					final int endRowIndex = y + h;
					
					for (int x = 0; x < columnCount && !progressMonitor.isCanceled(); x += bufferColumnCount) {
						final int w = x + bufferColumnCount <= columnCount ? bufferColumnCount : columnCount - x;
						final int endColumnIndex = x + w;
						final int channelSize = h * w;
						
						reader.openBytes(0, buffer, x, y, w, h);
						
						for (int rowIndex = y, yy = 0; rowIndex < endRowIndex; ++rowIndex, ++yy) {
							for (int columnIndex = x, xx = 0; columnIndex < endColumnIndex; ++columnIndex, ++xx) {
								final int i = yy * w + xx;
								int value = 0x000000FF;
								
								switch (channelCount) {
								default:
								case 1:
									value = unsigned(buffer[i]);
									break;
								case 4:
									// alpha
									value = unsigned(buffer[i + (channelCount - 1) * channelSize]);
								case 3:
									final int red = unsigned(buffer[i + 0 * channelSize]);
									final int green = unsigned(buffer[i + 1 * channelSize]);
									final int blue = unsigned(buffer[i + 2 * channelSize]);
									value = (value << 24) | (red << 16) | (green << 8) | blue;
									break;
								}
								
								result.setValue(rowIndex, columnIndex, value);
							}
						}
					}
					
					progressMonitor.setProgress(y);
				}
				
				progressMonitor.close();
				
				this.preferences.put("image:" + imageId, result.getFile().toString());
				
				return result;
			} catch (final Exception exception) {
				throw unchecked(exception);
			} finally {
				try {
					reader.close();
				} catch (final IOException exception) {
					throw unchecked(exception);
				}
			}
		}
		
		return LinearStorage.open(new File(fileId));
	}
	
	public static final ImageWrangler INSTANCE = new ImageWrangler();
	
	public static final int unsigned(final byte value) {
		return value & 0x000000FF;
	}
	
}
