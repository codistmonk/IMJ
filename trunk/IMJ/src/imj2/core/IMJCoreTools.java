package imj2.core;

import static java.util.Collections.sort;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-11-10)
 */
public abstract class IMJCoreTools {
	
	protected IMJCoreTools() {
		throw new IllegalInstantiationException();
	}
	
	private static final Map<Object, CachedValue> cache = new HashMap<Object, CachedValue>();
	
	static {
		CacheCleaner.setup();
	}
	
	public static final <V> V cache(final Object key, final Callable<V> valueFactory) {
		CachedValue cachedValue;
		
		synchronized (cache) {
			cachedValue = cache.get(key);
			
			if (cachedValue == null) {
				cachedValue = new CachedValue(valueFactory);
				cache.put(key, cachedValue);
			}
		}
		
		return cachedValue.getValue();
	}
	
	public static final void removeOldCacheEntries(final double ratio) {
		synchronized (cache) {
			final List<Map.Entry<Object, CachedValue>> entries = new ArrayList<Map.Entry<Object, CachedValue>>(cache.entrySet());
			
			sort(entries, new Comparator<Map.Entry<Object, CachedValue>>() {
				
				@Override
				public final int compare(final Entry<Object, CachedValue> entry1, final Entry<Object, CachedValue> entry2) {
					return Long.signum(entry1.getValue().getLastAccess() - entry2.getValue().getLastAccess());
				}
				
			});
			
			final int n = (int) (ratio * entries.size());
			
			for (int i = 0; i < n; ++i) {
				final Entry<Object, CachedValue> entry = entries.get(i);
				
				if (!entry.getValue().isBusy()) {
					cache.remove(entry.getKey());
				}
			}
		}
	}
	
	public static final int quantize(final int value, final int quantum) {
		return (value / quantum) * quantum;
	}
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	static final class CachedValue {
		
		private long lastAccess;
		
		private final Callable<?> valueFactory;
		
		private Object value;
		
		private final AtomicBoolean busy;
		
		CachedValue(final Callable<?> valueFActory) {
			this.valueFactory = valueFActory;
			this.busy = new AtomicBoolean();
		}
		
		final boolean isBusy() {
			return this.busy.get();
		}
		
		final long getLastAccess() {
			return this.lastAccess;
		}
		
		@SuppressWarnings("unchecked")
		final synchronized <T> T getValue() {
			if (this.value == null) {
				try {
					this.busy.set(true);
					this.value = this.valueFactory.call();
				} catch (final Exception exception) {
					throw Tools.unchecked(exception);
				} finally {
					this.busy.set(false);
				}
			}
			
			this.lastAccess = timestamp.addAndGet(1L);
			
			return (T) this.value;
		}
		
		private static final AtomicLong timestamp = new AtomicLong(Long.MIN_VALUE);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-08-13)
	 */
	private static final class CacheCleaner {
		
		private CacheCleaner() {
			cleaner = new WeakReference<CacheCleaner>(this);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			final Runtime runtime = Runtime.getRuntime();
			
			if (Tools.usedMemory() > runtime.maxMemory() / 2L) {
				removeOldCacheEntries(1.0 / 8.0);
			}
			
			super.finalize();
			
			new CacheCleaner();
		}
		
		private static Reference<CacheCleaner> cleaner;
		
		static final void setup() {
			if (cleaner == null) {
				new CacheCleaner();
			}
		}
		
	}
	
}
