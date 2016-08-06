package imj3.draft;

import imj3.core.Image2D;
import imj3.tools.AwtImage2D;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.ImageChangedEvent;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import multij.events.EventManager;
import multij.events.EventManager.Event.Listener;
import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2016-08-05)
 */
public final class Multiview {
	
	private Multiview() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String... commandLineArguments) {
		final Image2D defaultImage = new AwtImage2D("", 1, 1);
		final Image2DComponent component1 = new Image2DComponent(defaultImage).setDropImageEnabled(true);
		final Image2DComponent component2 = new Image2DComponent(defaultImage).setDropImageEnabled(true);
		
		new MouseHandler() {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.synchronize(event);
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				this.synchronize(event);
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.synchronize(event);
			}
			
			private final void synchronize(final AWTEvent event) {
				if (event.getSource() == component1) {
					component2.dispatchEvent(event);
				}
			}
			
			private static final long serialVersionUID = -3742345372787912928L;
			
		}.addTo(component1);
		
		final Map<Double, Map<Point, AffineTransform>> warpField = new TreeMap<>();
		final boolean useWarping = false;
		
		new MouseHandler() {
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				if (event.getSource() == component2) {
					this.updateWarpField();
				} else {
					this.applyWarpField();
				}
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.applyWarpField();
			}
			
			private final void updateWarpField() {
				if (!useWarping) {
					return;
				}
				
				final double scale = component1.getScaleX();
				final Map<Point, AffineTransform> lodWarpField = warpField.computeIfAbsent(scale, __ -> new HashMap<>());
				final Point2D center = new Point2D.Double(component1.getWidth() / 2.0, component1.getHeight() / 2.0);
				
				try {
					final AffineTransform view1 = component1.getView();
					final AffineTransform view2 = component2.getView();
					
					view1.inverseTransform(center, center);
					
					center.setLocation(center.getX() * component1.getScaleX(), center.getY() * component1.getScaleY());
					
					final int tileSide = 512;
					final int tileX = (int) (center.getX() / tileSide) * tileSide;
					final int tileY = (int) (center.getY() / tileSide) * tileSide;
					final double kx = (center.getX() - tileX) / tileSide;
					final double ky = (center.getY() - tileY) / tileSide;
					final double scaleX = component2.getScaleX();
					final double scaleY = component2.getScaleY();
					final AffineTransform topLeft = lodWarpField.computeIfAbsent(new Point(tileX, tileY), __ -> newTopLeft(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform topRight = lodWarpField.computeIfAbsent(new Point(tileX + tileSide, tileY), __ -> newTopRight(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform bottomLeft = lodWarpField.computeIfAbsent(new Point(tileX, tileY + tileSide), __ -> newBottomLeft(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform bottomRight = lodWarpField.computeIfAbsent(new Point(tileX + tileSide, tileY + tileSide), __ -> newBottomRight(view2, scaleX, scaleY, tileSide, kx, ky));
					
					Tools.debugPrint();
					Tools.debugPrint(view2);
					
					{
						final AffineTransform t = new AffineTransform(
								lerp(lerp(topLeft.getScaleX(), topRight.getScaleX(), kx), lerp(bottomLeft.getScaleX(), bottomRight.getScaleX(), kx), ky),
								lerp(lerp(topLeft.getShearY(), topRight.getShearY(), kx), lerp(bottomLeft.getShearY(), bottomRight.getShearY(), kx), ky),
								lerp(lerp(topLeft.getShearX(), topRight.getShearX(), kx), lerp(bottomLeft.getShearX(), bottomRight.getShearX(), kx), ky),
								lerp(lerp(topLeft.getScaleY(), topRight.getScaleY(), kx), lerp(bottomLeft.getScaleY(), bottomRight.getScaleY(), kx), ky),
								lerp(lerp(topLeft.getTranslateX(), topRight.getTranslateX(), kx), lerp(bottomLeft.getTranslateX(), bottomRight.getTranslateX(), kx), ky),
								lerp(lerp(topLeft.getTranslateY(), topRight.getTranslateY(), kx), lerp(bottomLeft.getTranslateY(), bottomRight.getTranslateY(), kx), ky));
						
						Tools.debugPrint(t);
						
						final double dScaleX = view2.getScaleX() - t.getScaleX();
						final double dShearY = view2.getShearY() - t.getShearY();
						final double dShearX = view2.getShearX() - t.getShearX();
						final double dScaleY = view2.getScaleY() - t.getScaleY();
						final double dTranslateX = view2.getTranslateX() - t.getTranslateX();
						final double dTranslateY = view2.getTranslateY() - t.getTranslateY();
						
						topLeft.setTransform(
								topLeft.getScaleX() + dScaleX,
								topLeft.getShearY() + dShearY,
								topLeft.getShearX() + dShearX,
								topLeft.getScaleY() + dScaleY,
								topLeft.getTranslateX() + dTranslateX,
								topLeft.getTranslateY() + dTranslateY);
						topRight.setTransform(
								topRight.getScaleX() + dScaleX,
								topRight.getShearY() + dShearY,
								topRight.getShearX() + dShearX,
								topRight.getScaleY() + dScaleY,
								topRight.getTranslateX() + dTranslateX,
								topRight.getTranslateY() + dTranslateY);
						bottomLeft.setTransform(
								bottomLeft.getScaleX() + dScaleX,
								bottomLeft.getShearY() + dShearY,
								bottomLeft.getShearX() + dShearX,
								bottomLeft.getScaleY() + dScaleY,
								bottomLeft.getTranslateX() + dTranslateX,
								bottomLeft.getTranslateY() + dTranslateY);
						bottomRight.setTransform(
								bottomRight.getScaleX() + dScaleX,
								bottomRight.getShearY() + dShearY,
								bottomRight.getShearX() + dShearX,
								bottomRight.getScaleY() + dScaleY,
								bottomRight.getTranslateX() + dTranslateX,
								bottomRight.getTranslateY() + dTranslateY);
					}
					
					{
						final AffineTransform t = new AffineTransform(
								lerp(lerp(topLeft.getScaleX(), topRight.getScaleX(), kx), lerp(bottomLeft.getScaleX(), bottomRight.getScaleX(), kx), ky),
								lerp(lerp(topLeft.getShearY(), topRight.getShearY(), kx), lerp(bottomLeft.getShearY(), bottomRight.getShearY(), kx), ky),
								lerp(lerp(topLeft.getShearX(), topRight.getShearX(), kx), lerp(bottomLeft.getShearX(), bottomRight.getShearX(), kx), ky),
								lerp(lerp(topLeft.getScaleY(), topRight.getScaleY(), kx), lerp(bottomLeft.getScaleY(), bottomRight.getScaleY(), kx), ky),
								lerp(lerp(topLeft.getTranslateX(), topRight.getTranslateX(), kx), lerp(bottomLeft.getTranslateX(), bottomRight.getTranslateX(), kx), ky),
								lerp(lerp(topLeft.getTranslateY(), topRight.getTranslateY(), kx), lerp(bottomLeft.getTranslateY(), bottomRight.getTranslateY(), kx), ky));
						
						Tools.debugPrint(t);
					}
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
			
			private final void applyWarpField() {
				if (!useWarping) {
					return;
				}
				
				final double scale = component1.getScaleX();
				final Map<Point, AffineTransform> lodWarpField = warpField.computeIfAbsent(scale, __ -> new HashMap<>());
				final Point2D center = new Point2D.Double(component2.getWidth() / 2.0, component2.getHeight() / 2.0);
				
				try {
					final AffineTransform view1 = component1.getView();
					final AffineTransform view2 = component2.getView();
					
					view1.inverseTransform(center, center);
					
					center.setLocation(center.getX() * component1.getScaleX(), center.getY() * component1.getScaleY());
					
					final int tileSide = 512;
					final int tileX = (int) (center.getX() / tileSide) * tileSide;
					final int tileY = (int) (center.getY() / tileSide) * tileSide;
					final double kx = (center.getX() - tileX) / tileSide;
					final double ky = (center.getY() - tileY) / tileSide;
					final double scaleX = component2.getScaleX();
					final double scaleY = component2.getScaleY();
					final AffineTransform topLeft = lodWarpField.getOrDefault(new Point(tileX, tileY), newTopLeft(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform topRight = lodWarpField.getOrDefault(new Point(tileX + tileSide, tileY), newTopRight(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform bottomLeft = lodWarpField.getOrDefault(new Point(tileX, tileY + tileSide), newBottomLeft(view2, scaleX, scaleY, tileSide, kx, ky));
					final AffineTransform bottomRight = lodWarpField.getOrDefault(new Point(tileX + tileSide, tileY + tileSide), newBottomRight(view2, scaleX, scaleY, tileSide, kx, ky));
					
					view2.setTransform(
							lerp(lerp(topLeft.getScaleX(), topRight.getScaleX(), kx), lerp(bottomLeft.getScaleX(), bottomRight.getScaleX(), kx), ky),
							lerp(lerp(topLeft.getShearY(), topRight.getShearY(), kx), lerp(bottomLeft.getShearY(), bottomRight.getShearY(), kx), ky),
							lerp(lerp(topLeft.getShearX(), topRight.getShearX(), kx), lerp(bottomLeft.getShearX(), bottomRight.getShearX(), kx), ky),
							lerp(lerp(topLeft.getScaleY(), topRight.getScaleY(), kx), lerp(bottomLeft.getScaleY(), bottomRight.getScaleY(), kx), ky),
							lerp(lerp(topLeft.getTranslateX(), topRight.getTranslateX(), kx), lerp(bottomLeft.getTranslateX(), bottomRight.getTranslateX(), kx), ky),
							lerp(lerp(topLeft.getTranslateY(), topRight.getTranslateY(), kx), lerp(bottomLeft.getTranslateY(), bottomRight.getTranslateY(), kx), ky));
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
			
			private static final long serialVersionUID = -3082466386304526114L;
			
		}.addTo(component2);
		
		EventManager.getInstance().addListener(component1, ImageChangedEvent.class, new Object() {
			
			@Listener
			public final void imageChanged(final ImageChangedEvent event) {
				((Frame) SwingUtilities.getWindowAncestor(event.getSource())).setTitle(new File(component1.getImage().getId()).getName() + " | " + new File(component2.getImage().getId()).getName());
				
				warpField.clear();
				
				if (component2.getImage() != defaultImage) {
					SwingUtilities.invokeLater(() -> {
						component1.setViewScale(component2.getScaleX());
					});
				}
			}
			
		});
		
		EventManager.getInstance().addListener(component2, ImageChangedEvent.class, new Object() {
			
			@Listener
			public final void imageChanged(final ImageChangedEvent event) {
				((Frame) SwingUtilities.getWindowAncestor(event.getSource())).setTitle(new File(component1.getImage().getId()).getName() + " | " + new File(component2.getImage().getId()).getName());
				
				warpField.clear();
				
				if (component1.getImage() != defaultImage) {
					SwingUtilities.invokeLater(() -> {
						component2.setViewScale(component1.getScaleX());
					});
				}
			}
			
		});
		
		SwingUtilities.invokeLater(() -> {
			final JSplitPane split = SwingTools.horizontalSplit(component1, component2);
			
			split.setPreferredSize(new Dimension(1024, 512));
			
			SwingTools.show(split, Multiview.class.getSimpleName(), false).addWindowListener(new WindowAdapter() {
				
				// XXX sftp handler prevents application from closing normally
				
				@Override
				public final void windowClosed(final WindowEvent event) {
					imj3.protocol.sftp.Handler.closeAll();
					System.exit(0);
				}
				
			});
			
			split.setDividerLocation(0.5);
		});
	}
	
	public static final double lerp(final double a, final double b, final double t) {
		return a * (1.0 - t) + b * t;
	}

	public static final AffineTransform newTopLeft(final AffineTransform view, final double scaleX, final double scaleY,
			final int tileSide, final double kx, final double ky) {
		final AffineTransform t = new AffineTransform(view);
		final double dx1 = kx * tileSide / scaleX;
		final double dy1 = ky * tileSide / scaleY;
		
		t.translate(dx1, dy1);
		
		return t;
	}
	
	public static final AffineTransform newTopRight(final AffineTransform view, final double scaleX, final double scaleY,
			final int tileSide, final double kx, final double ky) {
		final AffineTransform t = new AffineTransform(view);
		final double dx1 = -(1.0 - kx) * tileSide / scaleX;
		final double dy1 = ky * tileSide / scaleY;
		
		t.translate(dx1, dy1);
		
		return t;
	}
	
	public static final AffineTransform newBottomLeft(final AffineTransform view, final double scaleX, final double scaleY,
			final int tileSide, final double kx, final double ky) {
		final AffineTransform t = new AffineTransform(view);
		final double dx1 = kx * tileSide / scaleX;
		final double dy1 = -(1.0 - ky) * tileSide / scaleY;
		
		t.translate(dx1, dy1);
		
		return t;
	}
	
	public static final AffineTransform newBottomRight(final AffineTransform view, final double scaleX, final double scaleY,
			final int tileSide, final double kx, final double ky) {
		final AffineTransform t = new AffineTransform(view);
		final double dx1 = -(1.0 - kx) * tileSide / scaleX;
		final double dy1 = -(1.0 - ky) * tileSide / scaleY;
		
		t.translate(dx1, dy1);
		
		return t;
	}
	
}
