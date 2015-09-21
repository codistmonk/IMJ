package imj3.draft;

import static java.lang.Math.sqrt;
import static multij.tools.MathTools.square;
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
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2015-07-21)
 */
public final class SVGTools {
	
	private SVGTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final String COMMANDS = "MmLlQqCcZz";
	
	public static final String NUMERICAL = "0-9.-";
	
	public static final Pattern COMMAND_PATTERN = Pattern.compile("[" + COMMANDS + "]");
	
	public static final String NUMBER_PATTERN = "[" + NUMERICAL + "]+";
	
	private static final AffineTransform IDENTITY = new AffineTransform();
	
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
			ignore(exception);
			
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
		return getRegions(svg, null);
	}
	
	public static final Map<String, List<Area>> getRegions(final Document svg, final Map<Object, Object> regionElements) {
		final Map<String, List<Area>> result = new TreeMap<>();
		
		for (final Element regionElement : getRegionElements(svg)) {
			final String classId = regionElement.getAttribute("imj:classId");
			final Area region = newRegion(regionElement);
			
			result.computeIfAbsent(classId, k -> new ArrayList<>()).add(region);
			
			if (regionElements != null) {
				regionElements.put(region, regionElement);
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static final List<Element> getRegionElements(final Document svg) {
		return (List<Element>) (Object) getNodes(svg, "//path|//polygon");
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
	
	public static final double getSurface(final Shape shape, final double flatness) {
		return getSurface(shape, IDENTITY, flatness);
	}
	
	public static final double getSurface(final Shape shape, final AffineTransform transform, final double flatness) {
		return getMeasurement(shape, transform, flatness,
				(previous, current) -> previous[0] * current[1] - previous[1] * current[0]) / 2.0;
	}
	
	public static final double getPerimeter(final Shape shape, final double flatness) {
		return getPerimeter(shape, IDENTITY, flatness);
	}
	
	public static final double getPerimeter(final Shape shape, final AffineTransform transform, final double flatness) {
		return getMeasurement(shape, transform, flatness,
				(previous, current) -> sqrt(square(current[0] - previous[0]) + square(current[0] - previous[0])));
	}
	
	public static final double getMeasurement(final Shape shape, final AffineTransform transform, final double flatness,
			final BiFunction<double[], double[], Double> getDelta) {
		final PathIterator pathIterator = shape.getPathIterator(transform, flatness);
		final double[] first = new double[2];
		final double[] previous = new double[2];
		final double[] current = new double[2];
		double result = 0.0;
		
		while (!pathIterator.isDone()) {
			switch (pathIterator.currentSegment(current)) {
			case PathIterator.SEG_CLOSE:
				result += getDelta.apply(previous, first);
				break;
			case PathIterator.SEG_LINETO:
				result += getDelta.apply(previous, current);
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
		
		return result;
	}
	
	public static final Area newRegion(final Element svgShape) {
		final Path2D path = new Path2D.Double();
		
		path.setWindingRule(Path2D.WIND_EVEN_ODD);
		
		if ("polygon".equals(svgShape.getTagName())) {
			boolean first = true;
			
			try (final Scanner scanner = new Scanner(svgShape.getAttribute("points"))) {
				scanner.useLocale(Locale.ENGLISH);
				scanner.useDelimiter("[^0-9.-]");
				
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
			try (final Scanner scanner = new Scanner(svgShape.getAttribute("d"))) {
				scanner.useLocale(Locale.ENGLISH);
				
				double firstX = 0.0;
				double firstY = 0.0;
				double lastX = 0.0;
				double lastY = 0.0;
				String command = nextCommand(scanner, "M");
				
				while (scanner.hasNext()) {
					final boolean relative = Character.isLowerCase(command.charAt(0));
					
					if ("zZ".contains(command)) {
						path.closePath();
						lastX = firstX;
						lastY = firstY;
					} else if ("mM".contains(command)) {
						final double x = nextDouble(scanner, relative, lastX);
						final double y = nextDouble(scanner, relative, lastY);
						
						path.moveTo(x, y);
						command = relative ? "l" : "L";
						
						lastX = firstX = x;
						lastY = firstY = y;
					} else if ("lL".contains(command)) {
						final double x = nextDouble(scanner, relative, lastX);
						final double y = nextDouble(scanner, relative, lastY);
						
						path.lineTo(x, y);
						
						lastX = x;
						lastY = y;
					} else if ("qQ".contains(command)) {
						final double x1 = nextDouble(scanner, relative, lastX);
						final double y1 = nextDouble(scanner, relative, lastY);
						final double x = nextDouble(scanner, relative, x1);
						final double y = nextDouble(scanner, relative, y1);
						
						path.quadTo(x1, y1, x, y);
						
						lastX = x;
						lastY = y;
					} else if ("cC".contains(command)) {
						final double x1 = nextDouble(scanner, relative, lastX);
						final double y1 = nextDouble(scanner, relative, lastY);
						final double x2 = nextDouble(scanner, relative, x1);
						final double y2 = nextDouble(scanner, relative, y1);
						final double x = nextDouble(scanner, relative, x2);
						final double y = nextDouble(scanner, relative, y2);
						
						path.curveTo(x1, y1, x2, y2, x, y);
						
						lastX = x;
						lastY = y;
					}
					
					command = nextCommand(scanner, command);
				}
			}
		}
		
		return new Area(path);
	}
	
	public static final String nextCommand(final Scanner scanner, final String oldCommand) {
		scanner.useDelimiter(" *");
		
		final String result = scanner.hasNext(COMMAND_PATTERN) ? scanner.next(COMMAND_PATTERN) : oldCommand;
		
		scanner.useDelimiter("[^" + NUMERICAL + "]+");
		
		return result;
	}
	
	public static final double nextDouble(final Scanner scanner, final boolean relative, final double last) {
		return scanner.nextDouble() + (relative ? last : 0.0);
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
	
}
