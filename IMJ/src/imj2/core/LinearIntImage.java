package imj2.core;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class LinearIntImage implements Image {
	
	private final String id;
	
	private final int[] data;
	
	private final Channels channels;
	
	public LinearIntImage(final String id, final long pixelCount, final Channels channels) {
		this.id = id;
		this.data = new int[(int) pixelCount];
		this.channels = channels;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final long getPixelCount() {
		return this.data.length;
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@Override
	public final int getPixelValue(final long pixelIndex) {
		return this.data[(int) pixelIndex];
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		this.data[(int) pixelIndex] = pixelValue;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8861317534696763860L;
	
}
