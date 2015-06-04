package imj3.core;

import java.io.Serializable;
import java.util.Map;

/**
 * @author codistmonk (creation 2014-11-29)
 */
public abstract interface Image extends Serializable {
	
	public abstract Map<String, Object> getMetadata();
	
	public abstract String getId();
	
	public abstract long getPixelCount();
	
	public abstract Channels getChannels();
	
	public abstract long getPixelValue(long pixel);
	
	public abstract Image setPixelValue(long pixel, long value);
	
	public default double[] getPixelValue(final long pixel, final double[] result) {
		final int channelCount = this.getChannels().getChannelCount();
		final double[] actualResult = actualResult(result, channelCount);
		
		for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
			actualResult[channelIndex] = this.getPixelChannelValue(pixel, channelIndex);
		}
		
		return actualResult;
	}
	
	public default Image setPixelValue(final long pixel, final double[] value) {
		final int channelCount = this.getChannels().getChannelCount();
		
		for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
			this.setPixelChannelValue(pixel, channelIndex, Double.doubleToRawLongBits(value[channelIndex]));
		}
		
		return this;
	}
	
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
	
	public static double[] actualResult(final double[] result, final int expectedLength) {
		return result != null && result.length == expectedLength ? result : new double[expectedLength];
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 */
	public static abstract interface PixelProcessor extends Serializable {
		
		public abstract boolean pixel(long pixel);
		
	}
	
}
