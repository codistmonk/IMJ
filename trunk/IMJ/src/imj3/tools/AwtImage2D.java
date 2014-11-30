package imj3.tools;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import imj3.core.Channels;
import imj3.core.Image2D;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public final class AwtImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String id;
	
	private final BufferedImage source;
	
	private final long pixelCount;
	
	public AwtImage2D(final String id, final int width, final int height) {
		this(id, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
	}
	
	public AwtImage2D(final String id, final BufferedImage source) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.source = source;
		this.pixelCount = (long) source.getWidth() * source.getHeight();
	}
	
	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	public final BufferedImage getSource() {
		return this.source;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final long getPixelCount() {
		return this.pixelCount;
	}
	
	@Override
	public final Channels getChannels() {
		return Channels.Predefined.A8R8G8B8;
	}
	
	@Override
	public final long getPixelValue(final long pixel) {
		return this.getPixelValue(this.getX(pixel), this.getY(pixel));
	}
	
	@Override
	public final AwtImage2D setPixelValue(final long pixel, final long value) {
		return this.setPixelValue(this.getX(pixel), this.getY(pixel), value);
	}
	
	@Override
	public final int getWidth() {
		return this.getSource().getWidth();
	}
	
	@Override
	public final int getHeight() {
		return this.getSource().getHeight();
	}
	
	@Override
	public final long getPixelValue(final int x, final int y) {
		return this.getSource().getRGB(x, y);
	}
	
	@Override
	public final AwtImage2D setPixelValue(final int x, final int y, final long value) {
		this.getSource().setRGB(x, y, (int) value);
		
		return this;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1768785762610491131L;
	
}
