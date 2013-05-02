package imj.database;

import static imj.IMJTools.square;
import static imj.IMJTools.unsigned;
import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.Metric;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class SparseHistogramSampler extends HistogramSampler {
	
	public SparseHistogramSampler(final Image image, final Quantizer quantizer, final Channel[] channels,
			final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
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
				
				if (result != 0) {
					return result;
				}
			}
			
			return 0;
		}
		
	}
	
}
