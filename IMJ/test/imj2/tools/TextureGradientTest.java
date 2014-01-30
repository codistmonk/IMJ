package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.show;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-01-25)
 */
public final class TextureGradientTest {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final BufferedImage buffer = component.getBuffer();
					
					refreshMask(component.getImage());
					
					final BufferedImage mask = getMask();
					
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
								buffer.setRGB(x, y, 0);
							}
						}
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 7778289787781482757L;
				
			};
			
			private int radius;
			
			private float threshold;
			
			private BufferedImage mask;
			
			{
				this.radius = 16;
				this.threshold = 0.2F;
				imageView.getPainters().add(this.painter);
			}
			
			public final int getRadius() {
				return this.radius;
			}
			
			public final float getThreshold() {
				return this.threshold;
			}
			
			public final BufferedImage getMask() {
				return this.mask;
			}
			
			public final void refreshMask(final BufferedImage image) {
				final int width = image.getWidth();
				final int height = image.getHeight();
				
				if (this.getMask() == null ||
						this.getMask().getWidth() != width || this.getMask().getHeight() != height) {
					this.mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
				}
				
				final BufferedImage mask = this.getMask();
				
				{
					final Graphics2D g = mask.createGraphics();
					
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, width, height);
					g.dispose();
				}
				
				final int[] referenceHistogram = new int[BIN_COUNT];
				final int[] currentHistogram = new int[BIN_COUNT];
				final int radius = this.getRadius();
				final float threshold = this.getThreshold();
				
				for (int y = 0; y < height; ++y) {
					computeHistogram(image, 0, 0, radius, referenceHistogram);
					
					for (int x = 0; x < width; ++x) {
						final float distance = computeHistogramDistance(referenceHistogram, computeHistogram(image, x, y, radius, currentHistogram));
						
						if (threshold <= distance) {
							mask.setRGB(x, y, 0);
							System.arraycopy(currentHistogram, 0, referenceHistogram, 0, BIN_COUNT);
						}
					}
				}
				
				for (int x = 0; x < width; ++x) {
					computeHistogram(image, 0, 0, radius, referenceHistogram);
					
					for (int y = 0; y < height; ++y) {
						final float distance = computeHistogramDistance(referenceHistogram, computeHistogram(image, x, y, radius, currentHistogram));
						
						if (threshold <= distance) {
							mask.setRGB(x, y, 0);
							System.arraycopy(currentHistogram, 0, referenceHistogram, 0, BIN_COUNT);
						}
					}
				}
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3481507791896280638L;
			
		};
		
		show(imageView, "Simple Image View", true);
	}
	
	/**
	 * {@value}.
	 */
	public static final int Q = 5;
	
	/**
	 * {@value}.
	 */
	public static final int CHANNEL_BIT_COUNT = 8;
	
	/**
	 * {@value}.
	 */
	public static final int R = CHANNEL_BIT_COUNT - Q;
	
	/**
	 * {@value}.
	 */
	public static final int CHANNEL_COUNT = 3;
	
	/**
	 * {@value}.
	 */
	public static final int CHANNEL_MASK = (1 << CHANNEL_BIT_COUNT) - 1;
	
	/**
	 * {@value}.
	 */
	public static final int BIN_COUNT = 1 << (CHANNEL_COUNT * R);
	
	public static final int getColorIndex(final int pixelValue) {
		int result = 0;
		int v = pixelValue;
		
		for (int channelIndex = 0; channelIndex < CHANNEL_COUNT; ++channelIndex) {
			result |= ((v & CHANNEL_MASK) >> Q) << (channelIndex * R);
			v >>= CHANNEL_BIT_COUNT;
		}
		
		return result;
	}
	
	public static final int[] computeHistogram(final BufferedImage image, final int x, final int y, final int radius, final int[] result) {
		if (result != null) {
			fill(result, 0);
		}
		
		return updateHistogram(image, x, y, radius, result);
	}
	
	public static final int[] updateHistogram(final BufferedImage image, final int x, final int y, final int radius, final int[] result) {
		final int[] actualResult = result != null ? result : new int[BIN_COUNT];
		final int firstY = max(0, y - radius);
		final int lastY = min(image.getHeight() - 1, y + radius);
		final int firstX = max(0, x - radius);
		final int lastX= min(image.getWidth() - 1, x + radius);
		
		for (int yy = firstY; yy <= lastY; ++yy) {
			for (int xx = firstX; xx <= lastX; ++xx) {
				++actualResult[getColorIndex(image.getRGB(xx, yy))];
			}
		}
		
		return actualResult;
	}
	
	public static final float computeHistogramDistance(final int[] referenceHistogram, final int[] targetHistogram) {
		int referenceHistogramSize = 0;
		int targetHistogramSize = 0;
		float protoresult = 0F;
		
		for (int i = 0; i < BIN_COUNT; ++i) {
			final int referenceBinValue = referenceHistogram[i];
			final int targetBinValue = targetHistogram[i];
			protoresult += abs(referenceBinValue - targetBinValue);
			referenceHistogramSize += referenceBinValue;
			targetHistogramSize += targetBinValue;
		}
		
		final float maximumDistance = referenceHistogramSize + targetHistogramSize;
		
		return protoresult / maximumDistance;
	}
	
}
