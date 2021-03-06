package imj3.machinelearning;

/**
 * @author codistmonk (creation 2015-02-08)
 */
public final class ClassDataSource extends TransformedDataSource {
	
	public ClassDataSource(final DataSource source) {
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
	public final Iterator iterator() {
		return new Iterator.Abstract<Iterator>() {
			
			private final Iterator i = ClassDataSource.this.getSource().iterator();
			
			private final Datum result = new Datum.Default();
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				return this.result.setValue(this.i.next().getPrototype().getValue());
			}
			
			private static final long serialVersionUID = 6126895304133041154L;
			
		};
	}
	
	private static final long serialVersionUID = -4926248507282321872L;
	
	public static final ClassDataSource classes(final DataSource inputs) {
		return new ClassDataSource(inputs);
	}
	
}