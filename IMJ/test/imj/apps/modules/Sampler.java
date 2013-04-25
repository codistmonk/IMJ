package imj.apps.modules;

import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public abstract class Sampler implements PixelProcessor {
	
	private final Image image;
	
	private final Channel[] channels;
	
	private final byte[] sample;
	
	private final SampleProcessor processor;
	
	protected Sampler(final Image image, final Channel[] channels, final int sampleSize, final SampleProcessor processor) {
		this.image = image;
		this.channels = channels;
		this.sample = new byte[sampleSize];
		this.processor = processor;
	}
	
	public final Image getImage() {
		return this.image;
	}
	
	public final Channel[] getChannels() {
		return this.channels;
	}
	
	public final byte[] getSample() {
		return this.sample;
	}
	
	public final SampleProcessor getProcessor() {
		return this.processor;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static abstract interface SampleProcessor {
		
		public abstract void process(byte[] sample);
		
	}
	
}

