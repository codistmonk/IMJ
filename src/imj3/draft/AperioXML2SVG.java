package imj3.draft;

import static java.lang.Math.max;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.getResourceAsStream;
import static multij.xml.XMLTools.getNode;
import static multij.xml.XMLTools.getNodes;
import static multij.xml.XMLTools.parse;
import static multij.xml.XMLTools.write;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
		final String suffix = arguments.get("suffix", ".xml");
		final File[] aperioXMLFiles = root.listFiles(RegexFilter.newSuffixFilter(suffix));
		final String author = arguments.get("author", "unknown");
		final Document classesXML = parse("<classes/>");
		final Element classesRoot = classesXML.getDocumentElement();
		final Map<String, String> classIds = new HashMap<>();
		
		for (final File aperioXMLFile : aperioXMLFiles) {
			debugPrint(aperioXMLFile);
			
			final Document aperioXML = parse(getResourceAsStream(aperioXMLFile.getPath()));
			final Document svg = parse("<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
			final Element svgRoot  = svg.getDocumentElement();
			double lastX = 0.0;
			double lastY = 0.0;
			
			for (final Node aperioAnnotation : getNodes(aperioXML, "*//Annotation")) {
				final String className = ((Element) aperioAnnotation).getAttribute("Name");
				final String classDescription = ((Element) getNode(aperioAnnotation, "*//Attribute[@Id=0]")).getAttribute("Name");
				final String color = "#" + String.format("%06X", Long.decode(((Element) aperioAnnotation).getAttribute("LineColor")));
				final String classId = classIds.computeIfAbsent(classDescription, d -> {
					final String result = Integer.toString(classIds.size() + 1);
					final Element classElement = (Element) classesRoot.appendChild(classesXML.createElement("class"));
					
					classElement.setAttribute("id", result);
					classElement.setAttribute("name", className);
					classElement.setAttribute("description", classDescription);
					classElement.setAttribute("preferredColor", color);
					
					return result;
				});
				
				for (final Node aperioRegion : getNodes(aperioAnnotation, "*//Region")) {
					final Element svgRegion = (Element) svgRoot.appendChild(svg.createElement("polygon"));
					final StringBuilder points = new StringBuilder();
					boolean prependSpace = false;
					
					svgRegion.setAttribute("classId", classId);
					
					for (final Node aperioVertex : getNodes(aperioRegion, "*//Vertex")) {
						if (prependSpace) {
							points.append(' ');
						} else {
							prependSpace = true;
						}
						
						final String xAsString = ((Element) aperioVertex).getAttribute("X");
						final String yAsString = ((Element) aperioVertex).getAttribute("Y");
						
						points.append(xAsString).append(',').append(yAsString);
						lastX = max(lastX, Double.parseDouble(xAsString));
						lastY = max(lastY, Double.parseDouble(yAsString));
					}
					
					svgRegion.setAttribute("points", points.toString());
					svgRegion.setAttribute("style", "fill:" + color);
				}
			}
			
			svgRoot.setAttribute("width", Integer.toString((int) lastX + 1));
			svgRoot.setAttribute("height", Integer.toString((int) lastY + 1));
			
			long fileTime = 0L;
			
			try {
				fileTime = Files.readAttributes(aperioXMLFile.toPath(), BasicFileAttributes.class).creationTime().toMillis();
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
			
			if (fileTime == 0L) {
				fileTime = aperioXMLFile.lastModified();
			}
			
			final String outputFilePath = baseName(aperioXMLFile.getPath()) + "_" + author
					+ "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(fileTime) + ".svg";
			
			try (final OutputStream output = new FileOutputStream(outputFilePath)) {
				write(svg, output, 1);
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
		}
		
		try (final OutputStream output = new FileOutputStream(new File(root, "classes.xml"))) {
			write(classesXML, output, 1);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
}
