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
		final Image image = ImageWrangler.INSTANCE.load(imageId, 3);
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
	
	public static final int zeroIfNull(final Integer value) {
		return value == null ? 0 : value;
	}
	
	public final Image load(final String imageId, final int levelOfDetail) {
		debugPrint("LOD", levelOfDetail, "requested for image", imageId);
		
		if (0 == levelOfDetail) {
			return this.load(imageId);
		}
		
		final String imageKey = "image:" + imageId + ":" + levelOfDetail;
		final String fileId = this.preferences.get(imageKey, null);
		final File maybeExistsingFile = new File(fileId == null ? "" : fileId);
		
		if (fileId != null && maybeExistsingFile.exists() && maybeExistsingFile.isFile()) {
			debugPrint("Data file exist");
			return LinearStorage.open(maybeExistsingFile);
		}
		
		final Image source = this.load(imageId, levelOfDetail - 1);
		final TicToc timer = new TicToc();
		
		debugPrint("Creating data file:", new Date(timer.tic()));
		
		final int rowCount = source.getRowCount() / 2;
		final int columnCount = source.getColumnCount() / 2;
		final LinearStorage result = new LinearStorage(rowCount, columnCount, false);
		final int channelCount = zeroIfNull((Integer) source.getMetadata().get("channelCount"));
		
		result.getMetadata().put("channelCount", channelCount);
		
		for (int rowIndex = 0, sourceRowIndex = 0; rowIndex < rowCount; ++rowIndex, sourceRowIndex += 2) {
			for (int columnIndex = 0, sourceColumnIndex = 0; columnIndex < columnCount; ++columnIndex, sourceColumnIndex += 2) {
				final int value00 = source.getValue(sourceRowIndex + 0, sourceColumnIndex + 0);
				final int value01 = source.getValue(sourceRowIndex + 0, sourceColumnIndex + 1);
				final int value10 = source.getValue(sourceRowIndex + 1, sourceColumnIndex + 0);
				final int value11 = source.getValue(sourceRowIndex + 1, sourceColumnIndex + 1);
				int alpha = 0x000000FF;
				
				switch (channelCount) {
				default:
				case 1:
				{
					result.setValue(rowIndex, columnIndex, (value00 + value01 + value10 + value11) / 4);
					
					break;
				}
				case 4:
					alpha = (alpha(value00) + alpha(value01) + alpha(value10) + alpha(value11)) / 4;
				case 3:
					final int red = (red(value00) + red(value01) + red(value10) + red(value11)) / 4;
					final int green = (green(value00) + green(value01) + green(value10) + green(value11)) / 4;
					final int blue = (blue(value00) + blue(value01) + blue(value10) + blue(value11)) / 4;
					
					result.setValue(rowIndex, columnIndex, rgba(alpha, red, green, blue));
					
					break;
				}
			}
		}
		
		this.preferences.put(imageKey, result.getFile().toString());
		
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		return result;
	}
	
	public final Image load(final String imageId) {
		final String imageKey = "image:" + imageId + ":0";
		final String fileId = this.preferences.get(imageKey, null);
		final File maybeExistsingFile = new File(fileId == null ? "" : fileId);
		
		if (fileId != null && maybeExistsingFile.exists() && maybeExistsingFile.isFile()) {
			return LinearStorage.open(maybeExistsingFile);
		}

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
			final ProgressMonitor progressMonitor = new ProgressMonitor(null, "Loading " + imageId, null, 0, rowCount - 1);
			
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
								value = rgba(value, red, green, blue);
								break;
							}
							
							result.setValue(rowIndex, columnIndex, value);
						}
					}
				}
				
				progressMonitor.setProgress(y);
			}
			
			progressMonitor.close();
			
			this.preferences.put(imageKey, result.getFile().toString());
			
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
	
	public static final ImageWrangler INSTANCE = new ImageWrangler();
	
	public static final int unsigned(final byte value) {
		return value & 0x000000FF;
	}
	
	public static final int rgba(final int alpha, final int red, final int green, final int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	public static final int alpha(final int rgba) {
		return (rgba >> 24) & 0x000000FF;
	}
	
	public static final int red(final int rgba) {
		return (rgba >> 16) & 0x000000FF;
	}
	
	public static final int green(final int rgba) {
		return (rgba >> 8) & 0x000000FF;
	}
	
	public static final int blue(final int rgba) {
		return rgba & 0x000000FF;
	}
	
}
