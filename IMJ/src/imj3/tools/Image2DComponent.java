package imj3.tools;

import static imj3.core.IMJCoreTools.quantize;
import static java.lang.Math.*;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Image2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.events.EventManager;
import net.sourceforge.aprog.events.EventManager.AbstractEvent;
import net.sourceforge.aprog.events.EventManager.Event.Listener;
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
	
	private TileOverlay tileOverlay;
	
	private Overlay overlay;
	
	private Image2D image;
	
	private final AffineTransform view;
	
	private final Collection<TileKey> activeTiles;
	
	private int statusHashCode;
	
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
		final int w = min(image.getWidth(), 800);
		final int h = min(image.getHeight(), 600);
		this.setPreferredSize(new Dimension(w, h));
		
		final double scale = image.getScale();
		this.view.scale(scale, scale);
		this.view.translate((-image.getWidth() / 2.0 + w / 2) / scale, (-image.getHeight() / 2.0 + h / 2) / scale);
		
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
				
//				update_scale:
				{
					final double n = 8.0;
					final double scale = view.getScaleX();
					final double logScale = round(n * log(scale) / log(2.0)) / n;
					final double newScale = pow(2.0, logScale + signum(event.getWheelRotation()) / n);
					
					view.setTransform(newScale, view.getShearY(), view.getShearX(), newScale, view.getTranslateX(), view.getTranslateY());
				}
				
