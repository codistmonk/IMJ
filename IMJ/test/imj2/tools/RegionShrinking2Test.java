package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.*;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinking2Test.Segmenter.Segment;
import imj2.tools.RegionShrinking2Test.Segmenter.Segment.Processor;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-03)
 */
public final class RegionShrinking2Test {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		final int segmentSize = 16;
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private final Point mouseLocation = new Point(-1, 0);
			
			private final PolygonalSegment contour = new PolygonalSegment();
			
			private ContourState contourState = ContourState.OPEN;
			
			private final Segmenter segmenter = new GridSegmenter(segmentSize);
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -7379133579545792116L;
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final int n = contour.getVertices().size() - (contourState == ContourState.CLOSED ? 0 : 1);
					
					g.setColor(Color.GREEN);
					
					for (int i = 0; i < n; ++i) {
						final Point p1 = contour.getVertices().get(i);
						final Point p2 = contour.getVertices().get((i + 1) % contour.getVertices().size());
						
						g.drawLine(p1.x, p1.y, p2.x, p2.y);
					}
					
					if (0 <= n && 0 <= mouseLocation.x) {
						g.setColor(Color.RED);
						
						if (contourState == ContourState.OPEN) {
//						g.drawRect(ifloor(mouseLocation.x, segmentSize), ifloor(mouseLocation.y, segmentSize), segmentSize, segmentSize);
							g.drawLine(contour.getVertices().get(n).x, contour.getVertices().get(n).y,
									mouseLocation.x, mouseLocation.y);
						} else if (contourState == ContourState.SNAP) {
							g.drawLine(contour.getVertices().get(n).x, contour.getVertices().get(n).y,
									contour.getVertices().get(0).x, contour.getVertices().get(0).y);
						}
					}
					
					// TODO apply region shrinking mask
					
