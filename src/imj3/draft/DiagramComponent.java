package imj3.draft;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.signum;
import static java.lang.Math.sin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;

import multij.swing.MouseHandler;

/**
 * @author codistmonk (creation 2015-07-23)
 */
public final class DiagramComponent extends JComponent {
	
	private final List<Consumer<Graphics2D>> renderers = new ArrayList<>();
	
	private final AffineTransform transform = new AffineTransform();
	
	{
		this.setPreferredSize(new Dimension(512, 512));
		
		new MouseHandler() {
			
			private final Point mouse = new Point();
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouse.setLocation(event.getX(), event.getY());
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				final AffineTransform transform = getTransform();
				final double scale = transform.getScaleX();
				
				transform.translate((event.getX() - this.mouse.x) / scale, (event.getY() - this.mouse.y) / scale);
				
				this.mousePressed(event);
				
				repaint();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				final AffineTransform transform = getTransform();
				final double scale = transform.getScaleX();
				final double k = round(8.0 * log(scale) / log(2.0)) + signum(event.getWheelRotation());
				final double newScale = pow(2.0, k / 8.0);
				final double x0 = getWidth() / 2.0;
				final double y0 = getHeight() / 2.0;
				
				transform.setTransform(newScale, transform.getShearY(), transform.getShearX(), newScale,
						x0 + (transform.getTranslateX() - x0) * (newScale / scale),
						y0 + (transform.getTranslateY() - y0) * (newScale / scale));
				
				repaint();
			}
			
			private static final long serialVersionUID = 4131451978542990829L;
			
		}.addTo(this);
	}
	
	public final AffineTransform getTransform() {
		return this.transform;
	}
	
	public final List<Consumer<Graphics2D>> getRenderers() {
		return this.renderers;
	}
	
	@Override
	protected final void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		
		final Graphics2D g2d = (Graphics2D) graphics;
		final AffineTransform transform = g2d.getTransform();
		
		g2d.setTransform(this.getTransform());
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		this.getRenderers().forEach(r -> r.accept(g2d));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		g2d.setTransform(transform);
	}
	
	private static final long serialVersionUID = -4168074165108242314L;
	
	public static final void fillDisk(final Graphics2D graphics, final double centerX, final double centerY, final double radius) {
		final int diameter = (int) (2.0 * radius);
		
		graphics.fillOval((int) (centerX - radius), (int) (centerY - radius), diameter, diameter);
	}
	
	public static final Point2D getCenter(final Object pointOrShape) {
		if (pointOrShape instanceof Point2D) {
			return (Point2D) pointOrShape;
		}
		
		final Rectangle bounds = ((Shape) pointOrShape).getBounds();
		
		return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
	}
	
	/**
	 * @author codistmonk (creation 2015-07-23)
	 */
	public static final class Coordinates2D implements Serializable {
		
		private final Point2D origin = new Point2D.Double();
		
		private final Point2D unitX = new Point2D.Double(1.0, 0.0);
		
		private final Point2D unitY = new Point2D.Double(0.0, 1.0);
		
		public final Point2D getOrigin() {
			return this.origin;
		}
		
		public final Point2D getUnitX() {
			return this.unitX;
		}
		
		public final Point2D getUnitY() {
			return this.unitY;
		}
		
		public final void setOnEdge(final Point2D origin, final Point2D target, final boolean normalizeX, final boolean normalizeY) {
			this.getOrigin().setLocation(origin);
			final double lengthX = normalizeX ? target.distance(origin) : 1.0;
			this.getUnitX().setLocation((target.getX() - origin.getX()) / lengthX, (target.getY() - origin.getY()) / lengthX);
			final double lengthY = normalizeY ? this.getUnitX().distance(0.0, 0.0) : 1.0;
			this.getUnitY().setLocation(-this.getUnitX().getY() / lengthY, this.getUnitX().getX() / lengthY);
		}
		
		public final void setOnTriangle(final Point2D origin, final Point2D endX, final Point2D endY,
				final boolean normalizeX, final boolean normalizeY) {
			this.getOrigin().setLocation(origin);
			final double lengthX = normalizeX ? endX.distance(origin) : 1.0;
			this.getUnitX().setLocation((endX.getX() - origin.getX()) / lengthX, (endX.getY() - origin.getY()) / lengthX);
			final double lengthY = normalizeY ? endY.distance(origin) : 1.0;
			this.getUnitY().setLocation((endY.getX() - origin.getX()) / lengthY, (endY.getY() - origin.getY()) / lengthY);
		}
		
		public final Point2D get(final double x, final double y, final Point2D result) {
			result.setLocation(
					this.getOrigin().getX() + x * this.getUnitX().getX() + y * this.getUnitY().getX(),
					this.getOrigin().getY() + x * this.getUnitX().getY() + y * this.getUnitY().getY());
			
			return result;
		}
		
		private static final long serialVersionUID = 1668653292378738299L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-07-23)
	 */
	public static final class Arrow implements Serializable, Consumer<Graphics2D> {
		
		private final Map<Object, Object> attributes;
		
		private final DiagramComponent.Coordinates2D coordinates;
		
		public Arrow() {
			this.attributes = new HashMap<>();
			this.coordinates = new Coordinates2D();
		}
		
		public final Map<Object, Object> getAttributes() {
			return this.attributes;
		}
		
		public final DiagramComponent.Arrow set(final Object key, final Object value) {
			this.getAttributes().put(key, value);
			
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public final <T> T get(final Object key) {
			return (T) this.getAttributes().get(key);
		}
		
		@Override
		public final void accept(final Graphics2D graphics) {
			final Point2D source = getCenter(this.get("source"));
			final Point2D target = getCenter(this.get("target"));
			final double curvature = this.get("curvature");
			final String label = this.get("label");
			final Color color = this.get("color");
			
			this.coordinates.setOnEdge(source, target, false, true);
			
			final Point2D control = this.coordinates.get(0.5, curvature, new Point2D.Double());
			
			graphics.setColor(color);
			graphics.draw(new QuadCurve2D.Double(source.getX(), source.getY(), control.getX(), control.getY(), target.getX(), target.getY()));
			fillArrowHead(graphics, control, target, 10.0, PI / 6.0);
			
			if (label != null) {
				graphics.setColor(color);
				final Rectangle2D labelBounds = graphics.getFontMetrics().getStringBounds(label, graphics);
				graphics.drawString(label,
						(float) (control.getX() - labelBounds.getWidth() / 2.0),
						(float) (control.getY() + labelBounds.getHeight() / 2.0));
			}
		}
		
		private static final long serialVersionUID = -1127299531484028095L;
		
		public static final void fillArrowHead(final Graphics2D graphics,
				final Point2D control, final Point2D target,
				final double tipEdge, final double tipAngle) {
			final double angle = atan2(target.getY() - control.getY(), target.getX() - control.getX());
			final int[] arrowXs = {
					(int) (target.getX() + tipEdge * cos(angle + PI + tipAngle)),
					(int) target.getX(),
					(int) (target.getX() + tipEdge * cos(angle + PI - tipAngle)) };
			final int[] arrowYs = {
					(int) (target.getY() + tipEdge * sin(angle + PI + tipAngle)),
					(int) target.getY(),
					(int) (target.getY() + tipEdge * sin(angle + PI - tipAngle)) };
			
			graphics.fillPolygon(arrowXs, arrowYs, 3);
		}
		
	}
	
}