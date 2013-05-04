package imj.database;

import imj.Image;
import imj.apps.modules.FilteredImage.StatisticsFilter.ChannelStatistics;
import imj.apps.modules.ViewFilter.Channel;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class StatisticsSampler extends Sampler {
	
	private final Channel[] channels;
	
	private final ChannelStatistics[] statistics;
	
	private final ChannelStatistics.Selector selector;
	
	public StatisticsSampler(final Image image, final Quantizer quantizer, final Channel[] channels, final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
		this.channels = IMJDatabaseTools.RGB;
		final int n = this.channels.length;
		this.statistics = new ChannelStatistics[n];
		this.selector = ChannelStatistics.Selector.MEAN;
		
		for (int i = 0; i < n; ++i) {
			this.statistics[i] = new ChannelStatistics();
		}
	}
	
	@Override
	public final void process(final int pixel) {
		final int pixelValue = this.getImage().getValue(pixel);
		final int n = this.channels.length;
		
		this.getProcessor().processPixel(pixel, pixelValue);
		
		if (this.getQuantizer() != null) {
			for (int i = 0; i < n; ++i) {
				final Channel channel = this.channels[i];
				this.statistics[i].addValue(this.getQuantizer().getNewValue(channel, channel.getValue(pixelValue)));
			}
		} else {
			for (int i = 0; i < n; ++i) {
				final Channel channel = this.channels[i];
				this.statistics[i].addValue(channel.getValue(pixelValue));
			}
		}
	}
	
	@Override
	public final void finishPatch() {
		for (final ChannelStatistics s : this.statistics) {
			this.getSample().add((byte) this.selector.getValue(s));
			s.reset();
		}
		
		this.getProcessor().processSample(this.getSample());
		this.getSample().clear();
	}
	
}
	