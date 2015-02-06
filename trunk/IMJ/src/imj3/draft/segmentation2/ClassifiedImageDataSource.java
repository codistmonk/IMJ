package imj3.draft.segmentation2;

import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.ClassifierClass;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class ClassifiedImageDataSource<In extends ClassifierClass, Out extends ClassifierClass> extends ImageDataSource<Out> {
	
	private final ImageDataSource<In> source;
	
	private final Classifier<Out> classifier;
	
	public ClassifiedImageDataSource(final ImageDataSource<In> source, final Classifier<Out> classifier) {
		super(source.getPatchSize(), source.getPatchSparsity(), source.getStride());
		this.source = source;
		this.classifier = classifier;
	}
	
	public final ImageDataSource<In> getSource() {
		return this.source;
	}
	
	public final Classifier<Out> getClassifier() {
		return this.classifier;
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getClassDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.getClassifier().getClassDimension(this.getInputDimension());
	}
	
	@Override
	public final Iterator<Classification<Out>> iterator() {
		return new Iterator<Classification<Out>>() {
			
			private final Iterator<Classification<In>> inputs = ClassifiedImageDataSource.this.getSource().iterator();
			
			@Override
			public final boolean hasNext() {
				return this.inputs.hasNext();
			}
			
			@Override
			public final Classification<Out> next() {
				final Classification<Out> result = ClassifiedImageDataSource.this.getClassifier().classify(
						this.inputs.next().getClassifierClass().toArray());
				
				return result;
			}
			
		};
	}
	
	@Override
	public int getImageWidth() {
		return this.getSource().getImageWidth();
	}
	
	@Override
	public final int getImageHeight() {
		return this.getSource().getImageHeight();
	}
	
	private static final long serialVersionUID = -4480722678689454042L;
	
}