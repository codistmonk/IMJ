package pixel3d;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-04-27)
 */
public final class Pixel3DDemo {
	
	private Pixel3DDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final Statistics time = new Statistics();
		final SimpleImageView imageView = new SimpleImageView();
		
		imageView.setImage(new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR));
		
		SwingTools.show(imageView, Pixel3DDemo.class.getName(), false);
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			private OrthographicRenderer renderer = new OrthographicRenderer(imageView.getBufferImage());
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final TicToc timer = new TicToc();
				
				timer.tic();
				
				this.renderer.setCanvas(imageView.getBufferImage());
				this.renderer.clear();
				
				PolygonTools.render(new ARGBShader(this.renderer, 0xA0FF0000),
						10.0, 50.0, 0.0,
						80.0, 90.0, 1.0,
						80.0, 20.0, 1.0);
				
				PolygonTools.render(new ARGBShader(this.renderer, 0x8000FF00),
						10.0, 10.0, 1.0,
						90.0, 90.0, 0.0,
						10.0, 90.0, 1.0);
				
				PolygonTools.render(new ARGBShader(this.renderer, 0x8000FF00),
						10.0, 10.0, 1.0,
						90.0, 90.0, 0.0,
						90.0, 10.0, 0.0);
				
				this.renderer.render();
				
//				imageView.getBufferImage().setRGB(47, 162, Color.YELLOW.getRGB());
				
				time.addValue(timer.toc());
				
				debugPrint("frameTime:", round(time.getMean()), "frameRate:", round(1000.0 / time.getMean()));
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3358934187100520107L;
			
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				for (int i = 0; i < 1; ++i) {
					if (i % 21 == 0) {
						time.reset();
					}
					
					imageView.refreshBuffer();
				}
			}
			
		});
	}
	
}

/**
 * @author codistmonk (creation 2014-04-27)
 */
final class OrthographicRenderer implements Serializable {
	
	private final BufferedImage canvas;
	
//	private int[] indices;
	private Integer[] indices;
	
	private int[] pixels;
	
	private float[] zValues;
	
	private int[] colors;
	
	private int pixelCount;
	
	public OrthographicRenderer(final BufferedImage canvas) {
		this.canvas = canvas;
//		this.indices = new int[1];
		this.indices = new Integer[1];
		this.pixels = new int[1];
		this.zValues = new float[1];
		this.colors = new int[1];
	}
	
