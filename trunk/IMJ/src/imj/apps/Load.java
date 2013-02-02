package imj.apps;

import imj.Image;
import imj.ImageWrangler;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-02)
 */
public final class Load {
	
	private Load() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length != 2) {
			System.out.println("Arguments: file <imageId>");
			
			return;
		}
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		
		int lod = 0;
		Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		
		while (1 < image.getRowCount() && 1 < image.getColumnCount()) {
			image = ImageWrangler.INSTANCE.load(imageId, ++lod);
		}
	}
	
}
