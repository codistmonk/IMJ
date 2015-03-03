package imj3.draft.machinelearning;

import java.util.Iterator;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class RandomPrototypeSource extends DataSource.Abstract<RandomPrototypeSource.Metadata> {
	
	private final int dimension;
	
	private final int size;
	
	public RandomPrototypeSource(final int dimension, final int size, final long seed) {
		super(new Metadata(seed));
		this.dimension = dimension;
		this.size = size;
	}
	
	@Override
	public final Iterator<Datum> iterator() {
		final int d = this.getInputDimension();
		
		return new Iterator<Datum>() {
			
			private final Random random = new Random(RandomPrototypeSource.this.getMetadata().getSeed());
			
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
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static final class Metadata implements DataSource.Metadata {
		
		private final long seed;
		
		public Metadata(final long seed) {
			this.seed = seed;
		}
		
		public final long getSeed() {
			return this.seed;
		}
		
		private static final long serialVersionUID = 6674451727155317787L;
		
	}
	
}
