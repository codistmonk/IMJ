package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.core.ConcreteImage2D;
import imj2.core.Image2D;
import imj2.core.LinearIntImage;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * @author codistmonk (creation 2013-08-26)
 */
public final class LocalBinaryPatternImage extends TiledImage2D {
	
	private final Image2D source;
	
	private final LocalBinaryPatternGenerator patternGenerator;
	
	private Image2D tile;
	
	public LocalBinaryPatternImage(final Image2D source) {
		super(source.getId() + ".lbp");
		this.source = source;
		this.patternGenerator = new LocalBinaryPatternGenerator(source);
		this.useOptimalTileDimensionsOf(source, 256, 256);
	}
	
	public final Image2D getSource() {
		return this.source;
	}
	
	@Override
	public final int getWidth() {
		return this.getSource().getWidth();
	}
	
	@Override
	public final int getHeight() {
		return this.getSource().getHeight();
	}
	
	@Override
	public final Channels getChannels() {
		return this.getSource().getChannels();
	}
	
	@Override
	public final LocalBinaryPatternImage[] newParallelViews(final int n) {
		final LocalBinaryPatternImage[] result = new LocalBinaryPatternImage[n];
		
		if (0 < n) {
			final Image2D[] sources = this.getSource().newParallelViews(n);
			result[0] = this;
			
			for (int i = 1; i < n; ++i) {
				result[i] = new LocalBinaryPatternImage(sources[i]);
			}
		}
		
		return result;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		return this.tile.getPixelValue(xInTile, yInTile);
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	@Override
	protected final void updateTile() {
		this.tile = IMJTools.cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY()), new Callable<Image2D>() {
			
			@Override
			public final Image2D call() throws Exception {
				return LocalBinaryPatternImage.this.updateTile(LocalBinaryPatternImage.this.newTile());
			}
			
		});
	}
	
	final Image2D updateTile(final Image2D tile) {
		try {
			final int tileX = this.getTileX();
			final int tileY = this.getTileY();
			
			tile.forEachPixelInBox(tileX, tileY, tile.getWidth(), tile.getHeight(), new MonopatchProcess() {
				
				@Override
				public final void pixel(final int x, final int y) {
					tile.setPixelValue(x - tileX, y - tileY, LocalBinaryPatternImage.this.getPattern().getPatternAt(x, y));
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 1L;
				
			});
			
			return tile;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	final Image2D newTile() {
		final int tileWidth = this.getTileWidth();
		final int tileHeight = this.getTileHeight();
		
		return new ConcreteImage2D(
				new LinearIntImage("", (long) tileWidth * tileHeight, this.getChannels()), tileWidth, tileHeight);
	}
	
	final LocalBinaryPatternGenerator getPattern() {
		return this.patternGenerator;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1685382171194456690L;
	
}
