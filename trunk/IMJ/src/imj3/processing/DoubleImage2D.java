package imj3.processing;

import imj3.core.Channels;
import imj3.core.Image;
import imj3.core.Image2D;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-01)
 */
public final class DoubleImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String id;
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private final double[] data;
	
	public DoubleImage2D(final String id, final int width, final int height, final int channelCount) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.width = width;
		this.height = height;
		this.channels = new Channels.Default(channelCount, Double.SIZE);
		this.data = new double[width * height * channelCount];
	}
	
	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
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
	public final long getPixelValue(final long pixel) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final DoubleImage2D setPixelValue(final long pixel, final double[] value) {
		final int n = this.getChannels().getChannelCount();
		
		try {
			System.arraycopy(value, 0, this.data, (int) (pixel * n), n);
		} catch (final Exception exception) {
			Tools.debugError(pixel, n, value.length, this.data.length);
			
			throw Tools.unchecked(exception);
		}
		
		return this;
	}
	
	@Override
	public final double[] getPixelValue(final long pixel, final double[] result) {
		final int n = this.getChannels().getChannelCount();
		final double[] actualResult = Image.actualResult(result, n);
		
		System.arraycopy(this.data, (int) (pixel * n), actualResult, 0, n);
		
		return actualResult;
	}
	
	@Override
	public final long getPixelChannelValue(final long pixel, final int channelIndex) {
		return Double.doubleToRawLongBits(this.data[(int) (pixel * this.getChannels().getChannelCount() + channelIndex)]);
	}
	
	@Override
	public final DoubleImage2D setPixelChannelValue(final long pixel, final int channelIndex, final long channelValue) {
		this.data[(int) (pixel * this.getChannels().getChannelCount() + channelIndex)] = Double.longBitsToDouble(channelValue);
		
		return this;
	}
	
	private static final long serialVersionUID = 9009222978487985122L;
	
}