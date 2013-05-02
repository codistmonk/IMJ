package imj.database;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class LinearSampler extends Sampler {
	
	public LinearSampler(final Image image, final Quantizer quantizer, final Channel[] channels, final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
	}
	
	@Override
	public final void process(final int pixel) {
		final int pixelValue = this.getImage().getValue(pixel);
		
		if (this.getQuantizer() != null) {
			for (final Channel channel : this.getChannels()) {
				this.getSample().add((byte) this.getQuantizer().getNewValue(channel, channel.getValue(pixelValue)));
			}
		} else {
			for (final Channel channel : this.getChannels()) {
				this.getSample().add((byte) channel.getValue(pixelValue));
			}
		}
	}
	
	@Override
	public final void finishPatch() {
		this.getProcessor().process(this.getSample());
		this.getSample().clear();
	}
	
}
	