package imj3.draft.machinelearning;

import static imj3.draft.machinelearning.Datum.Default.datum;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 */
public final class ClassIndexDataSource extends TransformedDataSource {
	
	public ClassIndexDataSource(final DataSource source) {
		super(source);
	}
	
	@Override
	public final int getInputDimension() {
		return 1;
	}
	
	@Override
	public final int getClassDimension() {
		return this.getInputDimension();
	}
	
	@Override
	public final Iterator<Datum> iterator() {
		return new Iterator<Datum>() {
			
			private final Iterator<Datum> i = ClassIndexDataSource.this.getSource().iterator();
			
			private final Datum result = datum(null).setPrototype(datum(0.0));
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				this.result.getValue()[0] = this.i.next().getPrototype().getIndex();
				
				return this.result;
			}
			
		};
	}
	
	private static final long serialVersionUID = -4926248507282321872L;
	
	public static final ClassIndexDataSource classIndices(final DataSource inputs) {
		return new ClassIndexDataSource(inputs);
	}
	
}