package imj2.tools;

import static imj2.core.ConcreteImage2D.getX;
import static imj2.core.ConcreteImage2D.getY;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import net.sourceforge.aprog.tools.Tools;
import imj2.core.Image2D;
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
		
		this.bytesPerPixel = FormatTools.getBytesPerPixel(this.lociImage.getPixelType()) * this.lociImage.getSizeC();
		
		if (4 < this.bytesPerPixel) {
			throw new IllegalArgumentException();
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
	public final int getPixelValue(final int x, final int y) {
		this.openTileContaining(x, y);
		final int channelCount = this.getChannels().getChannelCount();
		final int bytesPerChannel = FormatTools.getBytesPerPixel(this.lociImage.getPixelType());
		final int tileChannelByteCount = this.tileWidth * this.tileHeight * bytesPerChannel;
		final int pixelFirstByteIndex = (y * this.tileWidth + x) * bytesPerChannel;
		int result = 0;
		
		for (int i = 0; i < channelCount; ++i) {
			result = (result << 8) | (this.tile[pixelFirstByteIndex + i * tileChannelByteCount] & 0x000000FF);
		}
		
		return result;
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
			
			Tools.debugPrint(tileX, tileY, this.tileWidth, this.tileHeight, this.getLociImage().getPixelType(), this.bytesPerPixel);
			
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
	
	public static final int quantize(final int value, final int quantum) {
		return (value / quantum) * quantum;
	}
	
}
