package imj3.draft.machinelearning;

import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-08)
 * 
 * @param <M>
 * @param <C>
 */
public final class ClassDataSource<M extends DataSource.Metadata> extends TransformedDataSource<M, ClassifierClass, ClassifierClass> {
	
	public ClassDataSource(final DataSource<M, ?> source) {
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
	public final Iterator<Classification<ClassifierClass>> iterator() {
		return new Iterator<Classification<ClassifierClass>>() {
			
			private final Iterator<Classification<ClassifierClass>> i = ClassDataSource.this.getSource().iterator();
			
			private final Classification<MutableClassifierClass> result = new Classification<>(null, new MutableClassifierClass(), 0.0);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public final Classification<ClassifierClass> next() {
				final double[] array = this.i.next().getClassifierClass().toArray();
				
				return (Classification) this.result.setInput(
						this.result.getClassifierClass().setArray(array).toArray());
			}
			
		};
	}
	
	private static final long serialVersionUID = -4926248507282321872L;
	
	public static final <M extends DataSource.Metadata> ClassDataSource<M> classes(final DataSource<M, ?> inputs) {
		return new ClassDataSource<>(inputs);
	}
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static final class MutableClassifierClass implements ClassifierClass {
		
		private int classIndex;
		
		private double[] array;
		
		@Override
		public final int getClassIndex() {
			return this.classIndex;
		}
		
		public final MutableClassifierClass setClassIndex(final int classIndex) {
			this.classIndex = classIndex;
			
			return this;
		}
		
		public final MutableClassifierClass setArray(final double[] array) {
			this.array = array;
			
			return this;
		}
		
		@Override
		public final double[] toArray() {
			return this.array;
		}
		
		private static final long serialVersionUID = 8563042210828121994L;
		
	}
	
}