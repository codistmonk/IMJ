package imj3.draft.machinelearning;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-09)
 */
public final class Max extends TransformedDataSource {
	
	private final int stride;
	
	public Max(final DataSource source, final int stride) {
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
	public final Iterator<Datum> iterator() {
		final int n = this.getInputDimension();
		final int stride = this.getClassDimension();
		
		return new Iterator<Datum>() {
			
			private final Iterator<Datum> i = Max.this.getSource().iterator();
			
			private final double[] datum = new double[Max.this.getClassDimension()];
			
			private final Datum result = new Datum.Default().setValue(this.datum);
			
			@Override
			public final boolean hasNext() {
				return this.i.hasNext();
			}
			
			@Override
			public final Datum next() {
				Arrays.fill(this.datum, Double.NEGATIVE_INFINITY);
				
				final double[] input = this.i.next().getValue();
				
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
	
	public static final Max max(final DataSource source) {
		return max(source, 1);
	}
	
	public static final Max max(final DataSource source, final int stride) {
		return new Max(source, stride);
	}
	
}