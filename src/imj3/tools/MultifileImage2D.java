package imj3.tools;

import static imj3.core.IMJCoreTools.cache;
import static java.lang.Math.*;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.*;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.w3c.dom.Document;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.CommandLineArgumentsParser;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-02-26)
 */
public final class MultifileImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String tilePrefix;
	
	private final MultifileSource source;
	
	private final int lod;
	
	private final int width;
	
	private final int height;
	
	private final int optimalTileWidth;
	
	private final int optimalTileHeight;
	
	private final String tileFormat;
	
	private final Channels channels;
	
	private final Map<Thread, TileHolder> tileHolders;
	
	public MultifileImage2D(final MultifileSource source, final Document metadata) {
		this(source, metadata, 0);
		
		try (final OutputStream output = source.getOutputStream("metadata.xml")) {
			XMLTools.write(metadata, output, 0);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public MultifileImage2D(final MultifileSource source, final int lod) {
		this(source, getMetadataFrom(source), lod);
	}
	
	private MultifileImage2D(final MultifileSource source, final Document metadata, final int lod) {
		this.metadata = new HashMap<>();
		this.source = source;
		this.lod = lod;
		
		final String imageXPath = "group/image[" + (lod + 1) + "]/";
		
		this.tilePrefix = getOrDefault(metadata, imageXPath + "@tilePrefix", OLD_TILE_PREFIX);
		this.width = getNumber(metadata, imageXPath + "@width").intValue();
		this.height = getNumber(metadata, imageXPath + "@height").intValue();
		this.optimalTileWidth = getNumber(metadata, imageXPath + "@tileWidth").intValue();
		this.optimalTileHeight = getNumber(metadata, imageXPath + "@tileHeight").intValue();
		this.tileFormat = getOrDefault(metadata, imageXPath + "@tileFormat", TILE_FORMAT);
		this.channels = this.getTile(0, 0).getChannels();
		this.tileHolders = new WeakHashMap<>();
		
		if (lod == 0) {
			this.metadata.put("micronsPerPixel", getNumber(metadata, imageXPath + "@micronsPerPixel"));
		}
	}
	
	public static final String getOrDefault(final Object context, final String xPath, final String defaultValue) {
		final String candidate = getString(context, xPath);
		
		return candidate == null || candidate.isEmpty() ? defaultValue : candidate;
	}
	
	public final String getTileName(final int tileX, final int tileY) {
		return this.getTilePrefix() + "lod" + this.getLod() + "_y" + tileY + "_x" + tileX + "." + this.getTileFormat();
	}
	
	@Override
	public final String getTileKey(final int tileX, final int tileY) {
		return this.getId() + "@" + this.getTileName(tileX, tileY);
	}
	
	public final String getTilePrefix() {
		return this.tilePrefix;
	}
	
	public final String getTileFormat() {
		return this.tileFormat;
	}
	
	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public final String getId() {
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
	public final double getScale() {
		return 1.0 / (1 << this.getLod());
	}
	
	@Override
	public final Image2D getScaledImage(final double scale) {
		final int newLod = max(0, (int) round(-log(scale) / log(2.0)));
		
		if (this.getLod() == newLod) {
			return this;
		}
		
		return cache(this.getId() + "_lod" + newLod, () -> new MultifileImage2D(this.getSource(), newLod));
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
	public final AwtImage2D getTile(final int tileX, final int tileY) {
		try (final InputStream input = MultifileImage2D.this.getSource().getInputStream(this.getTileName(tileX, tileY))) {
			return new AwtImage2D(this.getTileKey(tileX, tileY), ImageIO.read(input));
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		} catch (final Exception exception) {
			if (exception.getCause() instanceof FileNotFoundException) {
				return new AwtImage2D(this.getTileKey(tileX, tileY),
						new BufferedImage(this.getTileWidth(tileX), this.getTileHeight(tileY), BufferedImage.TYPE_3BYTE_BGR));
			}
			
			throw unchecked(exception);
		}
	}
	
	@Override
	public final Image2D setTile(final int tileX, final int tileY, final Image2D tile) {
		final int expectedTileWidth = this.getTileWidth(tileX);
		final int tileWidth = tile.getWidth();
		final int expectedTileHeight = this.getTileHeight(tileY);
		final int tileHeight = tile.getHeight();
		
		if (expectedTileWidth != tileWidth || expectedTileHeight != tileHeight) {
			throw new IllegalArgumentException("(" + tileWidth + "x" + tileHeight + ") != (" + expectedTileWidth + "x" + expectedTileHeight + ")");
		}
		
		try (final OutputStream output = this.getSource().getOutputStream(this.getTileName(tileX, tileY))) {
			ImageIO.write((RenderedImage) tile.toAwt(), this.getTileFormat(), output);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
		
		cache(this.getTileKey(tileX, tileY), () -> this.getTile(tileX, tileY), true);
		
		return this;
	}
	
	@Override
	public final TileHolder getTileHolder() {
		return this.tileHolders.computeIfAbsent(Thread.currentThread(), t -> new TileHolder());
	}
	
	@Override
	public final int getWidth() {
		return this.width;
	}
	
	@Override
	public final int getHeight() {
		return this.height;
	}
	
	private static final long serialVersionUID = -4265650676493772608L;
	
	public static final String TILE_PREFIX = "tiles/tile_";
	
	public static final String TILE_FORMAT = "jpg";
	
	@Deprecated
	private static final String OLD_TILE_PREFIX = "tile_";
	
	public static final Document getMetadataFrom(final MultifileSource source) {
		return cache(source.getPath("metadata.xml"), () -> {
			try (final InputStream metadataInput = source.getInputStream("metadata.xml")) {
				return parse(metadataInput);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		});
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		final View view = new View(new MultifileSource(path));
		final MultifileImage2D image = view.getImage();
		
		debugPrint(image.getWidth(), image.getHeight());
		debugPrint(image.getPixelValue(image.getWidth() / 2, image.getHeight() / 2));
		
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
							g.drawImage(image.getTile(tileX, tileY).toAwt(), x, y, null);
						}
					}
				}
			}
		}
		
		private static final long serialVersionUID = -633791094817644395L;
		
	}
	
}
