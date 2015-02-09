package imj3.draft.machinelearning;

import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-09)
 * 
 * @param <M>
 */
public final class Mean<M extends DataSource.Metadata> extends TransformedDataSource<M, ClassifierClass, ClassifierClass> {
	
	private final int stride;
	
	public Mean(final DataSource<? extends M, ? extends ClassifierClass> source, final int stride) {
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
			
			private final Iterator<Classification<ClassifierClass>> i = Mean.this.getSource().iterator();
			
			private final double[] datum = new double[Mean.this.getClassDimension()];
			
			private final Classification<ClassifierClass> result = new Classification<ClassifierClass>(this.datum, new ClassifierClass.Default(this.datum), 0.0);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Classification<ClassifierClass> next() {
				Arrays.fill(this.datum, 0.0);
				
				final double[] input = this.i.next().getInput();
				
				for (int j = 0; j < n; j += stride) {
					for (int k = 0; k < stride; ++k) {
						try {
							this.datum[k] += input[j + k];
						} catch (Exception exception) {
							Tools.debugError(n, stride, input.length, j);
							Tools.debugError(Mean.this);
							Tools.debugError(getSource());
							
							throw Tools.unchecked(exception);
						}
					}
				}
				
				for (int k = 0; k < stride; ++k) {
					this.datum[k] = this.datum[k] * stride / n;
				}
				
				return this.result;
			}
			
		};
	}
	
	private static final long serialVersionUID = -5585195651747133856L;
	
	public static final <M extends DataSource.Metadata> Mean<M> mean(final DataSource<? extends M, ? extends ClassifierClass> source) {
		return mean(source, 1);
	}
	
	public static final <M extends DataSource.Metadata> Mean<M> mean(final DataSource<? extends M, ? extends ClassifierClass> source, final int stride) {
		return new Mean<>(source, stride);
	}
	
}