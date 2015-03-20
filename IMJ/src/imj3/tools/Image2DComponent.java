package imj3.tools;

import static imj3.tools.IMJTools.quantize;
import static java.lang.Math.max;
import static java.lang.Math.min;

import imj2.tools.MultiThreadTools;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;

import loci.formats.IFormatReader;

import net.sourceforge.aprog.swing.MouseHandler;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-20)
 */
public final class Image2DComponent extends JComponent {
	
	private final Canvas canvas;
	
	private final Image2D image;
	
	private final AffineTransform view;
	
	private final Collection<Point> activeTiles;
	
	public Image2DComponent(final Image2D image) {
		this.canvas = new Canvas();
		this.image = image;
		this.view = new AffineTransform();
		this.activeTiles = Collections.synchronizedSet(new HashSet<>());
		
		this.setPreferredSize(new Dimension(800, 600));
		
		new MouseHandler() {
			
			private final Point mouse = new Point(-1, 0);
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouse.setLocation(event.getX(), event.getY());
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				Image2DComponent.this.getView().translate(event.getX() - this.mouse.x, event.getY() - this.mouse.y);
				
				this.mousePressed(event);
				
				Image2DComponent.this.repaint();
			}
			
			private static final long serialVersionUID = -8787564920294626502L;
			
		}.addTo(this);
	}
	
	public final Image2D getImage() {
		return this.image;
	}
	
	public final AffineTransform getView() {
		return this.view;
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		final Point2D topLeft = new Point2D.Double();
		final Point2D topRight = new Point2D.Double(this.getWidth() - 1.0, 0.0);
		final Point2D bottomRight = new Point2D.Double(this.getWidth() - 1.0, this.getHeight() - 1.0);
		final Point2D bottomLeft = new Point2D.Double(0, this.getHeight() - 1.0);
		
		try {
			this.view.inverseTransform(topLeft, topLeft);
			this.view.inverseTransform(topRight, topRight);
			this.view.inverseTransform(bottomRight, bottomRight);
			this.view.inverseTransform(bottomLeft, bottomLeft);
		} catch (final NoninvertibleTransformException exception) {
			exception.printStackTrace();
		}
		
		final double top = min(min(topLeft.getY(), topRight.getY()), min(bottomLeft.getY(), bottomRight.getY()));
		final double bottom = max(max(topLeft.getY(), topRight.getY()), max(bottomLeft.getY(), bottomRight.getY()));
		final double left = min(min(topLeft.getX(), topRight.getX()), min(bottomLeft.getX(), bottomRight.getX()));
		final double right = max(max(topLeft.getX(), topRight.getX()), max(bottomLeft.getX(), bottomRight.getX()));
		final int optimalTileWidth = this.getImage().getOptimalTileWidth();
		final int optimalTileHeight = this.getImage().getOptimalTileHeight();
		final int firstTileX = max(0, quantize((int) left, optimalTileWidth));
		final int lastTileX = min(quantize((int) right, optimalTileWidth), quantize(this.getImage().getWidth() - 1, optimalTileWidth));
		final int firstTileY = max(0, quantize((int) top, optimalTileHeight));
		final int lastTileY = min(quantize((int) bottom, optimalTileHeight), quantize(this.getImage().getHeight() - 1, optimalTileHeight));
		final Graphics2D canvasGraphics = this.canvas.setFormat(this.getWidth(), this.getHeight()).getGraphics();
		
		canvasGraphics.setTransform(this.view);
		
		for (int tileY = firstTileY; tileY <= lastTileY; tileY += optimalTileHeight) {
			for (int tileX = firstTileX; tileX <= lastTileX; tileX += optimalTileWidth) {
				final Point tileXY = new Point(tileX, tileY);
				
				if (this.activeTiles.add(tileXY)) {
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							final Image tile = (Image) getImage().getTileContaining(tileXY.x, tileXY.y).toAwt();
							
							synchronized (canvasGraphics) {
								canvasGraphics.drawImage(tile, tileXY.x, tileXY.y, null);
							}
							
							repaint();
							
							activeTiles.remove(tileXY);
						}
						
					});
				}
			}
		}
		
		g.drawImage(this.canvas.getImage(), 0, 0, null);
	}
	
	private static final long serialVersionUID = -1359039061498719576L;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		Image2D image = null;
		int lod = 0;
		
		if (path.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
			image = new MultifileImage2D(new MultifileSource(path), lod);
		} else {
			try {
				image = new AwtImage2D(path);
			} catch (final Exception exception) {
				IMJTools.toneDownBioFormatsLogger();
				image = new BioFormatsImage2D(path);
			}
		}
		
		SwingTools.show(new Image2DComponent(image), path, false);
	}
	
	/**
	 * @author codistmonk (creation 2015-03-20)
	 */
	public static final class BioFormatsImage2D implements Image2D {
		
		private final Map<String, Object> metadata;
		
		private final String id;
		
		private final IFormatReader reader;
		
		private final Channels channels;
		
		private byte[] tileBuffer;
		
		private final TileHolder tileHolder;
		
		public BioFormatsImage2D(final String id) {
			this.metadata = new HashMap<>();
			this.id = id;
			this.reader = SVS2Multifile.newImageReader(id);
			this.channels = SVS2Multifile.predefinedChannelsFor(this.reader);
			this.tileHolder = new TileHolder();
			
			Tools.debugPrint(this.getWidth(), this.getHeight(), this.getChannels());
			
			// TODO load metadata
		}
		
		public final IFormatReader getReader() {
			return this.reader;
		}
		
		@Override
		public final Map<String, Object> getMetadata() {
			return this.metadata;
		}
		
		@Override
		public final String getId() {
			return this.id;
		}
		
		@Override
		public final Channels getChannels() {
			return this.channels;
		}
		
		@Override
		public final int getWidth() {
			return this.getReader().getSizeX();
		}
		
		@Override
		public final int getHeight() {
			return this.getReader().getSizeY();
		}
		
		@Override
		public final int getOptimalTileWidth() {
			return this.getReader().getOptimalTileWidth();
		}
		
		@Override
		public final int getOptimalTileHeight() {
			return this.getReader().getOptimalTileHeight();
		}
		
		private final byte[] getTileBuffer() {
			final int expectedBufferSize = this.getOptimalTileWidth() * this.getOptimalTileHeight() * SVS2Multifile.getBytesPerPixel(this.getReader());
			
			if (this.tileBuffer == null) {
				this.tileBuffer = new byte[expectedBufferSize];
			}
			
			return this.tileBuffer;
		}
		
		@Override
		public final synchronized Image2D getTile(final int tileX, final int tileY) {
			final int tileWidth = min(this.getWidth() - tileX, this.getOptimalTileWidth());
			final int tileHeight = min(this.getHeight() - tileY, this.getOptimalTileHeight());
			final Image2D result = new PackedImage2D(this.getTileKey(tileX, tileY), tileWidth, tileHeight, this.getChannels());
			
			try {
				final byte[] buffer = this.getTileBuffer();
				
				this.getReader().openBytes(0, buffer, tileX, tileY, tileWidth, tileHeight);
				
				for (int y = 0; y < tileHeight; ++y) {
					for (int x = 0; x < tileWidth; ++x) {
						final int pixelValue = SVS2Multifile.getPixelValueFromBuffer(
								this.getReader(), buffer, tileWidth, tileHeight, this.getChannels().getChannelCount(), x, y);
						result.setPixelValue(x, y, pixelValue);
					}
				}
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
			
			return result;
		}
		
		@Override
		public final TileHolder getTileHolder() {
			return this.tileHolder;
		}
		
		private static final long serialVersionUID = 2586212892652688146L;
		
	}
	
}
