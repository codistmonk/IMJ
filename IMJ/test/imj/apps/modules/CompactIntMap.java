package imj.apps.modules;

import static imj.IMJTools.deepToString;
import static java.lang.Math.abs;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;
import static java.util.Collections.binarySearch;
import static net.sourceforge.aprog.tools.Tools.array;

import imj.IMJTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author codistmonk (creation 2013-04-24)
 */
public final class CompactIntMap {
	
	private final List<Chunk> chunks;
	
	private int elementCount;
	
	public CompactIntMap() {
		this.chunks = new ArrayList<Chunk>(1);
//		this.chunks = new LinkedList<Chunk>();
	}
	
	public final Object get(final int index) {
		int i = binarySearch(this.chunks, new Chunk(index, null));
		
		if (0 <= i) {
//			return Special.convert(this.chunks.get(i).getValues()[0]);
			return this.chunks.get(i).getValues()[0];
		}
		
		i = - (i + 2);
		
		if (i < 0) {
			return null;
		}
		
		final Chunk chunk = this.chunks.get(i);
		final int j = index - chunk.getStart();
		
		if (chunk.getValues().length <= j) {
			return null;
		}
		
//		return Special.convert(chunk.getValues()[j]);
		return chunk.getValues()[j];
	}
	
	public final void put(final int index, final Object value) {
		int i = binarySearch(this.chunks, new Chunk(index, null));
		
		if (0 <= i) {
			this.chunks.get(i).getValues()[0] = value;
			
			return;
		}
		
		i = - (i + 1);
		
		boolean mergeLeft = false;
		boolean mergeRight = false;
		final Chunk left;
		final Chunk right;
		
		if (0 < i) {
			left = this.chunks.get(i - 1);
			final int j = index - left.getStart();
			final int n = left.getValues().length;
			
			if (j < n) {
//				if (left.getValues()[j] == Special.NULL) {
				if (left.getValues()[j] == null) {
					++this.elementCount;
				}
				
				left.getValues()[j] = value;
				
				return;
			}
			
//			mergeLeft = j == n;
			mergeLeft = j < n + MINIMUM_CHUNK_SIZE;
		} else {
			left = null;
		}
		
		if (i < this.getChunkCount()) {
			right = this.chunks.get(i);
//			mergeRight = index + 1 == right.getStart();
			mergeRight = right.getStart() - MINIMUM_CHUNK_SIZE <= index;
		} else {
			right = null;
		}
		
		mergeLeft = mergeRight = false;
		
		if (mergeLeft && mergeRight) {
			final int n1 = left.getValues().length;
			final int n2 = right.getValues().length;
//			final Chunk newChunk = new Chunk(left.getStart(), copyOf(left.getValues(), n1 + 1 + n2));
//			System.arraycopy(right.getValues(), 0, newChunk.getValues(), n1 + 1, n2);
//			newChunk.getValues()[n1] = value;
			final Chunk newChunk = new Chunk(left.getStart(), copyOf(left.getValues(), n1 + MINIMUM_CHUNK_SIZE + n2));
//			fill(newChunk.getValues(), n1, n1 + MINIMUM_CHUNK_SIZE, Special.NULL);
			System.arraycopy(right.getValues(), 0, newChunk.getValues(), n1 + MINIMUM_CHUNK_SIZE, n2);
			newChunk.getValues()[index - newChunk.getStart()] = value;
			this.chunks.set(i - 1, newChunk);
			this.chunks.remove(i);
		} else if (mergeLeft) {
			final int n = left.getValues().length;
//			final Chunk newChunk = new Chunk(left.getStart(), copyOf(left.getValues(), n + 1));
//			newChunk.getValues()[n] = value;
			final Chunk newChunk = new Chunk(left.getStart(), copyOf(left.getValues(), n + MINIMUM_CHUNK_SIZE));
//			fill(newChunk.getValues(), n, n + MINIMUM_CHUNK_SIZE, Special.NULL);
			newChunk.getValues()[index - newChunk.getStart()] = value;
			this.chunks.set(i - 1, newChunk);
		} else if (mergeRight) {
			final int n = right.getValues().length;
//			final Chunk newChunk = new Chunk(index, new Object[n + 1]);
//			System.arraycopy(right.getValues(), 0, newChunk.getValues(), 1, n);
//			newChunk.getValues()[0] = value;
			final Chunk newChunk = new Chunk(index & MINIMUM_CHUNK_SIZE_MASK, new Object[n + MINIMUM_CHUNK_SIZE]);
//			fill(newChunk.getValues(), 0, right.getStart() - newChunk.getStart(), Special.NULL);
			System.arraycopy(right.getValues(), 0, newChunk.getValues(), right.getStart() - newChunk.getStart(), n);
			newChunk.getValues()[index - newChunk.getStart()] = value;
			this.chunks.set(i, newChunk);
		} else {
//			this.chunks.add(i, new Chunk(index, array(value)));
			final Chunk newChunk = new Chunk(index & MINIMUM_CHUNK_SIZE_MASK, new Object[MINIMUM_CHUNK_SIZE]);
//			fill(newChunk.getValues(), Special.NULL);
			newChunk.getValues()[index - newChunk.getStart()] = value;
			this.chunks.add(i, newChunk);
		}
		
		++this.elementCount;
	}
	
