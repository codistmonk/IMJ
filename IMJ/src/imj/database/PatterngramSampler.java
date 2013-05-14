package imj.database;

import static imj.IMJTools.square;
import static imj.IMJTools.unsigned;
import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.Metric;
import imj.database.PatterngramSampler.SparseImage.PixelProcessor;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author codistmonk (creation 2013-05-03)
 */
public final class PatterngramSampler extends Sampler {
	
	private final SparseImage patch;
	
	private final Map<Integer, int[]>[] histograms;
	
	private int patchPixelCount;
	
	@SuppressWarnings("unchecked")
	public PatterngramSampler(final Image image, final Quantizer quantizer, final Channel[] channels,
			final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
		this.patch = new SparseImage(image.getRowCount(), image.getColumnCount(), image.getChannelCount());
		this.histograms = new Map[2];
		
		for (int i = 0; i < this.histograms.length; ++i) {
			this.histograms[i] = new TreeMap<Integer, int[]>();
		}
	}
	
	@Override
	public final void process(final int pixel) {
		final int pixelValue = this.getImage().getValue(pixel);
		
		this.getProcessor().processPixel(pixel, pixelValue);
		
		this.patch.setValue(pixel, pixelValue);
		
		++this.patchPixelCount;
	}
	
	@Override
	public final void finishPatch() {
		final int imageRowCount = PatterngramSampler.this.getImage().getRowCount();
		final int imageColumnCount = PatterngramSampler.this.getImage().getColumnCount();
		
		this.patch.forEachPixel(new PixelProcessor() {
			
			private final int[] patternPixelValues = { 0, 0 };
			
			@Override
			public final void process(final int pixel, final int pixelValue) {
				final Integer valueIndex = PatterngramSampler.this.computeIndex(pixelValue);
				PatterngramSampler.this.count(0, valueIndex);
				
				final int pixelRowIndex = valueIndex / imageColumnCount;
				final int pixelColumnIndex = valueIndex % imageColumnCount;
				
				if (pixelColumnIndex + 1 < imageColumnCount) {
					final int[] eastValueHolder = PatterngramSampler.this.getPatchValueHolder(pixel + 1);
					
					if (null != eastValueHolder) {
						this.patternPixelValues[0] = pixelValue;
						this.patternPixelValues[1] = eastValueHolder[0];
						final Integer eastValueIndex = PatterngramSampler.this.computeIndex(pixelValue);
						PatterngramSampler.this.count(1, eastValueIndex);
					}
				}
				
				if (pixelRowIndex + 1 < imageRowCount) {
					final int[] southValueHolder = PatterngramSampler.this.getPatchValueHolder(pixel + imageColumnCount);
					
					if (null != southValueHolder) {
						this.patternPixelValues[0] = pixelValue;
						this.patternPixelValues[1] = southValueHolder[0];
						final Integer southValueIndex = PatterngramSampler.this.computeIndex(pixelValue);
						PatterngramSampler.this.count(1, southValueIndex);
					}
				}
			}
			
		});
		
		final int histogramCount = this.histograms.length;
		final int m = this.patchPixelCount;
		
		for (int histogramIndex = 0; histogramIndex < histogramCount; ++histogramIndex) {
			final Map<Integer, int[]> histogram = this.histograms[histogramIndex];
			
			for (final Map.Entry<Integer, int[]> entry : histogram.entrySet()) {
				this.getSample().add((byte) histogramIndex);
				
				int value = entry.getKey();
				final int count = entry.getValue()[0];
				
				for (int channelIndex = this.getChannels().length - 1; 0 <= channelIndex; --channelIndex) {
					this.getSample().add((byte) (value & 0x000000FF));
					value >>= 8;
				}
				
				this.getSample().add((byte) (count * 255L / m));
			}
			
			histogram.clear();
		}
		
		assert 0 == (this.getSample().size() % (2 + this.getChannels().length));
		
		this.getProcessor().processSample(this.getSample());
//		Tools.debugPrint(this.getSample());
		this.getSample().clear();
		this.patchPixelCount = 0;
		this.patch.clear();
	}
	
	final int[] getPatchValueHolder(final int index) {
		return this.patch.getValueHolder(index);
	}
	
