package imj2.tools;

import static java.util.Collections.sort;

import imj2.core.Image.Channels;
import imj2.core.Image.PredefinedChannels;
import imj2.core.Image2D;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

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
				cache.remove(entries.get(i).getKey());
			}
		}
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
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	static final class CachedValue {
		
		private long lastAccess;
		
		private final Callable<?> valueFactory;
		
		private Object value;
		
		CachedValue(final Callable<?> valueFActory) {
			this.valueFactory = valueFActory;
		}
		
		final long getLastAccess() {
			return this.lastAccess;
		}
		
		@SuppressWarnings("unchecked")
		final synchronized <T> T getValue() {
			this.lastAccess = System.nanoTime();
			
			if (this.value == null) {
				try {
					this.value = this.valueFactory.call();
				} catch (final Exception exception) {
					throw Tools.unchecked(exception);
				}
			}
			
			return (T) this.value;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	private static final class CacheCleaner {
		
		private CacheCleaner() {
			cleaner = new SoftReference<CacheCleaner>(this);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			removeOldCacheEntries(0.25);
			
			super.finalize();
			
			new CacheCleaner();
		}
		
		private static SoftReference<CacheCleaner> cleaner;
		
		static final void setup() {
			if (cleaner == null) {
				new CacheCleaner();
			}
		}
		
	}
	
}
