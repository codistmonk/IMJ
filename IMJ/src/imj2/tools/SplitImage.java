package imj2.tools;

import static imj2.tools.IMJTools.forEachTile;
import static imj2.tools.MultifileImage.setIdAttributes;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;
import imj2.core.RetiledImage2D;
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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Date;
import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Factory;
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
		try {
			URL.setURLStreamHandlerFactory(new SFTPStreamHandlerFactory());
		} catch (final Error error) {
			error.printStackTrace();
		}
	}
	
	public static final void main(final String[] commandLineArguments) throws Exception {
		if (commandLineArguments.length == 0) {
			System.out.println("Arguments: file <imageId> [to <databasePath>]");
			
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
		final int initialLOD = arguments.get("lod", 0)[0];
		final int lodCount = arguments.get("lodCount", 1)[0];
		final boolean generateTiles = arguments.get("generateTiles", 1)[0] != 0;
		
		System.out.println("input: " + imageId);
		
		final TiledImage2D[] image = { (TiledImage2D) new LociBackedImage(imageId).getLODImage(initialLOD) };
		final int optimalTileWidth = 0 < forcedTileWidth ? forcedTileWidth : min(maximumTileWidth, image[0].getOptimalTileWidth());
		final int optimalTileHeight = 0 < forcedTileHeight ? forcedTileHeight : min(maximumTileHeight, image[0].getOptimalTileHeight());
		final DefaultColorModel color = new DefaultColorModel(image[0].getChannels());
		final String databasePath = root + "/imj_database.xml";
		final Document database = getDatabase(databasePath);
		final int algo = 0;
		
		if (algo == 0) {
			final long[][] histogram = IMJTools.instances(8,
					new InvokerAsFactory<long[]>(Array.class, "newInstance", long.class, 64));
			
			Tools.debugPrint(new Date(timer.tic()));
			
			final FractalZTileGenerator generator = new FractalZTileGenerator(new RetiledImage2D(image[0], optimalTileWidth), lodCount, new FractalZTileGenerator.TileProcessor() {
				
				private final BufferedImage[] output = { null };
				
				@Override
				public final void processTile(final TiledImage2D image, final int tileX, final int tileY, final Image2D tile) {
					final int lod = image.getLOD();
					
					if (1 < lod) {
						Tools.debugPrint(lod, tileX, tileY, tile);
					}
					
					if (this.output[0] == null ||
							this.output[0].getWidth() != tile.getWidth() || this.output[0].getHeight() != tile.getHeight()) {
						this.output[0] = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
					}
					
					final BufferedImage output = this.output[0];
					
					final long[] h = histogram[lod];
					
					tile.forEachPixelInBox(0, 0, tile.getWidth(), tile.getHeight(), new MonopatchProcess() {
						
						@Override
						public final void pixel(final int x, final int y) {
							final int pixelValue = tile.getPixelValue(x, y);
							final int red = color.red(pixelValue);
							final int green = color.green(pixelValue);
							final int blue = color.blue(pixelValue);
							
							output.setRGB(x, y, new Color(red, green, blue).getRGB());
							
							++h[((red & 0xC0) >> 2) | ((green & 0xC0) >> 4) | ((blue & 0xC0) >> 6)];
						}
						
						/**
						 * {@value}.
						 */
						private static final long serialVersionUID = -8744158533992880186L;
						
					});
					
					if (generateTiles) {
						final String format = "jpg";
						
						try {
							ImageIO.write(output, format, getOutputStream(
									outputBasePath + "_lod" + lod + "_" + tileY + "_" + tileX + "." + format));
						} catch (final IOException exception) {
							throw unchecked(exception);
						}
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 4875727286116220095L;
				
			});
			
			while (generator.next());
			
			{
				for (FractalZTileGenerator g = generator; g != null; g = g.getSubsampling()) {
					final Image2D i = g.getImage();
					final int lod = i.getLOD();
					
					addEntry(database, imageName, lod, i.getWidth(), i.getHeight(),
							optimalTileWidth, optimalTileHeight, histogram[lod]);
				}
				
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
			
			Tools.debugPrint(timer.toc());
		} else {
			int currentLOD = 0;
			
			System.out.println("outputBasePath: " + outputBasePath);
			System.out.println("Splitting... " + new Date(timer.tic()));
			
			for (int lod0 = initialLOD; lod0 < lodCount; ++lod0) {
				final int lod = lod0;
				
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
				
				addEntry(database, imageName, lod, imageWidth, imageHeight,
						optimalTileWidth, optimalTileHeight, histogram);
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
	}

	public static void addEntry(final Document database,
			final String imageName, final int lod, final int imageWidth,
			final int imageHeight, final int optimalTileWidth,
			final int optimalTileHeight, final long[] histogram) {
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
			}
			
			return new FileInputStream(source);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final OutputStream getOutputStream(final String destination) {
		try {
			if (MultifileImage.PROTOCOL_PATTERN.matcher(destination).matches()) {
				return new URL(destination).openConnection().getOutputStream();
			}
			
			final File parent = new File(destination).getParentFile();
			
			if (!parent.canRead()) {
				parent.mkdirs();
			}
			
			return new FileOutputStream(destination);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-11-23)
	 */
	public static final class FractalZTileGenerator implements Serializable {
		
		private final RetiledImage2D image;
		
		private final TileProcessor processor;
		
		private final FractalZTileGenerator subsampling;
		
		private final int endTileX;
		
		private final int endTileY;
		
		private long tileIndex;
		
		public FractalZTileGenerator(final RetiledImage2D image, final int lodCount, final TileProcessor processor) {
			this.image = image;
			this.processor = processor;
			this.subsampling = lodCount == 1 ? null : new FractalZTileGenerator(
					image.getLODImage(image.getLOD() + 1), lodCount - 1, processor);
			final int optimalTileWidth = image.getOptimalTileWidth();
			final int optimalTileHeight = image.getOptimalTileHeight();
			final Image2D lastImage = this.getLastGenerator().image;
			final int lastImageHorizontalTileCount = (lastImage.getWidth() + optimalTileWidth - 1) / optimalTileWidth;
			final int lastImageVerticalTileCount = (lastImage.getHeight() + optimalTileHeight - 1) / optimalTileHeight;
//			this.endTileX = Integer.highestOneBit(image.getWidth() - 1) << 1;
//			this.endTileY = Integer.highestOneBit(image.getHeight() - 1) << 1;
			this.endTileX = (lastImageHorizontalTileCount << (lodCount - 1)) * optimalTileWidth;
			this.endTileY = (lastImageVerticalTileCount << (lodCount - 1)) * optimalTileHeight;
			
			Tools.debugPrint(image.getLOD(), image.getWidth(), image.getHeight(), this.endTileX, this.endTileY);
		}
		
		public final RetiledImage2D getImage() {
			return this.image;
		}
		
		public final FractalZTileGenerator getSubsampling() {
			return this.subsampling;
		}

		public final FractalZTileGenerator getLastGenerator() {
			return this.getSubsampling() == null ? this : this.getSubsampling().getLastGenerator();
		}
		
		public final boolean next() {
			final int tileX = FractalZ2D.getX(this.tileIndex) * this.getImage().getOptimalTileWidth();
			final int tileY = FractalZ2D.getY(this.tileIndex) * this.getImage().getOptimalTileHeight();
			
			if (tileX < this.getImage().getWidth() && tileY < this.image.getHeight()) {
				this.processor.processTile(this.getImage(), tileX, tileY,
						(Image2D) this.getImage().ensureTileContains(tileX, tileY).updateTile());
			}
			
			if (this.tileIndex++ % PERIODICITY == PERIODICITY - 1 && this.getSubsampling() != null) {
				this.getSubsampling().next();
			}
			
			return tileX < this.endTileX || tileY < this.endTileY;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -917956928847528913L;
		
		/**
		 * {@value}.
		 */
		public static final int PERIODICITY = 1 << FractalZ2D.D;
		
		/**
		 * @author codistmonk (creation 2013-11-23)
		 */
		public static abstract interface TileProcessor extends Serializable {
			
			public abstract void processTile(TiledImage2D image, int tileX, int tileY, Image2D tile);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-11-23)
	 */
	public static final class FractalZ2D {
		
		private FractalZ2D() {
			throw new IllegalInstantiationException();
		}
		
		public static final int getX(final long index) {
			int result = 0;
			
			for (int i = 0; i < USED_INT_BITS; ++i) {
				result |= mapBit(index, D * i + 0, i);
			}
			
			return result;
		}
		
		public static final int getY(final long index) {
			int result = 0;
			
			for (int i = 0; i < USED_INT_BITS; ++i) {
				result |= mapBit(index, D * i + 1, i);
			}
			
			return result;
		}
		
		public static final long getIndex(final int x, final int y) {
			long result = 0L;
			
			for (int i = 0; i < USED_INT_BITS; ++i) {
				result |= mapBit(y, i, D * i + 1) | mapBit(x, i, D * i + 0);
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		public static final int USED_INT_BITS = Integer.SIZE - 1;
		
		/**
		 * {@value}.
		 */
		public static final int D = 2;
		
		public static final long mapBit(final int value, final int bitIndexInValue, final int bitIndexInResult) {
			return ((value >> bitIndexInValue) & 1L) << bitIndexInResult;
		}
		
		public static final int mapBit(final long value, final int bitIndexInValue, final int bitIndexInResult) {
			return ((int) (value >> bitIndexInValue) & 1) << bitIndexInResult;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-11-24)
	 *
	 * @param <T>
	 */
	public static final class InvokerAsFactory<T> implements Factory<T> {
		
		private final Object objectOrClass;
		
		private final String methodName;
		
		private final Object[] arguments;
		
		public InvokerAsFactory(final Object objectOrClass, final String methodName, final Object... arguments) {
			this.objectOrClass = objectOrClass;
			this.methodName = methodName;
			this.arguments = arguments;
		}
		
		@Override
		public final T newInstance() {
			return Tools.invoke(this.objectOrClass, this.methodName, this.arguments);
		}
		
	}
	
}
