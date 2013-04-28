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
public final class TileDatabase<V extends TileDatabase.Value> implements Serializable, Iterable<Map.Entry<byte[], V>> {
	
	private final Map<byte[], V> data;
	
	private final Class<? extends Value> valueFactory;
	
	public TileDatabase() {
		this(Value.Default.class);
	}
	
	public TileDatabase(final Class<? extends Value> valueFactory) {
		this.data = new TreeMap<byte[], V>(ByteArrayComparator.INSTANCE);
		this.valueFactory = valueFactory;
	}
	
	@Override
	public final Iterator<Entry<byte[], V>> iterator() {
		return this.data.entrySet().iterator();
	}
	
	public final V add(final byte[] key) {
		V result = this.data.get(key);
		
		if (result == null) {
			try {
				result = (V) this.valueFactory.newInstance();
				this.data.put(key.clone(), result);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} else {
			result.incrementCount();
		}
		
		return (V) result;
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
