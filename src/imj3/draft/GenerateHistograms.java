package imj3.draft;

import static imj3.tools.IMJTools.*;
import static java.util.Arrays.fill;
import static multij.tools.Tools.*;

import imj3.core.Image2D;
import imj3.tools.MultifileSource;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2015-07-21)
 */
public final class GenerateHistograms {
	
	private GenerateHistograms() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final int n = 1 << 24;
		final long[] counts = new long[n];
		Image2D image = read(imagePath);
		final TicToc timer = new TicToc();
		
		try (final MultifileSource imageData = new MultifileSource(baseName(imagePath) + "_data.zip");
				final DataOutputStream output = new DataOutputStream(imageData.getOutputStream("rgb_histograms.raw"))) {
			while (image != null) {
				timer.tic();
				
				debugPrint(new Date());
				
				final Image2D img = image;
				debugPrint(image.getWidth(), image.getHeight());
				
				fill(counts, 0L);
				
				forEachPixelInEachTileIn(image, (x, y) -> {
					++counts[(int) img.getPixelValue(x, y) & 0x00FFFFFF];
					return true;
				});
				
				debugPrint(timer.toctic());
				
				for (final long c : counts) {
					output.writeLong(c);
				}
				
				debugPrint(timer.toc());
				
				image = 1 < image.getWidth() && 1 < image.getHeight() ? image.getScaledImage(image.getScale() / 2.0) : null;
			}
		}
	}
	
}
