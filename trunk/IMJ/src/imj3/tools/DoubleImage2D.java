package imj3.tools;

import static java.util.Arrays.stream;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Channels;
import imj3.core.Image;
import imj3.core.Image2D;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.MathTools.VectorStatistics;

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
			debugError(pixel, n, value.length, this.data.length);
			
			throw unchecked(exception);
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
	
	@Override
	public final BufferedImage toAwt() {
		final BufferedImage result = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final int channelCount = this.getChannels().getChannelCount();
		final int displayedChannels = channelCount <= 3 ? channelCount : 1;
		final VectorStatistics statistics = new VectorStatistics(channelCount);
		final double[] pixelValue = new double[channelCount];
		
		this.forEachPixel(pixel -> {
			statistics.addValues(this.getPixelValue(pixel, pixelValue));
			
			return true;
		});
		
		final double minimum = Arrays.stream(statistics.getMinima()).min().getAsDouble();
		final double maximum = Arrays.stream(statistics.getMaxima()).max().getAsDouble();
		final boolean normalize = minimum < 0.0 || 256.0 <= minimum || maximum <= 0.0 || 256.0 <= maximum;
		
		this.forEachPixel((x, y) -> {
			this.getPixelValue(x, y, pixelValue);
			
			if (channelCount == displayedChannels && 1 < displayedChannels) {
				int rgb = 0xFF000000;
				
				if (normalize) {
					final double[] normalizedPixelValue = statistics.getNormalizedValues(pixelValue);
					
					for (int i = 0; i < displayedChannels; ++i) {
						rgb |= (0xFF & (int) (normalizedPixelValue[i] * 255.0)) << (8 * i);
					}
				} else {
					for (int i = 0; i < displayedChannels; ++i) {
						rgb |= (0xFF & (int) (pixelValue[i])) << (8 * i);
					}
				}
				
				result.setRGB(x, y, rgb);
			} else {
				if (normalize) {
					final double[] normalizedPixelValue = statistics.getNormalizedValues(pixelValue);
					
					result.setRGB(x, y, 0xFF000000 | (0x00010101 * (0xFF & (int) (stream(normalizedPixelValue).average().getAsDouble() * 255.0))));
				} else {
					result.setRGB(x, y, 0xFF000000 | (0x00010101 * (0xFF & (int) stream(pixelValue).average().getAsDouble())));
				}
			}
			
			
			return true;
		});
		
		// TODO Auto-generated method stub
		return result;
	}
	
	private static final long serialVersionUID = 9009222978487985122L;
	
}