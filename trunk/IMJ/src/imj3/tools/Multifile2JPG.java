package imj3.tools;

import static net.sourceforge.aprog.tools.Tools.baseName;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import imj2.draft.AutoCloseableImageWriter;
import imj3.core.Image2D;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-04)
 */
public final class Multifile2JPG {
	
	private Multifile2JPG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "");
		final int[] lods = arguments.get("lod", 4);
		final String format = "jpg";
		
		for (final int lod : lods) {
			final File outputFile = new File(baseName(filePath) + "_lod" + lod + "." + format);
			
			if (outputFile.exists()) {
				continue;
			}
			
			final TicToc timer = new TicToc();
			
			Tools.debugPrint(new Date(timer.tic()));
			Tools.debugPrint("Processing", filePath, "at LOD", lod);
			
			final Image2D image = new MultifileImage2D(new MultifileSource(filePath), lod);
			final int width = image.getWidth();
			final int height = image.getHeight();
			final int optimalTileWidth = image.getOptimalTileWidth();
			final int optimalTileHeight = image.getOptimalTileHeight();
			final Canvas canvas = new Canvas().setFormat(width, height, BufferedImage.TYPE_3BYTE_BGR);
			
			for (int tileY = 0; tileY < height; tileY += optimalTileHeight) {
				for (int tileX = 0; tileX < width; tileX += optimalTileWidth) {
					canvas.getGraphics().drawImage((Image) image.getTile(tileX, tileY).toAwt(), tileX, tileY, null);
				}
			}
			
			final float compressionQuality = 0.95F;
			
			Tools.debugPrint("Writing", outputFile);
			
			try (final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter(format)
					.setCompressionQuality(compressionQuality).setOutput(new FileOutputStream(outputFile))) {
				imageWriter.write(canvas.getImage());
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
			
			Tools.debugPrint("Processing done in", timer.toc(), "ms");
		}
	}
	
}
