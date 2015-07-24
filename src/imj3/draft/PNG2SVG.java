package imj3.draft;

import static imj2.topology.Manifold.*;
import static imj2.topology.Manifold.Traversor.*;
import static imj3.draft.AperioXML2SVG.formatColor;
import static imj3.draft.PNG2SVG.WeakProperties.weakProperties;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.parse;

import imj2.topology.Manifold;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import multij.swing.SwingTools;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.ConsoleMonitor;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;
import multij.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2015-06-23)
 */
public final class PNG2SVG {
	
	private PNG2SVG() {
		throw new IllegalInstantiationException();
	}
	
	private static final int LEFT_OFFSET = 0;
	
	private static final int BOTTOM_OFFSET = 2;
	
	public static final double projection(final Point2D point, final Point2D origin, final double px, final double py) {
		return (point.getX() - origin.getX()) * px + (point.getY() - origin.getY()) * py;
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final String classIdsPath = arguments.get("classIds", "");
		final String outputPath = arguments.get("output", baseName(imagePath) + ".svg");
		
		debugPrint("imagePath:", imagePath);
		
		try {
			final BufferedImage image = ImageIO.read(new File(imagePath));
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			final Manifold manifold = newGrid(imageWidth, imageHeight);
			final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
			
			debugPrint("imageWidth:", imageWidth, "imageHeight:", imageHeight);
			debugPrint("dartCount:", manifold.getDartCount());
			debugPrint("Pruning...");
			
			int disconnections = 0;
			
			for (int y = 0; y < imageHeight; ++y) {
				monitor.ping("Pruning: " + y + "/" + imageHeight + "\r");
				for (int x = 0; x < imageWidth; ++x) {
					if (x + 1 < imageWidth && image.getRGB(x, y) == image.getRGB(x + 1, y)) {
//						debugPrint("Disconnecting right of (" + x + " " + y + ")");
						disconnectEdge(manifold, (y * imageWidth + x + 1) * 4 + LEFT_OFFSET);
						++disconnections;
					}
					if (y + 1 < imageHeight && image.getRGB(x, y) == image.getRGB(x, y + 1)) {
//						debugPrint("Disconnecting bottom of (" + x + " " + y + ")");
						disconnectEdge(manifold, (y * imageWidth + x) * 4 + BOTTOM_OFFSET);
						++disconnections;
					}
				}
			}
			
			debugPrint("Pruned:", disconnections);
			
			debugPrint("Checking topology...");
			assert manifold.isValid();
			debugPrint("Topology is OK");
			
			if (false) {
				final Point firstXY = new Point();
				final Point lastXY = new Point();
				final Point xy = new Point();
				final Statistics statistics = new Statistics();
				
				manifold.forEach(FACE, f -> {
					if (FACE.countDarts(manifold, f) < 3) {
						return true;
					}
					
					debugPrint(f, FACE.countDarts(manifold, f));
					
					int first = f;
					
					getDartXY(imageWidth, imageHeight, first, firstXY);
					
					int oldLast = -1;
					int last = manifold.getNext(first, 2);
					
					getDartXY(imageWidth, imageHeight, last, lastXY);
					
					final double px, py;
					
					{
						final double length = firstXY.distance(lastXY);
						
						if (length == 0.0) {
							return true;
						}
						
						px = (firstXY.y - lastXY.y) / length;
						py = (lastXY.x - firstXY.x) / length;
						
						assert 0.0 == projection(lastXY, firstXY, px, py);
					}
					
					statistics.reset();
					statistics.addValue(0.0);
					
					lengthen_current_segment:
					while (true) {
						for (int dart = manifold.getNext(first); dart != last; dart = manifold.getNext(dart)) {
							getDartXY(imageWidth, imageHeight, dart, xy);
							statistics.addValue(projection(xy, firstXY, px, py));
						}
						
						debugPrint(statistics.getAmplitude());
						
						if (statistics.getAmplitude() <= 1.0 && VERTEX.countDarts(manifold, last) == 2) {
							oldLast = last;
							last = manifold.getNext(last);
						} else if (oldLast != -1) {
							// TODO simplify topology
							// first -> oldLast
							
							/*
							 *   first       oldLast
							 * ##---->##...##---->#
							 * ##<----##...##<----#
							 * 
							 */
							
							final int afterFirst = manifold.getNext(first);
							final int beforeOldLast = manifold.getPrevious(oldLast);
							
							debugPrint(getDartXY(imageWidth, imageHeight, first, new Point()));
							debugPrint(getDartXY(imageWidth, imageHeight, oldLast, new Point()));
							debugPrint(VERTEX.countDarts(manifold, first), VERTEX.countDarts(manifold, afterFirst));
							debugPrint(VERTEX.countDarts(manifold, oldLast), VERTEX.countDarts(manifold, beforeOldLast));
							
//							manifold.setNext(afterFirst, opposite(afterFirst));
//							manifold.setNext(opposite(beforeOldLast), beforeOldLast);
//							manifold.setNext(first, oldLast);
//							manifold.setNext(opposite(oldLast), opposite(first));
							
							break lengthen_current_segment;
						} else {
							first = manifold.getNext(first);
							break lengthen_current_segment;
						}
					}
					
					return true;
				});
			}
			
			debugPrint(manifold.isValid());
			
			final Map<Integer, Collection<Polygon>> polygons = new HashMap<>();
			final Rectangle bounds = new Rectangle();
			
			collectPolygons(image, manifold, polygons, bounds);
			
			// TODO simplify polygons
			
			final Map<Integer, Collection<Area>> shapes = toShapes(polygons);
			
//			debugPrint(shapes.keySet().stream().map(Integer::toHexString).toArray());
			
			if (false) {
				final BufferedImage tmp = new BufferedImage(bounds.width + 1, bounds.height + 1, BufferedImage.TYPE_INT_ARGB);
				final Graphics2D graphics = tmp.createGraphics();
				
				for (final Map.Entry<Integer, ? extends Collection<? extends Shape>> entry : shapes.entrySet()) {
					graphics.setColor(new Color(entry.getKey()));
					entry.getValue().forEach(graphics::fill);
				}
				
				graphics.dispose();
				
				SwingTools.show(tmp, "polygons", false);
			}
			
			{
				debugPrint("Creating SVG...");
				
				final AffineTransform identity = new AffineTransform();
				final double[] segment = new double[6];
				final Document svg = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:imj=\"IMJ\"/>");
				final Element svgRoot  = svg.getDocumentElement();
				final List<String> classIds = classIdsPath.isEmpty() ? null : Files.readAllLines(new File("").toPath());
				
				for (final Map.Entry<Integer, ? extends Collection<? extends Shape>> entry : shapes.entrySet()) {
					for (final Shape s : entry.getValue()) {
						final StringBuilder pathData = new StringBuilder();
						
						for (final PathIterator pathIterator = s.getPathIterator(identity, 1.0);
								!pathIterator.isDone(); pathIterator.next()) {
							switch (pathIterator.currentSegment(segment)) {
							case PathIterator.SEG_CLOSE:
								pathData.append('Z');
								break;
							case PathIterator.SEG_CUBICTO:
								pathData.append('C');
								pathData.append(join(" ", segment, 6));
								break;
							case PathIterator.SEG_LINETO:
								pathData.append('L');
								pathData.append(join(" ", segment, 2));
								break;
							case PathIterator.SEG_MOVETO:
								pathData.append('M');
								pathData.append(join(" ", segment, 2));
								break;
							case PathIterator.SEG_QUADTO:
								pathData.append('Q');
								pathData.append(join(" ", segment, 4));
								break;
							}
						}
						
						final int label = entry.getKey() & 0x00FFFFFF;
						final String classId = classIds == null ? Integer.toString(label) : classIds.get(label);
						final Element svgRegion = (Element) svgRoot.appendChild(svg.createElement("path"));
						
						svgRegion.setAttribute("d", pathData.toString());
						svgRegion.setAttribute("style", "fill:" + formatColor(entry.getKey()));
						svgRegion.setAttribute("imj:classId", classId);
					}
				}
				
				debugPrint("Writing", outputPath);
				XMLTools.write(svg, new File(outputPath), 1);
			}
			
			{
				final DiagramComponent diagramComponent = new DiagramComponent();
				final double scale = 100.0;
				
				diagramComponent.getRenderers().add(g -> {
					for (final Map.Entry<Integer, ? extends Collection<? extends Shape>> entry : shapes.entrySet()) {
						final AffineTransform transform = g.getTransform();
						g.scale(scale, scale);
						g.setColor(new Color(entry.getKey()));
						entry.getValue().forEach(g::fill);
						g.setTransform(transform);
					}
				});
				
				manifold.forEach(DART, d -> {
					final int next = manifold.getNext(d);
					
					if (next != opposite(d)) {
						final Point xy1 = new Point();
						final Point xy2 = new Point();
						getDartXY(imageWidth, imageHeight, d, xy1);
						getDartXY(imageWidth, imageHeight, next, xy2);
						xy1.x *= scale;
						xy1.y *= scale;
						xy2.x *= scale;
						xy2.y *= scale;
						diagramComponent.getRenderers().add(new DiagramComponent.Arrow()
						.set("source", xy1).set("target", xy2).set("curvature", -0.2 * scale)
						.set("label", Integer.toString(d)).set("color", Color.BLACK));
					}
					
					return true;
				});
				
				SwingUtilities.invokeLater(() -> SwingTools.show(diagramComponent, "diagram", false));
			}
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final String join(final String separator, final double[] array, final int n) {
		final StringBuilder resultBuilder = new StringBuilder();
		
		if (0 < n) {
			resultBuilder.append(array[0]);
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(separator);
				resultBuilder.append(array[i]);
			}
		}
		
		return resultBuilder.toString();
	}
	
