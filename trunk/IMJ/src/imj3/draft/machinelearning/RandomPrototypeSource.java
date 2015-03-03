package imj3.draft.machinelearning;

import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class RandomPrototypeSource extends DataSource.Abstract<DataSource> {
	
	private final long seed;
	
	private final int dimension;
	
	private final int size;
	
	public RandomPrototypeSource(final int dimension, final int size, final long seed) {
		this.seed = seed;
		this.dimension = dimension;
		this.size = size;
	}
	
	public final long getSeed() {
		return this.seed;
	}
	
	@Override
	public final Iterator iterator() {
		final int d = this.getInputDimension();
		
		return new Iterator.Abstract<Iterator>() {
			
			private final Random random = new Random(RandomPrototypeSource.this.getSeed());
			
			private final Datum result = new Datum.Default().setValue(new double[d]);
			
			private int i = 0;
			
			@Override
			public final boolean hasNext() {
				return this.i < RandomPrototypeSource.this.size();
			}
			
			@Override
			public final Datum next() {
				final double[] datum = this.result.setIndex(this.i).getValue();
				
				++this.i;
				
				for (int i = 0; i < d; ++i) {
					datum[i] = this.random.nextDouble();
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
			
			private static final long serialVersionUID = 6126895304133041154L;
			
		};
	}
	
	@Override
	public final int getInputDimension() {
		return this.dimension;
	}
	
	@Override
	public final int getClassDimension() {
		return this.dimension;
	}
	
	@Override
	public final int size() {
		return this.size;
	}
	
	private static final long serialVersionUID = -5303911125576388280L;
	
	static final boolean SIMULATE_SLOW_ACCESS = false;
	
}
