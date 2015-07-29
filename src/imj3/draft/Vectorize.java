package imj3.draft;

import static imj2.topology.Manifold.*;
import static imj2.topology.Manifold.Traversor.*;
import static imj3.draft.AperioXML2SVG.formatColor;
import static imj3.draft.Vectorize.WeakProperties.weakProperties;
import static java.lang.Math.*;
import static multij.tools.MathTools.square;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.parse;

import imj2.topology.Manifold;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import multij.swing.SwingTools;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.ConsoleMonitor;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2015-06-23)
 */
public final class Vectorize {
	
	private Vectorize() {
		throw new IllegalInstantiationException();
	}
	
	private static final int LEFT_OFFSET = 0;
	
	private static final int BOTTOM_OFFSET = 2;
	
	static final AtomicBoolean debug = new AtomicBoolean();
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final String classIdsPath = arguments.get("classIds", "");
		final String outputPath = arguments.get("output", baseName(imagePath) + ".svg");
		final int quantization = arguments.get("quantization", 0)[0];
		final int smoothing = arguments.get("smoothing", 2)[0];
		final double flattening = Double.parseDouble(arguments.get("flattening", Double.toString(PI / 8.0)));
		final boolean binary = arguments.get("binary", 0)[0] != 0;
		
		debug.set(arguments.get("debug", 0)[0] != 0);
		
		debugPrint("imagePath:", imagePath);
		
		try {
			final BufferedImage image = ImageIO.read(new File(imagePath));
			final Map<Integer, Collection<Area>> shapes = getRegions(image, quantization, smoothing, flattening);
			
			{
				debugPrint("Creating SVG...");
				final List<String> classIds = classIdsPath.isEmpty() ? null : Files.readAllLines(new File(classIdsPath).toPath());
				final Document svg = toSVG(shapes, classIds, binary);
				
				debugPrint("Writing", outputPath);
				XMLTools.write(svg, new File(outputPath), 1);
			}
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final Map<Integer, Collection<Area>> getRegions(final BufferedImage image, final int quantization, final int smoothing, final double flattening) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Manifold manifold = contoursOf(image, quantization);
		final Point2D[] locations = generateLocations(imageWidth, imageHeight, manifold);
		
		simplify(manifold, locations, smoothing, flattening);
		
		final Map<Integer, Collection<Area>> result = toRegions(collectContours(image, quantization, manifold, locations, new HashMap<>()));
		
		if (debug.get()) {
			debugShow("result", manifold, locations, result);
		}
		
		return result;
	}
	
	public static final Document toSVG(final Map<Integer, ? extends Collection<? extends Shape>> shapes,
			final List<String> classIds, final boolean binary) {
		final AffineTransform identity = new AffineTransform();
		final double[] segment = new double[6];
		final Document svg = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:imj=\"IMJ\"/>");
		final Element svgRoot  = svg.getDocumentElement();
		
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
				final String classId = classIds == null ? Integer.toString(label) : classIds.get(binary ? (label != 0 ? 1 : 0) : label);
				final Element svgRegion = (Element) svgRoot.appendChild(svg.createElement("path"));
				
