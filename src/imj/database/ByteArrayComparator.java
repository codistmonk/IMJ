package imj.database;

import static java.lang.Math.min;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author codistmonk (creation 2013-04-26)
 */
public final class ByteArrayComparator implements Comparator<byte[]>, Serializable {
	
	@Override
	public final int compare(final byte[] array1, final byte[] array2) {
		final int n1 = array1.length;
		final int n2 = array2.length;
		final int n = min(n1, n2);
		int result = 0;
		
		for (int i = 0; i < n && result == 0; ++i) {
			result = array1[i] - array2[i];
		}
		
		if (result == 0) {
			result = n1 - n2;
		}
		
		return result;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 2810367124854655561L;
	
	public static final ByteArrayComparator INSTANCE = new ByteArrayComparator();
	
}
