package imj3.draft.segmentation;

import static multij.tools.Tools.baseName;
import static multij.tools.Tools.unchecked;

import imj3.draft.segmentation.ClassifierPrototype.Factory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class Classifier extends ClassifierNode {
	
	private String filePath = DEFAULT_FILE_PATH;
	
	private int scale = DEFAULT_SCALE;
	
	private int maximumScale = DEFAULT_MAXIMUM_SCALE;
	
	private ClassifierPrototype.Factory prototypeFactory = ClassifierRawPrototype.FACTORY;
	
	private final Map<Thread, int[]> buffers = new WeakHashMap<>();
	
	@Override
	public final Classifier copy() {
		final Classifier result = new Classifier();
		
		result.scale = this.scale;
		result.maximumScale = this.maximumScale; 
		
		return this.copyChildrenTo(result);
	}
	
	public final Classifier set(final Classifier that) {
		this.scale = that.scale;
		this.maximumScale = that.maximumScale;
		final int n = that.getChildCount();
		
		this.removeAllChildren();
		
		for (int i = 0; i < n; ++i) {
			this.add(((ClassifierNode) that.getChildAt(i)).copy());
		}
		
		return (Classifier) this.accept(new Visitor<ClassifierNode>() {
			
			@Override
			public final ClassifierNode visit(final Classifier quantizer) {
				return quantizer.visitChildren(this).setUserObject();
			}
			
			@Override
			public final ClassifierNode visit(final ClassifierCluster cluster) {
				return cluster.visitChildren(this).setUserObject();
			}
			
			@Override
			public final ClassifierNode visit(final ClassifierPrototype prototype) {
				return prototype.visitChildren(this).setUserObject();
			}
			
			private static final long serialVersionUID = 6586780367368082696L;
			
		});
	}
	
	@Override
	public final Classifier setUserObject() {
		this.setUserObject(this.new UserObject() {
			
			@Override
			public final String toString() {
				return Classifier.this.getName() + " (" + "scale: " + Classifier.this.getScaleAsString() + ")";
			}
			
			private static final long serialVersionUID = 948766593376210016L;
			
		});
		
		return this;
	}
	
	public final String getName() {
		return baseName(new File(Classifier.this.getFilePath()).getName());
	}
	
	public final String getFilePath() {
		return this.filePath;
	}
	
	public final Classifier setFilePath(final String filePath) {
		this.filePath = filePath;
		
		return this;
	}
	
	public final int getScale() {
		return this.scale;
	}
	
	public final Classifier setScale(final int scale) {
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
	
	public final Classifier setScale(final String scaleAsString) {
		return this.setScale(Integer.parseInt(scaleAsString));
	}
	
	public final int getMaximumScale() {
		return this.maximumScale;
	}
	
	public final Classifier setMaximumScale(final int maximumScale) {
		if (maximumScale <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.maximumScale = maximumScale;
		
		return this;
	}
	
	public final String getMaximumScaleAsString() {
		return Integer.toString(this.getMaximumScale());
	}
	
	public final Classifier setMaximumScale(final String maximumScaleAsString) {
		this.setMaximumScale(Integer.parseInt(maximumScaleAsString));
		
		return this;
	}
	
	public final ClassifierPrototype.Factory getPrototypeFactory() {
		return this.prototypeFactory;
	}
	
	public final Classifier setPrototypeFactory(final ClassifierPrototype.Factory prototypeFactory) {
		this.prototypeFactory = prototypeFactory;
		
		return this;
	}
	
	public final String getPrototypeFactoryAsString() {
		return this.getPrototypeFactory().getClass().getName();
	}
	
	public final Classifier setPrototypeFactory(final String prototypeFactoryAsString) {
		try {
			return this.setPrototypeFactory((Factory) Class.forName(prototypeFactoryAsString).newInstance());
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final ClassifierCluster quantize(final BufferedImage image, final int x, final int y) {
		int[] buffer = this.buffers.get(Thread.currentThread());
		
		{
			final int[] newBuffer = this.getPrototypeFactory().allocateDataBuffer(this.getScale(), buffer);
			
			if (newBuffer != buffer) {
				this.buffers.put(Thread.currentThread(), buffer = newBuffer);
			}
		}
		
		this.getPrototypeFactory().extractData(image, x, y, this.getScale(), buffer);
		
		final int n = this.getChildCount();
		ClassifierCluster result = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < n; ++i) {
			final ClassifierCluster cluster = (ClassifierCluster) this.getChildAt(i);
			final double distance = cluster.distanceTo(buffer, bestDistance);
			
			if (distance < bestDistance) {
				result = cluster;
				bestDistance = distance;
			}
		}
		
		return result;
	}
	
	public final ClassifierCluster findCluster(final String name) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final ClassifierCluster cluster = (ClassifierCluster) this.getChildAt(i);
			
			if (name.equals(cluster.getName())) {
				return cluster;
			}
		}
		
		return null;
	}
	
	public final ClassifierCluster findCluster(final int label) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final ClassifierCluster cluster = (ClassifierCluster) this.getChildAt(i);
			
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
	
	public static final String DEFAULT_FILE_PATH = "classifier.xml";
	
	public static final int DEFAULT_SCALE = 1;
	
	public static final int DEFAULT_MAXIMUM_SCALE = 1;
	
}
