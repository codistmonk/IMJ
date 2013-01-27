package imj;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class RankFiltering extends Labeling {
	
	private final int rank;
	
	public RankFiltering(final Image image, final int rank, final int[] structuringElement) {
		super(image);
		this.rank = rank;
		
		final int pixelCount = this.getPixelCount();
		final StructuringElement se = this.new StructuringElement(structuringElement);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			this.getResult().setValue(pixel, se.evaluate(pixel));
		}
	}
	
	public final int getRank() {
		return this.rank;
	}
	
	/**
	 * @author codistmonk (creation 2013-01-27)
	 */
	public final class StructuringElement {
		
		private final IntList values;
		
		private final Neighborhood neighborhood;
		
		public StructuringElement(final int... deltas) {
			this.values = new IntList((deltas.length + 1) / 2);
			this.neighborhood = RankFiltering.this.new Neighborhood(deltas);
		}
		
		public final int evaluate(final int pixel) {
			this.values.clear();
			this.neighborhood.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				this.values.add(RankFiltering.this.getImage().getValue(neighbor));
			}
			
			final int n = this.values.size();
			
			this.values.sort();
			
			return this.values.get((RankFiltering.this.getRank() + n) % n);
		}
		
	}
	
}
