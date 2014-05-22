package pixel3d;

import static java.lang.Math.min;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.Serializable;

import jgencode.primitivelists.IntList;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-04-27)
 */
public final class OrthographicRenderer implements Renderer {
	
	private final IntComparator comparator;
	
	private BufferedImage canvas;
	
	private int[] indices;
	
	private int[] pixels;
	
	private float[] zValues;
	
	private int[] colors;
	
	private int pixelCount;
	
	public OrthographicRenderer() {
		this.comparator = new IntComparator() {
			
			@Override
			public final int compare(final int index1, final int index2) {
				final float z1 = OrthographicRenderer.this.getZValue(index1);
				final float z2 = OrthographicRenderer.this.getZValue(index2);
				
				return Float.compare(z1, z2);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 1379706747335956894L;
			
		};
		this.indices = new int[1];
		this.pixels = new int[1];
		this.zValues = new float[1];
		this.colors = new int[1];
	}
	
	@Override
	public final OrthographicRenderer setCanvas(final BufferedImage canvas) {
		if (canvas.getType() != BufferedImage.TYPE_INT_ARGB && canvas.getType() != BufferedImage.TYPE_INT_RGB) {
			throw new IllegalArgumentException();
		}
		
		if (this.canvas != null) {
			final int oldW = this.canvas.getWidth();
			final int oldH = this.canvas.getHeight();
			final int newW = canvas.getWidth();
			final int newH = canvas.getHeight();
			
			if (oldW != newW || newH < oldH) {
				this.clear();
			}
		}
		
		this.canvas = canvas;
		
		return this;
	}
	
	public final OrthographicRenderer reserve(final int n) {
		if (this.pixels.length < n) {
			this.indices = copyOf(this.indices, n);
			this.pixels = copyOf(this.pixels, n);
			this.zValues = copyOf(this.zValues, n);
			this.colors = copyOf(this.colors, n);
		}
		
		return this;
	}
	
	public final void beforeAdd() {
		if (this.pixels.length <= this.pixelCount) {
			this.reserve((int) min(Integer.MAX_VALUE, 2L * (this.pixelCount + 1L)));
		}
	}
	
	@Override
	public final void clear() {
		this.pixelCount = 0;
	}
	
	@Override
	public final OrthographicRenderer addPixel(final double x, final double y, final double z, final int argb) {
		final int w = this.canvas.getWidth();
		final int h = this.canvas.getHeight();
		
		if (x < 0.0 || w <= x || y < 0.0 || h <= y) {
			return this;
		}
		
		this.beforeAdd();
		
		final int pixel = (h - 1 - (int) y) * w + (int) x;
		
		this.indices[this.pixelCount] = this.pixelCount;
		this.pixels[this.pixelCount] = pixel;
		this.zValues[this.pixelCount] = (float) z;
		this.colors[this.pixelCount] = argb;
		
		++this.pixelCount;
		
		return this;
	}
	
	@Override
	public final void render() {
		final boolean debug = false;
		final int n = this.pixelCount;
		
		quickSort(this.indices, 0, n, this.comparator);
		
		if (debug) {
			final int w = this.canvas.getWidth();
			double previousZ = Double.NEGATIVE_INFINITY;
			
			for (int i = 0; i < n; ++i) {
				final int index = this.indices[i];
				final int pixel = this.pixels[index];
				final int x = pixel % w;
				final int y = pixel / w;
				final double z = this.zValues[index];
				
				if (z < previousZ) {
					System.err.println(debug(DEBUG_STACK_OFFSET, previousZ, z));
				}
				
				this.canvas.setRGB(x, y, (this.canvas.getRGB(x, y) & 0xFF00FFFF) | this.colors[index]);
				previousZ = z;
			}
		} else {
			final DataBuffer dataBuffer = this.canvas.getRaster().getDataBuffer();
			
			for (int i = 0; i < n; ++i) {
				final int index = this.indices[i];
				final int pixel = this.pixels[index];
				final int previousRGB = dataBuffer.getElem(pixel);
				final int previousRed = previousRGB & R;
				final int previousGreen = previousRGB & G;
				final int previousBlue = previousRGB & B;
				final int rgb = this.colors[index];
				final int alpha = (rgb >> 24) & 0xFF;
				final int beta = 255 - alpha;
				final int red = (((rgb & R) * alpha + previousRed * beta) / 255) & R;
				final int green = (((rgb & G) * alpha + previousGreen * beta) / 255) & G;
				final int blue = (((rgb & B) * alpha + previousBlue * beta) / 255) & B;
				
				dataBuffer.setElem(pixel, 0xFF000000 | red | green | blue);
			}
		}
	}
	
	final int getPixel(final int index) {
		return this.pixels[index];
	}
	
	final float getZValue(final int index) {
		return this.zValues[index];
	}
	
	final int getColor(final int index) {
		return this.colors[index];
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -6618739135450612928L;
	
	/**
	 * {@value}.
	 */
	public static final int R = 0x00FF0000;
	
	/**
	 * {@value}.
	 */
	public static final int G = 0x0000FF00;
	
	/**
	 * {@value}.
	 */
	public static final int B = 0x000000FF;
	
	public static final DefaultFactory<OrthographicRenderer> FACTORY = DefaultFactory.forClass(OrthographicRenderer.class);
	
	public static final void quickSort(final int[] values, final IntComparator comparator) {
		quickSort(values, 0, values.length, comparator);
	}
	
	public static final void quickSort(final int[] values, final int start0, final int end0, final IntComparator comparator, final IntList queue) {
		queue.add(start0);
		queue.add(end0);
		
		while (!queue.isEmpty()) {
			final int start = queue.remove(0);
			final int end = queue.remove(0);
			final int size = end - start;
			
			if (size < 2) {
				continue;
			}
			
			final int pivot = values[start + size / 2];
			int middle = start;
			
			for (int i = start; i < end; ++i) {
				if (comparator.compare(values[i], pivot) < 0) {
					swap(values, middle++, i);
				}
			}
			
			if (start < middle) {
				queue.add(start);
				queue.add(middle);
				queue.add(middle + 1);
				queue.add(end);
			}
		}
	}
	
	public static final void quickSort(final int[] values, final int start, final int end, final IntComparator comparator) {
		final int size = end - start;
		
		if (size < 2) {
			return;
		}
		
		final int pivot = values[start + size / 2];
		int middle = start;
		
		for (int i = start; i < end; ++i) {
			if (comparator.compare(values[i], pivot) < 0) {
				swap(values, middle++, i);
			}
		}
		
		if (middle <= start) {
			return;
		}
		
		quickSort(values, start, middle, comparator);
		quickSort(values, middle + 1, end, comparator);
	}
	
	public static final void swap(final int[] values, final int i, final int j) {
		final int value = values[i];
		values[i] = values[j];
		values[j] = value;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-29)
	 */
	public static abstract interface IntComparator extends Serializable {
		
		public abstract int compare(int value1, int value2);
		
	}
	
}