				svgRegion.setAttribute("d", pathData.toString());
				svgRegion.setAttribute("style", "fill:" + formatColor(entry.getKey()));
				svgRegion.setAttribute("imj:classId", classId);
			}
		}
		return svg;
	}
	
	public static final Point2D[] identityClone(final Point2D[] locations) {
		final Map<Point2D, Point2D> existing = new IdentityHashMap<>();
		
		return Arrays.stream(locations).map(l -> existing.computeIfAbsent(l,
				p -> new Point2D.Double(p.getX(), p.getY()))).toArray(Point2D[]::new);
	}
	
	public static final Point2D setAverage(final Point2D result, final Point2D... points) {
		double x = 0.0;
		double y = 0.0;
		
		final int n = points.length;
		
		if (0 < n) {
			for (final Point2D point : points) {
				x += point.getX();
				y += point.getY();
			}
			
			x /= n;
			y /= n;
		}
		
		result.setLocation(x, y);
		
		return result;
	}
	
	public static final void simplify(final Manifold manifold, final Point2D[] locations, final int smoothing, final double flattening) {
		final Point2D[] newLocations = identityClone(locations);
		
		if (0 < smoothing) {
			debugPrint("Smoothing...");
			
			for (int j = 0; j < smoothing; ++j) {
				manifold.forEach(FACE, f -> {
					if (4 < FACE.countDarts(manifold, f)) {
						smootheLinearConnections(manifold, locations, newLocations, f);
						restoreCorners(manifold, locations, newLocations, f);
						setLocations(manifold, locations, newLocations, f);
					}
					
					return true;
				});
			}
		}
		
		if (0.0 <= flattening) {
			debugPrint("Flattening...");
			
			final double angularThresholdForLinearity = flattening;
			
			manifold.forEach(FACE, f -> {
				final int n = FACE.countDarts(manifold, f);
				
				if (4 < n) {
					for (int i = 0, d = f; i < 2 * n; ++i) {
						final int next = manifold.getNext(d);
						
						if (VERTEX.countDarts(manifold, d) == 2) {
							final int previous = manifold.getPrevious(d);
							final double angle = angle(locations[previous], locations[d], locations[next]);
							
							if (angle <= angularThresholdForLinearity) {
								/*
								 * #--p->##--d->##--n->#
								 * #<----##<----##<----#
								 *              ^|
								 *              ||
								 *              x|
								 *              ||
								 *              ||
								 *              ##
								 * 
								 *           V
								 *           V
								 *           V
								 * 
								 * #------p---->##--n->#
								 * #<-----------##<----#
								 *              ^|
								 *              ||
								 *              x|
								 *              ||
								 *              ||
								 *              ##
								 */
								
								final int x = manifold.getPrevious(opposite(d));
								
								manifold.setNext(previous, next);
								manifold.setNext(x, opposite(previous));
								locations[opposite(previous)] = locations[opposite(d)];
								manifold.setCycle(d, opposite(d));
								
								assert 0 <= manifold.getPrevious(previous);
								assert 0 <= manifold.getPrevious(x);
								assert 0 <= manifold.getPrevious(next);
							}
						}
						
						d = next;
					}
				}
				
				return true;
			});
		}
	}
	
	public static final double angle(final Point2D a, final Point2D b, final Point2D c) {
		final double abX = b.getX() - a.getX();
		final double abY = b.getY() - a.getY();
		final double bcX = c.getX() - b.getX();
		final double bcY = c.getY() - b.getY();
		final double ab = sqrt(square(abX) + square(abY));
		final double bc = sqrt(square(bcX) + square(bcY));
		
		return acos(max(-1.0, min((abX * bcX + abY * bcY) / ab / bc, 1.0)));
	}
	
	private static final void smootheLinearConnections(final Manifold manifold, final Point2D[] locations, final Point2D[] newLocations, final int face) {
		manifold.forEachDartIn(FACE, face, d -> {
			if (VERTEX.countDarts(manifold, d) == 2) {
				final int previous = manifold.getPrevious(d);
				final int next = manifold.getNext(d);
				
				setAverage(newLocations[d], locations[previous], locations[d], locations[next]);
			}
			
			return true;
		});
	}
	
	private static final void restoreCorners(final Manifold manifold, final Point2D[] locations, final Point2D[] newLocations, final int face) {
		manifold.forEachDartIn(FACE, face, d -> {
			final int previous = manifold.getPrevious(d);
			final int next = manifold.getNext(d);
			
			if (locations[previous].distance(newLocations[previous]) <= 1.0E-6
					&& locations[next].distance(newLocations[next]) <= 1.0E-6) {
				newLocations[d].setLocation(locations[d]);
			}
			
			return true;
		});
	}
	
	private static final void setLocations(final Manifold manifold, final Point2D[] locations, final Point2D[] newLocations, final int face) {
		manifold.forEachDartIn(FACE, face, d -> {
			locations[d].setLocation(newLocations[d]);
			
			return true;
		});
	}
	
	public static final Point2D[] generateLocations(final int imageWidth, final int imageHeight, final Manifold manifold) {
		final Point2D[] result = new Point2D[manifold.getDartCount()];
		
		manifold.forEach(VERTEX, v -> {
			final Point2D location = getDartXY(imageWidth, imageHeight, v, new Point2D.Double());
			
			manifold.forEachDartIn(VERTEX, v, d -> {
				result[d] = location;
				
				return true;
			});
			
			return true;
		});
		
		return result;
	}
	
	public static final Manifold contoursOf(final BufferedImage image, final int quantization) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Manifold result = newGrid(imageWidth, imageHeight);
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		
		debugPrint("imageWidth:", imageWidth, "imageHeight:", imageHeight);
		debugPrint("dartCount:", result.getDartCount());
		debugPrint("Pruning...");
		
		int disconnections = 0;
		
		for (int y = 0; y < imageHeight; ++y) {
			monitor.ping("Pruning: " + y + "/" + imageHeight + "\r");
			for (int x = 0; x < imageWidth; ++x) {
				if (x + 1 < imageWidth && getRGB(image, x, y, quantization) == getRGB(image, x + 1, y, quantization)) {
//					debugPrint("Disconnecting right of (" + x + " " + y + ")");
					disconnectEdge(result, (y * imageWidth + x + 1) * 4 + LEFT_OFFSET);
					++disconnections;
				}
				if (y + 1 < imageHeight && getRGB(image, x, y, quantization) == getRGB(image, x, y + 1, quantization)) {
//					debugPrint("Disconnecting bottom of (" + x + " " + y + ")");
					disconnectEdge(result, (y * imageWidth + x) * 4 + BOTTOM_OFFSET);
					++disconnections;
				}
			}
		}
		
		debugPrint("Pruned:", disconnections);
		
		debugPrint("Checking topology...");
		assert result.isValid();
		debugPrint("Topology is OK");
		
		return result;
	}

	public static final void debugShow(final String title,
			final Manifold manifold, final Point2D[] locations, final Map<Integer, Collection<Area>> shapes) {
		final DiagramComponent diagramComponent = new DiagramComponent();
		final double scale = 100.0;
		
		if (shapes != null) {
			diagramComponent.getRenderers().add(g -> {
				for (final Map.Entry<Integer, ? extends Collection<? extends Shape>> entry : shapes.entrySet()) {
					final AffineTransform transform = g.getTransform();
					g.scale(scale, scale);
					g.setColor(new Color(entry.getKey()));
					entry.getValue().forEach(g::fill);
					g.setTransform(transform);
				}
			});
		}
		
		manifold.forEach(DART, d -> {
			final int next = manifold.getNext(d);
			
			// XXX hide thin faces
			if (!locations[d].equals(locations[manifold.getNext(next)])
					&& !locations[manifold.getPrevious(d)].equals(locations[next])) {
				final Point2D xy1 = new Point.Double(locations[d].getX() * scale, locations[d].getY() * scale);
				final Point2D xy2 = new Point2D.Double(locations[next].getX() * scale, locations[next].getY() * scale);
				
				diagramComponent.getRenderers().add(new DiagramComponent.Arrow()
				.set("source", xy1).set("target", xy2).set("curvature", -0.2 * scale)
				.set("label", Integer.toString(d)).set("color", Color.BLACK));
			}
			
			return true;
		});
		
		SwingUtilities.invokeLater(() -> SwingTools.show(diagramComponent, title, false));
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
	
	public static final Map<Integer, Collection<Area>> toRegions(final Map<Integer, Collection<Path2D>> polygons) {
		debugPrint("Creating shapes...");
		
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		final Map<Integer, Collection<Area>> result = new HashMap<>();
		final int keyCount = polygons.size();
		final int[] progress = { -1 };
		
		polygons.forEach((k, v) -> {
			monitor.ping("Creating shapes " + (++progress[0]) + "/" + keyCount + "\r");
			
			final List<Area> shapes = new ArrayList<>();
			
			for (final Shape polygon : v) {
				final Area newShape = new Area(polygon);
				final double newShapeDeterminant = SVGTools.getSurface(polygon, 1.0);
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
					final double containerDeterminant = weakProperties.get(container, "determinant");
					
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
	
	public static final Map<Integer, Collection<Path2D>> collectContours(final BufferedImage image, final int quantization,
			final Manifold manifold, final Point2D[] locations, final Map<Integer, Collection<Path2D>> result) {
		debugPrint("Collecting polygons...");
		
		final ConsoleMonitor monitor = new ConsoleMonitor(10_000L);
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Point tmp = new Point();
		
		manifold.forEach(Traversor.FACE, f -> {
			monitor.ping("Collecting polygons " + f + "/" + manifold.getDartCount() + "\r");
			
			final Path2D polygon = new Path2D.Double();
			
			manifold.forEachDartIn(Traversor.FACE, f, d -> {
				final Point2D location = locations[d];
				
				if (f == d) {
					polygon.moveTo(location.getX(), location.getY());
				} else {
					polygon.lineTo(location.getX(), location.getY());
				}
				
				return true;
			});
			
			if (2 < FACE.countDarts(manifold, f)) {
				getPixelXY(imageWidth, imageHeight, f, tmp);
				
				if (f != 1 && 0 <= tmp.x && tmp.x < imageWidth && 0 <= tmp.y && tmp.y < imageHeight) {
					weakProperties.set(polygon, "label", getRGB(image, tmp.x, tmp.y, quantization));
					weakProperties.set(polygon, "determinant", SVGTools.getSurface(polygon, 1.0));
					
					final Collection<Path2D> collection = result.computeIfAbsent(getRGB(image, tmp.x, tmp.y, quantization),
							k -> new ArrayList<>());
					collection.add(polygon);
				}
			}
			
			return true;
		});
		
		monitor.pause();
		
		debugPrint("Sorting...");
		
		result.forEach((k, v) -> ((List<Path2D>) v).sort((p1, p2) -> Double.compare(
				abs((double) weakProperties.get(p2, "determinant")), abs((double) weakProperties.get(p1, "determinant")))));
		
		debugPrint("Polygons collected");
		
		return result;
	}
	
	public static final int getRGB(final BufferedImage image, final int x, final int y, final int quantization) {
		return quantizeRGB(image.getRGB(x, y), quantization);
	}
	
	public static final int quantizeRGB(final int rgb, final int quantization) {
		return rgb & (0x01010101 * (((~1) << quantization) & 0xFF));
	}
	
	public static final void processInternalDart(final int dart, final Point2D location, final int imageWidth, final int imageHeight, final Polygon polygon) {
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
//			polygon.addPoint(x, y);
			polygon.addPoint((int) location.getX(), (int) location.getY());
		}
	}
	
	public static final Point2D getDartXY(final int imageWidth, final int imageHeight, final int dart, final Point2D result) {
		final int topBorder = imageWidth * imageHeight * 4;
		final int rightBorder = topBorder + imageWidth * 2;
		final int orientation = dart & 1;
		
		if (dart < topBorder) {
			final int side = dart & 2;
			final int pixel = dart / 4;
			
			if (side == 0) {
				// vertical
				result.setLocation(pixel % imageWidth, pixel / imageWidth + orientation);
			} else {
				// horizontal
				result.setLocation(pixel % imageWidth + orientation, pixel / imageWidth + 1);
			}
		} else if (dart < rightBorder) {
			result.setLocation((dart - topBorder) / 2 + (orientation ^ 1), 0.0);
		} else {
			result.setLocation(imageWidth, (dart - rightBorder) / 2 + (orientation ^ 1));
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
