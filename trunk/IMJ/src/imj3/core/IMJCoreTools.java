package imj3.core;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-03-25)
 */
public final class IMJCoreTools {
	
	private IMJCoreTools() {
		throw new IllegalInstantiationException();
	}
	
	private static final Map<String, Reference<?>> cache = new HashMap<>();
	
	private static final List<Reference<?>> references = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	private static final WeakReference<Sentinel>[] sentinel = new WeakReference[1];
	
	public static final <T> T cache(final String key, final Supplier<T> supplier) {
		return cache(key, supplier, false);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T cache(final String key, final Supplier<T> supplier, final boolean refresh) {
		Reference<T> reference;
		
		synchronized (cache) {
			reference = (Reference<T>) cache.get(key);
			
			if (reference == null) {
				reference = new Reference<T>(key, supplier);
				cache.put(key, reference);
				references.add(reference);
				
				if (sentinel[0] == null) {
					new Sentinel(cache, references, sentinel);
				}
			} else if (refresh) {
				reference = new Reference<T>(key, supplier);
				cache.put(key, reference);
			}
		}
		
		return reference.getObject();
	}
	
	public static final void uncache(final String key) {
		synchronized (cache) {
			cache.remove(key);
			
			for (final Iterator<Reference<?>> i = references.iterator(); i.hasNext();) {
				if (key.equals(i.next().getKey())) {
					i.remove();
					return;
				}
			}
		}
	}
	
	public static final int quantize(final int value, final int q) {
		return q * (value / q);
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 */
	public static final class Sentinel implements Serializable {
		
		private final Map<?, Reference<?>> cache;
		
		private final List<Reference<?>> references;
		
		private final WeakReference<Sentinel>[] holder;
		
		public Sentinel(final Map<?, Reference<?>> cache, final List<Reference<?>> references,
				final WeakReference<Sentinel>[] holder) {
			this.cache = cache;
			this.references = references;
			this.holder = holder;
			
			holder[0] = new WeakReference<>(this);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			try {
				synchronized (this.cache) {
					this.holder[0] = null;
					final int n = (this.references.size() + 1) / 2;
					
					Collections.sort(this.references);
					
					final List<Reference<?>> toRemove = this.references.subList(0, n);
					
					toRemove.stream().map(Reference::getKey).forEach(this.cache::remove);
					toRemove.clear();
					
					if (!this.cache.isEmpty()) {
						new Sentinel(this.cache, this.references, this.holder);
					}
				}
			} finally {
				super.finalize();
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 8864723936263680017L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-11-29)
	 *
	 * @param <T>
	 */
	public static final class Reference<T> implements Serializable, Comparable<Reference<?>> {
		
		private final String key;
		
		private final Supplier<T> supplier;
		
		private T object;
		
		private final AtomicLong accessCount;
		
		public Reference(final String key, final Supplier<T> supplier) {
			this.key = key;
			this.supplier = supplier;
			this.accessCount = new AtomicLong();
		}
		
		public final String getKey() {
			return this.key;
		}
		
		public final synchronized T getObject() {
			this.accessCount.incrementAndGet();
			
			if (this.object == null) {
				this.object = this.supplier.get();
			}
			
			return this.object;
		}
		
		@Override
		public final int compareTo(final Reference<?> other) {
			return Long.compare(this.accessCount.get(), other.accessCount.get());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -4436070712210969420L;
		
	}
	
}
