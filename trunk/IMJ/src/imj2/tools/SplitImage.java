package imj2.tools;

import static imj2.tools.IMJTools.forEachTile;
import static imj2.tools.MultifileImage.setIdAttributes;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import imj2.core.SubsampledImage2D;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor;
import imj2.tools.SFTPStreamHandlerFactory.SFTPStreamHandler;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

/**
 * @author codistmonk (creation 2013-11-04)
 */
public final class SplitImage {
	
	private SplitImage() {
		throw new IllegalInstantiationException();
	}
	
	static {
		URL.setURLStreamHandlerFactory(new SFTPStreamHandlerFactory());
	}
	
	public static final void main(final String[] commandLineArguments) throws Exception {
		if (commandLineArguments.length == 0) {
			System.out.println("Arguments: file <imageId> [to <outputBasePath]");
			
			return;
		}
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final TicToc timer = new TicToc();
		final String imageId = arguments.get("file", "");
		final String imageName = removeExtension(new File(imageId).getName());
		final String root = arguments.get("to", "");
		final String outputBasePath = root + "/" + imageName + "/" + imageName;
		final int maximumTileWidth = arguments.get("maximumTileWidth", 1024)[0];
		final int maximumTileHeight = arguments.get("maximumTileWidth", maximumTileWidth)[0];
		final int forcedTileWidth = arguments.get("tileWidth", 0)[0];
		final int forcedTileHeight = arguments.get("tileHeight", forcedTileWidth)[0];
		final int[] lods = arguments.get("lods", 0, 1, 2, 3, 4, 5, 6, 7);
		final boolean generateTiles = arguments.get("generateTiles", 1)[0] != 0;
		
		System.out.println("input: " + imageId);
		
		final TiledImage2D[] image = { new LociBackedImage(imageId) };
		int currentLOD = 0;
		final int optimalTileWidth = 0 < forcedTileWidth ? forcedTileWidth : min(maximumTileWidth, image[0].getOptimalTileWidth());
		final int optimalTileHeight = 0 < forcedTileHeight ? forcedTileHeight : min(maximumTileHeight, image[0].getOptimalTileHeight());
		final DefaultColorModel color = new DefaultColorModel(image[0].getChannels());
		final String databasePath = root + "/imj_database.xml";
		final Document database = getDatabase(databasePath);
		
		System.out.println("outputBasePath: " + outputBasePath);
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
			final long[] histogram = new long[64];
			
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
					
					if (generateTiles && (this.tile == null ||
							this.tile.getWidth() != info.getActualTileWidth() ||
							this.tile.getHeight() != info.getActualTileHeight())) {
						this.tile = new BufferedImage(info.getActualTileWidth(), info.getActualTileHeight(),
								BufferedImage.TYPE_3BYTE_BGR);
					}
					
					final int pixelValue = image[0].getPixelValue(info.getTileX() + info.getPixelXInTile(),
							info.getTileY() + info.getPixelYInTile());
					final int red = color.red(pixelValue);
					final int green = color.green(pixelValue);
					final int blue = color.blue(pixelValue);
					
					if (generateTiles) {
						this.tile.setRGB(info.getPixelXInTile(), info.getPixelYInTile(),
								new Color(red, green, blue).getRGB());
					}
					
					++histogram[((red & 0xC0) >> 2) | ((green & 0xC0) >> 4) | ((blue & 0xC0) >> 6)];
				}
				
				@Override
				public final void endOfTile() {
					if (generateTiles) {
						final BufferedImage tile = this.tile;
						final String format = "jpg";
						
						try {
							ImageIO.write(tile, format, getOutputStream(
									outputBasePath + "_lod" + lod + "_" + this.tileY + "_" + this.tileX + "." + format));
						} catch (final IOException exception) {
							throw unchecked(exception);
						}
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 6955996121004415003L;
				
			});
			
			{
				final String id = imageName + "/" + imageName  + "_lod" + lod;
				Element imageElement = database.getElementById(id);
				
				if (imageElement == null) {
					imageElement = database.createElement("image");
				}
				
				imageElement.setAttribute("id", id);
				imageElement.setIdAttribute("id", true);
				imageElement.setAttribute("width", "" + imageWidth);
				imageElement.setAttribute("height", "" + imageHeight);
				imageElement.setAttribute("tileWidth", "" + optimalTileWidth);
				imageElement.setAttribute("tileHeight", "" + optimalTileHeight);
				
				{
					final Element histogramElement = database.createElement("histogram");
					final long pixelCount = (long) imageWidth * imageHeight;
					final StringBuilder histogramStringBuilder = new StringBuilder();
					final int n = histogram.length;
					
					if (0 < n) {
						histogramStringBuilder.append(255L * histogram[0] / pixelCount);
						
						for (int i = 1; i < n; ++i) {
							histogramStringBuilder.append(' ').append(255L * histogram[i] / pixelCount);
						}
					}
					
					for (final Node oldHistogramElement : XMLTools.getNodes(imageElement, "histogram")) {
						imageElement.removeChild(oldHistogramElement);
					}
					
					histogramElement.setTextContent(histogramStringBuilder.toString());
					imageElement.appendChild(histogramElement);
				}
				
				database.getDocumentElement().appendChild(imageElement);
			}
		}
		
		System.out.println("Splitting done time: " + timer.toc());
		
		final File temporaryDBFile = File.createTempFile("imj.db.", ".xml");
		temporaryDBFile.deleteOnExit();
		XMLTools.write(database, temporaryDBFile, 0);
		final InputStream input = new FileInputStream(temporaryDBFile);
		
		try {
			Tools.writeAndCloseOutput(input, getOutputStream(databasePath));
		} finally {
			input.close();
		}
		
		SFTPStreamHandler.closeAll();
	}
	
	public static final String removeExtension(final String path) {
		final int i = new File(path).getName().lastIndexOf('.');
		
		return i < 0 ? path : path.substring(0, i);
	}
	
	public static final Document getDatabase(final String path) {
		try {
			return setIdAttributes(parse(getInputStream(path)));
		} catch (final Exception exception) {
			Tools.debugPrint(exception);
		}
		
		return parse("<images/>");
	}
	
	public static final InputStream getInputStream(final String source) {
		try {
			if (MultifileImage.PROTOCOL_PATTERN.matcher(source).matches()) {
				return new URL(source).openConnection().getInputStream();
			} else {
				return new FileInputStream(source);
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final OutputStream getOutputStream(final String destination) {
		try {
			if (MultifileImage.PROTOCOL_PATTERN.matcher(destination).matches()) {
				return new URL(destination).openConnection().getOutputStream();
			} else {
				return new FileOutputStream(destination);
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
}
