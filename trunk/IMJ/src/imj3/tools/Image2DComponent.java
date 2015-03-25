package imj3.tools;

import static imj3.core.IMJCoreTools.quantize;
import static java.lang.Math.max;
import static java.lang.Math.min;

import imj2.tools.MultiThreadTools;

import imj3.core.Image2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import javax.swing.JComponent;

import net.sourceforge.aprog.swing.MouseHandler;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;

/**
 * @author codistmonk (creation 2015-03-20)
 */
public final class Image2DComponent extends JComponent {
	
	private final Canvas canvas;
	
	private TileOverlay tileOverlay;
	
	private Overlay overlay;
	
	private Image2D image;
	
	private final AffineTransform view;
	
	private final Collection<Point> activeTiles;
	
	private int statusHasCode;
	
	private boolean imageEnabled;
	
	private boolean dragEnabled;
	
	private boolean wheelZoomEnabled;
	
	public Image2DComponent(final Image2D image) {
		this.canvas = new Canvas();
		this.image = image;
		this.view = new AffineTransform();
		this.activeTiles = Collections.synchronizedSet(new HashSet<>());
		this.imageEnabled = true;
		this.dragEnabled = true;
		this.wheelZoomEnabled = true;
		
		this.setOpaque(true);
		this.setPreferredSize(new Dimension(800, 600));
		
		this.view.translate(-image.getWidth() / 2.0 + 400.0, -image.getHeight() / 2.0 + 300.0);
		
		new MouseHandler() {
			
			private final Point mouse = new Point(-1, 0);
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouse.setLocation(event.getX(), event.getY());
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				if (!isDragEnabled()) {
					return;
				}
				
				final AffineTransform view = Image2DComponent.this.getView();
				final Point2D translation = new Point2D.Double((event.getX() - this.mouse.x) / view.getScaleX(), (event.getY() - this.mouse.y) / view.getScaleY());
				
				view.translate(translation.getX(), translation.getY());
				
				this.mousePressed(event);
				
				Image2DComponent.this.repaint();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (!isWheelZoomEnabled()) {
					return;
				}
				
				final AffineTransform view = Image2DComponent.this.getView();
				final Point2D center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
				final Point2D newCenter = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
				
				try {
					view.inverseTransform(center, center);
				} catch (final NoninvertibleTransformException exception) {
					exception.printStackTrace();
				}
				
				if (event.getWheelRotation() < 0) {
					view.scale(0.8, 0.8);
				} else {
					view.scale(1.2, 1.2);
				}
				
				try {
					view.inverseTransform(newCenter, newCenter);
				} catch (final NoninvertibleTransformException exception) {
					exception.printStackTrace();
				}
				
				view.translate(-(center.getX() - newCenter.getX()), -(center.getY() - newCenter.getY()));
				
				newCenter.setLocation(getWidth() / 2.0, getHeight() / 2.0);
				
				try {
					view.inverseTransform(newCenter, newCenter);
				} catch (final NoninvertibleTransformException exception) {
					exception.printStackTrace();
				}
				
				Image2DComponent.this.repaint();
			}
			
			private static final long serialVersionUID = -8787564920294626502L;
			
		}.addTo(this);
	}
	
	public final boolean isImageEnabled() {
		return this.imageEnabled;
	}
	
	public final void setImageEnabled(final boolean imageEnabled) {
		this.imageEnabled = imageEnabled;
	}
	
	public final boolean isDragEnabled() {
		return this.dragEnabled;
	}
	
	public final void setDragEnabled(final boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
	}
	
	public final boolean isWheelZoomEnabled() {
		return this.wheelZoomEnabled;
	}
	
	public final void setWheelZoomEnabled(final boolean wheelZoomEnabled) {
		this.wheelZoomEnabled = wheelZoomEnabled;
	}
	
	public final TileOverlay getTileOverlay() {
		return this.tileOverlay;
	}
	
	public final void setTileOverlay(final TileOverlay tileOverlay) {
		this.tileOverlay = tileOverlay;
	}
	
	public final Overlay getOverlay() {
		return this.overlay;
	}
	
	public final void setOverlay(final Overlay overlay) {
		this.overlay = overlay;
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
		
		final Graphics2D canvasGraphics = this.canvas.setFormat(this.getWidth(), this.getHeight()).getGraphics();
		final double canvasScale = max(canvasGraphics.getTransform().getScaleX(), canvasGraphics.getTransform().getScaleY());
		{
			final Image2D image = this.getImage();
			this.image = image.getScaledImage(canvasScale);
			
			if (image != this.getImage()) {
				synchronized (canvasGraphics) {
					canvasGraphics.setTransform(new AffineTransform());
				}
			}
		}
		
		final int statusHashCode;
		
		synchronized (canvasGraphics) {
			// XXX not perfect but works for now
			statusHashCode = this.view.hashCode() + canvasGraphics.hashCode() + this.getImage().hashCode() + this.getSize().hashCode();
			canvasGraphics.setTransform(this.view);
		}
		
		if (!this.isImageEnabled()) {
			synchronized (canvasGraphics) {
				canvasGraphics.setTransform(new AffineTransform());
				canvasGraphics.fillRect(0, 0, this.getWidth(), this.getHeight());
				canvasGraphics.setTransform(this.view);
			}
		} else if (this.statusHasCode != statusHashCode) {
			this.statusHasCode = statusHashCode;
			final HashSet<Point> newActiveTiles = new HashSet<>();
			final Image2D image = this.getImage();
			final double imageScale = image.getScale();
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
			
			final double top = min(min(topLeft.getY(), topRight.getY()), min(bottomLeft.getY(), bottomRight.getY())) * imageScale;
			final double bottom = max(max(topLeft.getY(), topRight.getY()), max(bottomLeft.getY(), bottomRight.getY())) * imageScale;
			final double left = min(min(topLeft.getX(), topRight.getX()), min(bottomLeft.getX(), bottomRight.getX())) * imageScale;
			final double right = max(max(topLeft.getX(), topRight.getX()), max(bottomLeft.getX(), bottomRight.getX())) * imageScale;
			final int optimalTileWidth = image.getOptimalTileWidth();
			final int optimalTileHeight = image.getOptimalTileHeight();
			final int firstTileX = max(0, quantize((int) left, optimalTileWidth));
			final int lastTileX = min(quantize((int) right, optimalTileWidth), quantize(image.getWidth() - 1, optimalTileWidth));
			final int firstTileY = max(0, quantize((int) top, optimalTileHeight));
			final int lastTileY = min(quantize((int) bottom, optimalTileHeight), quantize(image.getHeight() - 1, optimalTileHeight));
			
			for (int tileY = firstTileY; tileY <= lastTileY; tileY += optimalTileHeight) {
				for (int tileX = firstTileX; tileX <= lastTileX; tileX += optimalTileWidth) {
					final Point tileXY = new Point(tileX, tileY);
					
					newActiveTiles.add(tileXY);
					
					if (this.getActiveTiles().add(tileXY)) {
						MultiThreadTools.getExecutor().submit(new Runnable() {
							
							@Override
							public final void run() {
								drawTile(image, tileXY, imageScale, canvasGraphics);
							}
							
						});
					}
				}
			}
			
			this.getActiveTiles().retainAll(newActiveTiles);
		}
		
		synchronized (canvasGraphics) {
			g.drawImage(this.canvas.getImage(), 0, 0, null);
			
			final Overlay overlay = this.getOverlay();
			
			if (overlay != null) {
				overlay.update((Graphics2D) g, this.getVisibleRect());
			}
		}
	}
	
	final Collection<Point> getActiveTiles() {
		return this.activeTiles;
	}
	
	final void drawTile(final Image2D image, final Point tileXY,
			final double imageScale, final Graphics2D canvasGraphics) {
		if (!getActiveTiles().contains(tileXY)) {
			return;
		}
		
		final BufferedImage tile = (BufferedImage) image.getTileContaining(tileXY.x, tileXY.y).toAwt();
		
		if (!getActiveTiles().contains(tileXY)) {
			return;
		}
		
		synchronized (canvasGraphics) {
			final Rectangle region = new Rectangle((int) (tileXY.x / imageScale), (int) (tileXY.y / imageScale),
					(int) (tile.getWidth() / imageScale), (int) (tile.getHeight() / imageScale));
			
			canvasGraphics.drawImage(tile, region.x, region.y, region.width, region.height, null);
			
			final TileOverlay tileOverlay = this.getTileOverlay();
			
			if (tileOverlay != null) {
				tileOverlay.update(canvasGraphics, tileXY, region);
			}
		}
		
		if (getActiveTiles().remove(tileXY)) {
			repaint();
		}
	}

	private static final long serialVersionUID = -1359039061498719576L;
	
	public static final Color CLEAR = new Color(0, true);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		
		SwingTools.show(new Image2DComponent(read(path, 0)), path, false);
	}
	
	public static Image2D read(final String path, int lod) {
		Image2D result = null;
		
		if (path.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
			result = new MultifileImage2D(new MultifileSource(path), lod);
		} else {
			try {
				result = new AwtImage2D(path);
			} catch (final Exception exception) {
				IMJTools.toneDownBioFormatsLogger();
				
				result = new BioFormatsImage2D(path);
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-22)
	 */
	public static abstract interface TileOverlay extends Serializable {
		
		public abstract void update(Graphics2D graphics, Point tileXY, Rectangle region);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-22)
	 */
	public static abstract interface Overlay extends Serializable {
		
		public abstract void update(Graphics2D graphics, Rectangle region);
		
	}
	
}
