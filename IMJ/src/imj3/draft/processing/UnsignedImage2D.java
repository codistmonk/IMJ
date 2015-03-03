package imj3.draft.processing;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.util.HashMap;
import java.util.Map;

/**
 * @author codistmonk (creation 2015-03-01)
 */
public final class UnsignedImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String id;
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private final long[] data;
	
	public UnsignedImage2D(final String id, final int width, final int height) {
		this(id, width, height, new Channels.Default(1, Long.SIZE));
	}
	
	public UnsignedImage2D(final String id, final int width, final int height, final Channels channels) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.width = width;
		this.height = height;
		this.channels = channels;
		this.data = new long[width * height];
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
		return this.data[(int) pixel];
	}
	
	@Override
	public final Image2D setPixelValue(final long pixel, final long value) {
		this.data[(int) pixel] = value;
		
		return this;
	}
	
	private static final long serialVersionUID = 9009222978487985122L;
	
}