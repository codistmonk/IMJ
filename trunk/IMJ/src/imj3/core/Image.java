package imj3.core;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Image extends Serializable {
	
	public abstract long getPixelCount();
	
	public abstract Channels getChannels();
	
	public abstract long getPixelValue(long pixel);
	
	public abstract Image setPixelValue(long pixel, long value);
	
	public default long getPixelChannelValue(final long pixel, final int channelIndex) {
		return this.getChannels().getChannelValue(this.getPixelValue(pixel), channelIndex);
	}
	
	public default Image setPixelChannelValue(final long pixel, final int channelIndex, final long channelValue) {
		return this.setPixelValue(pixel, this.getChannels().setChannelValue(this.getPixelValue(pixel), channelIndex, channelValue));
	}
	
	public default Image forEachPixel(final PixelProcessor process) {
		final long n = this.getPixelCount();
		
		for (long i = 0; i < n && process.pixel(i); ++i) {
			// NOP
		}
		
		return this;
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 */
	public static abstract interface PixelProcessor extends Serializable {
		
		public abstract boolean pixel(long pixel);
		
	}
	
}
