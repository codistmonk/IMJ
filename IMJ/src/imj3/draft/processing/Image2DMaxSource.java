package imj3.draft.processing;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static java.lang.Math.max;

import imj3.core.Image2D;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Image2DMaxSource extends Image2DPrototypeSource<Image2DDataSource.Metadata> {
	
	public Image2DMaxSource(final Image2D image, final int patchSize) {
		this(image, patchSize, 1, 1);
	}
	
	public Image2DMaxSource(final Image2D image, final int patchSize, final int patchSparsity, final int stride) {
		super(new Image2DDataSource.Metadata.Default(image, patchSize, patchSparsity, stride));
	}
	
	@Override
	public final int getInputDimension() {
		return 1;
	}
	
	@Override
	protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
		double max = 0.0;
		
		for (final int value : patchValues) {
			max = max(max, max(red8(value), max(green8(value), blue8(value))));
		}
		
		result[0] = max;
	}
	
	private static final long serialVersionUID = -3551891713169505385L;
	
}