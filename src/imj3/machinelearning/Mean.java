package imj3.machinelearning;

import java.util.Arrays;

import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-09)
 */
public final class Mean extends TransformedDataSource {
	
	private final int stride;
	
	public Mean(final DataSource source, final int stride) {
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
	public final Iterator iterator() {
		final int n = this.getInputDimension();
		final int stride = this.getClassDimension();
		
		return new Iterator.Abstract<Iterator>() {
			
			private final Iterator i = Mean.this.getSource().iterator();
			
			private final double[] datum = new double[Mean.this.getClassDimension()];
			
			private final Datum result = new Datum.Default().setValue(this.datum);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				Arrays.fill(this.datum, 0.0);
				
				final double[] input = this.i.next().getValue();
				
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
			
			private static final long serialVersionUID = 6126895304133041154L;
			
		};
	}
	
	private static final long serialVersionUID = -5585195651747133856L;
	
	public static final Mean mean(final DataSource source) {
		return mean(source, 1);
	}
	
	public static final Mean mean(final DataSource source, final int stride) {
		return new Mean(source, stride);
	}
	
}