	public static final long determinant(final Polygon polygon) {
		long result = 0L;
		final int n = polygon.npoints;
		
		for (int i = 0; i < n; ++i) {
			final long x1 = polygon.xpoints[i];
			final long y1 = polygon.ypoints[i];
			final long x2 = polygon.xpoints[(i + 1) % n];
			final long y2 = polygon.ypoints[(i + 1) % n];
			
			result += x1 * y2 - y1 * x2;
		}
		
		return result;
	}
	
	public static final Map<Integer, Collection<Area>> toShapes(final Map<Integer, Collection<Polygon>> polygons) {
		debugPrint("Creating shapes...");
		
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		final Map<Integer, Collection<Area>> result = new HashMap<>();
		final int keyCount = polygons.size();
		final int[] progress = { -1 };
		
		polygons.forEach((k, v) -> {
			monitor.ping("Creating shapes " + (++progress[0]) + "/" + keyCount + "\r");
			
			final List<Area> shapes = new ArrayList<>();
			
			for (final Polygon polygon : v) {
				final Area newShape = new Area(polygon);
				final long newShapeDeterminant = determinant(polygon);
				weakProperties.set(newShape, "determinant", newShapeDeterminant);
				Area container = null;
				final int n = shapes.size();
				
				for (int i = 0; i < n && container == null; ++i) {
					final Area shape = shapes.get(i);
					final Area tmp = new Area(shape);
					
					tmp.intersect(newShape);
					
					if (tmp.equals(newShape)) {
						container = shape;
					}
				}
				
				if (container == null) {
					shapes.add(newShape);
				} else {
					final long containerDeterminant = weakProperties.get(container, "determinant");
					
					container.subtract(newShape);
					
					if (0 < containerDeterminant * newShapeDeterminant) {
						debugError(Integer.toHexString(k), containerDeterminant, newShapeDeterminant);
						weakProperties.set(container, "determinant", containerDeterminant - newShapeDeterminant);
					} else {
						weakProperties.set(container, "determinant", containerDeterminant + newShapeDeterminant);
					}
				}
			}
			
			result.put(k, shapes);
		});
		
		monitor.pause();
		
		return result;
	}
	