					if (contourState == ContourState.CLOSED) {
						contour.process(new Processor() {
							
							@Override
							public final void pixel(final int x, final int y, final boolean isBorder) {
								if ((x & 3) == 0 && (y & 3) == 0) {
									imageView.getBufferImage().setRGB(x, y, Color.GREEN.getRGB());
								}
							}
							
						});
					}
				}
				
			};
			
			{
				imageView.getPainters().add(this.painter);
			}
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				this.mouseLocation.x = -1;
				imageView.refreshBuffer();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				this.mouseLocation.setLocation(event.getX(), event.getY());
				
				if (this.contourState != ContourState.CLOSED) {
					if (!this.contour.getVertices().isEmpty() && this.mouseLocation.distance(this.contour.getVertices().get(0)) < 10.0) {
						this.contourState = ContourState.SNAP;
					} else {
						this.contourState = ContourState.OPEN;
					}
				}
				
				imageView.refreshBuffer();
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (this.contourState == ContourState.OPEN) {
					this.contour.getVertices().add(event.getPoint());
				} else if (this.contourState == ContourState.SNAP) {
					this.contourState = ContourState.CLOSED;
					// TODO compute histogram
					// TODO compute region shrinking mask
				} else {
					this.contour.getVertices().clear();
					this.contourState = ContourState.OPEN;
				}
				
				imageView.refreshBuffer();
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -5481346778586979337L;
			
		};
		
		show(imageView, this.getClass().getName(), true);
	}
	
	public static final int ifloor(final int value, final int step) {
		return value - (value % step);
	}
	
	public static final int iceil(final int value, final int step) {
		return (value + step - 1) / step;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-03)
	 */
	public static enum ContourState {
		
		OPEN, SNAP, CLOSED;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-03)
	 */
	public static abstract interface Segmenter extends Serializable {
		
		public abstract List<? extends Segment> segment(BufferedImage image);
		
		public abstract Segment getSegment(BufferedImage image, int x, int y);
		
		/**
		 * @author codistmonk (creation 2014-04-03)
		 */
		public static abstract interface Segment extends Serializable {
			
			public abstract int getSomeX();
			
			public abstract int getSomeY();
			
			public abstract void process(Processor processor);
			
			/**
			 * @author codistmonk (creation 2014-04-03)
			 */
			public static abstract interface Processor extends Serializable {
				
				public abstract void pixel(int x, int y, boolean isBorder);
				
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2014-04-03)
		 */
		public static final class SegmentationCache {
			
			private final Segmenter segmenter;
			
			private final Map<BufferedImage, List<? extends Segment>> segmentations;
			
			public SegmentationCache(final Segmenter segmenter) {
				this.segmenter = segmenter;
				this.segmentations = new WeakHashMap<BufferedImage, List<? extends Segment>>();
			}
			
			public final List<? extends Segment> getOrCreateSegments(final BufferedImage image) {
				List<? extends Segment> result = this.segmentations.get(image);
				
				if (result == null) {
					result = this.segmenter.segment(image);
					this.segmentations.put(image, result);
				}
				
				return result;
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-03)
	 */
	public static final class GridSegmenter implements Segmenter {
		
		private final int segmentWidth;
		
		private final int segmentHeight;
		
		private final SegmentationCache cache;
		
		public GridSegmenter(final int segmentSize) {
			this.segmentWidth = segmentSize;
			this.segmentHeight = segmentSize;
			this.cache = new SegmentationCache(this);
		}
		
		@Override
		public final List<Segment> segment(final BufferedImage image) {
			final int w = image.getWidth();
			final int h = image.getHeight();
			final int yStep = this.segmentHeight;
			final int xStep = this.segmentWidth;
			final List<Segment> result = new ArrayList<Segment>(iceil(w, xStep) * iceil(h, yStep));
			
			for (int y = 0; y < h; y += yStep) {
				for (int x = 0; x < w; x += xStep) {
					result.add(new Segment(x, y, min(w, x + xStep) - x, min(h, y + yStep) - y));
				}
			}
			
			return result;
		}
		
		@Override
		public final Segment getSegment(final BufferedImage image, final int x, final int y) {
			return (Segment) this.cache.getOrCreateSegments(image).get(
					(y / this.segmentHeight) * iceil(image.getWidth(), this.segmentWidth) + x / this.segmentWidth);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6428408696797459367L;
		
		/**
		 * @author codistmonk (creation 2014-04-03)
		 */
		public static final class Segment implements Segmenter.Segment {
			
			private final int left;
			
			private final int top;
			
			private final int width;
			
			private final int height;
			
			public Segment(final int left, final int top, final int width, final int height) {
				this.left = left;
				this.top = top;
				this.width = width;
				this.height = height;
			}
			
			@Override
			public final int getSomeX() {
				return this.left;
			}
			
			@Override
			public final int getSomeY() {
				return this.top;
			}
			
			@Override
			public final void process(final Processor processor) {
				final int yEnd = this.top + this.height;
				final int xEnd = this.left + this.width;
				
				for (int y = this.top; y < yEnd; ++y) {
					for (int x = this.left; x < xEnd; ++x) {
						processor.pixel(x, y,
								y == this.top || y + 1 == yEnd || x == this.left || x + 1 == xEnd);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 9072561776713658215L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-03)
	 */
	public static final class PolygonalSegment implements Segmenter.Segment {
		
		private final List<Point> vertices = new ArrayList<Point>();
		
		public final List<Point> getVertices() {
			return this.vertices;
		}
		
		@Override
		public final int getSomeX() {
			return this.getVertices().isEmpty() ? 0 : this.getVertices().get(0).x;
		}
		
		@Override
		public final int getSomeY() {
			return this.getVertices().isEmpty() ? 0 : this.getVertices().get(0).y;
		}
		
		@Override
		public final void process(final Processor processor) {
			final List<Point> points = new ArrayList<Point>();
			
			{
				final int n = this.getVertices().size();
				
				for (int i = 0; i < n; ++i) {
					final Point p1 = this.getVertices().get(i);
					final Point p2 = this.getVertices().get((i + 1) % n);
					final int dy = p2.y - p1.y;
					final float previousDySignum = signum(this.getVertices().get((n + i - 1) % n).y - p1.y);
					
					if (dy == 0 && 0 < orientation(this.getVertices(), i)) {
						points.add(new Point(p1.x, p1.y));
					} else if (dy != 0) {
						if (signum(dy) == previousDySignum) {
							points.add(new Point(p1.x, p1.y));
						}
						
						final int dx = p2.x - p1.x;
						final int m = abs(dy);
						
						for (int j = 0; j < m; ++j) {
							points.add(new Point(p1.x + dx * j / m, p1.y + dy * j / m));
						}
					}
				}
			}
			
			Collections.sort(points, new Comparator<Point>() {
				
				@Override
				public final int compare(final Point p1, final Point p2) {
					int result = p1.y - p2.y;
					
					if (result == 0) {
						result = p1.x - p2.x;
					}
					
					return result;
				}
				
			});
			
			{
				final int n = points.size();
				
				if ((n & 1) != 0) {
					System.err.println(debug(DEBUG_STACK_OFFSET, "Internal error detected"));
				}
				
				for (int i = 0; i + 1 < n; i += 2) {
					final Point p1 = points.get(i);
					final Point p2 = points.get(i + 1);
					final int y = p1.y;
					
					if (y == p2.y) {
						for (int x = p1.x; x < p2.x; ++x) {
							processor.pixel(x, y, false);
						}
					} else {
						System.err.println(debug(DEBUG_STACK_OFFSET, "Internal error detected"));
						break;
					}
				}
			}
		}
		
		public static final int orientation(final List<Point> vertices, final int index) {
			final int n = vertices.size();
			final Point v1 = vertices.get((n + index - 1) % n);
			final Point v2 = vertices.get((n + index + 0) % n);
			final Point v3 = vertices.get((n + index + 1) % n);
			final int v21X = v1.x - v2.x;
			final int v21Y = v1.y - v2.y;
			final int v23X = v3.x - v2.x;
			final int v23Y = v3.y - v2.y;
			
			return v21X * v23Y - v21Y * v23X;
		}
		
	}
	
}
