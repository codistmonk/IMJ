package imj3.tools;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author codistmonk (creation 2014-04-10)
 */
public final class DoubleArrayComparator implements Serializable, Comparator<double[]> {
	
	@Override
	public final int compare(final double[] array1, final double[] array2) {
		final int n1 = array1.length;
		final int n2 = array2.length;
		final int n = Math.min(n1, n2);
		
		for (int i = 0; i < n; ++i) {
			final int comparison = Double.compare(array1[i], array2[i]);
			
			if (comparison != 0) {
				return comparison;
			}
		}
		
		return n1 - n2;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -88586465954519984L;
	
	public static final DoubleArrayComparator INSTANCE = new DoubleArrayComparator();
	
}