	public static final void collectPolygons(final BufferedImage image, final Manifold manifold,
			final Map<Integer, Collection<Polygon>> polygons, final Rectangle bounds) {
		debugPrint("Collecting polygons...");
		
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int topBorder = imageWidth * imageHeight * 4;
		final int rightBorder = topBorder + imageWidth * 2;
		final Point tmp = new Point();
		
		manifold.forEach(Traversor.FACE, f -> {
			monitor.ping("Collecting polygons " + f + "/" + manifold.getDartCount() + "\r");
			
			final Polygon polygon = new Polygon();
			
			manifold.forEachDartIn(Traversor.FACE, f, d -> {
				if (d < topBorder) {
					processInternalDart(d, imageWidth, imageHeight, polygon);
				} else if (d < rightBorder) {
					if ((d & 1) == 0) {
						polygon.addPoint((d - topBorder) / 2 + 1, 0);
					}
				} else {
					if ((d & 1) == 0) {
						polygon.addPoint(imageWidth, (d - rightBorder) / 2 + 1);
					}
				}
				
				return true;
			});
			
			if (2 < polygon.npoints) {
				getPixelXY(imageWidth, imageHeight, f, tmp);
				
				weakProperties.set(polygon, "label", image.getRGB(tmp.x, tmp.y));
				weakProperties.set(polygon, "determinant", determinant(polygon));
				
				final Collection<Polygon> collection = polygons.computeIfAbsent(image.getRGB(tmp.x, tmp.y),
						k -> new ArrayList<>());
				collection.add(polygon);
				
				bounds.add(polygon.getBounds());
			}
			
			return true;
		});
		
		monitor.pause();
		
		debugPrint("Sorting...");
		
		polygons.forEach((k, v) -> ((List<Polygon>) v).sort((p1, p2) -> Long.compare(
				abs((long) weakProperties.get(p2, "determinant")), abs((long) weakProperties.get(p1, "determinant")))));
		
		debugPrint("Polygons collected");
	}
	
