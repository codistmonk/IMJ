package imj3.tools;

import static imj3.tools.IMJTools.quantize;
import static java.lang.Math.max;
import static java.lang.Math.min;

import imj2.tools.MultiThreadTools;

import imj3.core.Image2D;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
	
	private final Image2D image;
	
	private final AffineTransform view;
	
	private final Collection<Point> activeTiles;
	
	private int statusHasCode;
	
	public Image2DComponent(final Image2D image) {
		this.canvas = new Canvas();
		this.image = image;
		this.view = new AffineTransform();
		this.activeTiles = Collections.synchronizedSet(new HashSet<>());
		
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
				final AffineTransform view = Image2DComponent.this.getView();
				final Point2D translation = new Point2D.Double((event.getX() - this.mouse.x) / view.getScaleX(), (event.getY() - this.mouse.y) / view.getScaleY());
				
				view.translate(translation.getX(), translation.getY());
				
				this.mousePressed(event);
				
				Image2DComponent.this.repaint();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				final AffineTransform view = Image2DComponent.this.getView();
				final Point2D center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
				final Point2D newCenter = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
				
				try {
					view.inverseTransform(center, center);
				} catch (final NoninvertibleTransformException exception) {
					exception.printStackTrace();
				}
				
				if (event.getWheelRotation() < 0) {
					view.scale(0.5, 0.5);
				} else {
					view.scale(2.0, 2.0);
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
		final int statusHashCode;
		
		synchronized (canvasGraphics) {
			statusHashCode = this.view.hashCode() + canvasGraphics.hashCode();
			canvasGraphics.setTransform(this.view);
		}
		
		if (this.statusHasCode != statusHashCode) {
			final HashSet<Point> newActiveTiles = new HashSet<>();
			
			this.statusHasCode = statusHashCode;
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
			
			for (int tileY = firstTileY; tileY <= lastTileY; tileY += optimalTileHeight) {
				for (int tileX = firstTileX; tileX <= lastTileX; tileX += optimalTileWidth) {
					final Point tileXY = new Point(tileX, tileY);
					
					newActiveTiles.add(tileXY);
					
					if (this.getActiveTiles().add(tileXY)) {
						MultiThreadTools.getExecutor().submit(new Runnable() {
							
							@Override
							public final void run() {
								if (!getActiveTiles().contains(tileXY)) {
									return;
								}
								
								final Image tile = (Image) getImage().getTileContaining(tileXY.x, tileXY.y).toAwt();
								
								if (!getActiveTiles().contains(tileXY)) {
									return;
								}
								
								synchronized (canvasGraphics) {
									canvasGraphics.drawImage(tile, tileXY.x, tileXY.y, null);
								}
								
								if (getActiveTiles().remove(tileXY)) {
									repaint();
								}
							}
							
						});
					}
				}
			}
			
			this.getActiveTiles().retainAll(newActiveTiles);
		}
		
		synchronized (canvasGraphics) {
			g.drawImage(this.canvas.getImage(), 0, 0, null);
		}
	}
	
	final Collection<Point> getActiveTiles() {
		return this.activeTiles;
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
	
}
