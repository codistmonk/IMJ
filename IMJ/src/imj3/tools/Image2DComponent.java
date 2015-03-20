package imj3.tools;

import static java.lang.Math.min;
import imj3.core.Channels;
import imj3.core.Image2D;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;

import loci.formats.IFormatReader;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-20)
 */
public final class Image2DComponent extends JComponent {
	
	private static final long serialVersionUID = -1359039061498719576L;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		Image2D image = null;
		int lod = 0;
		
		if (path.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
			image = new MultifileImage2D(new MultifileSource(path), lod);
		} else {
			try {
				image = new AwtImage2D(path);
			} catch (final Exception exception) {
				image = new BioFormatsImage2D(path);
			}
		}
		
		// TODO
	}
	
	/**
	 * @author codistmonk (creation 2015-03-20)
	 */
	public static final class BioFormatsImage2D implements Image2D {
		
		private final Map<String, Object> metadata;
		
		private final String id;
		
		private final IFormatReader reader;
		
		private final Channels channels;
		
		private final byte[] tileBuffer;
		
		private final TileHolder tileHolder;
		
		public BioFormatsImage2D(final String id) {
			this.metadata = new HashMap<>();
			this.id = id;
			this.reader = SVS2Multifile.newImageReader(id);
			this.channels = SVS2Multifile.predefinedChannelsFor(this.reader);
			this.tileBuffer = new byte[this.reader.getOptimalTileWidth() * this.reader.getOptimalTileHeight() * SVS2Multifile.getBytesPerPixel(this.reader)];
			this.tileHolder = new TileHolder();
			
			// TODO load metadata
		}
		
		public final IFormatReader getReader() {
			return this.reader;
		}
		
		@Override
		public final Map<String, Object> getMetadata() {
			return this.metadata;
		}
		
		@Override
		public final String getId() {
			return this.id;
		}
		
		@Override
		public final Channels getChannels() {
			return this.channels;
		}
		
		@Override
		public final int getWidth() {
			return this.getReader().getSizeX();
		}
		
		@Override
		public final int getHeight() {
			return this.getReader().getSizeY();
		}
		
		@Override
		public final int getOptimalTileWidth() {
			return this.getReader().getOptimalTileWidth();
		}
		
		@Override
		public final int getOptimalTileHeight() {
			return this.getReader().getOptimalTileHeight();
		}
		
		@Override
		public final Image2D getTile(final int tileX, final int tileY) {
			final int tileWidth = min(this.getWidth() - tileX, this.getOptimalTileWidth());
			final int tileHeight = min(this.getHeight() - tileY, this.getOptimalTileHeight());
			final Image2D result = new PackedImage2D(this.getTileKey(tileX, tileY), tileWidth, tileHeight, this.getChannels());
			
			try {
				this.getReader().openBytes(0, this.tileBuffer, tileX, tileY, tileWidth, tileHeight);
				
				for (int y = 0; y < tileHeight; ++y) {
					for (int x = 0; x < tileWidth; ++x) {
						result.setPixelValue(x, y, SVS2Multifile.getPixelValueFromBuffer(
								this.getReader(), this.tileBuffer, tileWidth, tileHeight, this.getChannels().getChannelCount(), x, y));
					}
				}
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
			
			return result;
		}
		
		@Override
		public final TileHolder getTileHolder() {
			return this.tileHolder;
		}
		
		private static final long serialVersionUID = 2586212892652688146L;
		
	}
	
}
