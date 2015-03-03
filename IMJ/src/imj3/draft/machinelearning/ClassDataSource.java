package imj3.draft.machinelearning;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 * 
 * @param <M>
 * @param <C>
 */
public final class ClassDataSource<M extends DataSource.Metadata> extends TransformedDataSource<M> {
	
	public ClassDataSource(final DataSource<M> source) {
		super(source);
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getClassDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.getInputDimension();
	}
	
	@Override
	public final Iterator<Datum> iterator() {
		return new Iterator<Datum>() {
			
			private final Iterator<Datum> i = ClassDataSource.this.getSource().iterator();
			
			private final Datum result = new Datum.Default();
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				return this.result.setValue(this.i.next().getPrototype().getValue());
			}
			
		};
	}
	
	private static final long serialVersionUID = -4926248507282321872L;
	
	public static final <M extends DataSource.Metadata> ClassDataSource<M> classes(final DataSource<M> inputs) {
		return new ClassDataSource<>(inputs);
	}
	
}