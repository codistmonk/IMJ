package imj3.draft;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Image2D;
import imj3.tools.MultifileImage2D;
import imj3.tools.MultifileSource;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-05-09)
 */
public final class BuildJSIndex {
	
	private BuildJSIndex() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final int thumbnailSize = arguments.get("thumbnailSize", 128)[0];
		
		try (final PrintStream output = new PrintStream(new File(root, "index.js"))) {
			output.println("index = [];");
			
			final File[] files = root.listFiles();
			
			Arrays.sort(files, new Comparator<File>() {
				
				@Override
				public final int compare(final File f1, final File f2) {
					return f1.getName().compareTo(f2.getName());
				}
				
			});
			
			for (final File file : files) {
				if (file.getName().endsWith(".zip")) {
					final File thumbnailFile = new File(baseName(file.getPath()) + "_thumbnail.jpg");
					
					if (!thumbnailFile.exists()) {
						try {
							Image2D image = new MultifileImage2D(new MultifileSource(file.getPath()), 0);
							final double scale = (double) thumbnailSize / max(image.getWidth(), image.getHeight());
							image = image.getScaledImage(scale);
							final int imageWidth = image.getWidth();
							final int imageHeight = image.getHeight();
							final int m = max(imageWidth, imageHeight);
							final int thumbnailWidth = imageWidth * thumbnailSize / m;
							final int thumbnailHeight = imageHeight * thumbnailSize / m;
							final Canvas canvas = new Canvas().setFormat(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_BGR);
							final int optimalTileWidth = image.getOptimalTileWidth();
							final int optimalTileHeight = image.getOptimalTileHeight();
							final int dx = optimalTileWidth * thumbnailSize / m;
							final int dy = optimalTileHeight * thumbnailSize / m;
							
							for (int imageTileY = 0, y = 0; imageTileY < imageHeight; imageTileY += optimalTileHeight, y += dy) {
								for (int imageTileX = 0, x = 0; imageTileX < imageWidth; imageTileX += optimalTileWidth, x += dx) {
									final Image tile = (Image) image.getTile(imageTileX, imageTileY).toAwt();
									
									canvas.getGraphics().drawImage(tile, x, y, min(x + dx, thumbnailWidth), min(y + dy, thumbnailHeight), 0, 0, tile.getWidth(null), tile.getHeight(null), null);
								}
							}
							
							debugPrint("Writing", thumbnailFile);
							ImageIO.write(canvas.getImage(), "jpg", thumbnailFile);
							
							output.println("index.push(\"" + file.getName() + "\");");
						} catch (final Exception exception) {
							debugError(exception);
						}
					} else {
						output.println("index.push(\"" + file.getName() + "\");");
					}
				}
			}
		} catch (final FileNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
}
