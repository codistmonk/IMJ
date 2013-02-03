package imj;

import static java.util.Arrays.copyOfRange;

import java.util.Arrays;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class IntList {
	
	private int[] values;
	
	private int first;
	
	private int end;
	
	public IntList() {
		this(16);
	}
	
	public IntList(final int initialCapacity) {
		this.values = new int[initialCapacity];
	}
	
	public final void clear() {
		this.first = 0;
		this.end = 0;
	}
	
	public final int size() {
		return this.end - this.first;
	}
	
	public final void add(final int value) {
		if (this.values.length <= this.end) {
			if (0 < this.first) {
				System.arraycopy(this.values, this.first, this.values, 0, this.size());
				this.end -= this.first;
				this.first = 0;
			} else {
				this.values = Arrays.copyOf(this.values, 2 * this.size());
			}
		}
		
		this.values[this.end++] = value;
	}
	
	public final void addAll(final int... values) {
		for (final int value : values) {
			this.add(value);
		}
	}
	
	public final int get(final int index) {
		return this.values[this.first + index];
	}
	
	public final void set(final int index, final int value) {
		this.values[this.first + index] = value;
	}
	
	public final int remove(final int index) {
		if (index == 0) {
			return this.values[this.first++];
		}
		
		final int result = this.get(index);
		
		System.arraycopy(this.values, this.first + index + 1, this.values, this.first + index, this.size() - 1 - index);
		--this.end;
		
		return result;
	}
	
	public final boolean isEmpty() {
		return this.size() <= 0;
	}
	
	public final void sort() {
		Arrays.sort(this.values, this.first, this.end);
	}
	
	public final int[] toArray() {
		return copyOfRange(this.values, this.first, this.end);
	}
	
	public final String toString() {
		final StringBuilder resultBuilder = new StringBuilder();
		
		resultBuilder.append('[');
		
		if (!this.isEmpty()) {
			resultBuilder.append(this.get(0));
			
			final int n = this.size();
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(' ').append(this.get(i));
			}
		}
		
		resultBuilder.append(']');
		
		return resultBuilder.toString();
	}
	
}
