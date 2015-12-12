package imj3.draft;

import imj3.core.Image2D;
import imj3.tools.IMJTools;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;
import multij.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2015-07-29)
 */
public final class PrintInfo {
	
	private PrintInfo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final String svgPath = arguments.get("svg", "");
		final String key = arguments.get("get", "");
		final String csv = arguments.get("csv", "");
		
		if (!imagePath.isEmpty()) {
			final Image2D image = IMJTools.read(imagePath);
			final Map<Object, Object> info = new LinkedHashMap<>();
			
			info.put("path", imagePath);
			info.put("width", image.getWidth());
			info.put("height", image.getHeight());
			info.put("channels", image.getChannels());
			info.put("optimalTileWidth", image.getOptimalTileWidth());
			info.put("optimalTileHeight", image.getOptimalTileHeight());
			
			print(info, csv, key);
		}
		
		if (!svgPath.isEmpty()) {
			final Document svg = SVGTools.readXML(new File(svgPath));
			final Map<Object, Object> info = new LinkedHashMap<>();
			
			info.put("path", svgPath);
			info.put("width", svg.getDocumentElement().getAttribute("width"));
			info.put("height", svg.getDocumentElement().getAttribute("height"));
			info.put("regions", XMLTools.getNodes(svg, "//*[@objectId]").size()); // XXX why is the namespace (imj:) not needed?
			
			print(info, csv, key);
		}
	}
	
	public static final void print(final Map<Object, Object> info, final String csv, final String key) {
		if (key.isEmpty()) {
			if (csv.isEmpty()) {
				info.forEach((k, v) -> System.out.println(k + ": " + v));
			} else {
				System.out.println(Tools.join(csv, info.values()));
			}
		} else {
			System.out.println(info.get(key));
		}
	}
	
}
