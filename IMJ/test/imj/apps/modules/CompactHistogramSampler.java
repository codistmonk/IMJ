package imj.apps.modules;

import static java.util.Arrays.sort;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class CompactHistogramSampler extends Sampler {
	
	private final int tilePixelCount;
	
	private final Map<Integer, int[]> histogram;
	
	private final Integer[] indices;
	
	private final int[] values;
	
	private final int[] counts;
	
	private int i;
	
	public CompactHistogramSampler(final Image image, final Channel[] channels,
			final int tilePixelCount, final SampleProcessor processor) {
		super(image, channels, tilePixelCount * (channels.length + 1), processor);
		this.tilePixelCount = tilePixelCount;
		this.histogram = new HashMap<Integer, int[]>();
		this.indices = new Integer[tilePixelCount];
		this.values = new int[tilePixelCount];
		this.counts = new int[tilePixelCount];
	}
	
	private final void count(final Integer value) {
		int[] count = this.histogram.get(value);
		
		if (count == null) {
			count = new int[] { 1 };
			this.histogram.put(value, count);
		} else {
			++count[0];
		}
	}
	
	@Override
	public final void process(final int pixel) {
		this.count(computeIndex(this.getImage().getValue(pixel), this.getChannels()));
		
		if (this.tilePixelCount <= ++this.i) {
			this.i = 0;
			
			this.postprocessHistogram();
			this.sortIndices();
			this.updateSample();
			
			this.getProcessor().process(this.getSample());
		}
	}
	
	private final void updateSample() {
		final int m = max(this.counts);
		
		for (int j = 0, k = 0; j < this.getSample().length; ++k) {
			final int index = this.indices[k];
			int value = this.values[index];
			final int count = this.counts[index];
			
			for (int channelIndex = this.getChannels().length - 1; 0 <= channelIndex; --channelIndex) {
				this.getSample()[j++] = (byte) (value & 0x000000FF);
				value >>= 8;
			}
			
			this.getSample()[j++] = (byte) (count * 255L / m);
		}
	}
	
	private final void sortIndices() {
		final int[] h = this.counts;
		
		sort(this.indices, new Comparator<Integer>() {
			
			@Override
			public final int compare(final Integer i1, final Integer i2) {
				return h[i2] - h[i1];
			}
			
		});
	}
	
	private final void postprocessHistogram() {
		for (int j = 0; j < this.indices.length; ++j) {
			this.indices[j] = j;
			this.values[j] = 0;
			this.counts[j] = 0;
		}
		
		{
			int j = 0;
			
			for (final Map.Entry<Integer, int[]> entry : this.histogram.entrySet()) {
				this.values[j] = entry.getKey();
				this.counts[j] = entry.getValue()[0];
				++j;
			}
			
			this.histogram.clear();
		}
	}
	
	public static final int max(final int[] values) {
		int result = Integer.MIN_VALUE;
		
		for (final int value : values) {
			if (result < value) {
				result = value;
			}
		}
		
		return result;
	}
	
	public static final int computeIndex(final int pixelValue, final Channel... channels) {
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << 8) | channel.getValue(pixelValue);
		}
		
		return result;
	}
	
}
