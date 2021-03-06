package imj.apps;

import static multij.tools.Tools.usedMemory;
import imj.IMJTools;
import imj.Image;
import imj.ImageWrangler;

import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2013-02-02)
 */
public class Convert {
	
	private Convert() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length % 2 != 0 || 6 < commandLineArguments.length) {
			System.out.println("Arguments: file <imageId> to <(pgm|ppm)> [lod <(N|*)>]");
			System.out.println("Default for lod is *");
			
			return;
		}
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final String outFormat = arguments.get("to", "pgm").toLowerCase(Locale.ENGLISH);
		final int[] lods = arguments.get("lod");
		
		if (lods.length == 0) {
			int lod = 0;
			Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			write(image, imageId, outFormat, lod);
			
			while (1 < image.getRowCount() && 1 < image.getColumnCount()) {
				image = ImageWrangler.INSTANCE.load(imageId, ++lod);
				write(image, imageId, outFormat, lod);
			}
		} else {
			for (final int lod : lods) {
				final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
				
				write(image, imageId, outFormat, lod);
			}
		}
	}
	
	public static void write(final Image image, final String imageId, final String outFormat, final int lod) {
		final TicToc timer = new TicToc();
		
		System.out.println("Converting " + imageId + ":" + lod + " to " + outFormat + "(" + new Date(timer.tic()) + ")");
		
		try {
			if ("pgm".equals(outFormat)) {
				IMJTools.writePGM(image, new FileOutputStream(imageId + ".lod" + lod + ".pgm"));
			} else if ("ppm".equals(outFormat)) {
				IMJTools.writePPM(image, new FileOutputStream(imageId + ".lod" + lod + ".ppm"));
			} else {
				throw new IllegalArgumentException(outFormat);
			}
			
			System.out.println("Done: time: " + timer.toc() + " memory: " + usedMemory());
		} catch (final Exception exception) {
			System.err.println(exception);
		}
	}
	
}
