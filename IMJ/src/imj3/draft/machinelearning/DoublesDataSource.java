package imj3.draft.machinelearning;

import imj3.draft.machinelearning.DataSource.Abstract;

/**
 * @author codistmonk (creation 2015-02-12)
 */
public final class DoublesDataSource extends Abstract<DataSource> {
	
	private final int dimension;
	
	private final double[] inputs;
	
	public DoublesDataSource(final int dimension, final double[] inputs) {
		this.dimension = dimension;
		this.inputs = inputs;
	}
	
	public final double[] getInputs() {
		return this.inputs;
	}
	
	@Override
	public final int getInputDimension() {
		return this.dimension;
	}
	
	@Override
	public final int getClassDimension() {
		return this.getInputDimension();
	}
	
	@Override
	public final int size() {
		return this.getInputs().length / this.getInputDimension();
	}
	
	@Override
	public final Iterator iterator() {
		final int d = this.getInputDimension();
		final int n = this.size();
		
		return new Iterator.Abstract<Iterator>() {
			
			private final Datum result = new Datum.Default().setValue(new double[d]).setScore(0.0);
			
			private int i;
			
			@Override
			public final boolean hasNext() {
				return this.i < n;
			}
			
			@Override
			public final Datum next() {
				System.arraycopy(DoublesDataSource.this.getInputs(), this.i, this.result.getValue(), 0, d);
				
				this.i += d;
				
				return this.result;
			}
			
			private static final long serialVersionUID = 6126895304133041154L;
			
		};
	}
	
	private static final long serialVersionUID = -4127598593568333362L;
	
	public static final DoublesDataSource source1(final double... inputs) {
		return source(1, inputs);
	}
	
	public static final DoublesDataSource source(final int dimension, final double... inputs) {
		return new DoublesDataSource(dimension, inputs);
	}
	
}
