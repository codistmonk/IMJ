package imj2.core;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class LinearPackedGrayImage extends Image.Abstract {
	
	private final String id;
	
	private final long pixelCount;
	
	private final Channels channels;
	
	private final int pixelsPerDatum;
	
	private final long shiftedPixelMask;
	
	private final long[] data;
	
	public LinearPackedGrayImage(final String id, final long pixelCount, final Channels channels) {
		if (1 != channels.getChannelCount()) {
			throw new IllegalArgumentException();
		}
		
		this.id = id;
		this.channels = channels;
		this.pixelCount = pixelCount;
		final int channelBitCount = channels.getChannelBitCount();
		this.pixelsPerDatum = Long.SIZE / channelBitCount;
		this.shiftedPixelMask = (~0L) >>> (Long.SIZE - channelBitCount);
		this.data = new long[(int) ((pixelCount + this.pixelsPerDatum - 1) / this.pixelsPerDatum)];
	}
	
	@Override
	public final Image getSource() {
		return null;
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
	public final int getPixelValue(final long pixelIndex) {
		final int datumIndex = (int) (pixelIndex / this.pixelsPerDatum);
		final int pixelShift = (int) (pixelIndex % this.pixelsPerDatum) * this.getChannels().getChannelBitCount();
		final long datum = this.data[datumIndex];
		
		return (int) ((datum >>> pixelShift) & this.shiftedPixelMask);
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		final int datumIndex = (int) (pixelIndex / this.pixelsPerDatum);
		final int pixelShift = (int) (pixelIndex % this.pixelsPerDatum) * this.getChannels().getChannelBitCount();
		final long datum = this.data[datumIndex];
		
		this.data[datumIndex] = (datum & ~(this.shiftedPixelMask << pixelShift))
				| ((pixelValue & this.shiftedPixelMask) << pixelShift);
	}
	
	@Override
	public final LinearPackedGrayImage[] newParallelViews(final int n) {
		return IMJCoreTools.newParallelViews(this, n);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 5253129129058704301L;
	
}
