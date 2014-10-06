package imj2.pixel3d;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.debug;

import java.io.Serializable;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-27)
 */
public final class PolygonTools {
	
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
	
	public static final void renderSegment(final Processor process
			, final double x1, final double y1, final double z1
			, final double x2, final double y2, final double z2) {
		final double dx = x2 - x1;
		final double dy = y2 - y1;
		final double dz = z2 - z1;
		final int d = 1 + (int) max(abs(dx), abs(dy));
		
		for (int i = 0; i < d; ++i) {
			process.pixel(x1 + i * dx / d, y1 + i * dy / d, z1 + i * dz / d);
		}
	}
	
	public static final void renderDisc(final Processor process
			, final double x, final double y, final double z, final double size) {
		final double radius = size / 2.0;
		final double radiusSquared = square(radius);
		final int startY = (int) (y - radius);
		final int endY = (int) (startY + size);
		
		for (int yy = startY; yy <= endY; ++yy) {
			// (firstX - x)² + (yy - y)² = (size/2)²
			// <- firstX - x = -sqrt((size/2)² - (yy - y)²) 
			// <- firstX = x - sqrt((size/2)² - (yy - y)²) 
			final double halfSpanX = sqrt(radiusSquared - square(yy - y));
			final int firstX = (int) (x - halfSpanX);
			final int endX = (int) (x + halfSpanX);
			
			for (int xx = firstX; xx <= endX; ++xx) {
				process.pixel(xx, yy, z);
			}
		}
	}
	
	public static final void render(final Processor process, final double... vertices) {
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
			fillPixelsBetweenNodePairs(nodeX, nodeZ, nodeCount, y, clipLeft, clipRight, process);
		}
	}
	
	public static final void fillPixelsBetweenNodePairs(final int[] nodeX,
			final double[] nodeZ, final int nodeCount, final int y,
			final int clipLeft, final int clipRight, final Processor process) {
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
					process.pixel(x, y, z1 + (x - x1) * dz / dx);
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
