package imj3.machinelearning;

import java.util.Collections;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-08)
 */
public final class ShuffledDataSource extends DataSource.Abstract<BufferedDataSource> {
	
	private final long seed;
	
	public ShuffledDataSource(final DataSource source, final int bufferLimit, final long seed) {
		this(new BufferedDataSource(source, bufferLimit), seed);
	}
	
	public ShuffledDataSource(final BufferedDataSource source, final long seed) {
		super(source);
		this.seed = seed;
		
		if (source.getDataset() != null) {
			Collections.shuffle(source.getDataset(), new Random(seed));
		}
	}
	
	public final long getSeed() {
		return this.seed;
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getInputDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.getSource().getClassDimension();
	}
	
	@Override
	public final Iterator iterator() {
		if (this.getSource().getDataset() != null) {
			return Iterator.wrap(this.getSource().getDataset().iterator());
		}
		
		final int n = this.getSource().getBufferLimit();
		
		return new Iterator.Abstract<Iterator>() {
			
			private final Random random = new Random(ShuffledDataSource.this.getSeed());
			
			private final BufferedDataSource.BufferedIterator i = (BufferedDataSource.BufferedIterator) ShuffledDataSource.this.getSource().iterator();
			
			private int j = n;
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				if (--this.j == 0) {
					this.j = n;
					Collections.shuffle(this.i.getBuffer(), this.random);
				}
				
				return this.i.next();
			}
			
			private static final long serialVersionUID = 6126895304133041154L;
			
		};
	}
	
	private static final long serialVersionUID = 8988786071992346132L;
	
}