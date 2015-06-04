package imj.database;

import imj.Image;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-05-02)
 */
public abstract class Quantizer {
	
	private int quantumBitCount;
	
	private Channel[] channels;
	
	public abstract void initialize(Image image, RegionOfInterest roi, Channel[] channels, int quantumBitCount);
	
	public abstract int getNewValue(Channel channel, int channelValue);
	
	public final int getQuantumBitCount() {
		return this.quantumBitCount;
	}
	
	public final Channel[] getChannels() {
		return this.channels;
	}
	
	protected final void setQuantumBitCount(final int quantumBitCount) {
		this.quantumBitCount = quantumBitCount;
	}
	
	protected final void setChannels(final Channel[] channels) {
		this.channels = channels;
	}
	
}
