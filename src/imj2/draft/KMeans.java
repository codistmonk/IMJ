package imj2.draft;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-01-15)
 */
public final class KMeans {
	
	private KMeans() {
		throw new IllegalInstantiationException();
	}
	
	public static final void computeMeans(final List<Point> points, final List<Integer> weights,
			final int[] clustering, final Point[] means, final int[] sizes, final int[] counts) {
		final int k = means.length;
		final int n = points.size();
		
		Arrays.fill(sizes, 0);
		Arrays.fill(counts, 0);
		
		for (int i = 0; i < n; ++i) {
			final Point p = points.get(i);
			final int w = weights.get(i);
			final int j = clustering[i];
			means[j].x += p.x * w;
			means[j].y += p.y * w;
			sizes[j] += w;
			++counts[j];
		}
		
		for (int i = 0; i < k; ++i) {
			final int weight = sizes[i];
			
			if (weight != 0) {
				means[i].x /= weight;
				means[i].y /= weight;
			}
		}
	}
	
	public static final void recluster(final List<Point> points,
			final int[] clustering, final Point[] centers) {
		final int n = points.size();
		final int k = centers.length;
		
		for (int i = 0; i < n; ++i) {
			final Point p = points.get(i);
			int nearest = clustering[i];
			double bestDistance = Double.POSITIVE_INFINITY;
			
			for (int j = 0; j < k; ++j) {
				final double distance = p.distance(centers[j]);
				
				if (distance < bestDistance) {
					nearest = j;
					bestDistance = distance;
				}
			}
			
			clustering[i] = nearest;
		}
	}
	
}