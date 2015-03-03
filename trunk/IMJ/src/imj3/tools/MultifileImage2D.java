package imj3.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.xml.XMLTools.getNumber;
import static net.sourceforge.aprog.xml.XMLTools.getString;
import static net.sourceforge.aprog.xml.XMLTools.parse;
import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.w3c.dom.Document;

import net.sourceforge.aprog.swing.MouseHandler;
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
		final View view = new View(new MultifileSource(path));
		final MultifileImage2D image = view.getImage();
		
		Tools.debugPrint(image.getWidth(), image.getHeight());
		Tools.debugPrint(image.getPixelValue(image.getWidth() / 2, image.getHeight() / 2));
		
		SwingTools.show(view, path, false);
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	public static final class View extends JComponent {
		
		private final MultifileSource source;
		
		private MultifileImage2D image;
		
		private final Point offset;
		
		public View(final MultifileSource source) {
			this.source = source;
			this.image = new MultifileImage2D(source, 0);
			this.offset = new Point();
			
			new MouseHandler() {
				
				private final Point mouse = new Point();
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.mouse.setLocation(event.getX(), event.getY());
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					final int x = event.getX();
					final int y = event.getY();
					final int lod = View.this.getImage().getLod();
					
					View.this.getOffset().translate((this.mouse.x - x) << lod, (this.mouse.y - y) << lod);
					
					this.mouse.setLocation(x, y);
					
					View.this.repaint();
				}
				
				private static final long serialVersionUID = 8541782923536349805L;
				
			}.addTo(this);
			
			this.setFocusable(true);
			
			this.addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyTyped(final KeyEvent event) {
					switch (event.getKeyChar()) {
					case '-':
						View.this.nextLOD();
						break;
					case '+':
						View.this.previousLOD();
						break;
					}
				}
				
			});
			
			final int preferredWidth = 512;
			final int preferredHeight = preferredWidth;
			
			this.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
			
			{
				final int width0 = this.image.getWidth();
				final int height0 = this.image.getHeight();
				
				this.getOffset().translate((width0 - preferredWidth) / 2, (height0 - preferredHeight) / 2);
				
				final int targetSize = min(preferredWidth, preferredHeight);
				int size = max(width0, height0);
				int lod = 0;
				
				while (targetSize < size) {
					++lod;
					size /= 2;
				}
				
				this.setSize(this.getPreferredSize());
				this.setLOD(lod);
			}
		}
		
		public final int getLOD() {
			return this.getImage().getLod();
		}
		
		public final void nextLOD() {
			if (1 < this.getImage().getWidth() && 1 < this.getImage().getHeight()) {
				this.setLOD(this.getLOD() + 1);
			}
		}
		
		public final void previousLOD() {
			final int lod = this.getLOD();
			
			if (0 < lod) {
				this.setLOD(lod - 1);
			}
		}
		
		public final void setLOD(final int newLOD) {
			final int w = this.getWidth() / 2;
			final int h = this.getHeight() / 2;
			final int oldLOD = this.getLOD();
			final int centerX = this.getOffset().x + (w << oldLOD);
			final int centerY = this.getOffset().y + (h << oldLOD);
			this.image = new MultifileImage2D(this.getSource(), newLOD);
			this.getOffset().setLocation(centerX - (w << newLOD), centerY - (h << newLOD));
			this.repaint();
		}
		
		public final MultifileSource getSource() {
			return this.source;
		}
		
		public final MultifileImage2D getImage() {
			return this.image;
		}
		
		public final Point getOffset() {
			return this.offset;
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			final MultifileImage2D image = this.getImage();
			final int optimalTileWidth = image.getOptimalTileWidth();
			final int optimalTileHeight = image.getOptimalTileHeight();
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			final int width = this.getWidth();
			final int height = this.getHeight();
			final int lod = image.getLod();
			
			for (int yInImage = this.getOffset().y >> lod, y = -(yInImage % optimalTileHeight), tileY = yInImage / optimalTileHeight * optimalTileHeight;
					y < height && tileY < imageHeight && tileY < imageHeight; y += optimalTileHeight, yInImage += optimalTileHeight, tileY += optimalTileHeight) {
				if (0 <= tileY) {
					for (int xInImage = this.getOffset().x >> lod, x = -(xInImage % optimalTileWidth), tileX = xInImage / optimalTileWidth * optimalTileWidth;
							x < width && tileX < imageWidth; x += optimalTileHeight, xInImage += optimalTileWidth, tileX += optimalTileWidth) {
						if (0 <= tileX) {
							g.drawImage((Image) image.getTile(tileX, tileY).toAwt(), x, y, null);
						}
					}
				}
			}
		}
		
		private static final long serialVersionUID = -633791094817644395L;
		
	}
	
}
