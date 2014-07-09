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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
		final File root = new File(arguments.get("root", ""));
		final String suffix = arguments.get("suffix", ".xml");
		final File[] aperioXMLFiles = root.listFiles(RegexFilter.newSuffixFilter(suffix));
		final String author = arguments.get("author", "?");
		final boolean compress = true;
		
		for (final File aperioXMLFile : aperioXMLFiles) {
			debugPrint(aperioXMLFile);
			
			final Document aperioXML = parse(getResourceAsStream(aperioXMLFile.getPath()));
			final Element aperioRoot = aperioXML.getDocumentElement();
			final Document imjXML = parse("<annotations/>");
			final Element imjRoot  = imjXML.getDocumentElement();
			
			imjRoot.setAttribute("micronsPerPixel", aperioRoot.getAttribute("MicronsPerPixel"));
			
			final Element imjLabels = (Element) imjRoot.appendChild(imjXML.createElement("labels"));
			final Element imjRegions = (Element) imjRoot.appendChild(imjXML.createElement("regions"));
			final Map<String, String> labelIds = new HashMap<>();
			
			for (final Node aperioAnnotation : getNodes(aperioXML, "*//Annotation")) {
				final Element imjLabel = (Element) imjLabels.appendChild(imjXML.createElement("label"));
				final String mnemonic = ((Element) aperioAnnotation).getAttribute("Name");
				final String labelId = Integer.toString(labelIds.size());
				final String description = ((Element) getNode(aperioAnnotation, "*//Attribute[@Id=0]")).getAttribute("Name");
				
				labelIds.put(description, labelId);
				
				imjLabel.setAttribute("labelId", labelId);
				imjLabel.setAttribute("mnemonic", mnemonic);
				imjLabel.setAttribute("description", description);
				imjLabel.setAttribute("lineColor", ((Element) aperioAnnotation).getAttribute("LineColor"));
				
				debugPrint(labelId, mnemonic, description);
				
				for (final Node aperioRegion : getNodes(aperioAnnotation, "*//Region")) {
					final Element imjRegion = (Element) imjRegions.appendChild(imjXML.createElement("region"));
					final Element imjRegionLabels = (Element) imjRegion.appendChild(imjXML.createElement("labels"));
					final Element imjRegionVertices = (Element) imjRegion.appendChild(imjXML.createElement("vertices"));
					final Element imjRegionLabel = (Element) imjRegionLabels.appendChild(imjXML.createElement("label"));
					
					imjRegionLabel.setAttribute("labelId", labelId);
					imjRegionLabel.setAttribute("author", author);
					
					for (final Node aperioVertex : getNodes(aperioRegion, "*//Vertex")) {
						final Element imjVertex = (Element) imjRegionVertices.appendChild(imjXML.createElement("vertex"));
						
						imjVertex.setAttribute("x", ((Element) aperioVertex).getAttribute("X"));
						imjVertex.setAttribute("y", ((Element) aperioVertex).getAttribute("Y"));
					}
				}
			}
			
			{
				final String baseName = baseName(aperioXMLFile.getName());
				
				new File(baseName).mkdir();
				
				if (compress) {
					try {
						final GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(
								new File(baseName, baseName + "_annotations.xml.gz")));
						write(imjXML, output, 1);
						output.close();
					} catch (final IOException exception) {
						throw unchecked(exception);
					}
				} else {
					write(imjXML, new File(baseName, baseName + "_annotations.xml"), 1);
				}
			}
		}
	}
	
}
