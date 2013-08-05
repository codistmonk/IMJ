package imj2.core;

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
		return this.getPixelValue(this.getPixelIndex(x, y));
	}
	
	@Override
	public final void setPixelValue(final int x, final int y, final int value) {
		this.setPixelValue(this.getPixelIndex(x, y), value);
	}
	
	public final long getPixelIndex(final int x, final int y) {
		return (long) y * this.getWidth() + x;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7130245486896515156L;
	
}