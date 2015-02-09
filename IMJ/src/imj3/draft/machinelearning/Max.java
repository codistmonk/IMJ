package imj3.draft.machinelearning;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-09)
 * 
 * @param <M>
 */
public final class Max<M extends DataSource.Metadata> extends TransformedDataSource<M, ClassifierClass, ClassifierClass> {
	
	private final int stride;
	
	public Max(final DataSource<? extends M, ? extends ClassifierClass> source, final int stride) {
		super(source);
		this.stride = stride;
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getInputDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.stride;
	}
	
	@Override
	public final Iterator<Classification<ClassifierClass>> iterator() {
		final int n = this.getInputDimension();
		final int stride = this.getClassDimension();
		
		return new Iterator<Classification<ClassifierClass>>() {
			
			private final Iterator<Classification<ClassifierClass>> i = Max.this.getSource().iterator();
			
			private final double[] datum = new double[Max.this.getClassDimension()];
			
			private final Classification<ClassifierClass> result = new Classification<ClassifierClass>(this.datum, new ClassifierClass.Default(this.datum), 0.0);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Classification<ClassifierClass> next() {
				Arrays.fill(this.datum, Double.NEGATIVE_INFINITY);
				
				final double[] input = this.i.next().getInput();
				
				for (int j = 0; j < n; j += stride) {
					for (int k = 0; k < stride; ++k) {
						final double value = input[j + k];
						
						if (this.datum[k] < value) {
							this.datum[k] = value;
						}
					}
				}
				
				return this.result;
			}
			
		};
	}
	
	private static final long serialVersionUID = -2472244801933971495L;
	
	public static final <M extends DataSource.Metadata> Max<M> max(final DataSource<? extends M, ? extends ClassifierClass> source) {
		return max(source, 1);
	}
	
	public static final <M extends DataSource.Metadata> Max<M> max(final DataSource<? extends M, ? extends ClassifierClass> source, final int stride) {
		return new Max<>(source, stride);
	}
	
}