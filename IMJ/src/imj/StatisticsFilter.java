package imj;

import net.sourceforge.aprog.tools.MathTools.Statistics;
import imj.IMJTools.StatisticsSelector;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public final class StatisticsFilter extends SyntheticFilter {
	
	private final StatisticsSelector selector;
	
	public StatisticsFilter(final Image image, final StatisticsSelector selector, final int[] structuringElement) {
		super(image, structuringElement);
		this.selector = selector;
		
		this.compute();
	}
	
	public final StatisticsSelector getSelector() {
		return this.selector;
	}
	
	@Override
	protected final Synthesizer newSynthesizer(int[] structuringElement) {
		return this.new Selector(structuringElement);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public final class Selector extends Synthesizer {
		
		private final Statistics statistics;
		
		public Selector(final int... deltas) {
			super(deltas);
			this.statistics = new Statistics();
		}
		
		@Override
		protected final void reset() {
			this.statistics.reset();
		}
		
		@Override
		protected final void addValue(final int pixel, final int value) {
			this.statistics.addValue(value);
		}
		
		@Override
		protected final void addFloatValue(final int pixel, final float value) {
			this.statistics.addValue(value);
		}
		
		@Override
		protected final int computeResult() {
			return (int) StatisticsFilter.this.getSelector().getValue(this.statistics);
		}
		
		@Override
		protected final float computeFloatResult() {
			return (float) StatisticsFilter.this.getSelector().getValue(this.statistics);
		}
		
	}
	
}
