package imj3.draft.processing;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static imj3.draft.machinelearning.Datum.Default.datum;
import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;

import imj3.core.Image2D;
import imj3.draft.machinelearning.Datum;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Image2DRawSource extends Image2DSource {
	
	public Image2DRawSource(final Image2D image, final int patchSize, final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
	}
	
	@Override
	public final int getInputDimension() {
		return this.getImage().getChannels().getChannelCount() * this.getPatchPixelCount();
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
	protected final Datum convert(final int x, final int y, final int[] patchValues, final Object context) {
		final Context c = (Context) context;
		
		this.convert(x, y, patchValues, c.getDatum());
		
		return c.getClassification();
	}
	
	private final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
		ignore(x);
		ignore(y);
		
		int i = -1;
		
		for (final int value : patchValues) {
			result[++i] = red8(value);
			result[++i] = green8(value);
			result[++i] = blue8(value);
		}
	}
	
	private static final long serialVersionUID = 3938160512172714562L;
	
	public static final Image2DRawSource raw(final Image2D image) {
		return raw(image, 1);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final int patchSize) {
		return raw(image, patchSize, 1, 1);
	}
	
	public static final Image2DRawSource raw(final Image2D image, final int patchSize, final int patchSparsity, final int stride) {
		return new Image2DRawSource(image, patchSize, patchSparsity, stride);
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public final class Context implements Serializable {
		
		private final double[] datum;
		
		private final Datum classification;
		
		public Context() {
			this.datum = new double[Image2DRawSource.this.getInputDimension()];
			this.classification = datum(this.datum);
		}
		
		public final double[] getDatum() {
			return this.datum;
		}
		
		public final Datum getClassification() {
			return this.classification;
		}
		
		private static final long serialVersionUID = -7200337284453859382L;
		
	}
	
}