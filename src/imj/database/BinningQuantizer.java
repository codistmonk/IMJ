package imj.database;

import imj.Image;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-30)
 */
public final class BinningQuantizer extends Quantizer {
	
	private int mask;
	
	@Override
	public final void initialize(final Image image, final RegionOfInterest roi, final Channel[] channels, final int quantumBitCount) {
		this.setChannels(channels);
		this.setQuantumBitCount(quantumBitCount);
		
		this.mask = ~0 << quantumBitCount;
	}
	
	@Override
	public final int getNewValue(final Channel channel, final int channelValue) {
		return channelValue & this.mask;
	}
	
}
