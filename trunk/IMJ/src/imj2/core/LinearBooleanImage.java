package imj2.core;

import java.util.BitSet;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class LinearBooleanImage implements Image {
	
	private final String id;
	
	private final BitSet data;
	
	private final long pixelCount;
	
	public LinearBooleanImage(final String id, final long pixelCount) {
		this.id = id;
		this.data = new BitSet();
		this.pixelCount = pixelCount;
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
		return PredefinedChannels.C1_U1;
	}
	
	@Override
	public final int getPixelValue(final long pixelIndex) {
		return this.data.get((int) pixelIndex) ? 1 : 0;
	}
	
	@Override
	public final void setPixelValue(final long pixelIndex, final int pixelValue) {
		final int bitIndex = (int) pixelIndex;
		
		if (this.data.get(bitIndex) != (pixelValue != 0)) {
			this.data.flip(bitIndex);
		}
	}
	
	@Override
	public final LinearBooleanImage[] newParallelViews(final int n) {
		return IMJCoreTools.newParallelViews(this, n);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7501385138271073364L;
	
}
