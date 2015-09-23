package imj3.draft;

import static multij.tools.Tools.debugPrint;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import imj3.core.Image2D;
import imj3.tools.IMJTools;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-09-23)
 */
public final class AddDimensionsToSVG {
	
	private AddDimensionsToSVG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File svgFile = new File(arguments.get("svg", ""));
		final String imagePath = arguments.get("image", "");
		
		debugPrint("svg:", svgFile);
		debugPrint("image:", imagePath);
		
		final Image2D image = IMJTools.read(imagePath);
		
		debugPrint("width:", image.getWidth(), "height:", image.getHeight());
		
		final Document svg = SVGTools.readXML(svgFile);
		final Element svgRoot = svg.getDocumentElement();
		
		svgRoot.setAttribute("width", "" + image.getWidth());
		svgRoot.setAttribute("height", "" + image.getHeight());
		
		debugPrint("Writing", svgFile);
		
		XMLTools.write(svg, svgFile, 0);
	}
	
}
