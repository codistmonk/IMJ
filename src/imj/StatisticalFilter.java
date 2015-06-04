package imj;

import imj.IMJTools.StatisticsSelector;
import multij.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public abstract class StatisticalFilter extends SyntheticFilter {
	
	private final StatisticsSelector selector;
	
	protected StatisticalFilter(final Image image, int resultChannelCount, final StatisticsSelector selector, final int[] structuringElement) {
		super(image, resultChannelCount, structuringElement);
		this.selector = selector;
	}
	
	public final StatisticsSelector getSelector() {
		return this.selector;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public abstract class Selector extends Synthesizer {
		
		private final int channelCount;
		
		private final Statistics[] statistics;
		
		protected Selector(final int... deltas) {
			super(deltas);
			this.channelCount = StatisticalFilter.this.getResult().getChannelCount();
			this.statistics = new Statistics[this.channelCount];
			
			for (int i = 0; i < this.channelCount; ++i) {
				this.statistics[i] = new Statistics();
			}
		}
		
		public final int getChannelCount() {
			return this.channelCount;
		}
		
		@Override
		protected void reset(final int pixel) {
			for (final Statistics statistics : this.statistics) {
				statistics.reset();
			}
		}
		
		@Override
		protected final void addValue(final int pixel, final int value) {
			if (1 < this.getChannelCount()) {
				for (int channel = this.getChannelCount() - 1; 0 <= channel ; --channel) {
					this.statistics[channel].addValue(this.getValue(pixel, channel, value));
				}
			} else {
				this.statistics[0].addValue(this.getValue(pixel, value));
			}
		}
		
		@Override
		protected final void addFloatValue(final int pixel, final float value) {
			this.statistics[0].addValue(this.getFloatValue(pixel, value));
		}
		
		@Override
		protected final int computeResult() {
			int result = 0;
			
			if (1 < this.getChannelCount()) {
				final int alpha = this.getChannelCount() < 4 ? 0xFF000000 : 0;
				
				for (final Statistics statistics : this.statistics) {
					result = (result << 8) | (0x000000FF & (int) StatisticalFilter.this.getSelector().getValue(statistics));
				}
				
				result |= alpha;
			} else {
				result = (int) StatisticalFilter.this.getSelector().getValue(this.statistics[0]);
			}
			
			return result;
		}
		
		@Override
		protected final float computeFloatResult() {
			return (float) StatisticalFilter.this.getSelector().getValue(this.statistics[0]);
		}
		
		protected abstract int getValue(int pixel, int channel, int value);
		
		protected abstract int getValue(int pixel, int value);
		
		protected abstract float getFloatValue(int pixel, float value);
		
	}
	
}
