package imj2.tools;

import static imj2.core.ConcreteImage2D.getX;
import static imj2.core.ConcreteImage2D.getY;
import static imj2.tools.IMJTools.quantize;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.core.Image2D;

import java.util.Locale;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

/**
 * @author codistmonk (creation 2013-08-07)
 */
public final class LociBackedImage implements Image2D {
	
	private final String id;
	
	private final IFormatReader lociImage;
	
	private final int bytesPerPixel;
	
	private transient byte[] tile;
	
	private transient int tileX;
	
	private transient int tileY;
	
	private transient int tileWidth;
	
	private transient int tileHeight;
	
	public LociBackedImage(final String id) {
		this.id = id;
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
	}
	
	public final IFormatReader getLociImage() {
		return this.lociImage;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final long getPixelCount() {
		return (long) this.getWidth() * this.getHeight();
	}
	
	@Override
	public final Channels getChannels() {
		return IMJTools.predefinedChannelsFor(this.getLociImage());
	}
	
	@Override
	public final int getPixelValue(final long pixelIndex) {
		return this.getPixelValue(getX(this, pixelIndex), getY(this, pixelIndex));
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		this.setPixelValue(getX(this, pixelIndex), getY(this, pixelIndex), pixelValue);
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
	public final synchronized int getPixelValue(final int x, final int y) {
		this.openTileContaining(x, y);
		final int xInTile = x % this.tileWidth;
		final int yInTile = y % this.tileHeight;
		final int channelCount = this.getChannels().getChannelCount();
		final int bytesPerChannel = FormatTools.getBytesPerPixel(this.lociImage.getPixelType());
		int result = 0;
		
		if (this.getLociImage().isIndexed()) {
			if (!this.getLociImage().isInterleaved()) {
				throw new IllegalArgumentException();
			}
			
			final int pixelFirstByteIndex = (yInTile * this.tileWidth + xInTile) * bytesPerChannel;
			
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
			final int pixelFirstByteIndex = (yInTile * this.tileWidth + xInTile) * bytesPerChannel * channelCount;
			
			for (int i = 0; i < channelCount; ++i) {
				result = (result << 8) | (this.tile[pixelFirstByteIndex + i] & 0x000000FF);
			}
		} else {
			final int tileChannelByteCount = this.tileWidth * this.tileHeight * bytesPerChannel;
			final int pixelFirstByteIndex = (yInTile * this.tileWidth + xInTile) * bytesPerChannel;
			
			for (int i = 0; i < channelCount; ++i) {
				result = (result << 8) | (this.tile[pixelFirstByteIndex + i * tileChannelByteCount] & 0x000000FF);
			}
		}
		
		// XXX Is it always ok to assume RGBA and convert to ARGB if channelCount == 4?
		return channelCount == 4 ? (result >> 8) | (result << 24) : result;
	}
	
	@Override
	public final void setPixelValue(final int x, final int y, final int value) {
		throw new UnsupportedOperationException("TODO"); // TODO
	}
	
	private final void openTileContaining(final int x, final int y) {
		this.tileWidth = this.getLociImage().getOptimalTileWidth();
		this.tileHeight = this.getLociImage().getOptimalTileHeight();
		final int tileX = quantize(x, this.tileWidth);
		final int tileY = quantize(y, this.tileHeight);
		boolean tileIsUpToDate = this.tileX == tileX && this.tileY == tileY;
		
		if (this.tile == null) {
			this.tile = new byte[this.tileWidth * this.tileHeight * this.bytesPerPixel];
			tileIsUpToDate = false;
		}
		
		if (!tileIsUpToDate) {
			this.tileX = tileX;
			this.tileY = tileY;
			
			try {
				this.getLociImage().openBytes(0, this.tile, tileX, tileY, this.tileWidth, this.tileHeight);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
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
