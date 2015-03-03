package imj3.draft.machinelearning;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 */
public final class ClassifiedDataSource extends TransformedDataSource {
	
	private final Classifier classifier;
	
	public ClassifiedDataSource(final DataSource source, final Classifier classifier) {
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
	public final Iterator<Datum> iterator() {
		return new Iterator<Datum>() {
			
			private final Iterator<Datum> inputs = ClassifiedDataSource.this.getSource().iterator();
			
			private final Datum tmp = new Datum.Default();
			
			@Override
			public final boolean hasNext() {
				return this.inputs.hasNext();
			}
			
			@Override
			public final Datum next() {
				return ClassifiedDataSource.this.getClassifier().classify(this.inputs.next(), this.tmp);
			}
			
		};
	}
	
	final Classifier getClassifier() {
		return this.classifier;
	}
	
	private static final long serialVersionUID = -8278471598281124440L;
	
}
