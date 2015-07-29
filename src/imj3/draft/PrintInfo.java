package imj3.draft;

import java.util.LinkedHashMap;
import java.util.Map;

import imj3.core.Image2D;
import imj3.tools.IMJTools;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

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
		final Image2D image = IMJTools.read(imagePath);
		final String key = arguments.get("get", "");
		final Map<Object, Object> info = new LinkedHashMap<>();
		
		info.put("path", imagePath);
		info.put("width", image.getWidth());
		info.put("height", image.getHeight());
		info.put("channels", image.getChannels());
		info.put("optimalTileWidth", image.getOptimalTileWidth());
		info.put("optimalTileHeight", image.getOptimalTileHeight());
		
		if (key.isEmpty()) {
			info.forEach((k, v) -> System.out.println(k + ": " + v));
		} else {
			System.out.println(info.get(key));
		}
	}

}
