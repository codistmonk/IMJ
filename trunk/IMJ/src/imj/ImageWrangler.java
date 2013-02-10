package imj;

import static imj.IMJTools.alpha;
import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.IMJTools.rgba;
import static imj.IMJTools.unsigned;
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
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class ImageWrangler {
	
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
			debugPrint("Data file exists");
			return LinearStorage.open(maybeExistsingFile);
		}
		
		final Image source = this.load(imageId, levelOfDetail - 1);
		final TicToc timer = new TicToc();
		
		debugPrint("Creating data file:", new Date(timer.tic()));
		
		final int rowCount = source.getRowCount() / 2;
		final int columnCount = source.getColumnCount() / 2;
		
		if (rowCount < 1 || columnCount < 1) {
			debugPrint("Invalid LOD:", levelOfDetail);
			
			return source;
		}
		
		final LinearStorage result = new LinearStorage(rowCount, columnCount, source.getChannelCount(), false);
		final int channelCount = source.getChannelCount();
		
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
			debugPrint("Data file exists");
			return LinearStorage.open(maybeExistsingFile);
		}
		
		final IFormatReader reader = new ImageReader();
		
		try {
			reader.setId(imageId);
			
			final int rowCount = reader.getSizeY();
			final int columnCount = reader.getSizeX();
			final int channelCount = reader.getSizeC();
			debugPrint("Allocating");
			final LinearStorage result = new LinearStorage(rowCount, columnCount, channelCount, false);
			debugPrint("Allocated in file:", result.getFile());
			final int optimalTileRowCount = reader.getOptimalTileHeight();
			final int optimalTileColumnCount = reader.getOptimalTileWidth();
			final int bufferRowCount;
			final int bufferColumnCount;
			final ProgressMonitor progressMonitor = new ProgressMonitor(null, "Loading " + imageId, null, 0, rowCount - 1);
			
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
	
}
