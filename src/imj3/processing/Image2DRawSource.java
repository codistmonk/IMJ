package imj3.processing;

import static imj3.machinelearning.Datum.Default.datum;

import java.io.Serializable;

import imj3.core.Image2D;
import imj3.machinelearning.Datum;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Image2DRawSource extends Image2DSource {
	
	private final Image2D labels;
	
	private final int classDimension;
	
	public Image2DRawSource(final Image2D image, final Image2D labels, final int patchSize, final int patchSparsity, final int stride, final boolean addXY) {
		super(image, patchSize, patchSparsity, stride, addXY);
		this.labels = labels;
		this.classDimension = labels == null ? this.getInputDimension() : 1;
	}
	
	public final Image2D getLabels() {
		return this.labels;
	}
	
	@Override
	public final int getClassDimension() {
		return this.classDimension;
	}
	
	@Override
	protected final Context newContext() {
		return new Context(this.getInputDimension(), this.getClassDimension(), this.getLabels() != null);
	}
	
	@Override
	protected final Datum convert(final int x, final int y, final double[] patchValues, final Object context) {
		final Context c = (Context) context;
		final double[] input = c.getDatum();
		int n = patchValues.length;
		
		System.arraycopy(patchValues, 0, input, 0, n);
		
		if (this.isAddingXY()) {
			input[n + 0] = x;
			input[n + 1] = y;
			n += 2;
		}
		
		if (this.getLabels() != null) {
			c.getLabel()[0] = this.getLabels().getPixelValue(x, y);
		}
		
		return c.getClassification();
	}
	
	public static final Image2DRawSource raw(final Image2D image) {
		return raw(image, null);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final Image2D labels) {
		return raw(image, labels, 1);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final int patchSize) {
		return raw(image, null, patchSize);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final Image2D labels, final int patchSize) {
		return raw(image, labels, patchSize, 1, 1);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final int patchSize, final int patchSparsity, final int stride) {
		return raw(image, null, patchSize, patchSparsity, stride);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final Image2D labels, final int patchSize, final int patchSparsity, final int stride) {
		return new Image2DRawSource(image, labels, patchSize, patchSparsity, stride, false);
	}
	
	public static final Image2DRawSource rawXY(final Image2D image, final Image2D labels, final int patchSize, final int patchSparsity, final int stride) {
		return new Image2DRawSource(image, labels, patchSize, patchSparsity, stride, true);
	}
	
	private static final long serialVersionUID = 761861479813211873L;
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Context implements Serializable {
		
		private final double[] datum;
		
		private final double[] label;
		
		private final Datum classification;
		
		public Context(final int inputDimension, final int classDimension, final boolean separateLabel) {
			this.datum = new double[inputDimension];
			
			if (separateLabel) {
				this.label = new double[classDimension];
				this.classification = datum(this.datum).setPrototype(datum(this.label));
			} else {
				this.label = this.datum;
				this.classification = datum(this.datum);
			}
		}
		
		public final double[] getDatum() {
			return this.datum;
		}
		
		public final double[] getLabel() {
			return this.label;
		}
		
		public final Datum getClassification() {
			return this.classification;
		}
		
		private static final long serialVersionUID = -4151299401539931812L;
		
	}
	
}