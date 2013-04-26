package imj.apps.modules;

import java.util.Comparator;

/**
 * @author codistmonk (creation 2013-04-26)
 */
public final class ByteArrayComparator implements Comparator<byte[]> {
	
	@Override
	public final int compare(final byte[] array1, final byte[] array2) {
		final int n = array1.length;
		int result = 0;
		
		for (int i = 0; i < n && result == 0; ++i) {
			result = array1[i] - array2[i];
		}
		
		return result;
	}
	
	public static final ByteArrayComparator INSTANCE = new ByteArrayComparator();
	
}
