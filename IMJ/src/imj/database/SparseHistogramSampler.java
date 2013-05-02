package imj.database;

import static imj.IMJTools.square;
import static imj.IMJTools.unsigned;
import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import imj.Image;
import imj.apps.modules.AdaptiveQuantizationViewFilter.AdaptiveQuantizer;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.Metric;

import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class SparseHistogramSampler extends Sampler {
	
	private final int tilePixelCount;
	
	private final Map<Integer, int[]> histogram;
	
	private final Integer[] indices;
	
	private final int[] values;
	
	private final int[] counts;
	
	private int i;
	
	public SparseHistogramSampler(final Image image, final AdaptiveQuantizer quantizer, final Channel[] channels,
			final int tilePixelCount, final SampleProcessor processor) {
		super(image, quantizer, channels, tilePixelCount * (channels.length + 1), processor);
		this.tilePixelCount = tilePixelCount;
		this.histogram = new TreeMap<Integer, int[]>();
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
		if (this.getQuantizer() != null) {
			this.count(computeIndex(this.getQuantizer(), this.getImage().getValue(pixel), this.getChannels()));
		} else {
			this.count(computeIndex(this.getImage().getValue(pixel), this.getChannels()));
		}
		
		if (this.tilePixelCount <= ++this.i) {
			this.i = 0;
			
			this.postprocessHistogram();
			this.updateSample();
			
			this.getProcessor().process(this.getSample());
		}
	}
	
	private final void updateSample() {
		final int m = this.tilePixelCount;
		
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
	
	public static final int computeIndex(final int pixelValue, final Channel... channels) {
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << 8) | channel.getValue(pixelValue);
		}
		
		return result;
	}
	
	public static final int computeIndex(final AdaptiveQuantizer quantizer, final int pixelValue, final Channel... channels) {
		int result = 0;
		
		for (final Channel channel : channels) {
			result = (result << 8) | quantizer.getNewValue(channel, channel.getValue(pixelValue));
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-05-02)
	 */
	public static final class SparseHistogramMetric implements Metric<byte[]> {
		
		private final int channelCount;
		
		private final int chunkSize;
		
		public SparseHistogramMetric(final int channelCount) {
			this.channelCount = channelCount;
			this.chunkSize = channelCount + 1;
		}
		
		@Override
		public final long getDistance(final byte[] h0, final byte[] h1) {
			long sumOfSquares = 0L;
			int i0 = 0;
			int i1 = 0;
			
			while (i0 < h0.length && i1 < h1.length) {
				final int colorDifference = this.compare(h0, i0, h1, i1);
				
				if (colorDifference < 0) {
					sumOfSquares += square(h0[i0 + this.channelCount]);
					i0 += this.chunkSize;
				} else if (colorDifference == 0) {
					sumOfSquares += square(h1[i1 + this.channelCount] - h0[i0 + this.channelCount]);
					i0 += this.chunkSize;
					i1 += this.chunkSize;
				} else {
					sumOfSquares += square(h1[i1 + this.channelCount]);
					i1 += this.chunkSize;
				}
			}
			
			return (long) ceil(sqrt(sumOfSquares));
		}
		
		private final int compare(final byte[] h0, final int i0, final byte[] h1, final int i1) {
			for (int j = 0; j < this.channelCount; ++j) {
				final int result = unsigned(h0[i0 + j]) - unsigned(h1[i1 + j]);
				
				if (result != 0) {
					return result;
				}
			}
			
			return 0;
		}
		
	}
	
}