	final void count(final int histogramIndex, final Integer value) {
		HistogramSampler.count(this.histograms[histogramIndex], value);
	}
	
	final int computeIndex(final int[] pixelValues) {
		final int[] accumulators = new int[this.getChannels().length];
		
		if (this.getQuantizer() != null) {
			for (final int pixelValue : pixelValues) {
				int i = 0;
				
				for (final Channel channel : this.getChannels()) {
					accumulators[i++] = this.getQuantizer().getNewValue(channel, channel.getValue(pixelValue));
				}
			}
		} else {
			for (final int pixelValue : pixelValues) {
				int i = 0;
				
				for (final Channel channel : this.getChannels()) {
					accumulators[i++] = channel.getValue(pixelValue);
				}
			}
		}
		
		int result = 0;
		
		for (final int value : accumulators) {
			result = (result << 8) | ((value / pixelValues.length) & 0x000000FF);
		}
		
		return result;
	}
	
	final int computeIndex(final int pixelValue) {
		if (this.getQuantizer() != null) {
			return HistogramSampler.computeIndex(this.getQuantizer(), pixelValue, this.getChannels());
		}
		
		return HistogramSampler.computeIndex(pixelValue, this.getChannels());
	}
	
	/**
	 * @author codistmonk (creation 2013-05-02)
	 */
	public static final class PatterngramMetric implements Metric<byte[]> {
		
		private final int channelCount;
		
		private final int chunkSize;
		
		public PatterngramMetric(final int channelCount) {
			this.channelCount = channelCount;
			this.chunkSize = channelCount + 2;
		}
		
		@Override
		public final long getDistance(final byte[] h0, final byte[] h1) {
			long sumOfSquares = 0L;
			int i0 = 1;
			int i1 = 1;
			
			while (i0 < h0.length || i1 < h1.length) {
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
			
//			if (0L < sumOfSquares) {
//				debugPrint(Arrays.toString(h0));
//				debugPrint(Arrays.toString(h1));
//				debugPrint(ceil(sqrt(sumOfSquares)));
//				
//				assert false;
//			}
			
			return (long) ceil(sqrt(sumOfSquares));
		}
		
		private final int compare(final byte[] h0, final int i0, final byte[] h1, final int i1) {
			if (h0.length <= i0) {
				return +1;
			}
			
			if (h1.length <= i1) {
				return -1;
			}
			
			for (int j = 0; j < this.channelCount; ++j) {
				final int result = unsigned(h0[i0 + j]) - unsigned(h1[i1 + j]);
				
//				debugPrint(j, i0, unsigned(h0[i0 + j]), i1, unsigned(h1[i1 + j]), result);
				
				if (result != 0) {
					return result;
				}
			}
			
			return 0;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-05-14)
	 */
	public static final class SparseImage extends Image.Abstract {
		
		private final Map<Integer, int[]> data;
		
		public SparseImage(final int rowCount, final int columnCount, final int channelCount) {
			super(rowCount, columnCount, channelCount);
			this.data = new TreeMap<Integer, int[]>();
		}
		
		public final void clear() {
			this.data.clear();
		}
		
		public final void forEachPixel(final PixelProcessor processor) {
			for (final Map.Entry<Integer, int[]> entry : this.data.entrySet()) {
				processor.process(entry.getKey(), entry.getValue()[0]);
			}
		}
		
		public final int[] getValueHolder(final int index) {
			return this.data.get(index);
		}
		
		@Override
		public final int getValue(final int index) {
			final int[] valueHolder = this.getValueHolder(index);
			
			return valueHolder == null ? 0 : valueHolder[0];
		}
		
		@Override
		public final int setValue(final int index, final int value) {
			int[] valueHolder = this.getValueHolder(index);
			
			if (valueHolder == null) {
				valueHolder = new int[1];
				this.data.put(index, valueHolder);
			}
			
			final int result = valueHolder[0];
			
			valueHolder[0] = value;
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2013-05-14)
		 */
		public static abstract interface PixelProcessor {
			
			public abstract void process(int pixel, int pixelValue);
			
		}
		
	}
	
}
