package imj.database;

import static java.lang.Math.max;
import static java.util.Arrays.fill;

import imj.IMJTools;
import imj.Image;
import imj.apps.modules.AdaptiveQuantizationViewFilter;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Arrays;

/**
 * @author codistmonk (creation 2013-04-30)
 */
public final class AdaptiveQuantizer extends Quantizer {
	
	private int[][] histograms;
	
	private int[][] accumulators;
	
	@Override
	public final void initialize(final Image image, final RegionOfInterest roi, final Channel[] channels, final int quantumBitCount) {
		final Channel[] oldChannels = this.getChannels();
		
		this.setChannels(channels);
		this.setQuantumBitCount(quantumBitCount);
		
		if (!Arrays.equals(this.getChannels(), oldChannels)) {
			int n = 0;
			
			for (final Channel channel : this.getChannels()) {
				n = max(n, channel.getChannelIndex() + 1);
			}
			
			this.histograms = new int[n][256];
			this.accumulators = new int[n][256];
			
			if (image != null) {
				this.updateHistograms(image, roi);
			}
		} else {
			this.updateAccumulators();
		}
	}
	
	@Override
	public final int getNewValue(final Channel channel, final int channelValue) {
		return this.accumulators[channel.getChannelIndex()][channelValue];
	}
	
	public final void updateHistograms(final Image image, final RegionOfInterest roi) {
		this.clearHistograms();
		IMJTools.updateHistograms(image, roi, this.getChannels(), this.histograms);
		this.updateAccumulators();
	}
	
	private final void clearHistograms() {
		for (final int[] histogram : this.histograms) {
			fill(histogram, 0);
		}
	}
	
	private final void updateAccumulators() {
		AdaptiveQuantizationViewFilter.updateAccumulators(this.getQuantumBitCount(), this.getChannels(),
				this.histograms, this.accumulators);
	}
	
}
