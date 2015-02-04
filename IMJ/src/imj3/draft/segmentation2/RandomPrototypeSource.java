package imj3.draft.segmentation2;

import imj3.draft.segmentation2.NearestNeighborClassifier.Prototype;

import java.util.Iterator;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class RandomPrototypeSource implements DataSource<Prototype> {
	
	private final int dimension;
	
	private final int size;
	
	private final long seed;
	
	public RandomPrototypeSource(final int dimension, final int size, final long seed) {
		this.dimension = dimension;
		this.size = size;
		this.seed = seed;
	}
	
	public final long getSeed() {
		return this.seed;
	}
	
	@Override
	public final Iterator<Classification<Prototype>> iterator() {
		final int d = this.getDimension();
		
		return new Iterator<Classification<Prototype>>() {
			
			private final Random random = new Random(RandomPrototypeSource.this.getSeed());
			
			private final double[] datum = new double[d];
			
			private final Classification<Prototype> result = new Classification<>(
					this.datum, new Prototype(this.datum), 0.0);
			
			private int i = 0;
			
			@Override
			public final boolean hasNext() {
				return this.i < RandomPrototypeSource.this.size();
			}
			
			@Override
			public final Classification<Prototype> next() {
				++this.i;
				
				for (int i = 0; i < d; ++i) {
					this.datum[i] = this.random.nextDouble();
				}
				
				if (SIMULATE_SLOW_ACCESS) {
					try {
						Thread.sleep(1L);
					} catch (final InterruptedException exception) {
						exception.printStackTrace();
					}
				}
				
				return this.result;
			}
			
		};
	}
	
	@Override
	public final int getDimension() {
		return this.dimension;
	}
	
	@Override
	public final int size() {
		return this.size;
	}
	
	private static final long serialVersionUID = -5303911125576388280L;
	
	static final boolean SIMULATE_SLOW_ACCESS = false;
	
}