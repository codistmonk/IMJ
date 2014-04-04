package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.util.Arrays.fill;
import static java.util.Collections.swap;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinking2Test.Segmenter.Segment.Processor;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.WeakHashMap;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-03)
 */
public final class RegionShrinking2Test {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		final int segmentSize = 24;
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private final Point mouseLocation = new Point(-1, 0);
			
			private final PolygonalSegment referenceContour = new PolygonalSegment();
			
			private ContourState contourState = ContourState.OPEN;
			
			private final Segmenter segmenter = new GridSegmenter(segmentSize);
			
			private final boolean[][] markedSegments = { null };
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -7379133579545792116L;
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final int n = referenceContour.getVertices().size() - (contourState == ContourState.CLOSED ? 0 : 1);
					
					g.setColor(Color.GREEN);
					
					for (int i = 0; i < n; ++i) {
						final Point p1 = referenceContour.getVertices().get(i);
						final Point p2 = referenceContour.getVertices().get((i + 1) % referenceContour.getVertices().size());
						
						g.drawLine(p1.x, p1.y, p2.x, p2.y);
					}
					
					if (0 <= n && 0 <= mouseLocation.x) {
						g.setColor(Color.RED);
						
						if (contourState == ContourState.OPEN) {
//						g.drawRect(ifloor(mouseLocation.x, segmentSize), ifloor(mouseLocation.y, segmentSize), segmentSize, segmentSize);
							g.drawLine(referenceContour.getVertices().get(n).x, referenceContour.getVertices().get(n).y,
									mouseLocation.x, mouseLocation.y);
						} else if (contourState == ContourState.SNAP) {
							g.drawLine(referenceContour.getVertices().get(n).x, referenceContour.getVertices().get(n).y,
									referenceContour.getVertices().get(0).x, referenceContour.getVertices().get(0).y);
						}
					}
					
					// TODO apply region shrinking mask
					
					if (markedSegments[0] != null) {
						final BufferedImage image = imageView.getImage();
						
						for (final Segmenter.Segment segment : segmenter.getSegments(image)) {
							if (!markedSegments[0][image.getWidth() * segment.getSomeY() + segment.getSomeX()]) {
								g.setColor(Color.BLUE);
								g.drawOval(segment.getSomeX() - 2, segment.getSomeY() - 2, 4, 4);
							}
						}
					}
					