	public static final void processInternalDart(final int dart, final int imageWidth, final int imageHeight, final Polygon polygon) {
		final int orientation = dart & 1;
		final int side = dart & 2;
		final int pixel = dart / 4;
		final int x, y;
		
		if (side == 0) {
			// vertical
			x = pixel % imageWidth;
			y = pixel / imageWidth + orientation;
		} else {
			// horizontal
			x = pixel % imageWidth + orientation;
			y = pixel / imageWidth + 1;
		}
		
		if (orientation == 0 || 0 < x && y + 1 <= imageHeight) {
			polygon.addPoint(x, y);
		}
	}
	
	public static final Point getDartXY(final int imageWidth, final int imageHeight, final int dart, final Point result) {
		final int topBorder = imageWidth * imageHeight * 4;
		final int rightBorder = topBorder + imageWidth * 2;
		final int orientation = dart & 1;
		
		if (dart < topBorder) {
			final int side = dart & 2;
			final int pixel = dart / 4;
			
			if (side == 0) {
				// vertical
				result.x = pixel % imageWidth;
				result.y = pixel / imageWidth + orientation;
			} else {
				// horizontal
				result.x = pixel % imageWidth + orientation;
				result.y = pixel / imageWidth + 1;
			}
		} else if (dart < rightBorder) {
			result.x = (dart - topBorder) / 2 + (orientation ^ 1);
			result.y = 0;
		} else {
			result.x = imageWidth;
			result.y = (dart - rightBorder) / 2 + (orientation ^ 1);
		}
		
		return result;
	}
	
	public static final Point getPixelXY(final int imageWidth, final int imageHeight, final int dart, final Point result) {
		final int topBorder = imageWidth * imageHeight * 4;
		final int rightBorder = topBorder + imageWidth * 2;
		
		if (dart < topBorder) {
			/*
			 * ###--?->###
			 * ###<-11-###
			 * ^ |     ^ |
			 * | |     | |
			 * ? 00   01 ?
			 * | |     | |
			 * | v     | v
			 * ###-10->###
			 * ###<-?--###
			 */
			final int side = dart & 3;
			final int pixel = dart / 4;
			result.x = pixel % imageWidth - (side == 1 ? 1 : 0);
			result.y = pixel / imageWidth + (side == 3 ? 1 : 0);
		} else if (dart < rightBorder) {
			result.x = (dart - topBorder) / 2;
			result.y = 0;
		} else {
			result.x = imageWidth - 1;
			result.y = (dart - rightBorder) / 2;
		}
		
		return result;
	}
	
