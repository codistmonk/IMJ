package imj2.core;

import static imj2.core.IMJCoreTools.cache;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * @author codistmonk (creation 2013-08-26)
 */
public abstract class FilteredTiledImage2D extends TiledImage2D {
	
	private final Image2D source;
	
	private Image2D tile;
	
	protected FilteredTiledImage2D(final String id, final Image2D source) {
		super(id);
		this.source = source;
	}
	
	public final Image2D getSource() {
		return this.source;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		return this.tile.getPixelValue(xInTile, yInTile);
	}
	
	@Override
	protected void setTilePixelValue(final int x, final int y, final int xInTile, final int yInTile,
			final int value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected final void updateTile() {
		final int tileWidth = this.getTileWidth();
		final int tileHeight = this.getTileHeight();
		
		this.tile = cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY()), new Callable<Image2D>() {
			
			@Override
			public final Image2D call() throws Exception {
				return FilteredTiledImage2D.this.updateTile(FilteredTiledImage2D.this.newTile(tileWidth, tileHeight));
			}
			
		});
	}
	
	protected abstract Image2D updateTile(Image2D tile);
	
	final Image2D newTile(final int tileWidth, final int tileHeight) {
		return new ConcreteImage2D(
				new LinearIntImage("", (long) tileWidth * tileHeight, this.getChannels()), tileWidth, tileHeight);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -8955541468755019991L;
	
}
