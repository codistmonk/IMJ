package imj.database;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public final class PatchDatabase<V extends PatchDatabase.Value> implements Serializable, Iterable<Map.Entry<byte[], V>> {
	
	private final Map<byte[], V> data;
	
	private final Class<? extends Value> valueFactory;
	
	public PatchDatabase() {
		this(Value.Default.class);
	}
	
	public PatchDatabase(final Class<? extends Value> valueFactory) {
		this.data = new TreeMap<byte[], V>(ByteArrayComparator.INSTANCE);
		this.valueFactory = valueFactory;
	}
	
	@Override
	public final Iterator<Entry<byte[], V>> iterator() {
		return this.data.entrySet().iterator();
	}
	
	public final V get(final byte[] key) {
		return this.data.get(key);
	}
	
	public final V add(final byte[] key) {
		V result = this.get(key);
		
		if (result == null) {
			try {
				result = (V) this.valueFactory.newInstance();
				result.setKey(key.clone());
				this.data.put(result.getKey(), result);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} else {
			result.incrementCount(1);
		}
		
		assert null != result.getKey() && key != result.getKey();
		
		return result;
	}
	
	public final V remove(final byte[] key) {
		return this.data.remove(key);
	}
	
	public final int getEntryCount() {
		return this.data.size();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 8212359447131338635L;
	
	/**
	 * @author codistmonk (creation 2013-04-22)
	 */
	public static abstract interface Value extends Serializable {
		
		public abstract byte[] getKey();
		
		public abstract void setKey(byte[] key);
		
		public abstract int getCount();
		
		public abstract void incrementCount(int increment);
		
		/**
		 * @author codistmonk (creation 2013-04-22)
		 */
		public static final class Default implements Value {
			
			private byte[] key;
			
			private int count = 1;
			
			public final byte[] getKey() {
				return this.key;
			}
			
			public final void setKey(final byte[] key) {
				this.key = key;
			}
			
			@Override
			public final int getCount() {
				return this.count;
			}
			
			@Override
			public final void incrementCount(final int increment) {
				this.count += increment;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 8585019119527978654L;
			
		}
		
	}
	
}
