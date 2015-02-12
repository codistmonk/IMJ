package imj3.draft.machinelearning;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 * 
 * @param <M>
 * @param <C>
 */
public final class ClassIndexDataSource<M extends DataSource.Metadata> extends TransformedDataSource<M, ClassifierClass, ClassifierClass> {
	
	public ClassIndexDataSource(final DataSource<M, ?> source) {
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
	public final Iterator<Classification<ClassifierClass>> iterator() {
		return new Iterator<Classification<ClassifierClass>>() {
			
			private final Iterator<Classification<ClassifierClass>> i = ClassIndexDataSource.this.getSource().iterator();
			
			private final Classification<ClassifierClass> result = new Classification<>(null, new ClassifierClass.Default(new double[1]), 0.0);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Classification<ClassifierClass> next() {
				this.result.getInput()[0] = this.i.next().getClassifierClass().getClassIndex();
				
				return this.result;
			}
			
		};
	}
	
	private static final long serialVersionUID = -4926248507282321872L;
	
	public static final <M extends DataSource.Metadata> ClassIndexDataSource<M> classIndices(final DataSource<M, ?> inputs) {
		return new ClassIndexDataSource<>(inputs);
	}
	
}