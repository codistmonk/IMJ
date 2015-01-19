package imj3.draft;

import static java.lang.Math.abs;

import java.util.Arrays;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-01-19)
 */
public final class KMeans {
	
	private KMeans() {
		throw new IllegalInstantiationException();
	}
	
	public static final void computeMeans(final Iterable<double[]> pointAndWeights,
			final int[] clustering, final double[][] means, final double[] sizes, final int[] counts) {
		final int k = means.length;
		final int d = means[0].length;
		
		Arrays.fill(sizes, 0);
		Arrays.fill(counts, 0);
		
		{
			int i = -1;
			
			for (final double[] pointAndWeight : pointAndWeights) {
				++i;
				final int j = clustering[i];
				final double w = pointAndWeight[d];
				
				for (int l = 0; l < d; ++l) {
					means[j][l] += pointAndWeight[l] * w;
				}
				
				sizes[j] += w;
				++counts[j];
			}
		}
		
		for (int i = 0; i < k; ++i) {
			final double weight = sizes[i];
			
			if (weight != 0.0) {
				for (int j = 0; j < d; ++j) {
					means[i][j] /= weight;
				}
			}
		}
	}
	
	public static final void recluster(final Iterable<double[]> points,
			final int[] clustering, final double[][] centers) {
		final int k = centers.length;
		int i = -1;
		
		for (final double[] point : points) {
			++i;
			int nearest = clustering[i];
			double bestDistance = Double.POSITIVE_INFINITY;
			
			for (int j = 0; j < k; ++j) {
				final double distance = distance(centers[j], point);
				
				if (distance < bestDistance) {
					nearest = j;
					bestDistance = distance;
				}
			}
			
			clustering[i] = nearest;
		}
	}
	
	public static final double distance(final double[] p1, final double[] p2) {
		double result = 0.0;
		final int n = p1.length;
		
		for (int i = 0; i < n; ++i) {
			result += abs(p2[i] - p1[i]);
		}
		
		return result;
	}
	
}