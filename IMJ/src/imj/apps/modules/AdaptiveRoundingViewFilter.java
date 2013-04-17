package imj.apps.modules;

import static imj.IMJTools.forEachPixel;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.util.Arrays.fill;
import imj.IMJTools.PixelProcessor;
import imj.Image;

import java.util.Arrays;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class AdaptiveRoundingViewFilter extends ViewFilter {
	
	private int bitCount;
	
	private Channel[] channels;
	
	private int[][] histograms;
	
	private int[][] accumulators;
	
	public AdaptiveRoundingViewFilter(final Context context) {
		super(context);
//		this.histograms = new int[CHANNELS.length][256];
//		this.accumulators = new int[CHANNELS.length][256];
		
		this.getParameters().put("bitCount", "0");
	}
	
	@Override
	protected final void sourceImageChanged() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			this.clearHistograms();
			updateHistograms(source, Sieve.getROI(this.getContext()), this.channels, this.histograms);
			this.updateAccumulators();
		}
	}
	
	public final int transform(final Channel channel, final int channelValue) {
		return this.accumulators[channel.getChannelIndex()][channelValue];
	}
	
	@Override
	protected final void doInitialize() {
		final Channel[] oldChannels = this.channels;
		this.channels = parseChannels(this.getParameters().get("channels"));
		this.bitCount = parseInt(this.getParameters().get("bitCount"));
		
		if (!Arrays.equals(this.channels, oldChannels)) {
			int n = 0;
			
			for (final Channel channel : this.channels) {
				n = max(n, channel.getChannelIndex() + 1);
			}
			
			this.histograms = new int[n][256];
			this.accumulators = new int[n][256];
			this.sourceImageChanged();
		} else {
			this.updateAccumulators();
		}
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return AdaptiveRoundingViewFilter.this.transform(channel, channel.getValue(oldValue));
			}
			
		};
	}
	
	private final void clearHistograms() {
		for (final int[] histogram : this.histograms) {
			fill(histogram, 0);
		}
	}
	
	private final void updateAccumulators() {
		if (this.getImage().getSource() == null) {
			return;
		}
		
		final int channelCount = this.histograms.length;
		final int pixelCount = this.getImage().getPixelCount();
		
		for (int i = 0; i < channelCount; ++i) {
			final int[] h = this.histograms[i];
			final int[] a = this.accumulators[i];
			int accumulator = 0;
			
			for (int j = 0; j < 256; ++j) {
				accumulator += h[j];
				
				a[j] = (int) (accumulator * 255L / pixelCount) >> this.bitCount;
			}
			
			for (int j = 0, clusterSize = 1; j < 256; j += clusterSize) {
				clusterSize = 1;
				
				for (int k = j + 1; k < 256 && a[j] == a[k]; ++k) {
					++clusterSize;
				}
				
				fill(a, j, j + clusterSize, j + clusterSize - 1);
			}
		}
	}
	
//	private static final Channel[] CHANNELS = { RED, GREEN, BLUE };
	
	public static final void updateHistograms(final Image image, final RegionOfInterest roi, final Channel[] channels,
			final int[][] histograms) {
		forEachPixel(image, roi, new PixelProcessor() {
			
			@Override
			public final void process(final int pixel) {
				if (roi == null || roi.get(pixel)) {
					for (final Channel channel : channels) {
						++histograms[channel.getChannelIndex()][channel.getValue(image.getValue(pixel))];
					}
				}
			}
			
		});
	}
	
}
