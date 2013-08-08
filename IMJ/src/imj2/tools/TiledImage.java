package imj2.tools;

import static imj2.core.ConcreteImage2D.getX;
import static imj2.core.ConcreteImage2D.getY;
import static imj2.tools.IMJTools.quantize;
import static java.lang.Math.min;
import imj2.core.Image2D;

/**
 * @author codistmonk (creation 2013-08-07)
 */
public abstract class TiledImage implements Image2D {
	
	private final String id;
	
	private transient int tileX;
	
	private transient int tileY;
	
	private transient int tileWidth;
	
	private transient int tileHeight;
	
	protected TiledImage(final String id) {
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
		
		return this.getPixelValueFromTile(x % this.tileHeight, y % this.tileHeight);
	}
	
	@Override
	public final void setPixelValue(final int x, final int y, final int value) {
		throw new UnsupportedOperationException("TODO"); // TODO
	}
	
	public final void forEachPixelInRectangle(final int left, final int top, final int width, final int height, final Process process) {
		final int right = min(this.getWidth(), left + width);
		final int bottom = min(this.getHeight(), top + height);
		
		for (int y0 = top, nextTop = min(bottom, quantize(top, this.getTileHeight()) + this.getTileHeight());
				y0 < bottom; y0 = nextTop, nextTop = min(bottom, nextTop + this.getTileHeight())) {
			for (int x0 = left, nextLeft = min(right, quantize(left, this.getTileWidth()) + this.getTileWidth());
					x0 < right; x0 = nextLeft, nextLeft = min(right, nextLeft + this.getTileWidth())) {
				for (int y = y0; y < nextTop; ++y) {
					for (int x = x0; x < nextLeft; ++x) {
						process.pixel(x, y);
					}
				}
				
				process.endOfPatch();
			}
		}
	}
	
	protected final int getTileX() {
		return this.tileX;
	}
	
	protected final int getTileY() {
		return this.tileY;
	}
	
	protected final int getTileWidth() {
		return this.tileWidth;
	}
	
	protected final void setTileWidth(final int tileWidth) {
		this.tileWidth = tileWidth;
	}
	
	protected final int getTileHeight() {
		return this.tileHeight;
	}
	
	protected final void setTileHeight(final int tileHeight) {
		this.tileHeight = tileHeight;
	}
	
	protected abstract int getPixelValueFromTile(int xInTile, int yInTile);
	
	protected abstract boolean makeNewTile();
	
	protected abstract void updateTile();
	
	private final void ensureTileContains(final int x, final int y) {
		final int tileX = quantize(x, this.getTileWidth());
		final int tileY = quantize(y, this.getTileHeight());
		final boolean tileIsUpToDate = this.getTileX() == tileX && this.getTileY() == tileY && !this.makeNewTile();
		
		if (!tileIsUpToDate) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.updateTile();
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1528836461168356542L;
	
}
