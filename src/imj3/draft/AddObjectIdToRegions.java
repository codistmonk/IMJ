package imj3.draft;

import static multij.tools.Tools.debugPrint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-09-18)
 */
public final class AddObjectIdToRegions {
	
	private AddObjectIdToRegions() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File svgFile = new File(arguments.get("svg", ""));
		
		try (final InputStream input = new FileInputStream(svgFile)) {
			final Document svg = XMLTools.parse(input);
			int objectId = 0;
			
			for (final Element regionElement : SVGTools.getRegionElements(svg)) {
				regionElement.setAttribute("imj:objectId", "" + (++objectId));
			}
			
			debugPrint("Added", objectId, "ids");
			
			input.close();
			
			debugPrint("Writing", svgFile);
			
			XMLTools.write(svg, svgFile, 0);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}

}
