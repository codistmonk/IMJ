package imj3.draft.segmentation2;

import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class Image2DPrototypeSource<M extends Image2DDataSource.Metadata> extends Image2DDataSource<M, Prototype> {
	
	protected Image2DPrototypeSource(final M metadata) {
		super(metadata);
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
		@SuppressWarnings("unchecked")
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
			this.datum = new double[Image2DPrototypeSource.this.getInputDimension()];
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