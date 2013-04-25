package imj.apps.modules;

import static imj.IMJTools.deepToString;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public final class TileDatabase implements Serializable, Iterable<Map.Entry<byte[], ? extends TileDatabase.Value>> {
	
	private final Map<Byte, Object> root;
	
	private final Class<? extends Value> valueFactory;
	
	private int entryCount;
	
	public TileDatabase() {
		this(Value.Default.class);
	}
	
	public TileDatabase(final Class<? extends Value> valueFactory) {
		this.root = newTree();
		this.valueFactory = valueFactory;
	}
	
	@Override
	public final Iterator<Entry<byte[], ? extends Value>> iterator() {
		final int d = getDepth(this.root);
		final MutableEntry<byte[], Value> entry = new MutableEntry<byte[], Value>(new byte[d]);
		final List<Iterator<Entry<Byte, Object>>> todo = new ArrayList<Iterator<Entry<Byte, Object>>>();
		
		todo.add(this.root.entrySet().iterator());
		
		return new Iterator<Map.Entry<byte[], ? extends Value>>() {
			
			@Override
			public final boolean hasNext() {
				return !todo.isEmpty();
			}
			
			@Override
			public final Entry<byte[], ? extends Value> next() {
				Entry<Byte, Object> nodeEntry = todo.get(0).next();
				entry.getKey()[todo.size() - 1] = nodeEntry.getKey().byteValue();
				
				while (todo.size() < d) {
					Map<Byte, Object> subTree = (Map<Byte, Object>) nodeEntry.getValue();
					todo.add(0, subTree.entrySet().iterator());
					nodeEntry = todo.get(0).next();
					entry.getKey()[todo.size() - 1] = nodeEntry.getKey().byteValue();
				}
				
				entry.setValue((Value) nodeEntry.getValue());
				
				while (!todo.isEmpty() && !todo.get(0).hasNext()) {
					todo.remove(0);
				}
				
				return entry;
			}
			
			@Override
			public final void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 *
	 * @param <K>
	 * @param <V>
	 */
	private static final class MutableEntry<K, V> implements Map.Entry<K, V> {
		
		private final K key;
		
		private V value;
		
		public MutableEntry(final K key) {
			this.key = key;
		}
		
		@Override
		public final K getKey() {
			return this.key;
		}
		
		@Override
		public final V getValue() {
			return this.value;
		}
		
		@Override
		public final V setValue(final V value) {
			final V result = this.value;
			this.value = value;
			
			return result;
		}
		
		public final String toString() {
			return deepToString(this.getKey()) + "=" + this.getValue();
		}
		
	}
	
	public final <V extends Value> V add(final byte[] key) {
		final int n = key.length;
		final int lastIndex = n - 1;
		Map<Byte, Object> node = this.root;
		
		for (int i = 0; i < lastIndex; ++i) {
			node = getOrCreateSubTree(node, key[i]);
		}
		
		final Byte lastValue = key[lastIndex];
		Value result = (Value) node.get(lastValue);
		
		if (result == null) {
			try {
				result = this.valueFactory.newInstance();
				node.put(lastValue, result);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
			
			++this.entryCount;
		} else {
			result.incrementCount();
		}
		
		return (V) result;
	}
	
	public final int getEntryCount() {
		return this.entryCount;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8212359447131338635L;
	
	private static final <K> Map<K, Object> newTree() {
		return new TreeMap<K, Object>();
	}
	
	public static final <K> int getDepth(final Object root) {
		final Map<K, Object> node = cast(Map.class, root);
		
		return node == null ? 0 : 1 + getDepth(node.values().iterator().next());
	}
	
	public static final <K> Map<K, Object> getOrCreateSubTree(final Map<K, Object> node, final K key) {
		Map<K, Object> result = (Map<K, Object>) node.get(key);
		
		if (result == null) {
			result = newTree();
			node.put(key, result);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static abstract interface Value extends Serializable {
		
		public abstract int getCount();
		
		public abstract void incrementCount();
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 */
		public static final class Default implements Value {
			
			private int count = 1;
			
			@Override
			public final int getCount() {
				return this.count;
			}
			
			@Override
			public final void incrementCount() {
				++this.count;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 8585019119527978654L;
			
		}
		
	}
	
}
