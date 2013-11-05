package imj2.tools;

import static imj2.tools.IMJTools.forEachTile;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.tools.IMJTools.TileProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-11-04)
 */
public final class SplitImage {
	
	private SplitImage() {
		throw new IllegalInstantiationException();
	}
	
	public static final void main(final String[] commandLineArguments) throws Exception {
		if (commandLineArguments.length == 0) {
			System.out.println("Arguments: file <imageId> [to <outputBasePath]");
			
			return;
		}
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final TicToc timer = new TicToc();
		final String imageId = arguments.get("file", "");
		final String outputBasePath = arguments.get("to", removeExtension(imageId));
		final int maximumTileWidth = arguments.get("maximumTileWidth", 1024)[0];
		final int maximumTileHeight = arguments.get("maximumTileWidth", maximumTileWidth)[0];
		final int forcedTileWidth = arguments.get("tileWidth", 0)[0];
		final int forcedTileHeight = arguments.get("tileHeight", forcedTileWidth)[0];
		final int[] lods = arguments.get("lods", 0, 1, 2, 3, 4, 5, 6, 7);
		
		System.out.println("input: " + imageId);
		
		final TiledImage2D[] image = { new LociBackedImage(imageId) };
		int currentLOD = 0;
		final int optimalTileWidth = 0 < forcedTileWidth ? forcedTileWidth : min(maximumTileWidth, image[0].getOptimalTileWidth());
		final int optimalTileHeight = 0 < forcedTileHeight ? forcedTileHeight : min(maximumTileHeight, image[0].getOptimalTileHeight());
		final DefaultColorModel color = new DefaultColorModel(image[0].getChannels());
		
		System.out.println("outputBase: " + outputBasePath);
		System.out.println("Splitting... " + new Date(timer.tic()));
		
		for (final int lod : lods) {
			for (; currentLOD < lod; ++currentLOD) {
				System.out.println("Subsampling for LOD " + (currentLOD + 1) + "... " + new Date());
				
				image[0] = new SubsampledImage2D(image[0], optimalTileWidth, optimalTileHeight);
				image[0].loadAllTiles();
			}
			
			if (currentLOD != lod) {
				throw new IllegalArgumentException();
			}
			
			final int imageWidth = image[0].getWidth();
			final int imageHeight = image[0].getHeight();
			final int preferredTileWidth = min(imageWidth, optimalTileWidth);
			final int preferredTileHeight = min(imageHeight, optimalTileHeight);
			
			System.out.println("LOD: " + lod + " " + new Date());
			System.out.println("width: " + imageWidth + " height: " + imageHeight +
					" tileWidth: " + preferredTileWidth + " tileHeight: " + preferredTileHeight);
			
			forEachTile(imageWidth, imageHeight, preferredTileWidth, preferredTileHeight, new TileProcessor() {
				
				private BufferedImage tile = null;
				
				private int tileX;
				
				private int tileY;
				
				@Override
				public final void pixel(final Info info) {
					this.tileX = info.getTileX();
					this.tileY = info.getTileY();
					
					if (this.tile == null || this.tile.getWidth() != info.getActualTileWidth() || this.tile.getHeight() != info.getActualTileHeight()) {
						this.tile = new BufferedImage(info.getActualTileWidth(), info.getActualTileHeight(), BufferedImage.TYPE_3BYTE_BGR);
					}
					
					final int pixelValue = image[0].getPixelValue(info.getTileX() + info.getPixelXInTile(),
							info.getTileY() + info.getPixelYInTile());
					
					this.tile.setRGB(info.getPixelXInTile(), info.getPixelYInTile(),
							new Color(color.red(pixelValue), color.green(pixelValue), color.blue(pixelValue)).getRGB());
				}
				
				@Override
				public final void endOfTile() {
					try {
						ImageIO.write(this.tile, "jpg",
								new File(outputBasePath + "_lod" + lod + "_" + this.tileY + "_" + this.tileX + ".jpg"));
					} catch (final IOException exception) {
						throw unchecked(exception);
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 6955996121004415003L;
				
			});
		}
		
		System.out.println("Splitting done time: " + timer.toc());
	}
	
	public static final String removeExtension(final String path) {
		final int i = path.lastIndexOf('.', max(0, max(path.lastIndexOf('/'), path.lastIndexOf('\\'))));
		
		return i < 0 ? path : path.substring(0, i);
	}
	
}
