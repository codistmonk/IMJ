package imj.database;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author codistmonk (creation 2013-05-03)
 */
public abstract class HistogramSampler extends Sampler {
	
	private int patchPixelCount;
	
	private final Map<Integer, int[]> histogram;
	
	private Integer[] indices;
	
	private int[] values;
	
	private int[] counts;
	
	public HistogramSampler(final Image image, final Quantizer quantizer, final Channel[] channels,
			final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
		this.histogram = new TreeMap<Integer, int[]>();
	}
	
	@Override
	public final void process(final int pixel) {
		final int pixelValue = this.getImage().getValue(pixel);
		
		this.getProcessor().processPixel(pixel, pixelValue);
		
		if (this.getQuantizer() != null) {
			count(this.histogram, computeIndex(this.getQuantizer(), pixelValue, this.getChannels()));
		} else {
			count(this.histogram, computeIndex(pixelValue, this.getChannels()));
		}
		
		++this.patchPixelCount;
	}
	
	@Override
	public final void finishPatch() {
		this.postprocessHistogram();
		this.sortIndices(this.indices, this.counts);
		this.updateSample();
		this.getProcessor().processSample(this.getSample());
		this.getSample().clear();
		this.patchPixelCount = 0;
	}
	
	protected void sortIndices(final Integer[] indices, final int[] counts) {
		// NOP
	}
	
	private final void postprocessHistogram() {
		this.indices = new Integer[this.patchPixelCount];
		this.values = new int[this.patchPixelCount];
		this.counts = new int[this.patchPixelCount];
		
		for (int j = 0; j < this.indices.length; ++j) {
			this.indices[j] = j;
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
	
	private final void updateSample() {
		final int m = this.patchPixelCount;
		final int countQuantizationMask = 0x000000F8;
		
		for (int k = 0; k < this.patchPixelCount; ++k) {
			final int index = this.indices[k];
			int value = this.values[index];
			final int count = this.counts[index];
			
			if (count <= 0) {
				break;
			}
			
			for (int channelIndex = this.getChannels().length - 1; 0 <= channelIndex; --channelIndex) {
				this.getSample().add((byte) (value & 0x000000FF));
				value >>= 8;
			}
			
			this.getSample().add((byte) ((count * 255L / m) & countQuantizationMask));
		}
	}
	
	public static final int computeIndex(final int pixelValue, final Channel... channels) {
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << 8) | channel.getValue(pixelValue);
		}
		
		return result;
	}
	
	public static final int computeIndex(final Quantizer quantizer, final int pixelValue, final Channel... channels) {
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << 8) | quantizer.getNewValue(channel, channel.getValue(pixelValue));
		}
		
		return result;
	}
	
	public static final void count(final Map<Integer, int[]> histogram, final Integer value) {
		final int[] count = histogram.get(value);
		
		if (count == null) {
			histogram.put(value, new int[] { 1 });
		} else {
			++count[0];
		}
	}
	
}