					if (contourState == ContourState.CLOSED) {
						referenceContour.process(new Processor() {

							@Override
							public final void pixel(final int x, final int y, final boolean isBorder) {
								if ((x & 3) == 0 && (y & 3) == 0) {
									imageView.getBufferImage().setRGB(x, y, Color.GREEN.getRGB());
								}
							}
							
							/**
							 * {@value}.
							 */
							private static final long serialVersionUID = -4519204375451527829L;
							
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
					if (!this.referenceContour.getVertices().isEmpty() && this.mouseLocation.distance(this.referenceContour.getVertices().get(0)) < 10.0) {
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
					this.referenceContour.getVertices().add(event.getPoint());
				} else if (this.contourState == ContourState.SNAP) {
					this.contourState = ContourState.CLOSED;
					final BufferedImage image = imageView.getImage();
					final QuantizedDenseHistogramExtractor histogramExtractor =
							new QuantizedDenseHistogramExtractor(image, 2);
					
					this.referenceContour.process(histogramExtractor);
					
					final int[] targetHistogram = histogramExtractor.takeHistogram();
					
					{
						final int w = image.getWidth();
						final int h = image.getHeight();
						
						new GridSegmenter.Segment(0, 0, w, h).process(histogramExtractor);
						
						final int[] shrinkingHistogram = histogramExtractor.takeHistogram();
						final List<Removable> shrinkingContour = new ArrayList<Removable>();
						
						for (int x = 0; x < w; x += segmentSize) {
							shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram, this.segmenter,
									x, 0, histogramExtractor));
							shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram, this.segmenter,
									x, ifloor(h - 1, segmentSize), histogramExtractor));
						}
						
						for (int y = 0; y < h; y += segmentSize) {
							shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram, this.segmenter,
									0, y, histogramExtractor));
							shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram, this.segmenter,
									ifloor(w - 1, segmentSize), y, histogramExtractor));
						}
						
						if (!shrinkingContour.isEmpty()) {
							double shrinkability = moveSmallestToFront(shrinkingContour, targetHistogram, shrinkingHistogram);
							
							debugPrint(shrinkability, w * h / segmentSize / segmentSize);
							
							final boolean[] markedSegments = new boolean[w * h];
							int remainingIterations = 300;
							
							while (shrinkability < 1.96 && 0 <= --remainingIterations) {
								shrinkability = shrink(shrinkingContour, markedSegments, image, targetHistogram, shrinkingHistogram,
										histogramExtractor, segmentSize, this.segmenter);
								debugPrint(shrinkability, remainingIterations);
							}
							
							this.markedSegments[0] = markedSegments;
						}
					}
				} else {
					this.referenceContour.getVertices().clear();
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
	
	public static final double moveSmallestToFront(final List<Removable> list,
			final int[] targetHistogram, final int[] currentHistogram) {
		double result = Double.POSITIVE_INFINITY;
		int indexOfSmallest = 0;
		final int n = list.size();
		
		for (int i = 0; i < n; ++i) {
			final Removable r = list.get(i);
			final double key = computeScore(targetHistogram, currentHistogram, r.getHistogram());
			
			if (key < result) {
				result = key;
				indexOfSmallest = i;
			}
		}
		
		swap(list, indexOfSmallest, 0);
		
		return result;
	}
	
	public static final double shrink(final List<Removable> shrinkingContour, final boolean[] markedSegments,
			final BufferedImage image, final int[] targetHistogram, final int[] shrinkingHistogram,
			final QuantizedDenseHistogramExtractor histogramExtractor, final int segmentSize, final Segmenter segmenter) {
		final Removable removed = shrinkingContour.remove(0);
		final int x = removed.getX();
		final int y = removed.getY();
		final int w = image.getWidth();
		final int h = image.getHeight();
		markedSegments[y * w + x] = true;
		
		if (segmentSize < y && !markedSegments[(y - segmentSize) * w + x]) {
			shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram,
					segmenter, x, y - segmentSize, histogramExtractor));
		}
		
		if (segmentSize < x && !markedSegments[y * w + x - segmentSize]) {
			shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram,
					segmenter, x - segmentSize, y, histogramExtractor));
		}
		
		if (x + segmentSize < w && !markedSegments[y * w + x + segmentSize]) {
			shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram,
					segmenter, x + segmentSize, y, histogramExtractor));
		}
		
		if (y + segmentSize < h && !markedSegments[(y + segmentSize) * w + x]) {
			shrinkingContour.add(newRemovable(image, targetHistogram, shrinkingHistogram,
					segmenter, x, y + segmentSize, histogramExtractor));
		}
		
		return moveSmallestToFront(shrinkingContour, targetHistogram, shrinkingHistogram);
	}
	
	public static final int count(final boolean...values) {
		int result = 0;
		
		for (final boolean value : values) {
			if (value) {
				++result;
			}
		}
		
		return result;
	}
	
	public static final int ifloor(final int value, final int step) {
		return value - (value % step);
	}
	
	public static final int iceil(final int value, final int step) {
		return (value + step - 1) / step;
	}
	
	public static final Removable newRemovable(final BufferedImage image, final int[] targetHistogram, final int[] currentHistogram,
			final Segmenter segmenter, final int x, final int y, final QuantizedDenseHistogramExtractor histogramExtractor) {
		segmenter.getSegment(image, x, y).process(histogramExtractor);
		
		return new Removable(x, y, histogramExtractor.takeHistogram());
	}
	
	public static final double computeScore(final int[] targetHistogram,
			final int[] currentHistogram, final int[] segmentHistogram) {
		double score = 0.0;
		final int n = targetHistogram.length;
		final double n1 = sum(targetHistogram);
		final double n2 = sum(currentHistogram);
		final double n3 = sum(segmentHistogram);
		
		for (int  i = 0; i < n; ++i) {
			if (currentHistogram[i] < segmentHistogram[i]) {
				score = Double.POSITIVE_INFINITY;
				break;
			}
			
			score += abs(targetHistogram[i] / n1 - (currentHistogram[i] - segmentHistogram[i]) / (n2 - n3));
		}
		
		return score;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-04)
	 */
	public static final class Removable implements Serializable {
		
		private final int x;
		
		private final int y;
		
		private final int[] histogram;
		
		public Removable(final int x, final int y, final int[] histogram) {
			this.x = x;
			this.y = y;
			this.histogram = histogram;
		}
		
		public final int getX() {
			return this.x;
		}
		
		public final int getY() {
			return this.y;
		}
		
		public final int[] getHistogram() {
			return this.histogram;
		}
		
		public final void removeFrom(final int[] histogram) {
			final int n = histogram.length;
			
			for (int i = 0; i < n; ++i) {
				histogram[i] -= this.histogram[i];
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6041578594737086733L;
		
	}
	
	public static final int sum(final int... values) {
		int result = 0;
		
		for (final int value : values) {
			result += value;
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-04)
	 * @param <T>
	 */
	public static final class GenericComparable<T> implements Serializable, Comparable<GenericComparable<T>> {
		
		private final T object;
		
		private double comparisonKey;
		
		public GenericComparable(final T object) {
			this.object = object;
		}
		
		public final T getObject() {
			return this.object;
		}
		
		public final double getComparisonKey() {
			return this.comparisonKey;
		}
		
		public final GenericComparable<T> setComparisonKey(final double comparisonKey) {
			this.comparisonKey = comparisonKey;
			
			return this;
		}
		
		@Override
		public final int compareTo(final GenericComparable<T> that) {
			return Double.compare(this.getComparisonKey(), that.getComparisonKey());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5910200560852476778L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-04-04)
	 */
	public static final class QuantizedDenseHistogramExtractor implements Processor {
		
		private final BufferedImage image;
		
		private final int q;
		
		private final int b;
		
		private final int[] histogram;
		
		private final int channelMask;
		
		public QuantizedDenseHistogramExtractor(final BufferedImage image, final int q) {
			this.image = image;
			this.q = q;
			this.b = 8 - q;
			this.histogram = new int[1 << (3 * this.b)];
			this.channelMask = 0xFF >> q;
		}
		
		public final int[] getHistogram() {
			return this.histogram;
		}
		
		public final int[] takeHistogram() {
			final int[] result = this.histogram.clone();
			
			fill(this.getHistogram(), 0);
			
			return result;
		}
		
		@Override
		public final void pixel(final int x, final int y, final boolean isBorder) {
			final int rgb = this.image.getRGB(x, y);
			final int red = (rgb >> (16 + this.q)) & this.channelMask;
			final int green = (rgb >> (8 + this.q)) & this.channelMask;
			final int blue = (rgb >> this.q) & this.channelMask;
			
			++this.histogram[(red << (2 * this.b)) | (green << this.b) | blue];
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 3704408875166069152L;
		
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
		
		public abstract List<? extends Segment> getSegments(BufferedImage image);
		
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
			try {
				return (Segment) this.getSegments(image).get(
						(y / this.segmentHeight) * iceil(image.getWidth(), this.segmentWidth) + x / this.segmentWidth);
			} catch (Exception exception) {
				debugPrint(image.getWidth(), image.getHeight(), this.segmentWidth, this.segmentHeight, x, y);
				throw unchecked(exception);
			}
		}
		
		@Override
		public final List<Segment> getSegments(final BufferedImage image) {
			return (List<Segment>) this.cache.getOrCreateSegments(image);
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
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -4739067006981388485L;
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
