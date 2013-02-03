package imj;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class RankFilter extends SyntheticFilter {
	
	private final int rank;
	
	public RankFilter(final Image image, final int rank, final int[] structuringElement) {
		super(image, structuringElement);
		this.rank = rank;
		
		this.compute();
	}
	
	public final int getRank() {
		return this.rank;
	}
	
	@Override
	protected final Synthesizer newSynthesizer(int[] structuringElement) {
		return this.new Sorter(structuringElement);
	}
	
	/**
	 * @author codistmonk (creation 2013-01-27)
	 */
	public final class Sorter extends Synthesizer {
		
		private final IntList values;
		
		public Sorter(final int... deltas) {
			super(deltas);
			this.values = new IntList(this.getValueCount());
		}
		
		@Override
		protected final void reset() {
			this.values.clear();
		}
		
		@Override
		protected final void addValue(final int pixel, final int value) {
			this.values.add(value);
		}
		
		@Override
		protected final void addFloatValue(final int pixel, final float value) {
			this.values.add((int) value);
		}
		
		@Override
		protected final int computeResult() {
			final int n = this.values.size();
			
			this.values.sort();
			
			return this.values.get((RankFilter.this.getRank() + n) % n);
		}

		@Override
		protected final float computeFloatResult() {
			return this.computeResult();
		}
		
	}
	
}
