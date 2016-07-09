package imj3.draft;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author codistmonk (creation 2015-04-22)
 */
public final class ConfusionMatrix implements Serializable {
	
	private final Map<Comparable<?>, Map<Comparable<?>, AtomicLong>> counts = new TreeMap<>();
	
	public final Map<Comparable<?>, Map<Comparable<?>, AtomicLong>> getCounts() {
		return this.counts;
	}
	
	public final <C extends Comparable<C>> void count(final C predicted, final C expected) {
		this.getCounts().computeIfAbsent(predicted, p -> new TreeMap<>()).computeIfAbsent(
				expected, e -> new AtomicLong()).incrementAndGet();
	}
	
	public final double computeAccuracy() {
		final AtomicLong tp = new AtomicLong();
		final AtomicLong total = new AtomicLong();
		
		for (final Entry<Comparable<?>, Map<Comparable<?>, AtomicLong>> entry : this.getCounts().entrySet()) {
			final Object predicted = entry.getKey();
			
			for (final Map.Entry<?, AtomicLong> subentry : entry.getValue().entrySet()) {
				final Object expected = subentry.getKey();
				final long delta = subentry.getValue().get();
				
				if (predicted.equals(expected)) {
					tp.addAndGet(delta);
				}
				
				total.addAndGet(delta);
			}
		}
		
		return (double) tp.get() / total.get();
	}
	
	public final Map<Comparable<?>, Double> computeF1s() {
		final Map<Object, AtomicLong> tps = new HashMap<>();
		final Map<Object, AtomicLong> fps = new HashMap<>();
		final Map<Object, AtomicLong> fns = new HashMap<>();
		final Collection<Comparable<?>> keys = new HashSet<>(this.getCounts().keySet());
		
		for (final Entry<Comparable<?>, Map<Comparable<?>, AtomicLong>> entry : this.getCounts().entrySet()) {
			final Object predicted = entry.getKey();
			
			keys.addAll(entry.getValue().keySet());
			
			for (final Map.Entry<?, AtomicLong> subentry : entry.getValue().entrySet()) {
				final Object expected = subentry.getKey();
				final long delta = subentry.getValue().get();
				
				if (predicted.equals(expected)) {
					increment(tps, predicted, delta);
				} else {
					increment(fps, predicted, delta);
					increment(fns, expected, delta);
				}
			}
		}
		
		{
			final Map<Comparable<?>, Double> result = new TreeMap<>();
			
			for (final Comparable<?> key : keys) {
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
	
	public static final AtomicLong ZERO = new AtomicLong();
	
	public static void increment(final Map<Object, AtomicLong> counts, final Object key, final long delta) {
		counts.computeIfAbsent(key, e -> new AtomicLong()).addAndGet(delta);
	}
	
	public static final double computeMacroF1(final Map<?, Double> f1s) {
		return f1s.values().stream().mapToDouble(Double::doubleValue).average().getAsDouble();
	}
	
}