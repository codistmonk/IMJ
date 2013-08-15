package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

/**
 * @author codistmonk (creation 2013-08-07)
 */
public final class LociBackedImage extends TiledImage {
	
	private final IFormatReader lociImage;
	
	private final int bytesPerPixel;
	
	private transient byte[] tile;
	
	public LociBackedImage(final String id) {
		super(id);
		this.lociImage = new ImageReader();
		
		try {
			this.lociImage.setId(id);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		this.bytesPerPixel = FormatTools.getBytesPerPixel(this.lociImage.getPixelType()) * this.lociImage.getRGBChannelCount();
		
		if (4 < this.bytesPerPixel) {
			throw new IllegalArgumentException();
		}
		
		if ("portable gray map".equals(this.lociImage.getFormat().toLowerCase(Locale.ENGLISH))) {
			// XXX This fixes a defect in Bio-Formats PPM loading, but is it always OK?
			this.lociImage.getCoreMetadata()[0].interleaved = true;
		}
		
		this.setOptimalTileWidth(this.lociImage.getOptimalTileWidth());
		this.setOptimalTileHeight(this.lociImage.getOptimalTileHeight());
	}
	
	public final IFormatReader getLociImage() {
		return this.lociImage;
	}
	
	@Override
	public final Channels getChannels() {
		return IMJTools.predefinedChannelsFor(this.getLociImage());
	}
	
	@Override
	public final int getWidth() {
		return this.getLociImage().getSizeX();
	}
	
	@Override
	public final int getHeight() {
		return this.getLociImage().getSizeY();
	}
	
	@Override
	public final LociBackedImage[] newParallelViews(final int n) {
		final LociBackedImage[] result = new LociBackedImage[n];
		
		result[0] = this;
		
		for (int i = 1; i < n; ++i) {
			result[i] = new LociBackedImage(this.getId());
		}
		
		return result;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		final int channelCount = this.getChannels().getChannelCount();
		final int bytesPerChannel = FormatTools.getBytesPerPixel(this.lociImage.getPixelType());
		int result = 0;
		
		if (this.getLociImage().isIndexed()) {
			if (!this.getLociImage().isInterleaved()) {
				throw new IllegalArgumentException();
			}
			
			final int pixelFirstByteIndex = (yInTile * this.getTileWidth() + xInTile) * bytesPerChannel;
			
			try {
				switch (bytesPerChannel) {
				case 1:
					return packPixelValue(this.getLociImage().get8BitLookupTable(),
							this.tile[pixelFirstByteIndex] & 0x000000FF);
				case 2:
					return packPixelValue(this.getLociImage().get16BitLookupTable(),
							((this.tile[pixelFirstByteIndex] & 0x000000FF) << 8) | (this.tile[pixelFirstByteIndex + 1] & 0x000000FF));
				default:
					throw new IllegalArgumentException();
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} else if (this.getLociImage().isInterleaved()) {
			final int pixelFirstByteIndex = (yInTile * this.getTileWidth() + xInTile) * bytesPerChannel * channelCount;
			
			for (int i = 0; i < channelCount; ++i) {
				result = (result << 8) | (this.tile[pixelFirstByteIndex + i] & 0x000000FF);
			}
		} else {
			final int tileChannelByteCount = this.getTileWidth() * this.getTileHeight() * bytesPerChannel;
			final int pixelFirstByteIndex = (yInTile * this.getTileWidth() + xInTile) * bytesPerChannel;
			
			for (int i = 0; i < channelCount; ++i) {
				result = (result << 8) | (this.tile[pixelFirstByteIndex + i * tileChannelByteCount] & 0x000000FF);
			}
		}
		
		// XXX Is it always ok to assume RGBA and convert to ARGB if channelCount == 4?
		return channelCount == 4 ? (result >> 8) | (result << 24) : result;
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	@Override
	protected final void updateTile() {
		this.tile = IMJTools.cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY()), new Callable<byte[]>() {
			
			@Override
			public final byte[] call() throws Exception {
				return LociBackedImage.this.updateTile(LociBackedImage.this.newTile());
			}
			
		});
	}
	
	final byte[] updateTile(final byte[] tile) {
		try {
			this.getLociImage().openBytes(0, tile, this.getTileX(), this.getTileY(),
					this.getTileWidth(), this.getTileHeight());
			
			return tile;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	final byte[] newTile() {
		return new byte[this.getTileWidth() * this.getTileHeight() * this.bytesPerPixel];
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2042409652657782660L;
	
	public static final int packPixelValue(final byte[][] channelTables, final int colorIndex) {
		int result = 0;
		
		for (final byte[] channelTable : channelTables) {
			result = (result << 8) | (channelTable[colorIndex] & 0x000000FF);
		}
		
		return result;
	}
	
	public static final int packPixelValue(final short[][] channelTables, final int colorIndex) {
		int result = 0;
		
		for (final short[] channelTable : channelTables) {
			result = (result << 16) | (channelTable[colorIndex] & 0x0000FFFF);
		}
		
		return result;
	}
	
}
