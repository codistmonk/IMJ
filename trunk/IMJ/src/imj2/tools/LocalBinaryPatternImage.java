package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.core.FilteredTiledImage2D;
import imj2.core.Image2D;

/**
 * @author codistmonk (creation 2013-08-26)
 */
public final class LocalBinaryPatternImage extends FilteredTiledImage2D {
	
	private final LocalBinaryPatternGenerator patternGenerator;
	
	public LocalBinaryPatternImage(final Image2D source) {
		super(source.getId() + ".lbp", source);
		this.patternGenerator = new LocalBinaryPatternGenerator(source);
		this.useOptimalTileDimensionsOf(source, 256, 256);
	}
	
	@Override
	public final int getLOD() {
		return this.getSource().getLOD();
	}
	
	@Override
	public final Image2D getLODImage(final int lod) {
		final int thisLOD = this.getLOD();
		
		if (lod == thisLOD) {
			return this;
		}
		
		return new LocalBinaryPatternImage(this.getSource().getLODImage(lod));
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
	protected final Image2D updateTile(final int tileX, final int tileY, final Image2D tile) {
		try {
			tile.forEachPixelInBox(tileX, tileY, tile.getWidth(), tile.getHeight(), new MonopatchProcess() {
				
				@Override
				public final void pixel(final int x, final int y) {
					tile.setPixelValue(x - tileX, y - tileY,
							LocalBinaryPatternImage.this.getPatternGenerator().getPatternAt(x, y));
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
	
	final LocalBinaryPatternGenerator getPatternGenerator() {
		return this.patternGenerator;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1685382171194456690L;
	
}