	public static final int signum(final int x) {
		return x < 0 ? -1 : x == 0 ? 0 : +1;
	}
	
	public final int getElementCount() {
		return this.elementCount;
	}
	
	public final Iterable<Entry> entries() {
		final Iterable<Chunk> chunks = this.chunks;
		
		return new Iterable<CompactIntMap.Entry>() {
			
			@Override
			public final Iterator<Entry> iterator() {
				final Entry entry = new Entry();
				
				return new Iterator<Entry>() {
					
					private Iterator<Chunk> chunkIterator = chunks.iterator();
					
					private Chunk chunk;
					
					private int i;
					
					@Override
					public final boolean hasNext() {
						return this.chunkIterator.hasNext() || (this.chunk != null && this.i < this.chunk.getValues().length);
					}
					
					@Override
					public final Entry next() {
						if (this.chunk == null || this.chunk.getValues().length <= this.i) {
							this.chunk = this.chunkIterator.next();
//							this.i = 0;
							this.i = nextValidIndex(this.chunk.getValues(), 0);
						}
						
						entry.setKey(this.chunk.getStart() + this.i);
						entry.setValue(this.chunk.getValues()[this.i]);
						
//						++this.i;
						this.i = nextValidIndex(this.chunk.getValues(), this.i + 1);
						
						return entry;
					}
					
					@Override
					public final void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}
	
	public static final int nextValidIndex(final Object[] array, final int start) {
		int i = start;
		
//		while (i < array.length && array[i] == Special.NULL) {
		while (i < array.length && array[i] == null) {
			++i;
		}
		
		return i;
	}
	
	final int getChunkCount() {
		return this.chunks.size();
	}
	
	/**
	 * {@value}.
	 */
	static final int MINIMUM_CHUNK_SIZE = 1 << 3;
	
	/**
	 * {@value}.
	 */
	static final int MINIMUM_CHUNK_SIZE_MASK = ~(MINIMUM_CHUNK_SIZE - 1);
	
//	/**
//	 * @author codistmonk (creation 2013-04-24)
//	 */
//	static enum Special {
//		
//		NULL;
//		
//		public static final Object convert(final Object object) {
//			return object == NULL ? null : object;
//		}
//		
//	}
	
	/**
	 * @author codistmonk (creation 2013-04-24)
	 */
	public static final class Entry {
		
		private int key;
		
		private Object value;
		
		public final int getKey() {
			return this.key;
		}
		
		public final Object getValue() {
			return this.value;
		}
		
		@Override
		public final String toString() {
			return this.getKey() + "=" + deepToString(this.getValue());
		}
		
		final void setKey(final int key) {
			this.key = key;
		}
		
		final void setValue(final Object value) {
			this.value = value;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-24)
	 */
	public static final class Chunk implements Comparable<Chunk> {
		
		private final int start;
		
		private final Object[] values;
		
		public Chunk(final int start, final Object[] values) {
			this.start = start;
			this.values = values;
		}
		
		public final int getStart() {
			return this.start;
		}
		
		public final Object[] getValues() {
			return this.values;
		}
		
		@Override
		public final int compareTo(final Chunk that) {
			return this.getStart() - that.getStart();
		}
		
		@Override
		public final String toString() {
			return this.getStart() + ":" + Arrays.toString(this.getValues());
		}
		
	}
	
}