//				center_view:
				{
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
				}
				
				Image2DComponent.this.repaint();
			}
			
			private static final long serialVersionUID = -8787564920294626502L;
			
		}.addTo(this);
	}
	
	public final void setImage(final Image2D image) {
		final Image2D oldImage = this.getImage();
		
		if (oldImage != image) {
			final AffineTransform view = this.getView();
			this.image = image.getScaledImage(view.getScaleX());
			final double newScale = image.getScale();
			this.view.setToScale(newScale, newScale);
			this.view.translate((-image.getWidth() / 2.0 + this.getWidth() / 2) / newScale,
					(-image.getHeight() / 2.0 + this.getHeight() / 2) / newScale);
			this.repaint();
			
			this.new ImageChangedEvent(oldImage).fire();
		}
	}
	
	public final Image2DComponent setDropImageEnabled(final boolean dropImageEnabled) {
		if (this.isDropImageEnabled() != dropImageEnabled) {
			this.setDropTarget(dropImageEnabled ? new DropImage(this) : null);
		}
		
		return this;
	}
	
	public final boolean isDropImageEnabled() {
		return this.getDropTarget() instanceof DropImage;
	}
	
	public final boolean isImageEnabled() {
		return this.imageEnabled;
	}
	
	public final Image2DComponent setImageEnabled(final boolean imageEnabled) {
		this.imageEnabled = imageEnabled;
		
		return this;
	}
	
	public final boolean isDragEnabled() {
		return this.dragEnabled;
	}
	
	public final Image2DComponent setDragEnabled(final boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
		
		return this;
	}
	
	public final boolean isWheelZoomEnabled() {
		return this.wheelZoomEnabled;
	}
	
	public final Image2DComponent setWheelZoomEnabled(final boolean wheelZoomEnabled) {
		this.wheelZoomEnabled = wheelZoomEnabled;
		
		return this;
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
		
		{
			final double scale = max(this.getView().getScaleX(), this.getView().getScaleY());
			final Image2D image = this.getImage();
			this.image = image.getScaledImage(scale);
			
			if (image != this.getImage()) {
				synchronized (canvasGraphics) {
					canvasGraphics.setTransform(new AffineTransform());
				}
			}
		}
		
		final int statusHashCode;
		
		synchronized (canvasGraphics) {
			// XXX not perfect but works for now
			statusHashCode = this.getView().hashCode() + canvasGraphics.hashCode() + this.getImage().hashCode() + this.getSize().hashCode()
					+ Tools.hashCode(this.getOverlay()) + Tools.hashCode(this.getTileOverlay());
			canvasGraphics.setTransform(this.getView());
		}
		
		if (!this.isImageEnabled()) {
			synchronized (canvasGraphics) {
				canvasGraphics.setTransform(IDENTITY);
				canvasGraphics.fillRect(0, 0, this.getWidth(), this.getHeight());
				canvasGraphics.setTransform(this.getView());
			}
		} else if (this.statusHashCode != statusHashCode) {
			this.statusHashCode = statusHashCode;
			final Collection<TileKey> newActiveTiles = new HashSet<>();
			final Image2D image = this.getImage();
			final double imageScale = image.getScale();
			final Point2D topLeft = new Point2D.Double();
			final Point2D topRight = new Point2D.Double(this.getWidth() - 1.0, 0.0);
			final Point2D bottomRight = new Point2D.Double(this.getWidth() - 1.0, this.getHeight() - 1.0);
			final Point2D bottomLeft = new Point2D.Double(0, this.getHeight() - 1.0);
			
			try {
				this.getView().inverseTransform(topLeft, topLeft);
				this.getView().inverseTransform(topRight, topRight);
				this.getView().inverseTransform(bottomRight, bottomRight);
				this.getView().inverseTransform(bottomLeft, bottomLeft);
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
					final TileKey tileKey = new TileKey(imageScale, new Point(tileX, tileY));
					
					newActiveTiles.add(tileKey);
					
					if (this.getActiveTiles().add(tileKey)) {
						MultiThreadTools.getExecutor().submit(new Runnable() {
							
							@Override
							public final void run() {
								drawTile(image, tileKey, imageScale, canvasGraphics);
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
	
	final Collection<TileKey> getActiveTiles() {
		return this.activeTiles;
	}
	
	final void drawTile(final Image2D image, final TileKey tileKey,
			final double imageScale, final Graphics2D canvasGraphics) {
		if (!getActiveTiles().contains(tileKey)) {
			return;
		}
		
		final Point tileXY = tileKey.getTileXY();
		final BufferedImage tile = getTile(image, tileXY.x, tileXY.y);
		final boolean clearLeft = tileXY.x == 0;
		final boolean clearTop = tileXY.y == 0;
		final boolean clearRight = image.getWidth() <= tileXY.x + image.getOptimalTileWidth();
		final boolean clearBottom = image.getHeight() <= tileXY.y + image.getOptimalTileHeight();
		final boolean clearSomething = clearLeft || clearTop || clearRight || clearBottom;
		final Rectangle region = new Rectangle((int) (tileXY.x / imageScale), (int) (tileXY.y / imageScale),
				(int) (tile.getWidth() / imageScale), (int) (tile.getHeight() / imageScale));
		
		synchronized (canvasGraphics) {
			if (clearSomething) {
				final AffineTransform transform = canvasGraphics.getTransform();
				final Point topLeft = new Point();
				final Point bottomRight = new Point(this.getWidth(), this.getHeight());
				
				try {
					transform.inverseTransform(topLeft, topLeft);
					transform.inverseTransform(bottomRight, bottomRight);
					
					final Rectangle canvasRegion = new Rectangle(
							topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
					
					canvasGraphics.setColor(Color.WHITE);
					
					if (clearLeft) {
						canvasGraphics.fillRect(canvasRegion.x, canvasRegion.y,
								region.x - canvasRegion.x, canvasRegion.height);
					}
					
					if (clearTop) {
						canvasGraphics.fillRect(canvasRegion.x, canvasRegion.y,
								canvasRegion.width, region.y - canvasRegion.y);
					}
					
					if (clearRight) {
						canvasGraphics.fillRect(region.x + region.width, canvasRegion.y,
								canvasRegion.width, canvasRegion.height);
					}
					
					if (clearBottom) {
						canvasGraphics.fillRect(canvasRegion.x, region.y + region.height,
								canvasRegion.width, canvasRegion.height);
					}
				} catch (final NoninvertibleTransformException exception) {
					exception.printStackTrace();
				}
			}
			
			canvasGraphics.drawImage(tile, region.x, region.y, region.width, region.height, null);
			
			final TileOverlay tileOverlay = this.getTileOverlay();
			
			if (tileOverlay != null) {
				tileOverlay.update(canvasGraphics, tileXY, region);
			}
		}
		
		if (getActiveTiles().remove(tileKey)) {
			repaint();
		}
	}
	
	/**
	 * @author codistmonk (creation 2015-05-01)
	 */
	public final class ImageChangedEvent extends AbstractEvent<Image2DComponent> {
		
		private final Image2D oldImage;
		
		protected ImageChangedEvent(final Image2D oldImage) {
			super(Image2DComponent.this);
			this.oldImage = oldImage;
		}
		
		public final Image2D getOldImage() {
			return this.oldImage;
		}
		
		private static final long serialVersionUID = -5641162710525216776L;
		
	}
	
	private static final long serialVersionUID = -1359039061498719576L;
	
	public static final Color CLEAR = new Color(0, true);
	
	static final AffineTransform IDENTITY = new AffineTransform();
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String path = arguments.get("file", "");
		final Image2DComponent component = new Image2DComponent(IMJTools.read(path)).setDropImageEnabled(true);
		
		EventManager.getInstance().addWeakListener(component, ImageChangedEvent.class, new Object() {
			
			@Listener
			public final void imageChanged(final ImageChangedEvent event) {
				((Frame) SwingUtilities.getWindowAncestor(component)).setTitle(component.getImage().getId());
			}
			
		});
		
		SwingTools.show(component, path, false).addWindowListener(new WindowAdapter() {
			
			// XXX sftp handler prevents application from closing normally
			
			@Override
			public final void windowClosed(final WindowEvent event) {
				imj3.protocol.sftp.Handler.closeAll();
				System.exit(0);
			}
			
		});
	}
	
	public static final BufferedImage getTile(final Image2D image, final int tileX, final int tileY) {
		try {
			// XXX potential synchronization issue here
			return (BufferedImage) image.getTileContaining(tileX, tileY).toAwt();
		} catch (final Exception exception) {
			exception.printStackTrace();
			return new BufferedImage(image.getTileWidth(tileX), image.getTileWidth(tileY), BufferedImage.TYPE_BYTE_BINARY);
		}
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
	
	/**
	 * @author codistmonk (creation 2015-03-25)
	 */
	static final class TileKey implements Serializable {
		
		private final double imageScale;
		
		private final Point tileXY;
		
		public TileKey(final double imageScale, final Point tileXY) {
			this.imageScale = imageScale;
			this.tileXY = tileXY;
		}
		
		public final double getImageScale() {
			return this.imageScale;
		}
		
		public final Point getTileXY() {
			return this.tileXY;
		}
		
		@Override
		public final int hashCode() {
			return Double.hashCode(this.getImageScale()) + this.getTileXY().hashCode();
		}
		
		@Override
		public final boolean equals(final Object object) {
			final TileKey that = cast(this.getClass(), object);
			
			return that != null && this.getImageScale() == that.getImageScale() && this.getTileXY().equals(that.getTileXY());
		}
		
		private static final long serialVersionUID = 2790290680678157622L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-05-01)
	 */
	public static final class DropImage extends DropTarget {
		
		private final Image2DComponent component;
		
		public DropImage(final Image2DComponent component) {
			this.component = component;
		}
		
		@Override
		public final synchronized void drop(final DropTargetDropEvent event) {
			final Image2D newImage = IMJTools.read(SwingTools.getFiles(event).get(0).getPath());
			
			SwingUtilities.invokeLater(() -> this.component.setImage(newImage));
		}
		
		private static final long serialVersionUID = 4728142083998773248L;
		
	}
	
}
