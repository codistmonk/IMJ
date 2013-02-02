package imj;

import static imj.IMJTools.image;
import static imj.MorphologicalOperations.edges8;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageOfBufferedImage.Feature;

import java.util.Date;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-29)
 */
public final class LinearFilterTest {
	
	@Test
	public final void test1() {
		final TicToc timer = new TicToc();
//		final String imageId = "test/imj/12003.jpg";
		final String imageId = "../Libraries/images/16088-4.png";
		
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = image(imageId, Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Extracting edges:", new Date(timer.tic()));
		final Image edges = new LinearFilter(image, new double[] {
				-1.0, -1.0, -1.0,
				-1.0, +1.0, +1.0,
				+1.0, -1.0, -1.0,
				+1.0, +1.0, +1.0,
		}).getResult();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		ImageComponent.showAdjusted(imageId, image, edges);
	}
	
}

/**
 * @author codistmonk (creation 2013-01-29)
 */
final class LinearFilter extends Labeling {
	
	public LinearFilter(final Image image, final double[] kernel) {
		super(image);
		final Kernel k = this.new Kernel(kernel);
		final int pixelCount = this.getPixelCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			this.getResult().setValue(pixel, (int) k.getValue(pixel));
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-01-29)
	 */
	public final class Kernel {
		
		private final Neighborhood neighborhood;
		
		private final double[] kernel;
		
		public Kernel(final double[] kernel) {
			this.kernel = kernel;
			final int n = kernel.length;
			final int[] deltas = new int[2 * n / 3];
			
			for (int i = 0, j = 0; i < n; i += 3, j += 2) {
				deltas[j + 0] = (int) kernel[i + 0];
				deltas[j + 1] = (int) kernel[i + 1];
			}
			
			this.neighborhood = LinearFilter.this.new Neighborhood(deltas);
		}
		
		public final double getValue(final int pixel) {
			this.neighborhood.reset(pixel);
			double result = 0.0;
			
			while (this.neighborhood.hasNext()) {
				final int i = this.neighborhood.getNextDeltaIndex();
				final int neighbor = this.neighborhood.get(i);
				final int neighborValue = LinearFilter.this.getImage().getValue(neighbor);
				final double k = this.kernel[i * 3 + 2];
				result += k * neighborValue;
			}
			
			return result;
		}
		
	}
	
}
