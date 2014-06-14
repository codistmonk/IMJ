package imj2.tools;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import imj2.core.Image.Channels;
import imj2.core.Image.PredefinedChannels;
import imj2.core.ConcreteImage2D;
import imj2.core.IMJCoreTools;
import imj2.core.Image;
import imj2.core.Image2D;
import imj2.core.LinearIntImage;
import imj2.core.TiledImage2D;
import imj2.tools.IMJTools.TileProcessor.Info;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Iterator;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class IMJTools extends IMJCoreTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	@SuppressWarnings("unchecked")
	public static final <C extends Component> C findComponent(final Container parent, final Class<C> componentClass) {
		if (componentClass.isInstance(parent)) {
			return (C) parent;
		}
		
		for (final Component child : parent.getComponents()) {
			if (child instanceof Container) {
				final C maybeResult = findComponent((Container) child, componentClass);
				
				if (maybeResult != null) {
					return maybeResult;
				}
			}
		}
		
		return null;
	}
	
	public static final ConcreteImage2D<LinearIntImage> newC4U8ConcreteImage2D(final int width, final int height) {
		return new ConcreteImage2D<LinearIntImage>(new LinearIntImage(
				"test", (long) width * height, Image.PredefinedChannels.C4_U8), width, height);
	}
	
	public static final int a8gray888(final int alpha8, final int gray8) {
		return (alpha8 << 24) | (gray8 * 0x00010101);
	}
	
	public static final int a8r8g8b8(final int alpha8, final int red8, final int green8, final int blue8) {
		return (alpha8 << 24) | (red8 << 16) | (green8 << 8) | (blue8 << 0);
	}
	
	public static final int alpha8(final int rgb) {
		return uint8(rgb >> 24);
	}
	
	public static final int red8(final int rgb) {
		return uint8(rgb >> 16);
	}
	
	public static final int green8(final int rgb) {
		return uint8(rgb >> 8);
	}
	
	public static final int blue8(final int rgb) {
		return uint8(rgb >> 0);
	}
	
	public static final int uint8(final int value) {
		return value & 0xFF;
	}
	
	public static final int uint8(final long value) {
		return (int) (value & 0xFF);
	}
	
	public static final int uint8(final float value) {
		return (int) value & 0xFF;
	}
	
	public static final int uint8(final double value) {
		return (int) value & 0xFF;
	}
	
	public static final long sum(final long... values) {
		long result = 0L;
		
		for (final double value : values) {
			result += value;
		}
		
		return result;
	}
	
	public static final double sum(final double... values) {
		double result = 0.0;
		
		for (final double value : values) {
			result += value;
		}
		
		return result;
	}
	
	public static final BufferedImage awtImage(final Image2D image) {
		return awtImage(image, 0, 0, image.getWidth(), image.getHeight());
	}
	
	public static final BufferedImage awtImage(final Image2D image
			, final int left, final int top, final int width, final int height) {
		final BufferedImage result = new BufferedImage(width, height, awtImageTypeFor(image));
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				result.setRGB(x, y, image.getPixelValue(left + x, top + y));
			}
		}
		
		return result;
	}
	
	public static final boolean contains(final Image2D image, final int x, final int y) {
		return 0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight();
	}
	
	public static final int[] getChannelValues(final Channels channels, final int pixelValue, final int[] result) {
		final int channelCount = channels.getChannelCount();
		final int[] actualResult = result != null ? result : new int[channelCount];
		
		for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
			actualResult[channelIndex] = channels.getChannelValue(pixelValue, channelIndex);
		}
		
		return actualResult;
	}
	
	public static final int awtImageTypeFor(final Image2D image) {
		switch (image.getChannels().getChannelCount()) {
		case 1:
			switch (image.getChannels().getChannelBitCount()) {
			case 1:
				return BufferedImage.TYPE_BYTE_BINARY;
			case 8:
				return BufferedImage.TYPE_BYTE_GRAY;
			case 16:
				return BufferedImage.TYPE_USHORT_GRAY;
			default:
				throw new IllegalArgumentException();
			}
		case 2:
			throw new IllegalArgumentException();
		case 3:
			return BufferedImage.TYPE_3BYTE_BGR;
		case 4:
			return BufferedImage.TYPE_INT_ARGB;
		default:
			throw new IllegalArgumentException();
		}
	}
		
	public static final Channels predefinedChannelsFor(final BufferedImage awtImage) {
		switch (awtImage.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
			return 1 == awtImage.getColorModel().getPixelSize() ?
					PredefinedChannels.C1_U1 : PredefinedChannels.C3_U8;
		case BufferedImage.TYPE_USHORT_GRAY:
			return PredefinedChannels.C1_U16;
		case BufferedImage.TYPE_BYTE_GRAY:
			return PredefinedChannels.C1_U8;
		case BufferedImage.TYPE_3BYTE_BGR:
			return PredefinedChannels.C3_U8;
		default:
			return PredefinedChannels.C4_U8;
		}
	}
	
	public static final Channels predefinedChannelsFor(final IFormatReader lociImage) {
		if (lociImage.isIndexed()) {
			return PredefinedChannels.C3_U8;
		}
		
		switch (lociImage.getRGBChannelCount()) {
		case 1:
			switch (FormatTools.getBytesPerPixel(lociImage.getPixelType()) * lociImage.getRGBChannelCount()) {
			case 1:
				return 1 == lociImage.getBitsPerPixel() ?
						PredefinedChannels.C1_U1 : PredefinedChannels.C1_U8;
			case 2:
				return PredefinedChannels.C1_U16;
			default:
				return PredefinedChannels.C1_S32;
			}
		case 2:
			return PredefinedChannels.C2_U16;
		case 3:
			return PredefinedChannels.C3_U8;
		case 4:
			return PredefinedChannels.C4_U8;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public static final Iterable<Rectangle> parallelTiles(final int boxLeft, final int boxTop,
			final int boxWidth, final int boxHeight, final int workerCount) {
		final int verticalOptimalTileCount = (int) sqrt(workerCount);
		final int horizontalOptimalTileCount = workerCount / verticalOptimalTileCount;
		final int optimalTileWidth = boxWidth / horizontalOptimalTileCount;
		final int optimalTileHeight = boxHeight / verticalOptimalTileCount;
		
		return tiles(boxLeft, boxTop, boxWidth, boxHeight, optimalTileWidth, optimalTileHeight);
	}
	
	public static final Iterable<Rectangle> tiles(final int boxLeft, final int boxTop, final int boxWidth, final int boxHeight,
			final int optimalTileWidth, final int optimalTileHeight) {
		return new Iterable<Rectangle>() {
			
			@Override
			public final Iterator<Rectangle> iterator() {
				return new Iterator<Rectangle>() {
					
					private final Rectangle tile = new Rectangle(min(boxWidth, optimalTileWidth),
							min(boxHeight, optimalTileHeight));
					
					@Override
					public final boolean hasNext() {
						return this.tile.y < boxHeight;
					}
					
					@Override
					public final Rectangle next() {
						final Rectangle result = new Rectangle(this.tile);
						
						result.translate(boxLeft, boxTop);
						
						this.tile.x += optimalTileWidth;
						
						if (boxWidth <= this.tile.x) {
							this.tile.x = 0;
							
							this.tile.y += optimalTileHeight;
							this.tile.height = min(boxHeight - this.tile.y, optimalTileHeight);
						}
						
						this.tile.width = min(boxWidth - this.tile.x, optimalTileWidth);
						
						return result;
					}
					
					@Override
					public final void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static final void forEachTileIn(final TiledImage2D image, final TileProcessor process) {
		forEachTile(image.getWidth(), image.getHeight(), image.getOptimalTileWidth(), image.getOptimalTileHeight(), process);
	}
	
	public static final void forEachTile(final int imageWidth, final int imageHeight,
			final int tileWidth, final int tileHeight, final TileProcessor process) {
		for (int tileY = 0; tileY < imageHeight; tileY += tileHeight) {
			final int h = min(tileHeight, imageHeight - tileY);
			
			for (int tileX = 0; tileX < imageWidth; tileX += tileWidth) {
				final int w = min(tileWidth, imageWidth - tileX);
				
				process.pixel(new Info(tileX, tileY, w, h, 0, 0));
			}
		}
	}
	
	public static final void forEachPixelInEachTile(final int imageWidth, final int imageHeight,
			final int tileWidth, final int tileHeight, final TileProcessor process) {
		forEachTile(imageWidth, imageHeight, tileWidth, tileHeight, new TileProcessor() {
			
			@Override
			public final void pixel(final Info info) {
				final int w = info.getActualTileWidth();
				final int h = info.getActualTileHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						process.pixel(new Info(info.getTileX(), info.getTileY(), w, h, x, y));
					}
				}
			}
			
			@Override
			public final void endOfTile() {
				process.endOfTile();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3431410834328760116L;
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2013-10-30)
	 */
	public static abstract interface TileProcessor extends Serializable {
		
		public abstract void pixel(Info info);
		
		public abstract void endOfTile();
		
		/**
		 * @author codistmonk (creation 2013-11-04)
		 */
		public static final class Info implements Serializable {
			
			private final int tileX;
			
			private final int tileY;
			
			private final int actualTileWidth;
			
			private final int actualTileHeight;
			
			private final int pixelXInTile;
			
			private final int pixelYInTile;
			
			public Info(final int tileX, final int tileY,
					final int actualTileWidth, final int actualTileHeight,
					final int pixelXInTile, final int pixelYInTile) {
				this.tileX = tileX;
				this.tileY = tileY;
				this.actualTileWidth = actualTileWidth;
				this.actualTileHeight = actualTileHeight;
				this.pixelXInTile = pixelXInTile;
				this.pixelYInTile = pixelYInTile;
			}
			
			public final int getTileX() {
				return this.tileX;
			}
			
			public final int getTileY() {
				return this.tileY;
			}
			
			public final int getActualTileWidth() {
				return this.actualTileWidth;
			}
			
			public final int getActualTileHeight() {
				return this.actualTileHeight;
			}
			
			public final int getPixelXInTile() {
				return this.pixelXInTile;
			}
			
			public final int getPixelYInTile() {
				return this.pixelYInTile;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 124947385663986060L;
			
		}
		
	}
	
}
