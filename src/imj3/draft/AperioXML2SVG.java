package imj3.draft;

import static imj3.tools.CommonTools.formatColor;
import static java.lang.Math.max;
import static multij.tools.Tools.array;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.getResourceAsStream;
import static multij.xml.XMLTools.getNode;
import static multij.xml.XMLTools.getNodes;
import static multij.xml.XMLTools.parse;
import static multij.xml.XMLTools.write;

import imj3.core.Image2D;
import imj3.tools.IMJTools;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.RegexFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2014-05-28)
 */
public final class AperioXML2SVG {
	
	private AperioXML2SVG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File outputRoot = new File(arguments.get("out", root.getPath()));
		final String suffix = arguments.get("suffix", ".xml");
		final File[] aperioFiles = root.listFiles(RegexFilter.newSuffixFilter(suffix));
		final String author = arguments.get("author", "unknown");
		final String seriesId = arguments.get("series", "");
		final String sourceClassesPath = arguments.get("classes", "");
		final Document classesXML = sourceClassesPath.isEmpty() ? parse("<classes nextId=\"1\"/>") : readXML(sourceClassesPath);
		final Element classesRoot = classesXML.getDocumentElement();
		final Map<String, String> classIds = new HashMap<>();
		final int[] nextId = { Integer.decode(classesRoot.getAttribute("nextId")) };
		
		for (final Node node : getNodes(classesXML, "//class")) {
			final Element element = (Element) node;
			
			classIds.put(element.getAttribute("description"), element.getAttribute("id"));
		}
		
		for (final File aperioFile : aperioFiles) {
			debugPrint("aperioFile:", aperioFile);
			
			final Document aperioXML = parse(getResourceAsStream(aperioFile.getPath()));
			final Document svg = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:imj=\"IMJ\"/>");
			final Element svgRoot  = svg.getDocumentElement();
			
			double lastX = 0.0;
			double lastY = 0.0;
			final List<Region> regions = new ArrayList<>();
			
			{
				File imageFile = null;
				
				for (final String imageSuffix : array(".zip", ".svs")) {
					imageFile = new File(baseName(aperioFile.getName()) + imageSuffix);
					
					if (imageFile.isFile()) {
						break;
					}
					
					imageFile = null;
				}
				
				if (imageFile != null) {
					try {
						debugPrint("imageFile:", imageFile);
						final Image2D image = IMJTools.read(imageFile.getPath());
						lastX = image.getWidth() - 1;
						lastY = image.getHeight() - 1;
					} catch (final Exception exception) {
						exception.printStackTrace();
					}
				}
			}
			
			// collect_all_regions:
			for (final Node aperioAnnotation : getNodes(aperioXML, "*//Annotation")) {
				final String className = ((Element) aperioAnnotation).getAttribute("Name");
				final String classDescription = ((Element) getNode(aperioAnnotation, "*//Attribute")).getAttribute("Name");
				final String color = formatColor(Long.decode(((Element) aperioAnnotation).getAttribute("LineColor")));
				final String classId = classIds.computeIfAbsent(classDescription, d -> {
					final Element classElement = (Element) classesRoot.appendChild(classesXML.createElement("class"));
					final String result = Integer.toString(nextId[0]++);
					
					classElement.setAttribute("id", result);
					classElement.setAttribute("name", className);
					classElement.setAttribute("description", classDescription);
					classElement.setAttribute("preferredColor", color);
					
					return result;
				});
				
				// collect_class_regions:
				for (final Node aperioRegion : getNodes(aperioAnnotation, "*//Region")) {
					final StringBuilder points = new StringBuilder();
					boolean prependSpace = false;
					final Path2D region = new Path2D.Double();
					boolean regionIsEmpty = true;
					final double size = Double.parseDouble(((Element) aperioRegion).getAttribute("Area"));
					
					region.setWindingRule(Path2D.WIND_EVEN_ODD);
					
					for (final Node aperioVertex : getNodes(aperioRegion, "*//Vertex")) {
						if (prependSpace) {
							points.append(' ');
						} else {
							prependSpace = true;
						}
						
						final String xAsString = ((Element) aperioVertex).getAttribute("X");
						final String yAsString = ((Element) aperioVertex).getAttribute("Y");
						
						points.append(xAsString).append(',').append(yAsString);
						final double x = Double.parseDouble(xAsString);
						final double y = Double.parseDouble(yAsString);
						lastX = max(lastX, x);
						lastY = max(lastY, y);
						
						if (regionIsEmpty) {
							region.moveTo(x, y);
							regionIsEmpty = false;
						} else {
							region.lineTo(x, y);
						}
					}
					
					region.closePath();
					regions.add(new Region(classId, new Area(region), size, new Color(Long.decode(color).intValue())));
				}
			}
			
