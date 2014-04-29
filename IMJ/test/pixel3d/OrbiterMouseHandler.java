package pixel3d;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sin;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author codistmonk (creation 2014-03-13)
 */
public final class OrbiterMouseHandler extends MouseHandler {
	
	private final Point mouse;
	
	private final OrbiterParameters parameters;
	
	public OrbiterMouseHandler(final AtomicBoolean sharedUpdateFlag) {
		super(sharedUpdateFlag);
		this.mouse = new Point();
		this.parameters = new OrbiterParameters();
	}
	
	public final OrbiterParameters getParameters() {
		return this.parameters;
	}
	
	public final double getCenterX() {
		return this.getParameters().getCenterX();
	}
	
	public final OrbiterMouseHandler setCenterX(final double centerX) {
		this.getParameters().setCenterX(centerX);
		
		return this;
	}
	
	public final double getCenterY() {
		return this.getParameters().getCenterY();
	}
	
	public final OrbiterMouseHandler setCenterY(final double centerY) {
		this.getParameters().setCenterY(centerY);
		
		return this;
	}
	
	public final double getCenterZ() {
		return this.getParameters().getCenterZ();
	}
	
	public final OrbiterMouseHandler setCenterZ(final double centerZ) {
		this.getParameters().setCenterZ(centerZ);
		
		return this;
	}
	
	public final double getRoll() {
		return this.getParameters().getRoll();
	}
	
	public final OrbiterMouseHandler setRoll(final double roll) {
		this.getParameters().setRoll(roll);
		
		return this;
	}
	
	public final double getPitch() {
		return this.getParameters().getPitch();
	}
	
	public final OrbiterMouseHandler setPitch(final double pitch) {
		this.getParameters().setPitch(pitch);
		
		return this;
	}
	
	public final double getScale() {
		return this.getParameters().getScale();
	}
	
	public final OrbiterMouseHandler setScale(final double scale) {
		this.getParameters().setScale(scale);
		
		return this;
	}
	
	@Override
	public final void mouseDragged(final MouseEvent event) {
		if (event.isConsumed()) {
			return;
		}
		
		final int dx = event.getX() - this.mouse.x;
		final int dy = event.getY() - this.mouse.y;
		
		updateAngles(signum(dx), signum(dy));
		
		this.mouse.setLocation(event.getPoint());
	}
	
	@Override
	public final void mouseWheelMoved(final MouseWheelEvent event) {
		if (event.isConsumed()) {
			return;
		}
		
		updateScale(pow(1.25, signum(event.getWheelRotation())));
	}
	
	public final void updateAngles(final double kRoll, final double kPitch) {
		this.getParameters().setRoll(this.getRoll() + kRoll * PI / 32.0);
		this.getParameters().setPitch(this.getPitch() + kPitch * PI / 32.0);
		this.getUpdateNeeded().set(true);
	}
	
	public final void updateScale(final double kScale) {
		this.getParameters().setScale(this.getScale() * kScale);
		this.getUpdateNeeded().set(true);
	}
	
	public final void transform(final double[] locations) {
		this.transform(locations, this.getCenterX(), this.getCenterY(), this.getCenterZ());
	}
	public final void transform(final double[] locations, final double centerX, final double centerY, final double centerZ) {
		transform(locations, this.getRoll(), this.getPitch(), this.getScale(), centerX, centerY, centerZ);
	}
	
	public final void inverseTransform(final double[] locations) {
		this.inverseTransform(locations, this.getCenterX(), this.getCenterY(), this.getCenterZ());
	}
	
	public final void inverseTransform(final double[] locations, final double centerX, final double centerY, final double centerZ) {
		inverseTransform(locations, this.getRoll(), this.getPitch(), this.getScale(), centerX, centerY, centerZ);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -5856300863217835793L;
	
	public static final void transform(final double[] locations, final double roll, final double pitch, final double scale,
			final double centerX, final double centerY, final double centerZ) {
		final int n = locations.length;
		final double cosRoll = cos(roll);
		final double sinRoll = sin(roll);
		final double cosPitch = cos(pitch);
		final double sinPitch = sin(pitch);
		
		for (int i = 0; i < n; i += 3) {
			final double x1 = scale * (locations[i + 0] - centerX);
			final double y1 = scale * (locations[i + 1] - centerY);
			final double z1 = scale * (locations[i + 2] - centerZ);
			final double x2 = x1 * cosRoll - y1 * sinRoll;
			final double y2 = x1 * sinRoll + y1 * cosRoll;
			final double z2 = z1;
			locations[i + 0] = centerX + x2;
			locations[i + 1] = centerY + y2 * cosPitch - z2 * sinPitch;
			locations[i + 2] = centerZ + y2 * sinPitch + z2 * cosPitch;
		}
	}
	
	public static final void inverseTransform(final double[] locations, final double roll, final double pitch, final double scale,
			final double centerX, final double centerY, final double centerZ) {
		final int n = locations.length;
		
		for (int i = 0; i < n; i += 3) {
			final double x4 = locations[i + 0] - centerX;
			final double y4 = locations[i + 1] - centerY;
			final double z4 = locations[i + 2] - centerZ;
			final double x5 = x4;
			final double y5 = y4 * cos(-pitch) - z4 * sin(-pitch);
			final double z5 = y4 * sin(-pitch) + z4 * cos(-pitch);
			final double x6 = x5 * cos(-roll) - y5 * sin(-roll);
			final double y6 = x5 * sin(-roll) + y5 * cos(-roll);
			final double z6 = z5;
			locations[i + 0] = x6 / scale + centerX;
			locations[i + 1] = y6 / scale + centerY;
			locations[i + 2] = z6 / scale + centerZ;
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-04-29)
	 */
	public static final class OrbiterParameters implements Serializable {
		
		private double centerX;
		
		private double centerY;
		
		private double centerZ;
		
		private double roll;
		
		private double pitch;
		
		private double scale = 1.0;
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1350623906106716211L;
		
		public final double getCenterX() {
			return this.centerX;
		}
		
		public final OrbiterParameters setCenterX(final double centerX) {
			this.centerX = centerX;
			
			return this;
		}
		
		public final double getCenterY() {
			return this.centerY;
		}
		
		public final OrbiterParameters setCenterY(final double centerY) {
			this.centerY = centerY;
			
			return this;
		}
		
		public final double getCenterZ() {
			return this.centerZ;
		}
		
		public final OrbiterParameters setCenterZ(final double centerZ) {
			this.centerZ = centerZ;
			
			return this;
		}
		
		public final double getRoll() {
			return this.roll;
		}
		
		public final OrbiterParameters setRoll(final double roll) {
			this.roll = roll;
			
			return this;
		}
		
		public final double getPitch() {
			return this.pitch;
		}
		
		public final void setPitch(final double pitch) {
			this.pitch = pitch;
		}
		
		public final double getScale() {
			return this.scale;
		}
		
		public final OrbiterParameters setScale(final double scale) {
			this.scale = scale;
			
			return this;
		}
		
	}
	
}
