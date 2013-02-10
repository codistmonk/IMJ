package imj;

import static imj.IMJTools.channelValue;

import imj.IMJTools.StatisticsSelector;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public final class ValueStatisticsFilter extends StatisticalFilter {
	
	public ValueStatisticsFilter(final Image image, final StatisticsSelector selector, final int[] structuringElement) {
		super(image, image.getChannelCount(), selector, structuringElement);
		
		this.compute();
	}
	
	@Override
	protected final Synthesizer newSynthesizer(final int[] structuringElement) {
		return this.new Selector(structuringElement);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public final class Selector extends StatisticalFilter.Selector {
		
		public Selector(final int... deltas) {
			super(deltas);
		}
		
		@Override
		protected final int getValue(final int pixel, int channel, final int value) {
			return channelValue(value, channel);
		}
		
		@Override
		protected final int getValue(final int pixel, final int value) {
			return value;
		}
		
		@Override
		protected final float getFloatValue(final int pixel, final float value) {
			return value;
		}
		
	}
	
}
