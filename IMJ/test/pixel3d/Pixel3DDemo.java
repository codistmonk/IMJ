package pixel3d;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.lang.Math.signum;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;

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
				
				PolygonTools.render(new PolygonTools.Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
//						renderer.addPixel(x, y, z, 0xFF800000 | ((int) (z * 255.0)) * 0x00000100);
						renderer.addPixel(x, y, z, 0x80FF0000);
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 2591204837732124053L;
					
				},
				10.0, 0.0, 0.0,
				80.0, 90.0, 1.0,
				80.0, 20.0, 1.0);
				
				PolygonTools.render(new PolygonTools.Processor() {
					
					@Override
					public final void pixel(final double x, final double y, final double z) {
//						renderer.addPixel(x, y, z, 0xFFFF0000 | ((int) (z * 255.0)) * 0x00000001);
						renderer.addPixel(x, y, z, 0x8000FF00);
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 2591204837732124053L;
					
				},
				10.0, 10.0, 1.0,
				90.0, 90.0, 0.0,
				90.0, 10.0, 0.0);
				
				renderer.render();
				
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
				for (int i = 0; i < 200; ++i) {
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
				return Float.compare(OrthographicRenderer.this.getZValue(index1), OrthographicRenderer.this.getZValue(index2));
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
		final List<double[]> points = new ArrayList<double[]>();
		
		{
			final int n = vertices.length;
			
			for (int offset = 0; offset < n; offset += 3) {
				final double p1X = vertices[offset + X];
				final double p1Y = vertices[offset + Y];
				final double p1Z = vertices[offset + Z];
				final double p2X = vertices[(offset + 3) % n + X];
				final double p2Y = vertices[(offset + 3) % n + Y];
				final double p2Z = vertices[(offset + 3) % n + Z];
				final double dy = p2Y - p1Y;
				final double previousDySignum = signum(vertices[(n + offset - 3) % n + Y] - p1Y);
				
				if (dy == 0 && 0 < orientation(vertices, offset)) {
					points.add(v(p1X, p1Y, p1Z));
				} else if (dy != 0) {
					if (signum(dy) == previousDySignum) {
						points.add(v(p1X, p1Y, p1Z));
					}
					
					final double dx = p2X - p1X;
					final double dz = p2Z - p1Z;
					final double m = abs(dy);
					
					for (int j = 0; j < m; ++j) {
						points.add(v(p1X + dx * j / m, p1Y + dy * j / m, p1Z + dz * j / m));
					}
				}
			}
		}
		
		Collections.sort(points, new Comparator<double[]>() {
			
			@Override
			public final int compare(final double[] p1, final double[] p2) {
				int result = Double.compare(p1[Y], p2[Y]);
				
				if (result == 0) {
					result = Double.compare(p1[X], p2[X]);
				}
				
				return result;
			}
			
		});
		
		{
			final int n = points.size();
			
			if ((n & 1) != 0) {
				System.err.println(debug(DEBUG_STACK_OFFSET, "Internal error detected"));
			}
			
			for (int i = 0; i + 1 < n; i += 2) {
				final double[] p1 = points.get(i);
				final double[] p2 = points.get(i + 1);
				final double y = p1[Y];
				
				if ((int) y == (int) p2[Y]) {
					final double x1 = p1[X];
					final double x2 = p2[X];
					final double dx = x2 - x1;
					final double z1 = p1[Z];
					final double dz = p2[Z] - z1;
					
					for (double x = x1; x < x2; ++x) {
						final double z = z1 + (x - x1) * dz / dx;
						
						processor.pixel(x, y, z);
					}
				} else {
					System.err.println(debug(DEBUG_STACK_OFFSET, "Internal error detected"));
					break;
				}
			}
		}
	}
	
	public static final double orientation(final double[] vertices, final int offset) {
		final int n = vertices.length;
		final double v1X = vertices[(n + offset - 3 + 0) % n];
		final double v1Y = vertices[(n + offset - 3 + 1) % n];
		final double v2X = vertices[(n + offset + 0 + 0) % n];
		final double v2Y = vertices[(n + offset + 0 + 1) % n];
		final double v3X = vertices[(n + offset + 3 + 0) % n];
		final double v3Y = vertices[(n + offset + 3 + 1) % n];
		final double v21X = v1X - v2X;
		final double v21Y = v1Y - v2Y;
		final double v23X = v3X - v2X;
		final double v23Y = v3Y - v2Y;
		
		return v21X * v23Y - v21Y * v23X;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-27)
	 */
	public static abstract interface Processor extends Serializable {
		
		public abstract void pixel(double x, double y, double z);
		
	}
	
}
