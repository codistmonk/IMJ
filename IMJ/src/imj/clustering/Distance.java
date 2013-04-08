package imj.clustering;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.Statistics.square;

/**
 * @author codistmonk (creation 2013-03-31)
 */
public abstract interface Distance {
	
	public abstract double getDistance(double[] sample1, double[] sample2);
	
	/**
	 * @author codistmonk (creation 2013-03-31)
	 */
	public static enum Predefined implements Distance {
		
		EUCLIDEAN {
			
			@Override
			public final double getDistance(final double[] sample1, final double[] sample2) {
				final int n = sample1.length;
				double sumOfSquares = 0.0;
				
				for (int i = 0; i < n; ++i) {
					sumOfSquares += square(sample2[i] - sample1[i]);
				}
				
				return sqrt(sumOfSquares);
			}
			
		}, CHI_SQUARE {
			
			@Override
			public final double getDistance(final double[] sample1, final double[] sample2) {
				final int n = sample1.length;
				double sumOfSquares = 0.0;
				
				for (int i = 0; i < n; ++i) {
					sumOfSquares += (1.0 + square(sample2[i] - sample1[i])) / (1.0 + abs(sample1[i] + sample2[i]));
				}
				
				return sumOfSquares / 2.0;
			}
			
		}, CITYBLOCK {
			
			@Override
			public final double getDistance(final double[] sample1, final double[] sample2) {
				final int n = sample1.length;
				double result = 0.0;
				
				for (int i = 0; i < n; ++i) {
					result += abs(sample2[i] - sample1[i]);
				}
				
				return result;
			}
			
		}, CHESSBOARD {
			
			@Override
			public final double getDistance(final double[] sample1, final double[] sample2) {
				final int n = sample1.length;
				double result = 0.0;
				
				for (int i = 0; i < n; ++i) {
					final double d = abs(sample2[i] - sample1[i]);
					
					if (result < d) {
						result = d;
					}
				}
				
				return result;
			}
			
		};
		
	}
	
}
