package imj3.draft.processing;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static imj3.draft.machinelearning.Datum.Default.datum;

import java.io.Serializable;

import imj3.core.Image2D;
import imj3.draft.machinelearning.Datum;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Image2DLabeledRawSource extends Image2DSource {
	
	private final Image2D labels;
	
	public Image2DLabeledRawSource(final Image2D image, final Image2D labels, final int patchSize, final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
		this.labels = labels;
	}
	
	public final Image2D getLabels() {
		return this.labels;
	}
	
	@Override
	public final int getInputDimension() {
		return this.getImage().getChannels().getChannelCount() * this.getPatchPixelCount();
	}
	
	@Override
	public final int getClassDimension() {
		return 1;
	}
	
	@Override
	protected final Context newContext() {
		return this.new Context();
	}
	
	@Override
	protected final Datum convert(final int x, final int y, final int[] patchValues, final Object context) {
		final Context c = (Context) context;
		
		this.convert(x, y, patchValues, c.getDatum());
		
		c.getLabel()[0] = this.getLabels().getPixelValue(x, y);
		
		return c.getClassification();
	}
	
	private final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
		int i = -1;
		
		for (final int value : patchValues) {
			result[++i] = red8(value);
			result[++i] = green8(value);
			result[++i] = blue8(value);
		}
	}
	
	public static final Image2DLabeledRawSource raw(final Image2D image, final Image2D labels) {
		return raw(image, labels, 1);
	}
	
	public static final Image2DLabeledRawSource raw(final Image2D image, final Image2D labels, final int patchSize) {
		return raw(image, labels, patchSize, 1, 1);
	}
	
	public static final Image2DLabeledRawSource raw(final Image2D image, final Image2D labels, final int patchSize, final int patchSparsity, final int stride) {
		return new Image2DLabeledRawSource(image, labels, patchSize, patchSparsity, stride);
	}
	
	private static final long serialVersionUID = 761861479813211873L;
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public final class Context implements Serializable {
		
		private final double[] datum;
		
		private final double[] label;
		
		private final Datum classification;
		
		public Context() {
			this.datum = new double[Image2DLabeledRawSource.this.getInputDimension()];
			this.label = new double[Image2DLabeledRawSource.this.getClassDimension()];
			this.classification = datum(this.datum).setPrototype(datum(this.label));
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