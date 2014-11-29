package imj3.core;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Image2D extends Image {
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public default long getPixelValue(final int x, final int y) {
		return this.getPixelValue(this.getPixel(x, y));
	}
	
	public default long getPixelChannelValue(final int x, final int y, final int channelIndex) {
		return this.getPixelChannelValue(this.getPixel(x, y), channelIndex);
	}
	
	public default Image2D setPixelValue(final int x, final int y, final long value) {
		return (Image2D) this.setPixelValue(this.getPixel(x, y), value);
	}
	
	public default Image2D setPixelChannelValue(final int x, final int y, final int channelIndex, final long channelValue) {
		return (Image2D) this.setPixelChannelValue(this.getPixel(x, y), channelIndex, channelValue);
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
	
}
