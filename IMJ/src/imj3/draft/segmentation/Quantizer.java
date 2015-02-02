package imj3.draft.segmentation;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class Quantizer extends QuantizerNode {
	
	private int scale = DEFAULT_SCALE;
	
	private int maximumScale = DEFAULT_MAXIMUM_SCALE;
	
	private final Map<Thread, int[]> buffers = new WeakHashMap<>();
	
	@Override
	public final Quantizer copy() {
		final Quantizer result = new Quantizer();
		
		result.scale = this.scale;
		result.maximumScale = this.maximumScale; 
		
		return this.copyChildrenTo(result);
	}
	
	public final Quantizer set(final Quantizer that) {
		this.scale = that.scale;
		this.maximumScale = that.maximumScale;
		final int n = that.getChildCount();
		
		this.removeAllChildren();
		
		for (int i = 0; i < n; ++i) {
			this.add(((QuantizerNode) that.getChildAt(i)).copy());
		}
		
		return (Quantizer) this.accept(new Visitor<QuantizerNode>() {
			
			@Override
			public final QuantizerNode visit(final Quantizer quantizer) {
				return quantizer.visitChildren(this).setUserObject();
			}
			
			@Override
			public final QuantizerNode visit(final QuantizerCluster cluster) {
				return cluster.visitChildren(this).setUserObject();
			}
			
			@Override
			public final QuantizerNode visit(final QuantizerPrototype prototype) {
				return prototype.visitChildren(this).setUserObject();
			}
			
			private static final long serialVersionUID = 6586780367368082696L;
			
		});
	}
	
	@Override
	public final Quantizer setUserObject() {
		this.setUserObject(this.new UserObject() {
			
			@Override
			public final String toString() {
				return "scale: " + Quantizer.this.getScaleAsString();
			}
			
			private static final long serialVersionUID = 948766593376210016L;
			
		});
		
		return this;
	}
	
	public final int getScale() {
		return this.scale;
	}
	
	public final Quantizer setScale(final int scale) {
		if (scale <= 0) {
			throw new IllegalArgumentException();
		}
		
		if (scale != this.getScale()) {
			this.scale = scale;
		}
		
		return this;
	}
	
	public final String getScaleAsString() {
		return Integer.toString(this.getScale());
	}
	
	public final Quantizer setScale(final String scaleAsString) {
		return this.setScale(Integer.parseInt(scaleAsString));
	}
	
	public final int getMaximumScale() {
		return this.maximumScale;
	}
	
	public final Quantizer setMaximumScale(final int maximumScale) {
		if (maximumScale <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.maximumScale = maximumScale;
		
		return this;
	}
	
	public final String getMaximumScaleAsString() {
		return Integer.toString(this.getMaximumScale());
	}
	
	public final Quantizer setMaximumScale(final String maximumScaleAsString) {
		this.setMaximumScale(Integer.parseInt(maximumScaleAsString));
		
		return this;
	}
	
	public final QuantizerCluster quantize(final BufferedImage image, final int x, final int y) {
		int[] buffer = this.buffers.get(Thread.currentThread());
		
		{
			final int n = this.getScale() * this.getScale();
			
			if (buffer == null || buffer.length != n) {
				this.buffers.put(Thread.currentThread(), buffer = new int[n]);
			}
		}
		
		extractValues(image, x, y, this.getScale(), buffer);
		final int n = this.getChildCount();
		QuantizerCluster result = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < n; ++i) {
			final QuantizerCluster cluster = (QuantizerCluster) this.getChildAt(i);
			final double distance = cluster.distanceTo(buffer, bestDistance);
			
			if (distance < bestDistance) {
				result = cluster;
				bestDistance = distance;
			}
		}
		
		return result;
	}
	
	public final QuantizerCluster findCluster(final String name) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final QuantizerCluster cluster = (QuantizerCluster) this.getChildAt(i);
			
			if (name.equals(cluster.getName())) {
				return cluster;
			}
		}
		
		return null;
	}
	
	public final QuantizerCluster findCluster(final int label) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final QuantizerCluster cluster = (QuantizerCluster) this.getChildAt(i);
			
			if (label == cluster.getLabel()) {
				return cluster;
			}
		}
		
		return null;
	}
	
	@Override
	public final <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
	
	private static final long serialVersionUID = 3228746395868315788L;
	
	public static final int DEFAULT_SCALE = 1;
	
	public static final int DEFAULT_MAXIMUM_SCALE = 1;
	
	public static final int[] extractValues(final BufferedImage image, final int x, final int y, final int patchSize, final int[] result) {
		Arrays.fill(result, 0);
		
		final int width = image.getWidth();
		final int height = image.getHeight();
		final int s = patchSize / 2;
		final int left = x - s;
		final int right = left + patchSize;
		final int top = y - s;
		final int bottom = top + patchSize;
		
		for (int yy = top, i = 0; yy < bottom; ++yy) {
			if (0 <= yy && yy < height) {
				for (int xx = left; xx < right; ++xx, ++i) {
					if (0 <= xx && xx < width) {
						result[i] = image.getRGB(xx, yy);
					}
				}
			} else {
				i += patchSize;
			}
		}
		
		return result;
	}
	
}