	public static final void disconnectEdge(final Manifold manifold, final int dart) {
		final int previous = manifold.getPrevious(dart);
		final int next = manifold.getNext(dart);
		final int previousOfOpposite = manifold.getPrevious(opposite(dart));
		final int nextOfOpposite = manifold.getNext(opposite(dart));
		
		/*
		 * ###-17->###-19->###
		 * ###<-16-###<-18-###
		 * ^ |     ^ |     ^ |
		 * | |     | |     | |
		 * 1 0     5 4    20 21
		 * | |     | |     | |
		 * | v     | v     | v
		 * ###--2->###--6->###
		 * ###<-3--###<-7--###
		 * ^ |     ^ |     ^ |
		 * | |     | |     | |
		 * 9 8    13 12   22 23
		 * | |     | |     | |
		 * | v     | v     | v
		 * ###-10->###-14->###
		 * ###<-11-###<-15-###
		 */
		
		/*
		 *  ###-65->###-67->###-69->###-71->###
		 *  ###<-64-###<-66-###<-68-###<-70-###
		 *  ^ |     ^ |     ^ |     ^ |     ^ |
		 *  | | 0 0 | | 1 0 | | 2 0 | | 3 0 | |
		 *  1 0     5 4     9 8    13 12   72 73
		 *  | |     | |     | |     | |     | |
		 *  | v     | v     | v     | v     | v
		 *  ###--2->###--6->###-10->###-14->###
		 *  ###<-3--###<-7--###<-11-###<-25-###
		 *  ^ |     ^ |     ^ |     ^ |     ^ |
		 *  | | 0 1 | | 1 1 | | 2 1 | | 3 1 | |
		 * 17 16   21 20   25 24   29 28   74 75
		 *  | |     | |     | |     | |     | |
		 *  | v     | v     | v     | v     | v
		 *  ###-18->###-22->###-26->###-30->###
		 *  ###<-19-###<-23-###<-27-###<-31-###
		 *  ^ |     ^ |     ^ |     ^ |     ^ |
		 *  | | 0 2 | | 1 2 | | 2 2 | | 3 2 | |
		 * 33 32   37 36   41 40   45 44   76 77
		 *  | |     | |     | |     | |     | |
		 *  | v     | v     | v     | v     | v
		 *  ###-34->###-38->###-42->###-46->###
		 *  ###<-35-###<-39-###<-43-###<-47-###
		 *  ^ |     ^ |     ^ |     ^ |     ^ |
		 *  | | 0 3 | | 1 3 | | 2 3 | | 3 3 | |
		 * 49 48   53 52   57 56   61 60   78 79
		 *  | |     | |     | |     | |     | |
		 *  | v     | v     | v     | v     | v
		 *  ###-50->###-54->###-58->###-62->###
		 *  ###<-51-###<-55-###<-59-###<-63-###
		 */
		
//		debugPrint("Disconnecting " + dart + "/" + opposite(dart));
//		debugPrint(previous, "->", nextOfOpposite);
//		debugPrint(previousOfOpposite, "->", next);
		manifold.setNext(previous, nextOfOpposite);
		manifold.setNext(previousOfOpposite, next);
		
		manifold.setCycle(dart, opposite(dart));
	}
	
