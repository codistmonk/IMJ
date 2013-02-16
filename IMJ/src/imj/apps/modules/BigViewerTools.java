package imj.apps.modules;

import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.logging.Level;

import loci.formats.IFormatReader;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-01-08)
 */
public final class BigViewerTools {
	
	private BigViewerTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final BufferedImage readTile(final IFormatReader reader,
			final int tileX, final int tileY, final int tileWidth, final int tileHeight) {
		try {
			final byte[] bytes = reader.openBytes(0, tileX, tileY, tileWidth, tileHeight);
			final BufferedImage result = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_3BYTE_BGR);
			final int pixelCount = tileWidth * tileHeight;
			final int bytesPerPixel = bytes.length / pixelCount;
			
			for (int y = 0; y < tileHeight; ++y) {
				for (int x = 0; x < tileWidth; ++x) {
					final int pixelIndex = y * tileWidth + x;
					final int red;
					final int green;
					final int blue;
					switch (bytesPerPixel) {
					case 1:
						red = unsigned(bytes[pixelIndex + 0 * pixelCount]);
						green = red;
						blue = red;
						break;
					case 2:
						red = (unsigned(bytes[pixelIndex * 2 + 0 * pixelCount]) << 8) | unsigned(bytes[pixelIndex * 2 + 1]);
						green = red;
						blue = red;
						break;
					case 3:
					case 4:
						red = unsigned(bytes[pixelIndex + 0 * pixelCount]);
						green = unsigned(bytes[pixelIndex + 1 * pixelCount]);
						blue = unsigned(bytes[pixelIndex + 2 * pixelCount]);
						break;
					default:
						red = 0;
						green = 0;
						blue = 0;
						Tools.getLoggerForThisMethod().log(Level.SEVERE, "Unhandled bytes per pixel: " + bytesPerPixel);
						break;
					}
					
					result.setRGB(x, y, new Color(red, green, blue).getRGB());
				}
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void debugPrintException(final Throwable exception) {
		System.err.println(debug(DEBUG_STACK_OFFSET + 1, exception.getClass().getName(), exception.getMessage()));
	}
	
	public static final int unsigned(final byte value) {
		return value & 0xFF;
	}
	
}
