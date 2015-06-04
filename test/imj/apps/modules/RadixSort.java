package imj.apps.modules;

import multij.primitivelists.IntList;
import multij.primitivelists.IntList.Processor;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-04-27)
 */
public final class RadixSort {
	
	private RadixSort() {
		throw new IllegalInstantiationException();
	}
	
	private static final int partialSort(final int[] source, final int[] destination, final int start, final int end,
			final int xor, final int shift) {
		int i = start;
		int j = end;
		final int mask = 1 << shift;
		
		for (int k = start; k < end; ++k) {
			final int value = source[k];
			
			if (((value ^ xor) & mask) == 0) {
				destination[i++] = value;
			} else {
				destination[--j] = value;
			}
		}
		
		assert i == j;
		
		return i;
	}
	
	public static final void sort(final int[] values) {
		final IntList[] lists1 = new IntList[1 << 8];
		final IntList[] lists2 = new IntList[lists1.length];
		
		for (final int value : values) {
			getOrCreate(lists1, value & 0x000000FF).add(value);
		}
		
		partialSort(lists1, lists2, 0x00000000, 8);
		partialSort(lists2, lists1, 0x00000000, 16);
		partialSort(lists1, lists2, 0x80000000, 24);
		
		final Processor collector = new Processor() {
			
			private int i;
			
			@Override
			public final boolean process(final int value) {
				values[this.i++] = value;
				
				return true;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -5141753932964748059L;
			
		};
		
		for (final IntList list : lists2) {
			if (list != null) {
				list.forEach(collector);
			}
		}
	}
	
	private static final void partialSort(final IntList[] lists1, final IntList[] lists2, final int xor, final int shift) {
		final Processor processor = new Processor() {
			
			@Override
			public final boolean process(final int value) {
				getOrCreate(lists2, ((value ^ xor) >> shift) & 0x000000FF).add(value);
				
				return true;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4847026989229701538L;
			
		};
		
		for (final IntList list : lists1) {
			if (list != null) {
				list.forEach(processor);
				
				list.clear();
			}
		}
	}
	
	static final IntList getOrCreate(final IntList[] lists, final int index) {
		IntList result = lists[index];
		
		if (result == null) {
			result = new IntList();
			lists[index] = result;
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-27)
	 */
	public static abstract interface IntValue {
		
		public abstract int intValue();
		
	}
	
}
