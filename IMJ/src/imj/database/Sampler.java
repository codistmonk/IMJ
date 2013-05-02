package imj.database;

import imj.ByteList;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public abstract class Sampler implements PixelProcessor {
	
	private final Image image;
	
	private final Quantizer quantizer;
	
	private final Channel[] channels;
	
	private final ByteList sample;
	
	private final SampleProcessor processor;
	
	protected Sampler(final Image image, final Quantizer quantizer, final Channel[] channels, final SampleProcessor processor) {
		this.image = image;
		this.quantizer = quantizer;
		this.channels = channels;
		this.sample = new ByteList();
		this.processor = processor;
	}
	
	public final Image getImage() {
		return this.image;
	}
	
	public final Quantizer getQuantizer() {
		return this.quantizer;
	}
	
	public final Channel[] getChannels() {
		return this.channels;
	}
	
	public final ByteList getSample() {
		return this.sample;
	}
	
	public final SampleProcessor getProcessor() {
		return this.processor;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static abstract interface SampleProcessor {
		
		public abstract void process(ByteList sample);
		
	}
	
}

