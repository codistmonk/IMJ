package imj.apps.modules;

import static java.util.Collections.binarySearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author codistmonk (creation 2013-04-24)
 */
public final class CompactIntMap implements Serializable {
	
	private final List<Entry> entries;
	
	public CompactIntMap() {
		this.entries = new ArrayList<Entry>(1);
	}
	
	public final Object get(final int index) {
		final int i = binarySearch(this.entries, new Entry(index, null));
		
		return 0 <= i ? this.entries.get(i).getValue() : null;
	}
	
	public final void put(final int index, final Object value) {
		final int i = binarySearch(this.entries, new Entry(index, null));
		
		if (0 <= i) {
			this.entries.get(i).setValue(value);
		} else {
			this.entries.add(- (i + 1), new Entry(index, value));
		}
		
	}
	
	public final int getElementCount() {
		return this.entries.size();
	}
	
	public final Iterable<Entry> entries() {
		return this.entries;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 7158305695907390197L;
	
	/**
	 * @author codistmonk (creation 2013-04-24)
	 */
	public static final class Entry implements Comparable<Entry>, Serializable {
		
		private final int key;
		
		private Object value;
		
		public Entry(final int key, final Object value) {
			this.key = key;
			this.value = value;
		}
		
		public final int getKey() {
			return this.key;
		}
		
		@Override
		public final int compareTo(final Entry that) {
			return this.getKey() - that.getKey();
		}
		
		public final Object getValue() {
			return this.value;
		}
		
		public final void setValue(final Object value) {
			this.value = value;
		}
		
		@Override
		public final String toString() {
			return this.getKey() + "=" + this.getValue();
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -2702810316273508458L;
		
	}
	
}
