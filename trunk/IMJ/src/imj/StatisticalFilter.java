package imj;

import net.sourceforge.aprog.tools.MathTools.Statistics;
import imj.IMJTools.StatisticsSelector;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public abstract class StatisticalFilter extends SyntheticFilter {
	
	private final StatisticsSelector selector;
	
	protected StatisticalFilter(final Image image, final StatisticsSelector selector, final int[] structuringElement) {
		super(image, structuringElement);
		this.selector = selector;
	}
	
	public final StatisticsSelector getSelector() {
		return this.selector;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public abstract class Selector extends Synthesizer {
		
		private final Statistics statistics;
		
		protected Selector(final int... deltas) {
			super(deltas);
			this.statistics = new Statistics();
		}
		
		@Override
		protected void reset(final int pixel) {
			this.statistics.reset();
		}
		
		@Override
		protected final void addValue(final int pixel, final int value) {
			this.statistics.addValue(this.getValue(pixel, value));
		}
		
		@Override
		protected final void addFloatValue(final int pixel, final float value) {
			this.statistics.addValue(this.getFloatValue(pixel, value));
		}
		
		@Override
		protected final int computeResult() {
			return (int) StatisticalFilter.this.getSelector().getValue(this.statistics);
		}
		
		@Override
		protected final float computeFloatResult() {
			return (float) StatisticalFilter.this.getSelector().getValue(this.statistics);
		}
		
		protected abstract int getValue(int pixel, int value);
		
		protected abstract float getFloatValue(int pixel, float value);
		
	}
	
}
