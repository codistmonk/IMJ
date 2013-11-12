package imj2.core;

import static imj2.core.IMJCoreTools.cache;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.List;
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
	
	@Override
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
		return this.tile == null || this.getTimestamp().get() != this.getTileTimestamp();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected final void updateTile() {
		final int tileX = this.getTileX();
		final int tileY = this.getTileY();
		final int tileWidth = this.getTileWidth();
		final int tileHeight = this.getTileHeight();
		final List<? extends Object> key = asList(this.getId(), tileX, tileY);
		final Callable<TimestampedValue<Image2D>> valueFactory = new Callable<TimestampedValue<Image2D>>() {
			
			@Override
			public final TimestampedValue<Image2D> call() throws Exception {
				return new TimestampedValue<Image2D>(
						FilteredTiledImage2D.this.getTimestamp().get(),
						FilteredTiledImage2D.this.updateTile(tileX, tileY,
								FilteredTiledImage2D.this.newTile(tileWidth, tileHeight)));
			}
			
		};
		
		// XXX Use while loop ?
		this.tile = cache(key, valueFactory,
				cache(key, valueFactory).getTimestamp() != this.getTimestamp().get()).getValue();
		this.setTileTimestamp(this.getTimestamp().get());
	}
	
	protected abstract Image2D updateTile(int tileX, int tileY, Image2D tile);
	
	final Image2D newTile(final int tileWidth, final int tileHeight) {
		return new ConcreteImage2D(
				new LinearIntImage("", (long) tileWidth * tileHeight, this.getChannels()), tileWidth, tileHeight);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -8955541468755019991L;
	
	/**
	 * @author codistmonk (creation 2013-11-10)
	 */
	public static final class TimestampedValue<V> implements Serializable {
		
		private final long timestamp;
		
		private final V value;
		
		public TimestampedValue(final long timestamp, final V value) {
			this.timestamp = timestamp;
			this.value = value;
		}
		
		public final long getTimestamp() {
			return this.timestamp;
		}
		
		public final V getValue() {
			return this.value;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7822086437658548820L;
		
	}
	
}
