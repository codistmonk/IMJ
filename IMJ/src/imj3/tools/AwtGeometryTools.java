package imj3.tools;

import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2015-03-24)
 */
public final class AwtGeometryTools {
	
	private AwtGeometryTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final double determinant(final double a, final double b, final double c, final double d) {
		return a * d - b * c;
	}
	
	public static final float length(final float x1, final float y1, final float x2, final float y2) {
		return length(x2 - x1, y2 - y1);
	}
	
	public static final float length(final float x, final float y) {
		return (float) sqrt(square(x) + square(y));
	}
	
	public static final AwtGeometryTools.PathElement getPathElement(final PathIterator iterator, final float[] segment) {
		return newPathElement(iterator.currentSegment(segment), segment);
	}
	
	public static final AwtGeometryTools.PathElement newPathElement(final int type, final float[] segment) {
		switch (type) {
			case PathIterator.SEG_CLOSE:
				return new ClosePath();
			case PathIterator.SEG_CUBICTO:
				return new CurveTo(segment);
			case PathIterator.SEG_LINETO:
				return new LineTo(segment);
			case PathIterator.SEG_MOVETO:
				return new MoveTo(segment);
			case PathIterator.SEG_QUADTO:
				return new QuadTo(segment);
		}
		
		throw new IllegalArgumentException();
	}
	
	public static final AwtGeometryTools.PathElement newPathElement(final String type, final float[] segment) {
		switch (type) {
			case ClosePath.TYPE:
				return new ClosePath();
			case CurveTo.TYPE:
				return new CurveTo(segment);
			case LineTo.TYPE:
				return new LineTo(segment);
			case MoveTo.TYPE:
				return new MoveTo(segment);
			case QuadTo.TYPE:
				return new QuadTo(segment);
		}
		
		throw new IllegalArgumentException();
	}
	
	public static final Iterable<AwtGeometryTools.PathElement> iterable(final PathIterator iterator) {
		return new Iterable<AwtGeometryTools.PathElement>() {
			
			@Override
			public final Iterator<AwtGeometryTools.PathElement> iterator() {
				return new Iterator<AwtGeometryTools.PathElement>() {
					
					private final float[] segment = new float[6];
					
					@Override
					public final AwtGeometryTools.PathElement next() {
						final AwtGeometryTools.PathElement result = getPathElement(iterator, this.segment);
						
						if (this.hasNext()) {
							iterator.next();
						}
						
						return result;
					}
					
					@Override
					public final boolean hasNext() {
						return !iterator.isDone();
					}
					
				};
			}
			
		};
	}
	
	public static final String join(final String separator, final float... values) {
		final int n = values.length;
		final StringBuilder resultBuilder = new StringBuilder();
		
		if (0 < n) {
			resultBuilder.append(values[0]);
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(separator).append(values[i]);
			}
		}
		
