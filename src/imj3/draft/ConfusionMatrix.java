package imj3.draft;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * @author codistmonk (creation 2015-04-22)
 */
public final class ConfusionMatrix<K extends Comparable<K>> implements Serializable {
	
	private final Map<K, Map<K, AtomicDouble>> counts = new TreeMap<>();
	
	public final Map<K, Map<K, AtomicDouble>> getCounts() {
		return this.counts;
	}
	
	public final void count(final K predicted, final K actual) {
		this.count(predicted, actual, 1.0);
	}
	
	public final void count(final K predicted, final K actual, final double n) {
		this.getCounts().computeIfAbsent(predicted, p -> new TreeMap<>()).computeIfAbsent(
				actual, e -> new AtomicDouble()).addAndGet(n);
	}
	
	public final double computeAccuracy() {
		final AtomicDouble tp = new AtomicDouble();
		final AtomicDouble total = new AtomicDouble();
		
		for (final Entry<K, Map<K, AtomicDouble>> entry : this.getCounts().entrySet()) {
			final Object predicted = entry.getKey();
			
			for (final Map.Entry<?, AtomicDouble> subentry : entry.getValue().entrySet()) {
				final Object actual = subentry.getKey();
				final double delta = subentry.getValue().get();
				
				if (predicted.equals(actual)) {
					tp.addAndGet(delta);
				}
				
				total.addAndGet(delta);
			}
		}
		
		return tp.get() / total.get();
	}
	
	public final Map<K, Double> computeF1s() {
		final Map<Object, AtomicDouble> tps = new HashMap<>();
		final Map<Object, AtomicDouble> fps = new HashMap<>();
		final Map<Object, AtomicDouble> fns = new HashMap<>();
		final Collection<K> keys = new HashSet<>(this.getCounts().keySet());
		
		for (final Entry<K, Map<K, AtomicDouble>> entry : this.getCounts().entrySet()) {
			final Object predicted = entry.getKey();
			
			keys.addAll(entry.getValue().keySet());
			
			for (final Map.Entry<?, AtomicDouble> subentry : entry.getValue().entrySet()) {
				final Object actual = subentry.getKey();
				final double delta = subentry.getValue().get();
				
				if (predicted.equals(actual)) {
					increment(tps, predicted, delta);
				} else {
					increment(fps, predicted, delta);
					increment(fns, actual, delta);
				}
			}
		}
		
		{
			final Map<K, Double> result = new TreeMap<>();
			
			for (final K key : keys) {
				final double tp = tps.getOrDefault(key, ZERO).get();
				final double fp = fps.getOrDefault(key, ZERO).get();
				final double fn = fns.getOrDefault(key, ZERO).get();
				
				result.put(key, 2.0 * tp / (2.0 * tp + fp + fn));
			}
			
			return result;
		}
		
	}
	
	public final double computeMacroF1() {
		return computeMacroF1(this.computeF1s());
	}
	
	private static final long serialVersionUID = -3078169987830724986L;
	
	public static final AtomicDouble ZERO = new AtomicDouble();
	
	public static void increment(final Map<Object, AtomicDouble> counts, final Object key, final double delta) {
		counts.computeIfAbsent(key, e -> new AtomicDouble()).addAndGet(delta);
	}
	
	public static final double computeMacroF1(final Map<?, Double> f1s) {
		return f1s.values().stream().mapToDouble(Double::doubleValue).average().getAsDouble();
	}
	
	/**
	 * @author codistmonk (creation 2016-07-09)
	 */
	public static final class AtomicDouble extends Number {
		
		private double value;
		
		public AtomicDouble() {
			this(0.0);
		}
		
		public AtomicDouble(final double value) {
			this.value = value;
		}
		
		public final synchronized double get() {
			return this.value;
		}
		
		public final synchronized void set(final double value) {
			this.value = value;
		}
		
		public final synchronized double addAndGet(final double delta) {
			this.set(this.get() + delta);
			
			return this.get();
		}
		
		public final synchronized double getAndAdd(final double delta) {
			final double result = this.get();
			
			this.addAndGet(delta);
			
			return result;
		}
		
		@Override
		public final int intValue() {
			return (int) this.get();
		}
		
		@Override
		public final long longValue() {
			return (long) this.get();
		}
		
		@Override
		public final float floatValue() {
			return (float) this.get();
		}
		
		@Override
		public final double doubleValue() {
			return this.get();
		}
		
		@Override
		public final String toString() {
			return Double.toString(this.get());
		}
		
		private static final long serialVersionUID = -8053745274905128807L;
		
	}
	
}