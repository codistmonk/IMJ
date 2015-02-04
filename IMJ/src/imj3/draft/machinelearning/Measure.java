package imj3.draft.machinelearning;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Measure extends Serializable {
	
	public abstract double compute(double[] v1, double[] v2, double limit);
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static enum Predefined implements Measure {
		
		L0 {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				double result = 0.0;
				
				for (int i = 0; i < n; ++i) {
					final double d = abs(v2[i] - v1[i]);
					
					if (result < d) {
						result = d;
					}
				}
				
				return result;
			}
			
		}, L0_ES {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				double result = 0.0;
				
				for (int i = 0; i < n && result < limit; ++i) {
					final double d = abs(v2[i] - v1[i]);
					
					if (result < d) {
						result = d;
					}
				}
				
				return result;
			}
			
		}, L1 {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				double result = 0.0;
				
				for (int i = 0; i < n; ++i) {
					result += abs(v2[i] - v1[i]);
				}
				
				return result;
			}
			
		}, L1_ES {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				double result = 0.0;
				
				for (int i = 0; i < n && result < limit; ++i) {
					result += abs(v2[i] - v1[i]);
				}
				
				return result;
			}
			
		}, L2 {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				double sumOfSquares = 0.0;
				
				for (int i = 0; i < n; ++i) {
					sumOfSquares += square(v2[i] - v1[i]);
				}
				
				return sqrt(sumOfSquares);
			}
			
		}, L2_ES {
			
			@Override
			public final double compute(final double[] v1, final double[] v2, final double limit) {
				final int n = v1.length;
				final double squaredLimit = square(limit);
				double sumOfSquares = 0.0;
				
				for (int i = 0; i < n; ++i) {
					sumOfSquares += square(v2[i] - v1[i]);
					
					if (squaredLimit <= sumOfSquares) {
						return limit;
					}
				}
				
				return sqrt(sumOfSquares);
			}
			
		},
		
	}
	
}
