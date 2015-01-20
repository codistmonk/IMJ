package imj3.draft.segmentation;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class CommonTools {
	
	private CommonTools() {
		throw new IllegalInstantiationException();
	}
	
	private static final Map<Object, Map<String, Object>> sharedProperties = new WeakHashMap<>();
	
	public static final void setSharedProperty(final Object object, final String key, final Object value) {
		sharedProperties.computeIfAbsent(object, o -> new HashMap<>()).put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getSharedProperty(final Object object, final String key) {
		return (T) sharedProperties.getOrDefault(object, Collections.emptyMap()).get(key);
	}
	
	public static final Iterable<int[]> cartesian(final int... minMaxes) {
		return new Iterable<int[]>() {
			
			@Override
			public final Iterator<int[]> iterator() {
				final int n = minMaxes.length / 2;
				
				return new Iterator<int[]>() {
					
					private int[] result;
					
					@Override
					public final boolean hasNext() {
						if (this.result == null) {
							this.result = new int[n];
							
							for (int i = 0; i < n; ++i) {
								this.result[i] = minMaxes[2 * i + 0];
							}
							
							--this.result[n - 1];
						}
						
						for (int i = 0; i < n; ++i) {
							if (this.result[i] < minMaxes[2 * i + 1]) {
								return true;
							}
						}
						
						return false;
					}
					
					@Override
					public final int[] next() {
						for (int i = n - 1; minMaxes[2 * i + 1] < ++this.result[i] && 0 < i; --i) {
							this.result[i] = minMaxes[2 * i + 0];
						}
						
						return this.result;
					}
					
				};
			}
			
		};
	}
	
	@SuppressWarnings("unchecked")
	public static final <A> A deepCopy(final A array) {
		if (!array.getClass().isArray()) {
			return array;
		}
		
		final int n = Array.getLength(array);
		final Object result = Array.newInstance(array.getClass().getComponentType(), n);
		
		for (int i = 0; i < n; ++i) {
			Array.set(result, i, deepCopy(Array.get(array, i)));
		}
		
		return (A) result;
	}
	
	public static final Property property(final String name, final Supplier<?> getter,
			final Function<String, ?> parser) {
		return new Property(name, getter, parser);
	}
	
	/**
	 * @author codistmonk (creation 2015-01-14)
	 */
	public static final class Property implements Serializable {
		
		private final String name;
		
		private final Supplier<?> getter;
		
		private final Function<String, ?> parser;
		
		public Property(final String name, final Supplier<?> getter,
				final Function<String, ?> parser) {
			this.name = name;
			this.getter = getter;
			this.parser = parser;
		}
		
		public final String getName() {
			return this.name;
		}
		
		public final Supplier<?> getGetter() {
			return this.getter;
		}
		
		public final Function<String, ?> getParser() {
			return this.parser;
		}
		
		private static final long serialVersionUID = -6068768247605642711L;
		
	}
	
}
