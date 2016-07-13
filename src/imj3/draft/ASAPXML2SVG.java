package imj3.draft;

import static imj3.draft.AperioXML2SVG.*;
import static imj3.draft.SVGTools.*;
import static imj3.tools.CommonTools.formatColor;
import static java.lang.Math.max;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.*;

import imj3.core.Image2D;
import imj3.draft.AperioXML2SVG.Region;
import imj3.tools.IMJTools;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
 * @author codistmonk (creation 2016-07-13)
 */
public final class ASAPXML2SVG {
	
	private ASAPXML2SVG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File outputRoot = new File(arguments.get("out", root.getPath()));
		final String suffix = arguments.get("suffix", ".xml");
		final File[] files = root.listFiles(RegexFilter.newSuffixFilter(suffix));
		final String author = arguments.get("author", "unknown");
		final String seriesId = arguments.get("series", "");
		final String sourceClassesPath = arguments.get("classes", "");
		final Document classesXML = sourceClassesPath.isEmpty() ? parse("<classes nextId=\"1\"/>") : SVGTools.readXML(new File(sourceClassesPath));
		final Element classesRoot = classesXML.getDocumentElement();
		final Map<String, String> classIds = new HashMap<>();
		final int[] nextId = { Integer.decode(classesRoot.getAttribute("nextId")) };
		
		for (final Node node : getNodes(classesXML, "//class")) {
			final Element element = (Element) node;
			
			classIds.put(element.getAttribute("name") + ":" + element.getAttribute("description"), element.getAttribute("id"));
		}
		
		for (final File file : files) {
			debugPrint("file:", file);
			
			final Document asapXML = parse(getResourceAsStream(file.getPath()));
			final Document svg = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:imj=\"IMJ\"/>");
			final Element svgRoot  = svg.getDocumentElement();
			
			double lastX = 0.0;
			double lastY = 0.0;
			final List<Region> regions = new ArrayList<>();
			
			{
				File imageFile = null;
				
				for (final String imageSuffix : array(".zip", ".svs")) {
					imageFile = new File(baseName(file.getName()) + imageSuffix);
					
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
			for (final Element asapRegion : getElements(asapXML, "//Annotation")) {
				final String className = "Tumor";
				final String classDescription = className;
				final String color = formatColor(Long.decode(asapRegion.getAttribute("Color")));
				final String key = className + ":" + classDescription;
				
				final String classId = classIds.computeIfAbsent(key, d -> {
					final Element classElement = (Element) classesRoot.appendChild(classesXML.createElement("class"));
					final String result = Integer.toString(nextId[0]++);
					
					classElement.setAttribute("id", result);
					classElement.setAttribute("name", className);
					classElement.setAttribute("description", classDescription);
					classElement.setAttribute("preferredColor", color);
					
					debugPrint(result, className, classDescription, color);
					
					return result;
				});
				
				{
					final StringBuilder points = new StringBuilder();
					boolean prependSpace = false;
					final Path2D region = new Path2D.Double();
					boolean regionIsEmpty = true;
					
					region.setWindingRule(Path2D.WIND_EVEN_ODD);
					
					for (final Node aperioVertex : getNodes(asapRegion, "*//Coordinate")) {
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
					double size = 0.0;
					
					try {
						size = Double.parseDouble(asapRegion.getAttribute("Area"));
					} catch (final NumberFormatException exception) {
						ignore(exception);
						
						size = getSurface(region, 1.0);
					}
					
					regions.add(new Region(classId, new Area(region), size, new Color(Long.decode(color).intValue())));
				}
			}
			
			removeSmallRegionsFromLargeRegions(regions);
			
			addPaths(regions, svgRoot);
			
			svgRoot.setAttribute("width", Integer.toString((int) lastX + 1));
			svgRoot.setAttribute("height", Integer.toString((int) lastY + 1));
			
			long fileTime = 0L;
			
			try {
				fileTime = Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
			
			if (fileTime == 0L) {
				fileTime = file.lastModified();
			}
			
			final File outputFile = new File(outputRoot, new File(baseName(file.getPath()) + "_" + author
					+ "_" + AperioXML2SVG.getOrDefault(seriesId, new SimpleDateFormat("yyyyMMddHHmmss").format(fileTime)) + ".svg").getName());
			
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
	
}
