package imj3.draft.machinelearning;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 *
 * @param <M>
 * @param <C>
 */
public final class ClassifiedDataSource<M extends DataSource.Metadata, In extends ClassifierClass, Out extends ClassifierClass> extends TransformedDataSource<M, In, Out> {
	
	private final Classifier<Out> classifier;
	
	public ClassifiedDataSource(final DataSource<M, In> source, final Classifier<Out> classifier) {
		super(source);
		this.classifier = classifier;
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getInputDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.getClassifier().getClassDimension(this.getInputDimension());
	}
	
	@Override
	public final Iterator<Classification<Out>> iterator() {
		return new Iterator<Classification<Out>>() {
			
			private final Iterator<Classification<In>> inputs = ClassifiedDataSource.this.getSource().iterator();
			
			private final Classification<Out> tmp = new Classification<>();
			
			@Override
			public final boolean hasNext() {
				return this.inputs.hasNext();
			}
			
			@Override
			public final Classification<Out> next() {
				final Classification<Out> result = ClassifiedDataSource.this.getClassifier().classify(
						this.tmp, this.inputs.next().getInput());
				
				return result;
			}
			
		};
	}
	
	final Classifier<Out> getClassifier() {
		return this.classifier;
	}
	
	private static final long serialVersionUID = -8278471598281124440L;
	
}