			// remove_small_regions_from_large_regions:
			{
				regions.sort(new Comparator<Region>() {
					
					@Override
					public final int compare(final Region r1, final Region r2) {
						return Double.compare(r2.getSize(), r1.getSize());
					}
					
				});
				
				for (int i = 0; i < regions.size(); ++i) {
					final Region regionI = regions.get(i);
					
					for (int j = i + 1; j < regions.size(); ++j) {
						final Region regionJ = regions.get(j);
						final Area tmp = new Area(regionJ.getArea());
						
						tmp.subtract(regionI.getArea());
						regionI.getArea().subtract(regionJ.getArea());
						
						if (tmp.isEmpty() && regionI.getClassId().equals(regionJ.getClassId())) {
							debugPrint("Region", j, " makes a hole in region", i);
							
							regions.remove(j--);
						}
					}
				}
			}
			
			// create_SVG_paths_from_regions:
			for (final Region region : regions) {
				final PathIterator pathIterator = region.getArea().getPathIterator(new AffineTransform());
				int segmentType;
				final double[] segment = new double[6];
				final StringBuilder pathData = new StringBuilder();
				
				while (!pathIterator.isDone()) {
					switch (segmentType = pathIterator.currentSegment(segment)) {
					case PathIterator.SEG_MOVETO:
						pathData.append('M').append(segment[0]).append(',').append(segment[1]);
						break;
					case PathIterator.SEG_LINETO:
						pathData.append('L').append(segment[0]).append(',').append(segment[1]);
						break;
					case PathIterator.SEG_CLOSE:
						pathData.append('Z');
						break;
					default:
						throw new UnsupportedOperationException("segmentType: " + segmentType);
					}
					
					pathIterator.next();
				}
				
				final Element svgRegion = (Element) svgRoot.appendChild(svg.createElement("path"));
				
				svgRegion.setAttribute("d", pathData.toString());
				svgRegion.setAttribute("style", "fill:" + formatColor(region.getColor().getRGB()));
				svgRegion.setAttribute("imj:classId", region.getClassId());
			}
			
			svgRoot.setAttribute("width", Integer.toString((int) lastX + 1));
			svgRoot.setAttribute("height", Integer.toString((int) lastY + 1));
			
			long fileTime = 0L;
			
			try {
				fileTime = Files.readAttributes(aperioFile.toPath(), BasicFileAttributes.class).creationTime().toMillis();
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
			
			if (fileTime == 0L) {
				fileTime = aperioFile.lastModified();
			}
			
			final File outputFile = new File(outputRoot, new File(baseName(aperioFile.getPath()) + "_" + author
					+ "_" + getOrDefault(seriesId, new SimpleDateFormat("yyyyMMddHHmmss").format(fileTime)) + ".svg").getName());
			
			debugPrint("Writing", outputFile);
			
			try (final OutputStream output = new FileOutputStream(outputFile)) {
				write(svg, output, 1);
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
		}
		
		classesXML.getDocumentElement().setAttribute("nextId", "" + nextId[0]);
		
		final File classesFile = new File(outputRoot, "classes.xml");
		
		debugPrint("Writing", classesFile);
		
		try (final OutputStream output = new FileOutputStream(classesFile)) {
			write(classesXML, output, 1);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final Document readXML(final String path) {
		try (final InputStream input = new FileInputStream(path)) {
			return parse(input);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final String getOrDefault(final String string, final String resultIfStringIsNullOrEmpty) {
		return string != null && !string.isEmpty() ? string : resultIfStringIsNullOrEmpty;
	}
	
	/**
	 * @author codistmonk (creation 2015-06-05)
	 */
	public static final class Region implements Serializable {
		
		private final String classId;
		
		private final Area area;
		
		private final double size;
		
		private final Color color;
		
		public Region(final String classId, final Area area, final double size, final Color color) {
			this.classId = classId;
			this.area = area;
			this.size = size;
			this.color = color;
		}
		
		public final String getClassId() {
			return this.classId;
		}
		
		public final Area getArea() {
			return this.area;
		}
		
		public final double getSize() {
			return this.size;
		}
		
		public final Color getColor() {
			return this.color;
		}
		
		private static final long serialVersionUID = -8706782430625804921L;
		
	}
	
}
