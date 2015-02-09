package imj3.draft.processing;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;

import imj3.core.Image2D;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Image2DRawSource extends Image2DPrototypeSource<Image2DDataSource.Metadata> {
	
	public Image2DRawSource(final Image2D image, final int patchSize, final int patchSparsity, final int stride) {
		super(new Image2DDataSource.Metadata.Default(image, patchSize, patchSparsity, stride));
	}
	
	@Override
	public final int getInputDimension() {
		return 3 * this.getMetadata().getPatchPixelCount();
	}
	
	@Override
	protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
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
	
}