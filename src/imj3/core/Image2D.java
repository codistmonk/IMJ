package imj3.core;

import static imj3.core.IMJCoreTools.cache;
import static imj3.core.IMJCoreTools.quantize;

import static java.lang.Math.min;

import static multij.tools.Tools.ignore;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Image2D extends Image {
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	@Override
	public default long getPixelCount() {
		return (long) this.getWidth() * this.getHeight();
	}
	
	@Override
	public default long getPixelValue(final long pixel) {
		return this.getPixelValue(this.getX(pixel), this.getY(pixel));
	}
	
	@Override
	public default Image2D setPixelValue(final long pixel, final long value) {
		return this.setPixelValue(this.getX(pixel), this.getY(pixel), value);
	}
	
	public default Image2D setPixelValue(final int x, final int y, double[] value) {
		return (Image2D) this.setPixelValue(this.getPixel(x, y), value);
	}
	
	public default double[] getPixelValue(final int x, final int y, double[] result) {
		return this.getPixelValue(this.getPixel(x, y), result);
	}
	
	public default long getPixelValue(final int x, final int y) {
		final Image2D tile = this.getTileContaining(x, y);
		
		return tile == this ? this.getPixelValue(this.getPixel(x, y))
				: tile.getPixelValue(x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight());
	}
	
	public default long getPixelChannelValue(final int x, final int y, final int channelIndex) {
		final Image2D tile = this.getTileContaining(x, y);
		
		return tile == this ? this.getPixelChannelValue(this.getPixel(x, y), channelIndex)
				: tile.getPixelChannelValue(
						x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight(), channelIndex);
	}
	
	public default Image2D setPixelValue(final int x, final int y, final long value) {
		final Image2D tile = this.getTileContaining(x, y);
		
		if (tile == this) {
			this.setPixelValue(this.getPixel(x, y), value);
		} else {
			tile.setPixelValue(x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight(), value);
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
	
	public default int getTileXContaining(final int x) {
		return quantize(x, this.getOptimalTileWidth());
	}
	
	public default int getTileYContaining(final int y) {
		return quantize(y, this.getOptimalTileHeight());
	}
	
	public default Image2D getTileContaining(final int x, final int y) {
		final int tileX = quantize(x, this.getOptimalTileWidth());
		final int tileY = quantize(y, this.getOptimalTileHeight());
		final TileHolder tileHolder = this.getTileHolder();
		
		if (tileHolder == null) {
			return this.getTile(tileX, tileY);
		}
		
		if (tileX == tileHolder.getX() && tileY == tileHolder.getY()) {
			return tileHolder.getTile();
		}
		
		final String tileKey = this.getTileKey(tileX, tileY);
		
		return tileHolder.setTile(tileX, tileY, cache(tileKey, () -> this.getTile(tileX, tileY))).getTile();
	}
	
	public default String getTileKey(final int tileX, final int tileY) {
		return this.getId() + "_x" + tileX + "_y" + tileY;
	}
	
	public default Image2D getTile(final int tileX, final int tileY) {
		ignore(tileX);
		ignore(tileY);
		
		return this;
	}
	
	public default Image2D setTile(final int tileX, final int tileY, final Image2D tile) {
		ignore(tileX);
		ignore(tileY);
		ignore(tile);
		
		return this;
	}
	
	public default int getTileWidth(final int tileX) {
		return min(this.getOptimalTileWidth(), this.getWidth() - tileX);
	}
	
	public default int getTileHeight(final int tileY) {
		return min(this.getOptimalTileHeight(), this.getHeight() - tileY);
	}
	
	public default TileHolder getTileHolder() {
		return null;
	}
	
	public default Image2D forEachPixel(final Pixel2DProcessor process) {
		final int w = this.getWidth();
		final int h = this.getHeight();
		
		loop:
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				if (!process.pixel(x, y)) {
					break loop;
				}
			}
			
		}
		
		return this;
	}
	
	public default double getScale() {
		return 1.0;
	}
	
	public default Image2D getScaledImage(final double scale) {
		return this;
	}
	
	public default Object toAwt() {
		return null;
	}
	
	/**
	 * @author codistmonk (creation 2014-12-03)
	 */
	public static abstract interface Pixel2DProcessor extends Serializable {
		
		public abstract boolean pixel(int x, int y);
		
		public default void endOfPatch() {
			// NOP
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-30-11)
	 */
	public static final class TileHolder implements Serializable {
		
		private int x = -1;
		
		private int y;
		
		private Image2D tile;
		
		public final int getX() {
			return this.x;
		}
		
		public final int getY() {
			return this.y;
		}
		
		public final Image2D getTile() {
			return this.tile;
		}
		
		public final TileHolder setTile(final int x, final int y, final Image2D tile) {
			this.x = x;
			this.y = y;
			this.tile = tile;
			
			return this;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2483266583077586699L;
		
	}
	
}
