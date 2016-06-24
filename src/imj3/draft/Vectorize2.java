package imj3.draft;

import static imj2.topology.Manifold.opposite;
import static imj2.topology.Manifold.Traversor.FACE;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;

import imj2.topology.Manifold;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		
		debugPrint("image:", imagePath);
		
		try {
			final BufferedImage image = ImageIO.read(new File(imagePath));
			final Structure structure = getStructure(image);
			debugPrint("points:", new HashSet<>(structure.getGeometry()).size());
			debugPrint("objects:", FACE.count(structure.getTopology()));
			final Document svg = SVGTools.newSVG(image.getWidth(), image.getHeight());
			final List<Segment> segments = collectSegments(structure);
			
			debugPrint(segments.size());
			
			segments.sort((s1, s2) -> Double.compare(s1.getSurface(), s2.getSurface()));
			final BitSet remove = new BitSet(segments.size());
			
			for (int i = 0; i < segments.size(); ++i) {
				debugPrint(i, "/", segments.size());
				
				final Segment segmentI = segments.get(i);
				
				for (int j = i + 1; j < segments.size(); ++j) {
					final Segment segmentJ = segments.get(j);
					final Area tmp = new Area(segmentJ.getGeometry());
					
					tmp.subtract(segmentI.getGeometry());
					
					if (tmp.isEmpty()) {
						segmentI.getGeometry().subtract(segmentJ.getGeometry());
						remove.set(j);
					}
				}
			}
			
			debugPrint(remove.cardinality());
			
			final List<Segment> newSegments = new ArrayList<>();
			
			for (int i = 0; i < segments.size(); ++i) {
				if (remove.get(i)) {
					newSegments.add(segments.get(i));
				}
			}
			
			debugPrint(newSegments.size());
			
			XMLTools.write(svg, new File(baseName(imagePath) + ".svg"), 1);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public static List<Segment> collectSegments(final Structure structure) {
		final List<Segment> result = new ArrayList<>();
		final int[] objectId = { 0 };
		
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
				result.add(new Segment(objectId[0], new Area(shape)));
				
				++objectId[0];
			} else {
				positive.addValue(s);
			}
			
			return true;
		});
		
		debugPrint("Skipped", positive.getCount(), "objects, total surface:", new BigDecimal(positive.getSum()));
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
		
		private final int id;
		
		private final Area geometry;
		
		private final double surface;
		
		public Segment(final int id, final Area geometry) {
			this.id = id;
			this.geometry = geometry;
			this.surface = SVGTools.getSurface(geometry, 0.0);
		}
		
		public final int getId() {
			return this.id;
		}
		
		public final Area getGeometry() {
			return this.geometry;
		}
		
		public final double getSurface() {
			return this.surface;
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
