package imj2.draft;

import static multij.xml.XMLTools.parse;
import imj2.tools.SplitImage;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2014-01-23)
 */
public final class ConvertDatabaseFromV1ToV2 {
	
	private ConvertDatabaseFromV1ToV2() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String rootPath = arguments.get("path", "");
		final Document databaseV1 = FixDatabaseV1.getDatabaseV1(rootPath);
		final List<Element> imageNodes = (List<Element>) (Object) XMLTools.getNodes(databaseV1, "//image");
		final Document databaseV2 = parse("<images/>");
		final boolean writeMetadata = arguments.get("writeMetadata", 1)[0] != 0;
		String imageDirectory = null;
		Document metadata = null;
		
		System.out.println(imageNodes.size() + " images referenced");
		
		for (final Element image : imageNodes) {
			final String id = image.getAttribute("id");
			
			if (id.endsWith("_lod0")) {
				if (imageDirectory != null && writeMetadata) {
					System.out.println("Writing " + imageDirectory + "/metadata.xml");
					XMLTools.write(metadata, SplitImage.getOutputStream(imageDirectory + "/metadata.xml"), 0);
				}
				
				System.out.println("Processing " + id);
				
				imageDirectory = rootPath + "/" + id.substring(0, id.lastIndexOf('/'));
				metadata = parse("<metadata/>");
				
				final String idV2 = id.substring(0, id.lastIndexOf('/'));
				final Element imageV2 = databaseV2.createElement("image");
				
				imageV2.setAttribute("id", idV2);
				
				databaseV2.getDocumentElement().appendChild(imageV2);
			}
			
			if (metadata == null) {
				throw new IllegalStateException();
			}
			
			final Element metadataForImage = (Element) metadata.adoptNode(image.cloneNode(true));
			
			metadataForImage.setAttribute("id", id.substring(id.lastIndexOf('/') + 1));
			metadata.getDocumentElement().appendChild(metadataForImage);
		}
		
		if (imageDirectory != null && writeMetadata) {
			System.out.println("Writing " + imageDirectory + "/metadata.xml");
			XMLTools.write(metadata, SplitImage.getOutputStream(imageDirectory + "/metadata.xml"), 0);
		}
		
		System.out.println("Writing " + rootPath + "/images.xml");
		XMLTools.write(databaseV2, SplitImage.getOutputStream(rootPath + "/images.xml"), 0);
		
		System.out.println("All done");
		System.exit(0);
	}
	
}
