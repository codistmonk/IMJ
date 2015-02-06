package imj3.draft.segmentation2;

import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class BufferedImagePrototypeSource extends BufferedImageDataSource<Prototype> {
	
	public BufferedImagePrototypeSource(final BufferedImage image, final int patchSize) {
		super(image, patchSize);
	}
	
	public BufferedImagePrototypeSource(final BufferedImage image, final int patchSize,
			final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
	}
	
	@Override
	public final int getClassDimension() {
		return this.getInputDimension();
	}
	
	@Override
	protected final Context newContext() {
		return this.new Context();
	}
	
	@Override
	protected final Classification<Prototype> convert(final int x, final int y, final int[] patchValues, final Object context) {
		final Context c = (Context) context;
		
		this.convert(x, y, patchValues, c.getDatum());
		
		return c.getClassification();
	}
	
	protected abstract void convert(int x, int y, int[] patchValues, double[] result);
	
	private static final long serialVersionUID = 1904026018619304970L;
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public final class Context implements Serializable {
		
		private final double[] datum;
		
		private final Classification<Prototype> classification;
		
		public Context() {
			this.datum = new double[BufferedImagePrototypeSource.this.getInputDimension()];
			this.classification = new Classification<>(this.datum, new Prototype(this.datum), 0.0);
		}
		
		public final double[] getDatum() {
			return this.datum;
		}
		
		public final Classification<Prototype> getClassification() {
			return this.classification;
		}
		
		private static final long serialVersionUID = -7200337284453859382L;
		
	}
	
}