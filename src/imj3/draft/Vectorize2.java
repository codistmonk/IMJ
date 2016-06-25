package imj3.draft;

import static imj2.topology.Manifold.opposite;
import static imj2.topology.Manifold.Traversor.FACE;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static multij.tools.Tools.*;

import imj2.topology.Manifold;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;
import multij.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2016-06-24)
 */
public final class Vectorize2 {
	
	private Vectorize2() {
		throw new IllegalInstantiationException();
	}
	
	private static final boolean DEBUG = false;
	
	static final Map<Integer, String> typeNames = new HashMap<>();
	
	static {
		try {
			for (final Field field : PathIterator.class.getDeclaredFields()) {
				if (field.getName().startsWith("SEG_")) {
					typeNames.put((Integer) field.get(null), field.getName());
				}
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final int smoothing = arguments.get1("smoothing", 1);
		final Collection<Integer> excludedLabels = Arrays.stream(arguments.get("exclude", new int[0])).mapToObj(x -> x).collect(Collectors.toSet());
		
		debugPrint("image:", imagePath);
		
		try {
			final BufferedImage image = ImageIO.read(new File(imagePath));
			final Structure structure = getStructure(image);
			debugPrint("points:", new HashSet<>(structure.getGeometry()).size());
			debugPrint("objects:", FACE.count(structure.getTopology()));
			final Document svg = SVGTools.newSVG(image.getWidth(), image.getHeight());
			final List<Segment> segments = collectSegments(image, structure);
			
			debugPrint(segments.size());
			
			final List<Segment> newSegments = combineSegments(segments, excludedLabels);
			
			debugPrint(newSegments.size());
			
			smoothe(newSegments, smoothing);
			
			for (int objectId = 0; objectId < newSegments.size(); ++objectId) {
				final Segment segment = newSegments.get(objectId);
				
				SVGTools.addTo(svg, segment.getGeometry(), segment.getLabel(), "TODO", "" + objectId);
			}
			
			XMLTools.write(svg, new File(baseName(imagePath) + ".svg"), 1);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final void smoothe(final List<Segment> segments, final int smoothing) {
		if (0 < smoothing) {
			final double[] xy = new double[2];
			
			for (final Segment segment : segments) {
				Arrays.fill(xy, 0F);
				
				final List<Point2D> vertices = new ArrayList<>();
				final List<Point2D> newVertices = new ArrayList<>();
				final Path2D newShape = new Path2D.Double();
				
				for (final PathIterator i = segment.getGeometry().getPathIterator(new AffineTransform(), 0.0); !i.isDone(); i.next()) {
					final int type = i.currentSegment(xy);
					
					if (type == PathIterator.SEG_MOVETO) {
						if (!vertices.isEmpty()) {
							smoothe(vertices, newVertices);
							updateShape(newVertices, newShape, false);
						}
						
						vertices.clear();
						newVertices.clear();
						vertices.add(new Point2D.Double(xy[0], xy[1]));
					} else if (type == PathIterator.SEG_CLOSE) {
						smoothe(vertices, newVertices);
						updateShape(newVertices, newShape, true);
						
						vertices.clear();
						newVertices.clear();
					} else {
						vertices.add(new Point2D.Double(xy[0], xy[1]));
					}
				}
				
				// TODO simplify new geometry
				segment.getGeometry().reset();
				segment.getGeometry().add(new Area(newShape));
			}
		}
	}
	
	public static final void updateShape(final List<Point2D> newVertices, final Path2D newShape, final boolean close) {
		newShape.moveTo(newVertices.get(0).getX(), newVertices.get(0).getY());
		
		for (int j = 0; j < newVertices.size(); ++j) {
			newShape.lineTo(newVertices.get(j).getX(), newVertices.get(j).getY());
		}
		
		if (close) {
			newShape.closePath();
		}
	}
	
	public static final void smoothe(final List<Point2D> vertices, final List<Point2D> newVertices) {
		final double s = abs(getSurface(vertices));
		final int n = vertices.size();
		final List<Point2D> deltas = new ArrayList<>(n);
		
		for (int i = 0; i < n; ++i) {
			final Point2D p = vertices.get(i);
			final Point2D q = vertices.get((i + n - 1) % n);
			final Point2D r = vertices.get((i + 1) % n);
			final Point2D v = new Point2D.Double((p.getX() + q.getX() + r.getX()) / 3.0 , (p.getY() + q.getY() + r.getY()) / 3.0);
			
			newVertices.add(v);
			deltas.add(new Point2D.Double(v.getX() - p.getX(), v.getY() - p.getY()));
		}
		
		final double newS = abs(getSurface(newVertices));
		final double scale = sqrt(s / newS);
		
		for (int i = 0; i < n; ++i) {
			final Point2D p = vertices.get(i);
			final Point2D delta = deltas.get(i);
			
			newVertices.get(i).setLocation(p.getX() + scale * delta.getX(), p.getY() + scale * delta.getY());
		}
	}
	
	public static final double getSurface(final List<Point2D> vertices) {
		double result = 0.0;
		final int n = vertices.size();
		
		for (int i = 0; i < n; ++i) {
			final Point2D p = vertices.get(i);
			final Point2D q = vertices.get((i + 1) % n);
			result += p.getX() * q.getY() - p.getY() * q.getX(); 
		}
		
		return result;
	}
	
	public static final List<Segment> combineSegments(final List<Segment> segments,
			final Collection<Integer> excludedLabels) {
		segments.sort((s1, s2) -> Double.compare(s1.getSurface(), s2.getSurface()));
		
		final BitSet remove = new BitSet(segments.size());
		
		for (int i = 0; i < segments.size(); ++i) {
			debugPrint(i, "/", segments.size());
			
			final Segment segmentI = segments.get(i);
			
			if (excludedLabels.contains(segmentI.getLabel())) {
				remove.set(i);
			}
			
			find_parent:
			for (int j = i + 1; j < segments.size(); ++j) {
				final Segment segmentJ = segments.get(j);
				
				if (excludedLabels.contains(segmentJ.getLabel())) {
					continue;
				}
				
				if (segmentI.getSurface() != segmentJ.getSurface() && segmentI.getLabel() != segmentJ.getLabel()) {
					final Area tmp = new Area(segmentI.getGeometry());
					
					tmp.subtract(segmentJ.getGeometry());
					
					if (tmp.isEmpty()) {
						segmentJ.getGeometry().subtract(segmentI.getGeometry());
						segmentI.setParent(segmentJ);
						break find_parent;
					}
				}
			}
		}
		
		debugPrint(remove.cardinality());
		
		final List<Segment> newSegments = new ArrayList<>();
		
		for (int i = 0; i < segments.size(); ++i) {
			if (!remove.get(i)) {
				newSegments.add(segments.get(i));
			}
		}
		return newSegments;
	}
	
	public static List<Segment> collectSegments(final BufferedImage image, final Structure structure) {
		final List<Segment> result = new ArrayList<>();
		final int[] tmp = new int[1];
		
		final Statistics negative = new Statistics();
		final Statistics positive = new Statistics();
		
		FACE.traverse(structure.getTopology(), f -> {
			final int dartCount = FACE.countDarts(structure.getTopology(), f);
			final int[] xs = new int[dartCount];
			final int[] ys = new int[dartCount];
			final int[] i = { 0 };
			
			FACE.traverse(structure.getTopology(), f, d -> {
				final Point p = structure.getGeometry().get(d);
				
				xs[i[0]] = p.x;
				ys[i[0]] = p.y;
				
				++i[0];
				
				return true;
			});
			
			final Polygon shape = new Polygon(xs, ys, dartCount);
			final double s = SVGTools.getSurface(shape, 0.0);
			
			if (s < 0.0) {
				negative.addValue(s);
				
				int label = 0;
				
				if (ys[0] > ys[1]) {
					checkEqual(xs[0], xs[1]);
					label = getValue(image, xs[0] - 1, ys[0] - 1, tmp);
				} else if (ys[0] == ys[1]) {
					if (xs[0] > xs[1]) {
						label = getValue(image, xs[0] - 1, ys[0], tmp);
					} else if (xs[0] < xs[1]) {
						debugPrint(xs[0], ys[0], xs[1], ys[1]);
						label = getValue(image, xs[0], ys[0] - 1, tmp);
					} else {
						throw new IllegalStateException();
					}
				} else if (ys[0] < ys[1]) {
					label = getValue(image, xs[0], ys[0], tmp);
				}
				
				result.add(new Segment(label, new Area(shape), abs(s)));
			} else {
				positive.addValue(s);
			}
			
			return true;
		});
		
		debugPrint("Discarded", positive.getCount(), "objects, total surface:", new BigDecimal(positive.getSum()));
		debugPrint("Collected", negative.getCount(), "objects, total surface:", new BigDecimal(negative.getSum()));
		
		return result;
	}
	
	public static final Structure getStructure(final BufferedImage image) {
		final Structure result = new Structure();
		final Manifold topology = result.getTopology();
		final List<Point> geometry = result.getGeometry();
		final int w = image.getWidth();
		final int h = image.getHeight();
		final int[] northEdges = new int[w * (h + 1)];
		final int[] westEdges = new int[(w + 1) * h];
		final int[] pixelBuffer = new int[image.getRaster().getDataBuffer().getSize()];
		final Map<Point, Point> points = new HashMap<>();
		
		Arrays.fill(northEdges, -1);
		Arrays.fill(westEdges, -1);
		
		debugPrint("width:", w, "height:", h);
		
		for (int y = 0; y <= h; ++y) {
			for (int x = 0; x <= w; ++x) {
				final int northEdgeIndex = x + y * w;
				final int westEdgeIndex = y + x * h;
				final int northWestEdgeIndex = westEdgeIndex - 1;
				final int westNorthEdgeIndex = northEdgeIndex - 1;
				boolean northEdgeExists = false;
				boolean northWestEdgeExists = false;
				boolean westEdgeExists = false;
				boolean westNorthEdgeExists = false;
				
				if (x < w) {
					if (y == 0 || y == h) {
						northEdges[northEdgeIndex] = topology.newEdge();
						northEdgeExists = true;
						
						geometry.add(points.computeIfAbsent(new Point(x + 1, y), k -> k));
						geometry.add(points.computeIfAbsent(new Point(x, y), k -> k));
						
						if (DEBUG) {
							debugPrint("x:", x, "y:", y, "northEdge:", northEdges[northEdgeIndex]);
						}
					} else {
						final int value = getValue(image, x, y, pixelBuffer);
						final int northValue = image.getRaster().getPixel(x, y - 1, pixelBuffer)[0];
						
						if (value != northValue) {
							northEdges[northEdgeIndex] = topology.newEdge();
							northEdgeExists = true;
							
							geometry.add(points.computeIfAbsent(new Point(x + 1, y), k -> k));
							geometry.add(points.computeIfAbsent(new Point(x, y), k -> k));
							
							if (DEBUG) {
								debugPrint("x:", x, "y:", y, "northEdge:", northEdges[northEdgeIndex]);
							}
						}
					}
				}
				
				if (y < h) {
					if (x == 0 || x == w) {
						westEdges[westEdgeIndex] = topology.newEdge();
						westEdgeExists = true;
						
						geometry.add(points.computeIfAbsent(new Point(x, y), k -> k));
						geometry.add(points.computeIfAbsent(new Point(x, y + 1), k -> k));
						
						if (DEBUG) {
							debugPrint("x:", x, "y:", y, "westEdge:", westEdges[westEdgeIndex]);
						}
					} else {
						final int value = getValue(image, x, y, pixelBuffer);
						final int westValue = image.getRaster().getPixel(x - 1, y, pixelBuffer)[0];
						
						if (value != westValue) {
							westEdges[westEdgeIndex] = topology.newEdge();
							westEdgeExists = true;
							
							geometry.add(points.computeIfAbsent(new Point(x, y), k -> k));
							geometry.add(points.computeIfAbsent(new Point(x, y + 1), k -> k));
							
							if (DEBUG) {
								debugPrint("x:", x, "y:", y, "westEdge:", westEdges[westEdgeIndex]);
							}
						}
					}
				}
				
				if (0 < y && 0 <= westEdges[northWestEdgeIndex]) {
					northWestEdgeExists = true;
				}
				
				if (0 < x && 0 <= northEdges[westNorthEdgeIndex]) {
					westNorthEdgeExists = true;
				}
				
				/*
				 *  ?
				 * ?._
				 *  |
				 */
				if (northEdgeExists && westEdgeExists) {
					{
						final int dart = northEdges[northEdgeIndex];
						final int next = westEdges[westEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *   
					 *  ._
					 *  |#
					 */
					if (!northWestEdgeExists && !westNorthEdgeExists) {
						final int dart = opposite(westEdges[westEdgeIndex]);
						final int next = opposite(northEdges[northEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *  |#
					 *  ._
					 *  |#
					 */
					if (northWestEdgeExists && !westNorthEdgeExists) {
						final int dart = opposite(westEdges[westEdgeIndex]);
						final int next = opposite(westEdges[northWestEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *   
					 * _._
					 * #|#
					 */
					if (!northWestEdgeExists && westNorthEdgeExists) {
						final int dart = opposite(northEdges[westNorthEdgeIndex]);
						final int next = opposite(northEdges[northEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
				}
				
				/*
				 *  ?
				 * _.?
				 *  |
				 */
				if (westEdgeExists && westNorthEdgeExists) {
					{
						final int dart = opposite(westEdges[westEdgeIndex]);
						final int next = northEdges[westNorthEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *   
					 * _. 
					 * #|
					 */
					if (!northEdgeExists && !northWestEdgeExists) {
						final int dart = opposite(northEdges[westNorthEdgeIndex]);
						final int next = westEdges[westEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *   
					 * _._
					 * #|#
					 */
					if (northEdgeExists && !northWestEdgeExists) {
						final int dart = opposite(northEdges[westNorthEdgeIndex]);
						final int next = opposite(northEdges[northEdgeIndex]);
						
						checkNext(topology, dart, next);
					}
					
					/*
					 * #|
					 * _. 
					 * #|
					 */
					if (!northEdgeExists && northWestEdgeExists) {
						final int dart = westEdges[northWestEdgeIndex];
						final int next = westEdges[westEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
				}
				
				/*
				 *  |
				 * _.?
				 *  ?
				 */
				if (westNorthEdgeExists && northWestEdgeExists) {
					{
						final int dart = opposite(northEdges[westNorthEdgeIndex]);
						final int next = opposite(westEdges[northWestEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 * #|
					 * _. 
					 *   
					 */
					if (!westEdgeExists && !northEdgeExists) {
						final int dart = westEdges[northWestEdgeIndex];
						final int next = northEdges[westNorthEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 * #|
					 * _. 
					 * #|
					 */
					if (westEdgeExists && !northEdgeExists) {
						final int dart = westEdges[northWestEdgeIndex];
						final int next = westEdges[westEdgeIndex];
						
						checkNext(topology, dart, next);
					}
					
					/*
					 * #|#
					 * _._
					 *   
					 */
					if (!westEdgeExists && northEdgeExists) {
						final int dart = northEdges[northEdgeIndex];
						final int next = northEdges[westNorthEdgeIndex];
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
				}
				
				/*
				 *  |
				 * ?._
				 *  ?
				 */
				if (northWestEdgeExists && northEdgeExists) {
					{
						final int dart = westEdges[northWestEdgeIndex];
						final int next = opposite(northEdges[northEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 *  |#
					 *  ._
					 *   
					 */
					if (!westNorthEdgeExists && !westEdgeExists) {
						final int dart = northEdges[northEdgeIndex];
						final int next = opposite(westEdges[northWestEdgeIndex]);
						
						if (DEBUG) {
							debugPrint(dart, "->", next);
						}
						
						topology.initializeNext(dart, next);
					}
					
					/*
					 * #|#
					 * _._
					 *   
					 */
					if (westNorthEdgeExists && !westEdgeExists) {
						final int dart = northEdges[northEdgeIndex];
						final int next = northEdges[westNorthEdgeIndex];
						
						checkNext(topology, dart, next);
					}
					
					/*
					 *  |#
					 *  ._
					 *  |#
					 */
					if (!westNorthEdgeExists && westEdgeExists) {
						final int dart = opposite(westEdges[westEdgeIndex]);
						final int next = opposite(westEdges[northWestEdgeIndex]);
						
						checkNext(topology, dart, next);
					}
				}
				
				/*
				 *   
				 * _._
				 *   
				 */
				if (!westEdgeExists && northEdgeExists && !northWestEdgeExists && westNorthEdgeExists) {
					final int dart1 = northEdges[northEdgeIndex];
					final int next1 = northEdges[westNorthEdgeIndex];
					final int dart2 = opposite(next1);
					final int next2 = opposite(dart1);
					
					if (DEBUG) {
						debugPrint(dart1, "->", next1);
						debugPrint(dart2, "->", next2);
					}
					
					topology.initializeNext(dart1, next1);
					topology.initializeNext(dart2, next2);
				}
				
				/*
				 *  |
				 *  . 
				 *  |
				 */
				if (westEdgeExists && !northEdgeExists && northWestEdgeExists && !westNorthEdgeExists) {
					final int dart1 = westEdges[northWestEdgeIndex];
					final int next1 = westEdges[westEdgeIndex];
					final int dart2 = opposite(next1);
					final int next2 = opposite(dart1);
					
					if (DEBUG) {
						debugPrint(dart1, "->", next1);
						debugPrint(dart2, "->", next2);
					}
					
					topology.initializeNext(dart1, next1);
					topology.initializeNext(dart2, next2);
				}
			}
		}
		
		debugPrint("edges:", topology.getEdgeCount());
		
		if (false) {
			checkEqual(geometry.size(), topology.getDartCount());
			
			debugPrint("Checking topology...");
			
			checkConnected(topology, northEdges);
			checkConnected(topology, westEdges);
			
			debugPrint("valid:", topology.isValid());
		}
		
		return result;
	}
	
	public static final int getValue(final BufferedImage image, int x, int y, final int[] buffer) {
		return image.getRaster().getPixel(x, y, buffer)[0];
	}
	
	public static final void checkConnected(final Manifold manifold, final int[] darts) {
		for (final int i : darts) {
			if (0 < i) {
				if (manifold.getNext(i) < 0) {
					throw new IllegalStateException("Unconnected dart: " + i);
				}
				
				if (manifold.getNext(opposite(i)) < 0) {
					throw new IllegalStateException("Unconnected dart: " + opposite(i));
				}
			}
		}
	}
	
	public static final void checkNext(final Manifold manifold, final int dart, final int next) {
		checkEqual(manifold.getNext(dart), next);
	}
	
	public static final void checkEqual(final int a, final int b) {
		if (a != b) {
			throw new IllegalStateException(a + " != " + b);
		}
	}
	
	/**
	 * @author codistmonk (creation 2016-06-24)
	 */
	public static final class Segment implements Serializable {
		
		private final int label;
		
		private final Area geometry;
		
		private final double surface;
		
		private Segment parent;
		
		public Segment(final int label, final Area geometry, final double surface) {
			this.label = label;
			this.geometry = geometry;
			this.surface = surface;
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final Area getGeometry() {
			return this.geometry;
		}
		
		public final double getSurface() {
			return this.surface;
		}
		
		public final Segment getParent() {
			return this.parent;
		}
		
		public final void setParent(final Segment parent) {
			this.parent = parent;
		}
		
		private static final long serialVersionUID = -6171824164546674105L;
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-24)
	 */
	public static final class Structure implements Serializable {
		
		private final Manifold topology = new Manifold();
		
		private final List<Point> geometry = new ArrayList<>();
		
		public final Manifold getTopology() {
			return this.topology;
		}
		
		public final List<Point> getGeometry() {
			return this.geometry;
		}
		
		private static final long serialVersionUID = -6659682585235198330L;
		
	}
	
}
