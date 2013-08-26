package imj2.tools;

import static imj2.tools.IMJTools.contains;
import static imj2.tools.IMJTools.getChannelValues;

import imj2.core.Image.Channels;
import imj2.core.Image2D;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2013-08-26)
 */
public final class LocalBinaryPatternGenerator implements Serializable {
	
	private final Image2D image;
	
	private final Channels channels;
	
	private final int channelBitCount;
	
	private final int channelCount;
	
	private final transient int[] pixelChannelValues;
	
	public LocalBinaryPatternGenerator(final Image2D image) {
		this.image = image;
		this.channels = image.getChannels();
		this.channelBitCount = this.channels.getChannelBitCount();
		this.channelCount = this.channels.getChannelCount();
		this.pixelChannelValues = getChannelValues(this.channels, 0, null);
	}
	
	public final int getPatternAt(final int x, final int y) {
		getChannelValues(this.channels, this.image.getPixelValue(x, y), this.pixelChannelValues);
		
		int result = this.getPatternBits(x - 1, y - 1);
		result = (result << 1) | this.getPatternBits(x, y - 1);
		result = (result << 1) | this.getPatternBits(x + 1, y - 1);
		result = (result << 1) | this.getPatternBits(x + 1, y);
		result = (result << 1) | this.getPatternBits(x + 1, y + 1);
		result = (result << 1) | this.getPatternBits(x, y + 1);
		result = (result << 1) | this.getPatternBits(x - 1, y + 1);
		result = (result << 1) | this.getPatternBits(x - 1, y);
		
		return result;
	}
	
	private final int getPatternBits(final int neighborX, final int neighborY) {
		if (!contains(this.image, neighborX, neighborY)) {
			return 0;
		}
		
		final int neighborValue = this.image.getPixelValue(neighborX, neighborY);
		int result = 0;
		
		for (int channelIndex = 0; channelIndex < this.channelCount; ++channelIndex) {
			final int pixelChannelValue = this.pixelChannelValues[channelIndex];
			final int neighborChannelValue = this.channels.getChannelValue(neighborValue, channelIndex);
			final int bit = neighborChannelValue < pixelChannelValue ? 1 : 0;
			result = (result << this.channelBitCount) | bit;
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -1861557434497604653L;
	
}