		return resultBuilder.toString();
	}
	
	public static final float[] parseFloats(final String string, final String separator) {
		final String trimmed = string.trim();
		
		if (trimmed.isEmpty()) {
			return new float[0];
		}
		
		final String[] substrings = trimmed.split(separator);
		final int n = substrings.length;
		final float[] result = new float[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = Float.parseFloat(substrings[i]);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static abstract interface PathElement extends XMLSerializable {
		
		public abstract void update(Path2D path);
		
		public abstract float getEndX();
		
		public abstract float getEndY();
		
		public abstract float getLengthFrom(AwtGeometryTools.PathElement previous);
		
		public abstract float[] getParameters();
		
		public abstract String getType();
		
		@Override
		public default Element toXML(final Document document, final Map<Object, Integer> ids) {
			final Element result = XMLSerializable.super.toXML(document, ids);
			
			result.setTextContent(join(",", this.getParameters()));
			
			return result;
		}
		
		public static PathElement objectFromXML(final Element xml, final Map<Integer, Object> objects) {
			try {
				return (PathElement) Class.forName(xml.getTagName()).getConstructor(float[].class).newInstance(parseFloats(xml.getTextContent(), ","));
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static final class ClosePath implements AwtGeometryTools.PathElement {
		
		public ClosePath(final float... ignored) {
			this();
			
			ignore(ignored);
		}
		
		public ClosePath() {
			// NOP
		}
		
		@Override
		public final void update(final Path2D path) {
			path.closePath();
		}
		
		@Override
		public final float getEndX() {
			return Float.NaN;
		}
		
		@Override
		public final float getEndY() {
			return Float.NaN;
		}
		
		@Override
		public final float getLengthFrom(final AwtGeometryTools.PathElement previous) {
			return 0F;
		}
		
		@Override
		public final float[] getParameters() {
			return PARAMETERS;
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8327249273581066144L;
		
		private static final float[] PARAMETERS = {};
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "closePath";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static final class MoveTo implements AwtGeometryTools.PathElement {
		
		private final float x;
		
		private final float y;
		
		public MoveTo(final float[] segment) {
			this(segment[0], segment[1]);
		}
		
		public MoveTo(final float x, final float y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public final void update(final Path2D path) {
			path.moveTo(this.x, this.y);
		}
		
		@Override
		public final float getEndX() {
			return this.x;
		}
		
		@Override
		public final float getEndY() {
			return this.y;
		}
		
		@Override
		public final float getLengthFrom(final AwtGeometryTools.PathElement previous) {
			return 0F;
		}
		
		@Override
		public final float[] getParameters() {
			return new float[] { this.x, this.y };
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5303924812398249561L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "move";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static final class LineTo implements AwtGeometryTools.PathElement {
		
		private final float x;
		
		private final float y;
		
		public LineTo(final float[] segment) {
			this(segment[0], segment[1]);
		}
		
		public LineTo(final float x, final float y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public final void update(final Path2D path) {
			path.lineTo(this.x, this.y);
		}
		
		@Override
		public final float getEndX() {
			return this.x;
		}
		
		@Override
		public final float getEndY() {
			return this.y;
		}
		
		@Override
		public final float getLengthFrom(final AwtGeometryTools.PathElement previous) {
			return length(previous.getEndX(), previous.getEndY(), this.getEndX(), this.getEndY());
		}
		
		@Override
		public final float[] getParameters() {
			return new float[] { this.x, this.y };
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8913039760717772217L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "line";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static final class QuadTo implements AwtGeometryTools.PathElement {
		
		private final float x1;
		
		private final float y1;
		
		private final float x2;
		
		private final float y2;
		
		public QuadTo(final float[] segment) {
			this(segment[0], segment[1], segment[2], segment[3]);
		}
		
		public QuadTo(final float x1, final float y1, final float x2, final float y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		
		@Override
		public final void update(final Path2D path) {
			path.quadTo(this.x1, this.y1, this.x2, this.y2);
		}
		
		@Override
		public final float getEndX() {
			return this.x2;
		}
		
		@Override
		public final float getEndY() {
			return this.y2;
		}
		
		@Override
		public final float getLengthFrom(final AwtGeometryTools.PathElement previous) {
			return 0.7F * (length(previous.getEndX(), previous.getEndY(), this.x1, this.y1) + length(this.x1, this.y1, this.getEndX(), this.getEndY()));
		}
		
		@Override
		public final float[] getParameters() {
			return new float[] { this.x1, this.y1, this.getEndX(), this.getEndY() };
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 2580429586679770153L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "quad";
		
	}
	
	/**
	 * @author codistmonk (creation 2014-09-09)
	 */
	public static final class CurveTo implements AwtGeometryTools.PathElement {
		
		private final float x1;
		
		private final float y1;
		
		private final float x2;
		
		private final float y2;
		
		private final float x3;
		
		private final float y3;
		
		public CurveTo(final float[] segment) {
			this(segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]);
		}
		
		public CurveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.x3 = x3;
			this.y3 = y3;
		}
		
		@Override
		public final void update(final Path2D path) {
			path.curveTo(this.x1, this.y1, this.x2, this.y2, this.x3, this.y3);
		}
		
		@Override
		public final float getEndX() {
			return this.x3;
		}
		
		@Override
		public final float getEndY() {
			return this.y3;
		}
		
		@Override
		public final float getLengthFrom(final AwtGeometryTools.PathElement previous) {
			return 0.5F * (length(previous.getEndX(), previous.getEndY(), this.x1, this.y1)
					+ length(this.x1, this.y1, this.x2, this.y2)
					+ length(this.x2, this.y2, this.getEndX(), this.getEndY()));
		}
		
		@Override
		public final float[] getParameters() {
			return new float[] { this.x1, this.y1, this.x2, this.y2, this.getEndX(), this.getEndY() };
		}
		
		@Override
		public final String getType() {
			return TYPE;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 3054151236275682347L;
		
		/**
		 * {@value}.
		 */
		public static final String TYPE = "curve";
		
	}
	
}