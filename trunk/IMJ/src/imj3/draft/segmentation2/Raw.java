package imj3.draft.segmentation2;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;

import java.awt.image.BufferedImage;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Raw extends BufferedImagePrototypeSource {
	
	public Raw(final BufferedImage image, final int patchSize) {
		super(image, patchSize);
	}
	
	public Raw(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
	}
	
	@Override
	public final int getInputDimension() {
		return 3 * this.getPatchPixelCount();
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
	
}