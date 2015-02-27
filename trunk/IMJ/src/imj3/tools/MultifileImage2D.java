package imj3.tools;

import static net.sourceforge.aprog.xml.XMLTools.getNumber;
import static net.sourceforge.aprog.xml.XMLTools.getString;
import static net.sourceforge.aprog.xml.XMLTools.parse;
import imj2.pixel3d.MouseHandler;
import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.w3c.dom.Document;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-26)
 */
public final class MultifileImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final MultifileSource source;
	
	private final int lod;
	
	private final int width;
	
	private final int height;
	
	private final int optimalTileWidth;
	
	private final int optimalTileHeight;
	
	private final String tileFormat;
	
	private final Channels channels;
	
	private final Tile tile;
	
	public MultifileImage2D(final MultifileSource source, final int lod) {
		this.metadata = new HashMap<>();
		this.source = source;
		this.lod = lod;
		
		final Document metadata = parse(source.open("metadata.xml"));
		final String imageXPath = "group/image[" + (lod + 1) + "]/";
		
		this.width = getNumber(metadata, imageXPath + "@width").intValue();
		this.height = getNumber(metadata, imageXPath + "@height").intValue();
		this.optimalTileWidth = getNumber(metadata, imageXPath + "@tileWidth").intValue();
		this.optimalTileHeight = getNumber(metadata, imageXPath + "@tileHeight").intValue();
		this.tileFormat = getString(metadata, imageXPath + "@tileFormat");
		this.tile = this.new Tile().load(0, 0);
		this.channels = this.tile.getChannels();
		
		if (lod == 0) {
			this.metadata.put("micronsPerPixel", getNumber(metadata, imageXPath + "@micronsPerPixel"));
		}
	}
	
	public final BufferedImage getAwtTile(final int tileX, final int tileY) {
		final String tileName = this.getTileName(tileX, tileY);
		
		return IMJTools.cache(this.getId() + "/" + tileName, () -> {
			try {
				return ImageIO.read(MultifileImage2D.this.getSource().open(tileName));
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		});
	}
	
	public final String getTileName(final int tileX, final int tileY) {
		return "tile_lod" + this.getLod() + "_y"+ tileY + "_x" + tileX + "." + this.getTileFormat();
	}
	
	public final String getTileFormat() {
		return this.tileFormat;
	}
	
	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public String getId() {
		return this.source.getId();
	}
	
	@Override
	public final long getPixelCount() {
		return (long) this.getWidth() * this.getHeight();
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	public final MultifileSource getSource() {
		return this.source;
	}
	
	public final int getLod() {
		return this.lod;
	}
	
	@Override
	public final int getOptimalTileWidth() {
		return this.optimalTileWidth;
	}
	
	@Override
	public final int getOptimalTileHeight() {
		return this.optimalTileHeight;
	}
	
	@Override
	public final Tile getTile(final int tileX, final int tileY) {
		return this.tile.load(tileX, tileY);
	}
	
	@Override
	public final int getWidth() {
		return this.width;
	}
	
	@Override
	public final int getHeight() {
		return this.height;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	public final class Tile implements Image2D {
		
		private final Canvas canvas = new Canvas();
		
		public final Tile load(final int tileX, final int tileY) {
			final BufferedImage awtTile = MultifileImage2D.this.getAwtTile(tileX, tileY);
			
			this.getCanvas().setFormat(awtTile.getWidth(), awtTile.getHeight(), awtTile.getType());
			this.getCanvas().getGraphics().drawImage(awtTile, 0, 0, null);
			
			return this;
		}
		
		public final Canvas getCanvas() {
			return this.canvas;
		}
		
		@Override	
		public final Map<String, Object> getMetadata() {
			return null;
		}
		
		@Override
		public final String getId() {
			return null;
		}
		
		@Override
		public final Channels getChannels() {
			return AwtImage2D.predefinedChannelsFor(this.getCanvas().getImage());
		}
		
		@Override
		public final int getWidth() {
			return this.getCanvas().getWidth();
		}
		
		@Override
		public final int getHeight() {
			return this.getCanvas().getHeight();
		}
		
		@Override
		public final long getPixelValue(final int x, final int y) {
			return this.getCanvas().getImage().getRGB(x, y);
		}
		
		@Override
		public BufferedImage toAwt() {
			return this.getCanvas().getImage();
		}
		
		private static final long serialVersionUID = 4021134779378231129L;
		
	}
	
	private static final long serialVersionUID = -4265650676493772608L;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		final MultifileImage2D image = new MultifileImage2D(new MultifileSource(path), 0);
		
		Tools.debugPrint(image.getWidth(), image.getHeight());
		Tools.debugPrint(image.getPixelValue(image.getWidth() / 2, image.getHeight() / 2));
		
		SwingTools.show(new View(image), path, false);
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	public static final class View extends JComponent {
		
		private final Image2D image;
		
		private final Point offset;
		
		public View(final Image2D image) {
			this.image = image;
			this.offset = new Point();
			
			new MouseHandler(null) {
				
				private final Point mouse = new Point();
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.mouse.setLocation(event.getX(), event.getY());
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					final int x = event.getX();
					final int y = event.getY();
					
					View.this.getOffset().translate(this.mouse.x - x, this.mouse.y - y);
					
					this.mouse.setLocation(x, y);
					
					View.this.repaint();
				}
				
				private static final long serialVersionUID = 8541782923536349805L;
				
			}.addTo(this);
			
			this.setPreferredSize(new Dimension(512, 512));
		}
		
		public final Image2D getImage() {
			return this.image;
		}
		
		public final Point getOffset() {
			return this.offset;
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			final Image2D image = this.getImage();
			final int optimalTileWidth = image.getOptimalTileWidth();
			final int optimalTileHeight = image.getOptimalTileHeight();
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			final int width = this.getWidth();
			final int height = this.getHeight();
			
			for (int yInImage = this.getOffset().y, y = -(yInImage % optimalTileHeight); y < height; y += optimalTileHeight, yInImage += optimalTileHeight) {
				final int tileY = yInImage / optimalTileHeight * optimalTileHeight;
				
				if (0 <= tileY && tileY < imageHeight) {
					for (int xInImage = this.getOffset().x, x = -(xInImage % optimalTileWidth); x < width; x += optimalTileHeight, xInImage += optimalTileWidth) {
						final int tileX = xInImage / optimalTileWidth * optimalTileWidth;
						
						if (0 <= tileX && tileX < imageWidth) {
							g.drawImage((Image) image.getTile(tileX, tileY).toAwt(), x, y, null);
						}
					}
				}
			}
		}
		
		private static final long serialVersionUID = -633791094817644395L;
		
	}
	
}
