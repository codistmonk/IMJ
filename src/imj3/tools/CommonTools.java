package imj3.tools;

import static multij.tools.Tools.unchecked;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class CommonTools {
	
	private CommonTools() {
		throw unchecked(new InstantiationException());
	}
	
	private static final Map<Object, Map<String, Object>> sharedProperties = new WeakHashMap<>();
	
	public static final void setSharedProperty(final Object object, final String key, final Object value) {
		sharedProperties.computeIfAbsent(object, o -> new HashMap<>()).put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getSharedProperty(final Object object, final String key) {
		return (T) sharedProperties.getOrDefault(object, Collections.emptyMap()).get(key);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getSharedProperty(final Object object, final String key,
			final Function<? super String, ? extends Object> mappingIfAbsent) {
		return (T) sharedProperties.computeIfAbsent(object, o -> new HashMap<>()).computeIfAbsent(key, mappingIfAbsent);
	}
	
	public static final <T> T newInstanceOf(final Class<T> cls) {
		try {
			return cls.newInstance();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T newInstanceOf(final Class<T> cls, final Object... arguments) {
		for (final Constructor<?> constructor : cls.getConstructors()) {
			try {
				return (T) constructor.newInstance(arguments);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		return null;
	}
	
	public static final void swap(final Object[] array1, final int index1, final Object[] array2, final int index2) {
		final Object tmp = array1[index1];
		array1[index1] = array2[index2];
		array2[index2] = tmp;
	}
	
	public static final String formatColor(final long color) {
		return "#" + String.format("%06X", color & 0x00FFFFFF);
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
	
	public static final Class<?> classForName(final String className) {
		try {
			return Class.forName(className);
		} catch (final ClassNotFoundException exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Object fieldValue(final Object objectOrClass, final String fieldName) {
		final Class<?> cls = objectOrClass instanceof Class<?> ? (Class<?>) objectOrClass : objectOrClass.getClass();
		
		try {
			return cls.getField(fieldName).get(objectOrClass);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Field accessible(final Field field) {
		field.setAccessible(true);
		
		return field;
	}
	
	public static final Field field(final Object object, final String fieldName) {
		return field(object.getClass(), fieldName);
	}
	
	public static final Field field(final Class<?> cls, final String fieldName) {
		try {
			try {
				return accessible(cls.getDeclaredField(fieldName));
			} catch (final NoSuchFieldException exception) {
				return accessible(cls.getField(fieldName));
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getFieldValue(final Object object, final String fieldName) {
		try {
			return (T) field(object, fieldName).get(object);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getFieldValue(final Class<?> cls, final String fieldName) {
		try {
			return (T) field(cls, fieldName).get(null);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
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
	
	/**
	 * @author codistmonk (creation 2015-02-21)
	 */
	public static abstract interface FileProcessor extends Serializable {
		
		public abstract void process(File file);
		
		public static void deepForEachFileIn(final File root, final FileProcessor processor) {
			final File[] files = root.listFiles();
			
			if (files == null) {
				return;
			}
			
			for (final File file : files) {
				if (file.isDirectory()) {
					deepForEachFileIn(file, processor);
				} else {
					processor.process(file);
				}
			}
		}
		
	}
	
}
