package imj3.draft.segmentation2;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import imj3.draft.machinelearning.KMeansClustering;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class Mean extends BufferedImagePrototypeSource {
	
	public Mean(final BufferedImage image, final int patchSize) {
		super(image, patchSize);
	}
	
	public Mean(final BufferedImage image, final int patchSize, final int patchSparsity, final int stride) {
		super(image, patchSize, patchSparsity, stride);
	}
	
	@Override
	public final int getInputDimension() {
		return 3;
	}
	
	@Override
	protected final void convert(final int x, final int y, final int[] patchValues, final double[] result) {
		Arrays.fill(result, 0.0);
		
		for (final int value : patchValues) {
			result[0] += red8(value);
			result[1] += green8(value);
			result[2] += blue8(value);
		}
		
		KMeansClustering.divide(result, patchValues.length);
	}
	
	private static final long serialVersionUID = 3938160512172714562L;
	
}