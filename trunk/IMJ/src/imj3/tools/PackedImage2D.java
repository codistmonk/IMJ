package imj3.tools;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author codistmonk (creation 2015-03-01)
 */
public final class PackedImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String id;
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private final int valuesPerLong;
	
	private final long[] data;
	
	private transient WeakReference<BufferedImage> awtHolder;
	
	public PackedImage2D(final String id, final int width, final int height, final Channels channels) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.width = width;
		this.height = height;
		this.channels = channels;
		final int valueBitCount = this.channels.getValueBitCount();
		
		if (Long.SIZE < valueBitCount) {
			throw new IllegalArgumentException();
		}
		
		this.valuesPerLong = Long.SIZE / valueBitCount;
		this.data = new long[(int) ((long) width * height / this.valuesPerLong)];
	}
	
	public final long[] getData() {
		return this.data;
	}
	
	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@Override
	public final int getWidth() {
		return this.width;
	}
	
	@Override
	public final int getHeight() {
		return this.height;
	}
	
	@Override
	public final long getPixelValue(final long pixel) {
		final int packedIndex = (int) (pixel / this.valuesPerLong);
		final int valueBitCount = this.getChannels().getValueBitCount();
		final long valueMask = ~((~0L) << valueBitCount);
		final long valueShift = valueBitCount * (pixel % this.valuesPerLong);
		final long packed = this.getData()[packedIndex];
		
		return (packed >> valueShift) & valueMask;
	}
	
	@Override
	public final PackedImage2D setPixelValue(final long pixel, final long value) {
		final int packedIndex = (int) (pixel / this.valuesPerLong);
		final int valueBitCount = this.getChannels().getValueBitCount();
		final long valueShift = valueBitCount * (pixel % this.valuesPerLong);
		final long valueMask = ~((~0L) << valueBitCount) << valueShift;
		final long packed = this.getData()[packedIndex];
		
		this.getData()[packedIndex] = (packed & ~valueMask) | ((value << valueShift) & valueMask);
		
		return this;
	}
	
	@Override
	public final BufferedImage toAwt() {
		BufferedImage result = this.awtHolder == null ? null : this.awtHolder.get();
		
		if (result == null) {
			final int width = this.getWidth();
			final int height = this.getHeight();
			result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					result.setRGB(x, y, (int) this.getPixelValue(x, y));
				}
			}
			
			this.awtHolder = new WeakReference<>(result);
		}
		
		return result;
	}
	
	private static final long serialVersionUID = 9009222978487985122L;
	
}