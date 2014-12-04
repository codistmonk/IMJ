package imj3.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

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
	
	private final Channels channels;
	
	public AwtImage2D(final String id, final int width, final int height) {
		this(id, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
	}
	
	public AwtImage2D(final String id) {
		this(id, awtRead(id));
	}
	
	public AwtImage2D(final String id, final BufferedImage source) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.source = source;
		this.pixelCount = (long) source.getWidth() * source.getHeight();
		this.channels = predefinedChannelsFor(source);
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
		return this.channels;
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
	
	public static final BufferedImage awtRead(final String path) {
		try {
			return ImageIO.read(new File(path));
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final Channels predefinedChannelsFor(final BufferedImage awtImage) {
		switch (awtImage.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
			return 1 == awtImage.getColorModel().getPixelSize() ?
					Channels.Predefined.C1_U1 : Channels.Predefined.C3_U8;
		case BufferedImage.TYPE_USHORT_GRAY:
			return Channels.Predefined.C1_U16;
		case BufferedImage.TYPE_BYTE_GRAY:
			return Channels.Predefined.C1_U8;
		case BufferedImage.TYPE_3BYTE_BGR:
			return Channels.Predefined.C3_U8;
		default:
			return Channels.Predefined.A8R8G8B8;
		}
	}
	
}
