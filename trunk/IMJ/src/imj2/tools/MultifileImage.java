package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import imj2.core.ConcreteImage2D;
import imj2.core.Image2D;

/**
 * @author codistmonk (creation 2013-11-05)
 */
public final class MultifileImage extends TiledImage2D {
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private transient Image2D tile;
	
	public MultifileImage(final String id) {
		super(id);
		try {
			final BufferedImage tile00 = ImageIO.read(new File(id + "_0_0.jpg"));
			this.channels = IMJTools.predefinedChannelsFor(tile00);
			
			int tileY = 0;
			
			while (new File(id + "_" + (tileY + tile00.getHeight()) + "_0.jpg").canRead()) {
				tileY += tile00.getHeight();
			}
			
			int tileX = 0;
			
			while (new File(id + "_0_" + (tileX + tile00.getWidth()) + ".jpg").canRead()) {
				tileX += tile00.getWidth();
			}
			
			final BufferedImage tileMN = ImageIO.read(new File(id + "_" + tileY + "_" + tileX + ".jpg"));
			
			this.width = tileX + tileMN.getWidth();
			this.height = tileY + tileMN.getHeight();
			
			this.setOptimalTileDimensions(tile00.getWidth(), tile00.getHeight());
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final int getWidth() {
		return this.width;
	}
	
	@Override
	public final int getHeight() {
		return this.height;
	}
	
	@Override
	public final Image2D[] newParallelViews(final int n) {
		return ConcreteImage2D.newParallelViews(this, n);
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		return this.tile.getPixelValue(xInTile, yInTile);
	}
	
	@Override
	protected final void setTilePixelValue(final int x, final int y, final int xInTile, final int yInTile,
			final int value) {
		this.tile.setPixelValue(xInTile, yInTile, value);
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void updateTile() {
		final int tileX = this.getTileX();
		final int tileY = this.getTileY();
		this.tile = IMJTools.cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY()), new Callable<Image2D>() {
			
			@Override
			public final Image2D call() throws Exception {
				final String tileId = MultifileImage.this.getId() + "_" + tileY + "_" + tileX + ".jpg";
				
				return new AwtBackedImage(tileId, ImageIO.read(new File(tileId)));
			}
			
		});
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1577111610648812112L;
	
}