	public final OrthographicRenderer setCanvas(final BufferedImage canvas) {
		final int oldW = this.canvas.getWidth();
		final int oldH = this.canvas.getHeight();
		final int newW = canvas.getWidth();
		final int newH = canvas.getHeight();
		
		if (oldW != newW || newH < oldH) {
			this.clear();
		}
		
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
	
	public final void clear() {
		this.pixelCount = 0;
	}
	
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
	
	public final void render() {
		final boolean debug = false;
		final int n = this.pixelCount;
		
		Arrays.sort(this.indices, 0, n, new Comparator<Integer>() {
			
			@Override
			public final int compare(final Integer index1, final Integer index2) {
				final float z1 = OrthographicRenderer.this.getZValue(index1);
				final float z2 = OrthographicRenderer.this.getZValue(index2);
				
				return Float.compare(z1, z2);
			}
			
		});
		
		final int w = this.canvas.getWidth();
		
		if (debug) {
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
			for (int i = 0; i < n; ++i) {
				final int index = this.indices[i];
				final int pixel = this.pixels[index];
				final int x = pixel % w;
				final int y = pixel / w;
				final int previousRGB = this.canvas.getRGB(x, y);
				final int previousRed = previousRGB & R;
				final int previousGreen = previousRGB & G;
				final int previousBlue = previousRGB & B;
				final int rgb = this.colors[index];
				final int alpha = (rgb >> 24) & 0xFF;
				final int beta = 255 - alpha;
				final int red = (((rgb & R) * alpha + previousRed * beta) / 255) & R;
				final int green = (((rgb & G) * alpha + previousGreen * beta) / 255) & G;
				final int blue = (((rgb & B) * alpha + previousBlue * beta) / 255) & B;
				
				this.canvas.setRGB(x, y, 0xFF000000 | red | green | blue);
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
	
}

/**
 * @author codistmonk (creation 2014-04-27)
 */
final class PolygonTools {
	
	private PolygonTools() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final int X = 0;
	
	/**
	 * {@value}.
	 */
	public static final int Y = 1;
	
	/**
	 * {@value}.
	 */
	public static final int Z = 2;
	
	public static final double[] v(final double... result) {
		return result;
	}
	
	public static final void render(final Processor processor, final double... vertices) {
		//  Adapted from http://alienryderflex.com/polygon_fill/ (public-domain code by Darel Rex Finley, 2007)
		
		final int clipLeft = 0;
		final int clipRight = Integer.MAX_VALUE;
		final int clipBottom = Integer.MAX_VALUE;
		final int clipTop = 0;
		
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		
		for (int i = 0; i < vertices.length; i += 3) {
			final int y = (int) vertices[i + Y];
			top = max(clipTop, min(top, y));
			bottom = min(clipBottom, max(bottom, y));
		}
		
		final int maximumCorners = vertices.length / 3;
		final int[] nodeX = new int[maximumCorners];
		final double[] nodeZ = new double[maximumCorners];
		
		// Loop through the rows.
		for (int y = top; y < bottom; y++) {
			final int nodeCount = buildNodeList(nodeX, nodeZ, y, vertices);
			
			sortNodes(nodeX, nodeZ, nodeCount);
			fillPixelsBetweenNodePairs(nodeX, nodeZ, nodeCount, y, clipLeft, clipRight, processor);
		}
	}
	
	public static final void fillPixelsBetweenNodePairs(final int[] nodeX,
			final double[] nodeZ, final int nodeCount, final int y,
			final int clipLeft, final int clipRight, final Processor processor) {
		// Fill the pixels between node pairs.
		for (int i = 0; i < nodeCount; i += 2) {
			int x1 = nodeX[i];
			
			if (clipRight <= x1) {
				break;
			}
			
			int x2 = nodeX[i + 1];
			
			if (clipLeft < x2) {
				if (x1 < clipLeft) {
					x1 = clipLeft;
				}
				
				if (clipRight < x2) {
					x2 = clipRight;
				}
				
				final int dx = x2 - x1;
				final double z1 = nodeZ[i];
				final double z2 = nodeZ[i + 1];
				final double dz = z2 - z1;
				
				for (int x = x1; x < x2; x++) {
					processor.pixel(x, y, z1 + (x - x1) * dz / dx);
				}
			}
		}
	}
	
	public static final int buildNodeList(final int[] nodeX, final double[] nodeZ,
			final int y, final double[] vertices) {
		int result = 0;
		
		for (int i = 0, j = vertices.length - 3; i < vertices.length; j = i, i += 3) {
			final int y1 = (int) vertices[i + Y];
			final int y2 = (int) vertices[j + Y];
			final int dy = y2 - y1;
			
			if (y1 < y && y <= y2 || y2 < y && y <= y1) {
				final int x1 = (int) vertices[i + X];
				final int x2 = (int) vertices[j + X];
				final int dx = x2 - x1;
				nodeX[result] = (int) (x1 + (double) (y - y1) * dx / dy);
				
				final double z1 = vertices[i + Z];
				final double z2 = vertices[j + Z];
				final double dz = z2 - z1;
				nodeZ[result] = z1 + (y - y1) * dz / dy;
				
				++result;
			}
		}
		
		if ((result & 1) != 0) {
			System.err.println(debug(DEBUG_STACK_OFFSET, "Internal error detected"));
		}
		
		return result;
	}
	
	public static final void sortNodes(final int[] nodeX, final double[] nodeZ, final int nodeCount) {
		// Sort the nodes, via a simple “Bubble” sort.
		for (int i = 0; i < nodeCount - 1;) {
			if (nodeX[i] > nodeX[i + 1]) {
				swap(nodeX, i, i + 1);
				swap(nodeZ, i, i + 1);
				
				if (i != 0) {
					--i;
				}
			} else {
				++i;
			}
		}
	}
	
	public static final void swap(final int[] array, final int i, final int j) {
		final int tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}
	
	public static final void swap(final double[] array, final int i, final int j) {
		final double tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-27)
	 */
	public static abstract interface Processor extends Serializable {
		
		public abstract void pixel(double x, double y, double z);
		
	}
	
}

/**
 * @author codistmonk (creation 2014-04-28)
 */
final class ARGBShader implements PolygonTools.Processor {
	
	private final OrthographicRenderer renderer;
	
	private final int argb;
	
	public ARGBShader(final OrthographicRenderer renderer, final int argb) {
		this.argb = argb;
		this.renderer = renderer;
	}
	
	@Override
	public final void pixel(final double x, final double y, final double z) {
		this.renderer.addPixel(x, y, z, this.argb);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 2591204837732124053L;
	
}
