package imj3.core;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Image2D extends Image {
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public default long getPixelValue(final int x, final int y) {
		final Image2D tile = this.getTileContaining(x, y);
		
		return tile == this ? this.getPixelValue(this.getPixel(x, y))
				: tile.getPixelValue(x % tile.getWidth(), y % tile.getHeight());
	}
	
	public default long getPixelChannelValue(final int x, final int y, final int channelIndex) {
		final Image2D tile = this.getTileContaining(x, y);
		
		return tile == this ? this.getPixelChannelValue(this.getPixel(x, y), channelIndex)
				: tile.getPixelChannelValue(
						x % tile.getOptimalTileWidth(), y % tile.getOptimalTileHeight(), channelIndex);
	}
	
	public default Image2D setPixelValue(final int x, final int y, final long value) {
		final Image2D tile = this.getTileContaining(x, y);
		
		if (tile == this) {
			this.setPixelValue(this.getPixel(x, y), value);
		} else {
			tile.setPixelValue(x % tile.getOptimalTileWidth(), y % tile.getOptimalTileHeight(), value);
		}
		
		return this;
	}
	
	public default Image2D setPixelChannelValue(final int x, final int y, final int channelIndex, final long channelValue) {
		final Image2D tile = this.getTileContaining(x, y);
		
		if (tile == this) {
			this.setPixelChannelValue(this.getPixel(x, y), channelIndex, channelValue);
		} else {
			tile.setPixelChannelValue(x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight(),
					channelIndex, channelValue);
		}
		
		return this;
	}
	
	public default long getPixel(final int x, final int y) {
		return y * this.getWidth() + x;
	}
	
	public default int getX(final long pixel) {
		return (int) (pixel % this.getWidth());
	}
	
	public default int getY(final long pixel) {
		return (int) (pixel / this.getWidth());
	}
	
	public default int getOptimalTileWidth() {
		return this.getWidth();
	}
	
	public default int getOptimalTileHeight() {
		return this.getHeight();
	}
	
	public default Image2D getTileContaining(final int x, final int y) {
		return this;
	}
	
}
