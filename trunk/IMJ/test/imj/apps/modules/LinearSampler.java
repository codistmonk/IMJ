package imj.apps.modules;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class LinearSampler extends Sampler {
	
	private final int tilePixelCount;
	
	private int i;
	
	public LinearSampler(final Image image, final Channel[] channels,
			final int tilePixelCount, final SampleProcessor processor) {
		super(image, channels, tilePixelCount * channels.length, processor);
		this.tilePixelCount = tilePixelCount;
	}
	
	@Override
	public final void process(final int pixel) {
		final int pixelValue = this.getImage().getValue(pixel);
		
		for (final Channel channel : this.getChannels()) {
			this.getSample()[this.i++] = (byte) channel.getValue(pixelValue);
		}
		
		if (this.tilePixelCount <= this.i) {
			this.i = 0;
			
			this.getProcessor().process(this.getSample());
		}
	}
	
}
	