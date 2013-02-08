package imj;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public abstract class SyntheticFilter extends Labeling {
	
	private final Synthesizer synthesizer;
	
	protected SyntheticFilter(final Image image, final int[] structuringElement) {
		super(image);
		
		this.synthesizer = this.newSynthesizer(structuringElement);
	}
	
	/**
	 * Must be called in derived classes.
	 */
	protected final void compute() {
		final int pixelCount = this.getPixelCount();
		
		if (this.getImage() instanceof ImageOfFloats) {
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
					this.getResult().setFloatValue(pixel, this.synthesizer.computeFloat(pixel));
			}
		} else {
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				this.getResult().setValue(pixel, this.synthesizer.compute(pixel));
			}
		}
	}
	
	protected abstract Synthesizer newSynthesizer(int[] structuringElement);
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public abstract class Synthesizer {
		
		private final int valueCount;
		
		private final Neighborhood neighborhood;
		
		protected Synthesizer(final int... deltas) {
			this.valueCount = (deltas.length + 1) / 2;
			this.neighborhood = SyntheticFilter.this.new Neighborhood(deltas);
		}
		
		public final int getValueCount() {
			return this.valueCount;
		}
		
		public final int compute(final int pixel) {
			this.neighborhood.reset(pixel);
			this.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				this.addValue(neighbor, SyntheticFilter.this.getImage().getValue(neighbor));
			}
			
			return this.computeResult();
		}
		
		public final float computeFloat(final int pixel) {
			this.neighborhood.reset(pixel);
			this.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				this.addFloatValue(neighbor, SyntheticFilter.this.getImage().getFloatValue(neighbor));
			}
			
			return this.computeFloatResult();
		}
		
		protected abstract void reset(int pixel);
		
		protected abstract void addValue(int pixel, int value);
		
		protected abstract void addFloatValue(int pixel, float value);
		
		protected abstract int computeResult();
		
		protected abstract float computeFloatResult();
		
	}
	
}
