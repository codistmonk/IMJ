package imj2.core;

import static imj2.core.ConcreteImage2D.getX;
import static imj2.core.ConcreteImage2D.getY;
import static imj2.core.IMJCoreTools.quantize;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.cast;

import imj.IntList;

/**
 * @author codistmonk (creation 2013-08-07)
 */
public abstract class TiledImage2D implements Image2D {
	
	private final String id;
	
	private int optimalTileWidth;
	
	private int optimalTileHeight;
	
	private transient int tileX;
	
	private transient int tileY;
	
	private transient int tileWidth;
	
	private transient int tileHeight;
	
	protected TiledImage2D(final String id) {
		this.id = id;
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
	public final int getPixelValue(final long pixelIndex) {
		return this.getPixelValue(getX(this, pixelIndex), getY(this, pixelIndex));
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		this.setPixelValue(getX(this, pixelIndex), getY(this, pixelIndex), pixelValue);
	}
	
	@Override
	public final synchronized int getPixelValue(final int x, final int y) {
		this.ensureTileContains(x, y);
		
		return this.getPixelValueFromTile(x, y, x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight());
	}
	
	@Override
	public final void setPixelValue(final int x, final int y, final int value) {
		this.ensureTileContains(x, y);
		
		this.setTilePixelValue(x, y, x % this.getOptimalTileWidth(), y % this.getOptimalTileHeight(), value);
	}
	
	@Override
	public final void forEachPixelInBox(final int left, final int top, final int width, final int height, final Process process) {
		final int right = min(this.getWidth(), left + width);
		final int bottom = min(this.getHeight(), top + height);
		
		for (int y0 = top, nextTop = min(bottom, quantize(top, this.getOptimalTileHeight()) + this.getOptimalTileHeight());
				y0 < bottom; y0 = nextTop, nextTop = min(bottom, nextTop + this.getOptimalTileHeight())) {
			for (int x0 = left, nextLeft = min(right, quantize(left, this.getOptimalTileWidth()) + this.getOptimalTileWidth());
					x0 < right; x0 = nextLeft, nextLeft = min(right, nextLeft + this.getOptimalTileWidth())) {
				for (int y = y0; y < nextTop; ++y) {
					for (int x = x0; x < nextLeft; ++x) {
						process.pixel(x, y);
					}
				}
				
				process.endOfPatch();
			}
		}
	}
	
	public final int getOptimalTileWidth() {
		return this.optimalTileWidth;
	}
	
	public final int getOptimalTileHeight() {
		return this.optimalTileHeight;
	}
	
	public final void loadAllTiles() {
		final IntList xys = new IntList();
		
		for (int y = 0; y < this.getHeight(); y += this.getOptimalTileHeight()) {
			for (int x = 0; x < this.getWidth(); x += this.getOptimalTileWidth()) {
				xys.add(x);
				xys.add(y);
				
				for (int i = 0; i < xys.size(); i += 2) {
					this.ensureTileContains(xys.get(i), xys.get(i + 1));
				}
			}
		}
	}
	
	protected final int getTileX() {
		return this.tileX;
	}
	
	protected final int getTileY() {
		return this.tileY;
	}
	
	protected final void setOptimalTileWidth(final int optimalTileWidth) {
		this.tileX = 0;
		
		if (0 < optimalTileWidth && optimalTileWidth != this.getOptimalTileWidth()) {
			this.optimalTileWidth = optimalTileWidth;
			this.setTileWidth();
		}
	}
	
	protected final void setOptimalTileHeight(final int optimalTileHeight) {
		this.tileY = 0;
		
		if (0 < optimalTileHeight && optimalTileHeight != this.getOptimalTileHeight()) {
			this.optimalTileHeight = optimalTileHeight;
			this.setTileHeight();
		}
	}
	
	protected final int getTileWidth() {
		return this.tileWidth;
	}
	
	protected final void setTileWidth() {
		this.tileWidth = min(this.getOptimalTileWidth(), this.getWidth() - this.getTileX());
	}
	
	protected final int getTileHeight() {
		return this.tileHeight;
	}
	
	protected final void setTileHeight() {
		this.tileHeight = min(this.getOptimalTileHeight(), this.getHeight() - this.getTileY());
	}
	
	protected final void useOptimalTileDimensionsOf(final Image2D source, final int defaultWidth, final int defaultHeight) {
		final TiledImage2D tiledSource = cast(TiledImage2D.class, source);
		
		if (tiledSource != null &&
				(long) tiledSource.getOptimalTileWidth() * tiledSource.getOptimalTileHeight() <= Integer.MAX_VALUE) {
			this.setOptimalTileDimensions(tiledSource.getOptimalTileWidth(), tiledSource.getOptimalTileHeight());
		} else {
			this.setOptimalTileDimensions(defaultWidth, defaultHeight);
		}
	}
	
	protected final void setOptimalTileDimensions(final int width, final int height) {
		this.setOptimalTileWidth(min(width, this.getWidth()));
		this.setOptimalTileHeight(min(height, this.getHeight()));
	}
	
	protected abstract int getPixelValueFromTile(int x, int y, int xInTile, int yInTile);
	
	protected abstract void setTilePixelValue(int x, int y, int xInTile, int yInTile, int value);
	
	/**
	 * @return <code>true</code> if a new tile has been generated or needs to be initialized
	 */
	protected abstract boolean makeNewTile();
	
	protected abstract void updateTile();
	
	private final void ensureTileContains(final int x, final int y) {
		final int tileX = quantize(x, this.getOptimalTileWidth());
		final int tileY = quantize(y, this.getOptimalTileHeight());
		final boolean tileIsUpToDate = this.getTileX() == tileX && this.getTileY() == tileY && !this.makeNewTile();
		
		if (!tileIsUpToDate) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.setTileWidth();
			this.setTileHeight();
			this.updateTile();
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1528836461168356542L;
	
}
