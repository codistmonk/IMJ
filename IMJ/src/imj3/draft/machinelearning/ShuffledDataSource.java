package imj3.draft.machinelearning;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-08)
 *
 * @param <M>
 * @param <C>
 */
public final class ShuffledDataSource<M extends DataSource.Metadata> implements DataSource<M> {
	
	private final BufferedDataSource<M> source;
	
	private final long seed;
	
	public ShuffledDataSource(final DataSource<M> source, final int bufferLimit, final long seed) {
		this(new BufferedDataSource<>(source, bufferLimit), seed);
	}
	
	public ShuffledDataSource(final BufferedDataSource<M> source, final long seed) {
		this.source = source;
		this.seed = seed;
		
		if (this.source.getDataset() != null) {
			Collections.shuffle(this.source.getDataset(), new Random(seed));
		}
	}
	
	public final BufferedDataSource<M> getSource() {
		return this.source;
	}
	
	public final long getSeed() {
		return this.seed;
	}
	
	@Override
	public final M getMetadata() {
		return this.getSource().getMetadata();
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
	public final Iterator<Datum> iterator() {
		if (this.getSource().getDataset() != null) {
			return this.getSource().getDataset().iterator();
		}
		
		final int n = this.getSource().getBufferLimit();
		
		return new Iterator<Datum>() {
			
			private final Random random = new Random(ShuffledDataSource.this.getSeed());
			
			private final BufferedDataSource<M>.BufferedIterator i = (BufferedDataSource<M>.BufferedIterator) ShuffledDataSource.this.getSource().iterator();
			
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
			
		};
	}
	
	private static final long serialVersionUID = 8988786071992346132L;
	
}