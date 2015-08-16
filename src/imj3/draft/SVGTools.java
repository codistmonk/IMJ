package imj3.draft;

import static multij.tools.Tools.*;
import static multij.xml.XMLTools.getNodes;
import static multij.xml.XMLTools.parse;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-21)
 */
public final class SVGTools {
	
	private SVGTools() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File svgFile = new File(arguments.get("svg", ""));
		final Document svg = readXML(svgFile);
		final Map<String, List<Area>> regions = getRegions(svg);
		final int lod = arguments.get("lod", 0)[0];
		final int x = arguments.get("x", 6000)[0];
		final int y = arguments.get("y", 12000)[0];
		final int w = arguments.get("w", 512)[0];
		final int h = arguments.get("h", 512)[0];
		final int x0 = x << lod;
		final int y0 = y << lod;
		final int w0 = w << lod;
		final int h0 = h << lod;
		
		{
			final Map<String, Double> classRatios = normalize(getClassSurfaces(new Rectangle(x0, y0, w0, h0), regions));
			
			debugPrint(classRatios);
		}
	}
	
	public static final Document readXML(final File file) {
		try (final InputStream input = new FileInputStream(file)) {
			return parse(input);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final Document readXMLOrNull(final File file) {
		try {
			return readXML(file);
		} catch (final Exception exception) {
			return null;
		}
	}
	
	public static final <K> Map<K, Double> normalize(final Map<K, Double> map) {
		final double[] total = { 0.0 };
		
		map.values().forEach(v -> total[0] += v);
		
		return normalize(map, total[0]);
	}
	
	public static final <K> Map<K, Double> normalize(final Map<K, Double> map, final double total) {
		map.replaceAll((k, v) -> v / total);
		
		return map;
	}
	
	public static final Map<String, List<Area>> getRegions(final Document svg) {
		final Map<String, List<Area>> result = new TreeMap<>();
		
		for (final Node regionNode : getNodes(svg, "//path|//polygon")) {
			final Element regionElement = (Element) regionNode;
			final String classId = regionElement.getAttribute("imj:classId");
			
			result.computeIfAbsent(classId, k -> new ArrayList<>()).add(newRegion(regionElement));
		}
		
		return result;
	}
	
	public static final Map<String, Double> getClassSurfaces(final Shape shape, final Map<String, List<Area>> regions) {
		final Map<String, Double> result = new TreeMap<>();
		final double flatness = 1.0;
		
		for (final Map.Entry<String, List<Area>> entry : regions.entrySet()) {
			final String classId = entry.getKey();
			
			for (final Area region : entry.getValue()) {
				final Area intersection = new Area(shape);
				
				intersection.intersect(region);
				
				if (!intersection.isEmpty()) {
					result.compute(classId, (k, v) -> (v == null ? 0.0 : v) + getSurface(intersection, flatness));
				}
			}
		}
		
		return result;
	}

	public static double getSurface(final Shape shape, final double flatness) {
		final double[] surface = { 0.0 };
		final PathIterator pathIterator = shape.getPathIterator(new AffineTransform(), flatness);
		final double[] first = new double[2];
		final double[] previous = new double[2];
		final double[] current = new double[2];
		
		while (!pathIterator.isDone()) {
			switch (pathIterator.currentSegment(current)) {
			case PathIterator.SEG_CLOSE:
				surface[0] -= previous[0] * first[1] - previous[1] * first[0];
				break;
			case PathIterator.SEG_LINETO:
				surface[0] -= previous[0] * current[1] - previous[1] * current[0];
				System.arraycopy(current, 0, previous, 0, 2);
				break;
			case PathIterator.SEG_MOVETO:
				System.arraycopy(current, 0, first, 0, 2);
				System.arraycopy(current, 0, previous, 0, 2);
				break;
			case PathIterator.SEG_CUBICTO:
			case PathIterator.SEG_QUADTO:
				throw new IllegalStateException();
			}
			
			pathIterator.next();
		}
		
		surface[0] /= 2.0;
		
		final double s = surface[0];
		return s;
	}
	
	public static final Area newRegion(final Element svgShape) {
		final Path2D path = new Path2D.Double();
		
		path.setWindingRule(Path2D.WIND_EVEN_ODD);
		
		if ("polygon".equals(svgShape.getTagName())) {
			boolean first = true;
			
			try (final Scanner scanner = new Scanner(svgShape.getAttribute("points"))) {
				scanner.useLocale(Locale.ENGLISH);
				scanner.useDelimiter("[^0-9.]");
				
				while (scanner.hasNext()) {
					final double x = scanner.nextDouble();
					final double y = scanner.nextDouble();
					
					if (first) {
						first = false;
						path.moveTo(x, y);
					} else {
						path.lineTo(x, y);
					}
				}
			}
			
			path.closePath();
		} else if ("path".equals(svgShape.getTagName())) {
			String pathElement = "m";
			final double[] buffer = new double[8];
			int i = 2;
			SVGPathDataParserState state = SVGPathDataParserState.READ_COMMAND;
			
			try (final Scanner scanner = new Scanner(svgShape.getAttribute("d"))) {
				scanner.useLocale(Locale.ENGLISH);
				
				while (scanner.hasNext()) {
					switch (state) {
					case READ_COMMAND:
						scanner.useDelimiter(" *");
						
						final String command = scanner.next("[MmLlQqCcZz]");
						
						switch (command) {
						case "M":
						case "m":
						case "L":
						case "l":
						case "Q":
						case "q":
						case "C":
						case "c":
							pathElement = command;
						case "":
							state = SVGPathDataParserState.READ_NUMBER;
							break;
						case "Z":
						case "z":
							path.closePath();
						}
						break;
					case READ_NUMBER:
						scanner.useDelimiter("[^0-9.-]");
						String s = scanner.next();
						
						while (s.isEmpty()) {
							s = scanner.next();
						}
						
						buffer[i] = Double.parseDouble(s);
						boolean changeState = true;
						
						switch (++i) {
						case 4:
							switch (pathElement) {
							case "m":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
							case "M":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
								path.moveTo(buffer[2], buffer[3]);
								pathElement = "m".equals(pathElement) ? "l" : "L";
								scanner.useDelimiter(" *");
								changeState = scanner.hasNext("[MmLlQqCcZz]");
								scanner.useDelimiter("[^0-9.-]");
								if (!changeState) {
									System.arraycopy(buffer, 2, buffer, 0, 2);
									i = 2;
								}
								break;
							case "l":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
							case "L":
								path.lineTo(buffer[2], buffer[3]);
								scanner.useDelimiter(" *");
								changeState = scanner.hasNext("[MmLlQqCcZz]");
								scanner.useDelimiter("[^0-9.-]");
								if (!changeState) {
									System.arraycopy(buffer, 2, buffer, 0, 2);
									i = 2;
								}
								break;
							case "q":
							case "c":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
							case "Q":
							case "C":
								changeState = false;
								break;
							default:
								throw new UnsupportedOperationException(pathElement);
							}
							
							if (changeState) {
								System.arraycopy(buffer, 2, buffer, 0, 2);
								i = 2;
								state = SVGPathDataParserState.READ_COMMAND;
							}
							
							break;
						case 6:
							switch (pathElement) {
							case "q":
								buffer[4] += buffer[0];
								buffer[5] += buffer[1];
							case "Q":
								path.quadTo(buffer[2], buffer[3], buffer[4], buffer[5]);
								break;
							case "c":
								buffer[4] += buffer[0];
								buffer[5] += buffer[1];
							case "C":
								changeState = false;
								break;
							default:
								throw new UnsupportedOperationException(pathElement);
							}
							
							if (changeState) {
								System.arraycopy(buffer, 4, buffer, 0, 2);
								i = 2;
								state = SVGPathDataParserState.READ_COMMAND;
							}
							break;
						case 8:
							switch (pathElement) {
							case "c":
								buffer[6] += buffer[0];
								buffer[7] += buffer[1];
							case "C":
								path.curveTo(buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]);
								break;
							default:
								throw new UnsupportedOperationException(pathElement);
							}
							
							System.arraycopy(buffer, 6, buffer, 0, 2);
							i = 2;
							state = SVGPathDataParserState.READ_COMMAND;
							break;
						}
					}
				}
			}
		}
		
		return new Area(path);
	}
	
	/**
	 * @author codistmonk (creation 2015-06-05)
	 */
	public static final class Region implements Serializable {
		
		private final int label;
		
		private final Rectangle bounds;
		
		private final BufferedImage mask;
		
		public Region(final int label, final Rectangle bounds, final BufferedImage mask) {
			this.label = label;
			this.bounds = bounds;
			this.mask = mask;
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		public final BufferedImage getMask() {
			return this.mask;
		}
		
		public final long getSize() {
			final int width = this.getMask().getWidth();
			final int height = this.getMask().getHeight();
			long result = 0L;
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if ((this.getMask().getRGB(x, y) & 1) != 0) {
						++result;
					}
				}
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 3168354082192346491L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-06-05)
	 */
	private static enum SVGPathDataParserState {
		
		READ_COMMAND, READ_NUMBER;
		
	}
	
}