	public static final Manifold newGrid(final int width, final int height) {
		final Manifold manifold = new Manifold();
		
		/*
		 *  _ _
		 *  
		 * |_|_ |
		 * |_|_ |
		 */
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				manifold.newEdge();
				manifold.newEdge();
			}
		}
		
		final int topBorder = width * height * 4;
		
		assert topBorder == manifold.getDartCount();
		
		for (int x = 0; x < width; ++x) {
			// top border
			manifold.newEdge();
		}
		
		final int rightBorder = topBorder + 2 * width;
		
		assert rightBorder == manifold.getDartCount();
		
		for (int y = 0; y < height; ++y) {
			// right border
			manifold.newEdge();
		}
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				final int vertex = (y * width + x) * 4;
				final int left = vertex + LEFT_OFFSET;
				final int bottom = vertex + BOTTOM_OFFSET;
				final int rightVertex = vertex + 4;
				final int topVertex = vertex - width * 4;
				final boolean isTopBorder = y == 0;
				final boolean isRightBorder = x + 1 == width;
				final boolean isTopRightCorner = isTopBorder && isRightBorder;
				
				if (isTopRightCorner) {
					manifold.initializeCycle(left, bottom, rightBorder + y * 2, topBorder + x * 2);
				} else if (isTopBorder) {
					manifold.initializeCycle(left, bottom, opposite(rightVertex + LEFT_OFFSET), topBorder + x * 2);
				} else if (isRightBorder) {
					manifold.initializeCycle(left, bottom, rightBorder + y * 2, opposite(topVertex + BOTTOM_OFFSET));
				} else {
					manifold.initializeCycle(left, bottom, opposite(rightVertex + LEFT_OFFSET), opposite(topVertex + BOTTOM_OFFSET));
				}
			}
		}
		
		for (int x = 0; x + 1 < width; ++x) {
			// top border
			manifold.initializeNext(opposite(topBorder + 2 * x), opposite(topBorder + 2 * (x + 1)));
			// bottom border
			manifold.initializeNext(opposite(((height - 1) * width + x + 1) * 4 + BOTTOM_OFFSET),
					opposite(((height - 1) * width + x) * 4 + BOTTOM_OFFSET));
		}
		
		for (int y = 0; y + 1 < height; ++y) {
			// right border
			manifold.initializeNext(opposite(rightBorder + 2 * y), opposite(rightBorder + 2 * (y + 1)));
			// left border
			manifold.initializeNext(opposite(((y + 1) * width + 0) * 4),
					opposite((y * width + 0) * 4));
		}
		
		{
			// top left corner
			final int topLeft = (0 * width + 0) * 4;
			manifold.initializeNext(opposite(topLeft + LEFT_OFFSET), opposite(topBorder + 0));
		}
		{
			// bottom left corner
			final int bottomLeft = ((height - 1) * width + 0) * 4;
			manifold.initializeNext(opposite(bottomLeft + BOTTOM_OFFSET), opposite(bottomLeft + LEFT_OFFSET));
		}
		{
			// bottom right corner
			final int bottomRight = ((height - 1) * width + width - 1) * 4;
			manifold.initializeNext(opposite(rightBorder + (height - 1) * 2), opposite(bottomRight + BOTTOM_OFFSET));
		}
		{
			// top right corner
			manifold.initializeNext(opposite(topBorder + (width - 1) * 2), opposite(rightBorder + 0 * 2));
		}
		
		debugPrint("Checking topology...");
		assert manifold.isValid();
		debugPrint("Topology is OK");
		return manifold;
	}
	
	/**
	 * @author codistmonk (creation 2015-07-03)
	 */
	public static final class WeakProperties implements Serializable {
		
		private final Map<Object, Map<Object, Object>> allProperties = new WeakHashMap<>();
		
		public final Map<Object, Object> get(final Object object) {
			return this.allProperties.getOrDefault(object, Collections.emptyMap());
		}
		
		public final <T> T get(final Object object, final Object key) {
			return this.get(object, key, null);
		}
		
		@SuppressWarnings("unchecked")
		public final <T> T get(final Object object, final Object key, final T defaultValue) {
			return (T) this.allProperties.getOrDefault(object, Collections.emptyMap()).getOrDefault(key, defaultValue);
		}
		
		public final <T> T set(final T object, final Object key, final Object value) {
			this.allProperties.computeIfAbsent(object, o -> new HashMap<>()).put(key, value);
			
			return object;
		}
		
		private static final long serialVersionUID = 8486939636170800184L;
		
		public static final WeakProperties weakProperties = new WeakProperties();
		
	}
	
}
