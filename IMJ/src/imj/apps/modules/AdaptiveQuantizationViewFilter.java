package imj.apps.modules;

import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj.Image;
import imj.database.AdaptiveQuantizer;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class AdaptiveQuantizationViewFilter extends ViewFilter {
	
	private final AdaptiveQuantizer quantizer;
	
	public AdaptiveQuantizationViewFilter(final Context context) {
		super(context);
		this.quantizer = new AdaptiveQuantizer();
		
		this.getParameters().put("bitCount", "0");
	}
	
	@Override
	protected final void sourceImageChanged() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			this.quantizer.updateHistograms(source, Sieve.getROI(this.getContext()));
		}
	}
	
	@Override
	protected final void doInitialize() {
		this.quantizer.initialize(this.getImage().getSource(), Sieve.getROI(this.getContext()),
				parseChannels(this.getParameters().get("channels")), this.getIntParameter("bitCount"));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return AdaptiveQuantizationViewFilter.this.getNewValue(channel, channel.getValue(oldValue));
			}
			
		};
	}
	
	public final int getNewValue(final Channel channel, final int channelValue) {
		return this.quantizer.getNewValue(channel, channelValue);
	}
	
	public static final int sum(final int[] histogram) {
		int result = 0;
		
		for (final int count : histogram) {
			result += count;
		}
		
		return result;
	}
	
	public static final void updateAccumulators(final int bitCount, final Channel[] channels, final int[][] histograms, final int[][] accumulators) {
		final int pixelCount = sum(histograms[channels[0].getChannelIndex()]);
		
		if (pixelCount == 0) {
			debugPrint();
			return;
		}
		
		for (final Channel channel : channels) {
			final int[] h = histograms[channel.getChannelIndex()];
			final int[] a = accumulators[channel.getChannelIndex()];
			int accumulator = 0;
			
			for (int j = 0; j < 256; ++j) {
				accumulator += h[j];
				
				a[j] = (int) (accumulator * 255L / pixelCount) >> bitCount;
			}
			
			for (int clusterStart = 0, clusterSize = 1; clusterStart < 256; clusterStart += clusterSize) {
				int clusterValue = clusterStart;
				int clusterValueCount = 1;
				clusterSize = 1;
				
				for (int channelValue = clusterStart + 1; channelValue < 256 && a[clusterStart] == a[channelValue]; ++channelValue) {
					final int channelValueCount = h[channelValue];
					
					if (0 < channelValueCount) {
						clusterValue += channelValue * channelValueCount;
						clusterValueCount += channelValueCount;
					}
					
					++clusterSize;
				}
				
				if (Channel.Synthetic.HUE.equals(channel)) {
					fill(a, clusterStart, clusterStart + clusterSize, clusterValue / clusterValueCount);
				} else {
					fill(a, clusterStart, clusterStart + clusterSize, clusterStart + clusterSize - 1);
				}
			}
		}
	}
	
}
