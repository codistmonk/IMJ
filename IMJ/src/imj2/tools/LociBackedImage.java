package imj2.tools;

import static imj2.core.IMJCoreTools.cache;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj2.core.Image;
import imj2.core.Image2D;
import imj2.core.SubsampledImage2D;
import imj2.core.TiledImage2D;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

/**
 * @author codistmonk (creation 2013-08-07)
 */
public final class LociBackedImage extends TiledImage2D {
	
	private final IFormatReader reader;
	
	private final int bytesPerPixel;
	
	private transient byte[] tile;
	
	public LociBackedImage(final String id) {
		super(id);
		this.reader = new ImageReader();
		
		try {
			this.reader.setId(id);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		this.bytesPerPixel = FormatTools.getBytesPerPixel(this.reader.getPixelType()) * this.reader.getRGBChannelCount();
		
		if (4 < this.bytesPerPixel) {
			throw new IllegalArgumentException();
		}
		
		if ("portable gray map".equals(this.reader.getFormat().toLowerCase(Locale.ENGLISH))) {
			// XXX This fixes a defect in Bio-Formats PPM loading, but is it always OK?
			this.reader.getCoreMetadata()[0].interleaved = true;
		}
		
		this.setSeries(0);
	}
	
	public final IFormatReader getReader() {
		return this.reader;
	}
	
	public final int getSeriesCount() {
		return this.getReader().getSeriesCount();
	}
	
	public final void setSeries(final int series) {
		this.getReader().setSeries(series);
		this.setOptimalTileWidth(this.getReader().getOptimalTileWidth());
		this.setOptimalTileHeight(this.getReader().getOptimalTileHeight());
	}
	
	@Override
	public final Image getSource() {
		return null;
	}
	
	@Override
	public final int getLOD() {
		return 0;
	}
	
	@Override
	public final Image2D getLODImage(final int lod) {
		if (lod <= 0) {
			return this;
		}
		
		return new SubsampledImage2D(this).getLODImage(lod);
	}
	
	@Override
	public final Channels getChannels() {
		return IMJTools.predefinedChannelsFor(this.getReader());
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
	public final LociBackedImage[] newParallelViews(final int n) {
		final LociBackedImage[] result = new LociBackedImage[n];
		
		result[0] = this;
		
		for (int i = 1; i < n; ++i) {
			result[i] = new LociBackedImage(this.getId());
		}
		
		return result;
	}
	
	@Override
	public final byte[] updateTile() {
		final int tileWidth = this.getTileWidth();
		final int tileHeight = this.getTileHeight();
		
		this.tile = cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY(), this.getReader().getSeries()), new Callable<byte[]>() {
			
			@Override
			public final byte[] call() throws Exception {
				return LociBackedImage.this.updateTile(LociBackedImage.this.newTile(tileWidth, tileHeight));
			}
			
		});
		
		return this.tile;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		final int channelCount = this.getChannels().getChannelCount();
		final int bytesPerChannel = FormatTools.getBytesPerPixel(this.reader.getPixelType());
		int result = 0;
		
		if (this.getReader().isIndexed()) {
			if (!this.getReader().isInterleaved()) {
				throw new IllegalArgumentException();
			}
			
			final int pixelFirstByteIndex = (yInTile * this.getTileWidth() + xInTile) * bytesPerChannel;
			
			try {
				switch (bytesPerChannel) {
				case 1:
					return packPixelValue(this.getReader().get8BitLookupTable(),
							this.tile[pixelFirstByteIndex] & 0x000000FF);
				case 2:
					return packPixelValue(this.getReader().get16BitLookupTable(),
							((this.tile[pixelFirstByteIndex] & 0x000000FF) << 8) | (this.tile[pixelFirstByteIndex + 1] & 0x000000FF));
				default:
					throw new IllegalArgumentException();
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} else if (this.getReader().isInterleaved()) {
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
	protected final void setTilePixelValue(final int x, final int y, final int xInTile, final int yInTile,
			final int value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	final byte[] updateTile(final byte[] tile) {
		try {
			this.getReader().openBytes(0, tile, this.getTileX(), this.getTileY(),
					this.getTileWidth(), this.getTileHeight());
			
			return tile;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	final byte[] newTile(final int tileWidth, final int tileHeight) {
		return new byte[tileWidth * tileHeight * this.bytesPerPixel];
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
