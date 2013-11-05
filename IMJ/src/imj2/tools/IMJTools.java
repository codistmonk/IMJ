package imj2.tools;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Collections.sort;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.core.Image.Channels;
import imj2.core.Image.PredefinedChannels;
import imj2.core.Image2D;
import imj2.tools.IMJTools.TileProcessor.Info;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class IMJTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	private static final Map<Object, CachedValue> cache = new HashMap<Object, CachedValue>();
	
	static {
		CacheCleaner.setup();
	}
	
	public static final <V> V cache(final Object key, final Callable<V> valueFactory) {
		CachedValue cachedValue;
		
		synchronized (cache) {
			cachedValue = cache.get(key);
			
			if (cachedValue == null) {
				cachedValue = new CachedValue(valueFactory);
				cache.put(key, cachedValue);
			}
		}
		
		return cachedValue.getValue();
	}
	
	public static final void removeOldCacheEntries(final double ratio) {
		synchronized (cache) {
			final List<Map.Entry<Object, CachedValue>> entries = new ArrayList<Map.Entry<Object, CachedValue>>(cache.entrySet());
			
			sort(entries, new Comparator<Map.Entry<Object, CachedValue>>() {
				
				@Override
				public final int compare(final Entry<Object, CachedValue> entry1, final Entry<Object, CachedValue> entry2) {
					return Long.signum(entry1.getValue().getLastAccess() - entry2.getValue().getLastAccess());
				}
				
			});
			
			final int n = (int) (ratio * entries.size());
			
			for (int i = 0; i < n; ++i) {
				final Entry<Object, CachedValue> entry = entries.get(i);
				
				if (!entry.getValue().isBusy()) {
					cache.remove(entry.getKey());
				}
			}
		}
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
	
	public static final int quantize(final int value, final int quantum) {
		return (value / quantum) * quantum;
	}
	
	public static final BufferedImage awtImage(final Image2D image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final BufferedImage result = new BufferedImage(width, height, awtImageTypeFor(image));
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				result.setRGB(x, y, image.getPixelValue(x, y));
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
	
	public static final void forEachTile(final int imageWidth, final int imageHeight,
			final int tileWidth, final int tileHeight, final TileProcessor process) {
		try {
			for (int tileY = 0; tileY < imageHeight; tileY += tileHeight) {
				final int h = min(tileHeight, imageHeight - tileY);
				
				for (int tileX = 0; tileX < imageWidth; tileX += tileWidth) {
					final int w = min(tileWidth, imageWidth - tileX);
					
					for (int y = 0; y < h; ++y) {
						for (int x = 0; x < w; ++x) {
							process.pixel(new Info(tileX, tileY, w, h, x, y));
						}
					}
					
					process.endOfTile();
				}
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
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
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	static final class CachedValue {
		
		private long lastAccess;
		
		private final Callable<?> valueFactory;
		
		private Object value;
		
		private final AtomicBoolean busy;
		
		CachedValue(final Callable<?> valueFActory) {
			this.valueFactory = valueFActory;
			this.busy = new AtomicBoolean();
		}
		
		final boolean isBusy() {
			return this.busy.get();
		}
		
		final long getLastAccess() {
			return this.lastAccess;
		}
		
		@SuppressWarnings("unchecked")
		final synchronized <T> T getValue() {
			if (this.value == null) {
				try {
					this.busy.set(true);
					this.value = this.valueFactory.call();
				} catch (final Exception exception) {
					throw Tools.unchecked(exception);
				} finally {
					this.busy.set(false);
				}
			}
			
			this.lastAccess = timestamp.addAndGet(1L);
			
			return (T) this.value;
		}
		
		private static final AtomicLong timestamp = new AtomicLong(Long.MIN_VALUE);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	private static final class CacheCleaner {
		
		private CacheCleaner() {
			cleaner = new WeakReference<CacheCleaner>(this);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			final Runtime runtime = Runtime.getRuntime();
			
			if (Tools.usedMemory() > runtime.maxMemory() / 2L) {
				removeOldCacheEntries(1.0 / 8.0);
			}
			
			super.finalize();
			
			new CacheCleaner();
		}
		
		private static Reference<CacheCleaner> cleaner;
		
		static final void setup() {
			if (cleaner == null) {
				new CacheCleaner();
			}
		}
		
	}
	
}
