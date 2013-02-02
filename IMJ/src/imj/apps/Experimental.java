package imj.apps;

import static imj.BigViewerTools.readTile;
import static java.util.Arrays.copyOfRange;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.ImageComponent;
import imj.ImageOfBufferedImage;
import imj.ImageOfBufferedImage.Feature;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-01-22)
 */
public final class Experimental {
	
	private Experimental() {
		throw new IllegalInstantiationException();
	}
	
	private static final Map<String, Class<?>> primitiveClasses;
	
	static {
		primitiveClasses = new HashMap<String, Class<?>>();
		
		primitiveClasses.put("boolean", boolean.class);
		primitiveClasses.put("byte", byte.class);
		primitiveClasses.put("short", short.class);
		primitiveClasses.put("char", char.class);
		primitiveClasses.put("int", int.class);
		primitiveClasses.put("long", long.class);
		primitiveClasses.put("float", float.class);
		primitiveClasses.put("double", double.class);
		primitiveClasses.put("void", void.class);
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final int n = commandLineArguments.length;
		
		Operation.valueOf(commandLineArguments[0].toUpperCase()).process(
				new CommandLineArgumentsParser(copyOfRange(commandLineArguments, 1, n)));
	}
	
	public static final Class<?> classForName(final String name) {
		final Class<?> primitiveClass = primitiveClasses.get(name);
		
		try {
			return primitiveClass != null ? primitiveClass : Class.forName(name);
		} catch (final ClassNotFoundException exception) {
			throw unchecked(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static enum Operation {
		
		IM2JO {
			
			@Override
			public final void process(final CommandLineArgumentsParser arguments) {
				final String infile = arguments.get("file", (String) null);
				final String outfile0 = arguments.get("to", (String) null);
				final String outfile = outfile0 != null ? outfile0 : infile + ".jo";
				final String format = arguments.get("format", "double");
				final String layout = arguments.get("layout", "y,x,rgb");
				
				try {
					final BufferedImage image = ImageIO.read(new File(infile));
					final Map<String, Object> data = new HashMap<String, Object>();
					final int width = image.getWidth();
					final int height = image.getHeight();
					final int[] colorBits = image.getColorModel().getComponentSize();
					
					debugPrint("colorBits:", Arrays.toString(colorBits));
					
					data.put("width", width);
					data.put("height", height);
					data.put("layout", layout);
					
					final int elementCount = width * height * colorBits.length;
					final ArraySetter<?> arraySetter = ArraySetter.forElements(classForName(format), elementCount);
					new ImageCopier(image, layout.split(","), arraySetter).run();
					
					debugPrint("dataLength", Array.getLength(arraySetter.getArray()));
					data.put("data", arraySetter.getArray());
					
					final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outfile));
					oos.writeObject(data);
					oos.flush();
					oos.reset();
					oos.close();
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
			
		}, JO2IM {
			
			@Override
			public final void process(final CommandLineArgumentsParser arguments) {
				final String infile = arguments.get("file", (String) null);
				final String outfile0 = arguments.get("to", (String) null);
				final String format = arguments.get("format", "png");
				final String outfile = outfile0 != null ? outfile0 : infile + "." + format;
				
				try {
					final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(infile));
					final Map<String, Object> data = (Map<String, Object>) ois.readObject();
					ois.close();
					
					final int width = (Integer) data.get("width");
					final int height = (Integer) data.get("height");
					final Object array = data.get("data");
					final int elementCount = Array.getLength(array);
					final int componentsPerPixel = elementCount / width / height;
					final BufferedImage image;
					
					switch (componentsPerPixel) {
					case 1:
						image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
						break;
					case 3:
						image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
						break;
					case 4:
						image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						break;
					default:
						throw new IllegalArgumentException();
					}
					
					ImageIO.write(image, format, new File(outfile));
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
			
		}, JO2JO {
			
			@Override
			public final void process(final CommandLineArgumentsParser arguments) {
				final String infile = arguments.get("file", (String) null);
				final String outfile0 = arguments.get("to", (String) null);
				final String outfile = outfile0 != null ? outfile0 : infile + ".jo";
				final String format = arguments.get("format", "double");
				final String layout = arguments.get("layout", "*");
				
				try {
					final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(infile));
					final Map<String, Object> data = (Map<String, Object>) ois.readObject();
					ois.close();
					
					final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outfile));
					oos.writeObject(data);
					oos.close();
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
			
		}, SHOW {
			
			@Override
			public final void process(final CommandLineArgumentsParser arguments) {
				final String infile = arguments.get("file", (String) null);
				
				try {
					final IFormatReader reader = new ImageReader();
					reader.setId(infile);
//					ImageComponent.show(new ImageOfBufferedImage(ImageIO.read(new File(infile))), infile);
					ImageComponent.show(infile, new ImageOfBufferedImage(
							readTile(reader, 0, 0, reader.getSizeX(), reader.getSizeY()), Feature.MAX_RGB));
					reader.close();
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
			
		};
		
		public abstract void process(CommandLineArgumentsParser arguments);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static final class ImageCopier implements Runnable {
		
		private final BufferedImage image;
		
		private final Index x;
		
		private final Index y;
		
		private final Index color;
		
		private final IndexIterator traversor;
		
		private final ArraySetter<?> arraySetter;
		
		public ImageCopier(final BufferedImage image, final String[] layout, final ArraySetter<?> arraySetter) {
			this.image = image;
			this.x = new SequentialIndex(0, image.getWidth());
			this.y = new SequentialIndex(0, image.getHeight());
			this.color = rgba(image.getColorModel().getComponentSize().length, layout);
			this.traversor = this.newTraversor(layout);
			this.arraySetter = arraySetter;
		}
		
		private final IndexIterator newTraversor(final String[] layout) {
			final Index[] indices = new Index[3];
			int n = indices.length;
			
			for (final String dimension : layout) {
				if (dimension.matches("[rgba]+")) {
					indices[--n] = this.color;
				} else if (dimension.matches("x")) {
					indices[--n] = this.x;
				} else if (dimension.matches("y")) {
					indices[--n] = this.y;
				}
			}
			
			debugPrint(layout);
			debugPrint(indices);
			
			return new IndexIterator(indices);
		}
		
		public ImageCopier(final BufferedImage image, final Index x, final Index y, final Index color,
				final IndexIterator traversor, final ArraySetter<?> targetArray) {
			this.image = image;
			this.x = x;
			this.y = y;
			this.color = color;
			this.traversor = traversor;
			this.arraySetter = targetArray;
		}
		
		@Override
		public final void run() {
			final int[] buffer = new int[4];
			final WritableRaster raster = this.image.getRaster();
			final int n = Array.getLength(this.arraySetter.getArray());
			
			for (final IndexIterator i : this.traversor) {
				final int x = this.x.getValue();
				final int y = this.y.getValue();
				final int c = this.color.getValue();
				
				raster.getPixel(x, y, buffer);
				
				this.arraySetter.set(i.getIndex(), buffer[c]);
			}
		}
		
		public static final Index rgba(final int componentsPerPixel, final String[] layout) {
			for (final String dimension : layout) {
				if (dimension.matches("[rgba]+")) {
					final int n = dimension.length();
					final int[] indices = new int[n];
					
					for (int i = 0; i < n; ++i) {
						switch (dimension.charAt(i)) {
						case 'r':
							indices[i] = 0;
							break;
						case 'g':
							indices[i] = 1;
							break;
						case 'b':
							indices[i] = 2;
							break;
						case 'a':
							indices[i] = 3;
							break;
						default:
							break;
						}
					}
					
					return new ShuffledIndex(indices);
				}
			}
			
			return new SequentialIndex(0, componentsPerPixel);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static abstract class ArraySetter<A> {
		
		private final A array;
		
		public ArraySetter(final A array) {
			this.array = array;
		}
		
		public final A getArray() {
			return this.array;
		}
		
		public abstract void set(int index, int value);
		
		public static final ArraySetter<?> forElements(final Class<?> elementClass, final int elementCount) {
			for (final Class<?> implementationClass : ArraySetter.class.getDeclaredClasses()) {
				for (final Constructor<?> constructor : implementationClass.getConstructors()) {
					if (constructor.getParameterTypes()[0].getComponentType().isAssignableFrom(elementClass)) {
						try {
							return (ArraySetter<?>) constructor.newInstance(Array.newInstance(elementClass, elementCount));
						} catch (final Exception exception) {
							ignore(exception);
						}
					}
				}
			}
			
			return null;
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class BooleanArraySetter extends ArraySetter<boolean[]> {
			
			public BooleanArraySetter(final boolean[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = value != 0;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class ByteArraySetter extends ArraySetter<byte[]> {
			
			public ByteArraySetter(final byte[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = (byte) value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class ShortArraySetter extends ArraySetter<short[]> {
			
			public ShortArraySetter(final short[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = (short) value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class IntArraySetter extends ArraySetter<int[]> {
			
			public IntArraySetter(final int[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class LongArraySetter extends ArraySetter<long[]> {
			
			public LongArraySetter(final long[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class FloatArraySetter extends ArraySetter<float[]> {
			
			public FloatArraySetter(final float[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class DoubleArraySetter extends ArraySetter<double[]> {
			
			public DoubleArraySetter(final double[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				this.getArray()[index] = (short) value;
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-01-22)
		 */
		public static final class ObjectArraySetter extends ArraySetter<Object[]> {
			
			public ObjectArraySetter(final Object[] array) {
				super(array);
			}
			
			@Override
			public final void set(final int index, final int value) {
				Array.setInt(this.getArray(), index, value);
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static final class IndexIterator implements Iterable<IndexIterator>, Iterator<IndexIterator> {
		
		private final Index[] indices;
		
		private Boolean hasNext;
		
		public IndexIterator(final Index... littleEndianIndices) {
			this.indices = littleEndianIndices;
			
			this.iterator();
		}
		
		public final int getIndex() {
			int result = 0;
			int stride = 1;
			
			for (final Index index : this.indices) {
				result += stride * index.getIndex();
				stride *= index.getCount();
			}
			
			return result;
		}
		
		@Override
		public final boolean hasNext() {
			if (this.hasNext == null) {
				this.hasNext = false;
				
				for (final Index index : this.indices) {
					index.getNext();
					
					if (index.hasNext()) {
						this.hasNext = true;
						break;
					}
					
					index.reset();
				}
			}
			
			return this.hasNext;
		}
		
		@Override
		public final IndexIterator next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}
			
			this.hasNext = null;
			
			return this;
		}
		
		@Override
		public final void remove() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public final Iterator<IndexIterator> iterator() {
			this.hasNext = true;
			
			for (final Index index : this.indices) {
				this.hasNext &= index.reset().hasNext();
			}
			
			return this;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static abstract class Index {
		
		public abstract Index reset();
		
		public abstract boolean hasNext();
		
		public abstract int getNext();
		
		public abstract int getCount();
		
		public abstract int getValue();
		
		public abstract int getIndex();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static final class ShuffledIndex extends Index {
		
		private final int[] values;
		
		private int i;
		
		public ShuffledIndex(final int... values) {
			this.values = values;
		}
		
		@Override
		public final ShuffledIndex reset() {
			this.i = 0;
			
			return this;
		}
		
		@Override
		public final boolean hasNext() {
			return this.i < this.values.length;
		}
		
		@Override
		public final int getNext() {
			return this.values[this.i++];
		}
		
		@Override
		public final int getCount() {
			return this.values.length;
		}
		
		@Override
		public final int getValue() {
			return this.values[this.i];
		}
		
		@Override
		public final int getIndex() {
			return this.i;
		}
		
		@Override
		public final String toString() {
			return Arrays.toString(this.values);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-22)
	 */
	public static final class SequentialIndex extends Index {
		
		private final int start;
		
		private final int end;
		
		private int value;
		
		public SequentialIndex(final int start, final int end) {
			this.start = start;
			this.end = end;
		}
		
		@Override
		public final SequentialIndex reset() {
			this.value = 0;
			
			return this;
		}
		
		@Override
		public final boolean hasNext() {
			return this.value < this.end;
		}
		
		@Override
		public final int getNext() {
			if (!this.hasNext()) {
				this.value = 0;
			}
			
			return this.value++;
		}
		
		@Override
		public final int getCount() {
			return this.end - this.start;
		}
		
		@Override
		public final int getValue() {
			return this.value;
		}
		
		@Override
		public final int getIndex() {
			return this.value - this.start;
		}
		
		@Override
		public final String toString() {
			return this.start + ":" + (this.end - 1);
		}
		
	}
	
}
