package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.xml.XMLTools.getNode;
import static net.sourceforge.aprog.xml.XMLTools.getNodes;
import static net.sourceforge.aprog.xml.XMLTools.parse;
import static net.sourceforge.aprog.xml.XMLTools.write;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-05-28)
 */
public final class AperioXML2IMJXML {
	
	private AperioXML2IMJXML() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
//		final String[] aperioXMLIds = {
//				"../Libraries/images/svs/45656.xml"
//				, "../Libraries/images/svs/45657.xml"
//				, "../Libraries/images/svs/45659.xml"
//				, "../Libraries/images/svs/45660.xml"
//				, "../Libraries/images/svs/45662.xml"
//				, "../Libraries/images/svs/45668.xml"
//				, "../Libraries/images/svs/45683.xml"
//		};
		final File[] aperioXMLIds = new File("F:/sysimit/data/Pilot_Series_Final").listFiles(RegexFilter.newSuffixFilter("_005.xml"));
		final String author = arguments.get("author", "?");
		final boolean compress = true;
		
		for (final File aperioXMLId : aperioXMLIds) {
			debugPrint(aperioXMLId);
			
			final Document aperioXML = parse(getResourceAsStream(aperioXMLId.getPath()));
			final Element aperioRoot = aperioXML.getDocumentElement();
			final Document imjXML = parse("<annotations/>");
			final Element imjRoot  = imjXML.getDocumentElement();
			
			imjRoot.setAttribute("micronsPerPixel", aperioRoot.getAttribute("MicronsPerPixel"));
			
			final Element imjLabels = (Element) imjRoot.appendChild(imjXML.createElement("labels"));
			final Element imjRegions = (Element) imjRoot.appendChild(imjXML.createElement("regions"));
			final Map<String, Integer> labelIds = new HashMap<>();
			
			for (final Node aperioAnnotation : getNodes(aperioXML, "*//Annotation")) {
				final Element imjLabel = (Element) imjLabels.appendChild(imjXML.createElement("label"));
				final Integer labelId = labelIds.size();
				final String description = ((Element) getNode(aperioAnnotation, "*//Attribute[@Id=0]")).getAttribute("Name");
				
				labelIds.put(description, labelId);
				
				imjLabel.setAttribute("labelId", labelId.toString());
				imjLabel.setAttribute("description", description);
				imjLabel.setAttribute("lineColor", ((Element) aperioAnnotation).getAttribute("LineColor"));
				
				debugPrint(description);
				
				for (final Node aperioRegion : getNodes(aperioAnnotation, "*//Region")) {
					final Element imjRegion = (Element) imjRegions.appendChild(imjXML.createElement("region"));
					final Element imjRegionLabels = (Element) imjRegion.appendChild(imjXML.createElement("labels"));
					final Element imjRegionVertices = (Element) imjRegion.appendChild(imjXML.createElement("vertices"));
					final Element imjRegionLabel = (Element) imjRegionLabels.appendChild(imjXML.createElement("label"));
					
					imjRegionLabel.setAttribute("labelId", labelId.toString());
					imjRegionLabel.setAttribute("author", author);
					
					for (final Node aperioVertex : getNodes(aperioRegion, "*//Vertex")) {
						final Element imjVertex = (Element) imjRegionVertices.appendChild(imjXML.createElement("vertex"));
						
						imjVertex.setAttribute("x", ((Element) aperioVertex).getAttribute("X"));
						imjVertex.setAttribute("y", ((Element) aperioVertex).getAttribute("Y"));
					}
				}
			}
			
			{
				final String baseName = baseName(aperioXMLId.getName());
				
				new File(baseName).mkdir();
				
				if (compress) {
					try {
						write(imjXML, new GZIPOutputStream(new FileOutputStream(
								new File(baseName, baseName + "_annotations.xml.gz"))), 1);
					} catch (final IOException exception) {
						throw unchecked(exception);
					}
				} else {
					write(imjXML, new File(baseName, baseName + "_annotations.xml"), 1);
				}
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-05-29)
	 */
	public static final class RegexFilter implements FilenameFilter, Serializable {
		
		private final Pattern pattern;
		
		public RegexFilter(final String regex) {
			this.pattern = Pattern.compile(regex);
		}
		
		public final Pattern getPattern() {
			return this.pattern;
		}
		
		@Override
		public final boolean accept(final File directory, final String name) {
			return this.getPattern().matcher(name).matches();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 7931715226064553450L;
		
		public static final RegexFilter newPrefixFilter(final String prefix) {
			return new RegexFilter(prefix + ".*");
		}
		
		public static final RegexFilter newSuffixFilter(final String suffix) {
			return new RegexFilter(".*" + suffix);
		}
		
		public static final RegexFilter newExtensionFilter(final String extension) {
			return newSuffixFilter(".\\." + extension);
		}
		
	}

}
