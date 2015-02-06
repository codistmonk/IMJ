package imj3.draft.segmentation2;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static java.lang.Math.max;

import java.awt.image.BufferedImage;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Max extends BufferedImagePrototypeSource {
	
	public Max(final BufferedImage image, final int patchSize) {
		super(image, patchSize);
	}
	
	public Max(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
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