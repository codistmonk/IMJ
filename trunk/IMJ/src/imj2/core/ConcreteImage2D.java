package imj2.core;

import java.lang.reflect.Array;
import java.util.Arrays;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class ConcreteImage2D implements Image2D {
	
	private final Image source;
	
	private final int width;
	
	private final int height;
	
	public ConcreteImage2D(final Image source, final int width, final int height) {
		if (source.getPixelCount() != (long) width * height) {
			throw new IllegalArgumentException();
		}
		
		this.source = source;
		this.width = width;
		this.height = height;
	}
	
	public Image getSource() {
		return this.source;
	}
	
	@Override
	public final String getId() {
		return this.getSource().getId();
	}
	
	@Override
	public final long getPixelCount() {
		return this.getSource().getPixelCount();
	}
	
	@Override
	public final Channels getChannels() {
		return this.getSource().getChannels();
	}
	
	@Override
	public final int getPixelValue(final long pixelIndex) {
		return this.getSource().getPixelValue(pixelIndex);
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		this.getSource().setPixelValue(pixelIndex, pixelValue);
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
	public final int getPixelValue(final int x, final int y) {
		return this.getPixelValue(getPixelIndex(this, x, y));
	}
	
	@Override
	public final void setPixelValue(final int x, final int y, final int value) {
		this.setPixelValue(getPixelIndex(this, x, y), value);
	}
	
	@Override
	public final void forEachPixelInBox(final int left, final int top, final int width, final int height, final Process process) {
		forEachPixelInBox(this, left, top, width, height, process);
	}
	
	@Override
	public final ConcreteImage2D[] newParallelViews(final int n) {
		return newParallelViews(this, n);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7130245486896515156L;
	
	public static final long getPixelIndex(final Image2D image, final int x, final int y) {
		return (long) y * image.getWidth() + x;
	}
	
	public static final int getX(final Image2D image, final long pixelIndex) {
		return (int) (pixelIndex % image.getWidth());
	}
	
	public static final int getY(final Image2D image, final long pixelIndex) {
		return (int) (pixelIndex / image.getWidth());
	}
	
	public static final void forEachPixelInBox(final Image2D image, final int left, final int top,
			final int width, final int height, final Process process) {
		final int right = left + width;
		final int bottom = top + height;
		
		for (int y = top; y < bottom; ++y) {
			for (int x = left; x < right; ++x) {
				process.pixel(x, y);
			}
		}
		
		process.endOfPatch();
	}
	
	public static final <I extends Image> I[] newParallelViews(final I image, final int n) {
		try {
			final I[] result = (I[]) Array.newInstance(image.getClass(), n);
			
			Arrays.fill(result, image);
			
			return result;
		} catch (final NegativeArraySizeException exception) {
			throw Tools.unchecked(exception);
		}
	}
	
}
