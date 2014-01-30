package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.ProgressMonitor;

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
					final BufferedImage gradient = getGradient();
					final boolean showGradient = true;
					
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							if (showGradient) {
								buffer.setRGB(x, y, 0xFF000000 | (0x00010101 * (0xFF & gradient.getRGB(x, y))));
							} else {
								if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
									buffer.setRGB(x, y, 0xFF000000);
								}
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
			
			private BufferedImage gradient;
			
			private boolean process;
			
			{
				this.radius = 16;
				this.threshold = 0F;
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
			
			public final BufferedImage getGradient() {
				return this.gradient;
			}
			
			public final void refreshMask(final BufferedImage image) {
				this.process = !this.process;
				
				if (!this.process) {
					return;
				}
				
				final int width = image.getWidth();
				final int height = image.getHeight();
				
				if (this.getMask() == null ||
						this.getMask().getWidth() != width || this.getMask().getHeight() != height) {
					this.mask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
					this.gradient = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				}
				
				final BufferedImage mask = this.getMask();
				final BufferedImage gradient = this.getGradient();
				
				fill(mask, Color.WHITE);
				fill(gradient, Color.BLACK);
				
				new Thread() {
					
					@Override
					public final void run() {
						final int[] referenceHistogram = new int[BIN_COUNT];
						final int[] currentHistogram = new int[BIN_COUNT];
						final int radius = getRadius();
						final float threshold = getThreshold();
						final ProgressMonitor monitor = new ProgressMonitor(imageView, "Processing...", null, 0, width + height);
						
						// Horizontal scans
						
						for (int y = 0; y < height; ++y) {
							computeHistogram(image, 0, 0, radius, referenceHistogram);
							
							for (int x = 0; x < width; ++x) {
								final float distance = computeHistogramDistance(referenceHistogram, computeHistogram(image, x, y, radius, currentHistogram));
								final int g = (int) clamp(distance * 255F, 0F, 255F);
								
								gradient.setRGB(x, y, g);
								
								if (threshold <= distance) {
									mask.setRGB(x, y, 0);
									System.arraycopy(currentHistogram, 0, referenceHistogram, 0, BIN_COUNT);
								}
							}
							
							for (int x = 0; x < width - 1; ++x) {
								gradient.setRGB(x, y, max(gradient.getRGB(x, y), gradient.getRGB(x + 1, y)));
							}
							
							monitor.setProgress(y);
						}
						
						// Vertical scans
						
						for (int x = 0; x < width; ++x) {
							computeHistogram(image, 0, 0, radius, referenceHistogram);
							
							for (int y = 0; y < height; ++y) {
								final float distance = computeHistogramDistance(referenceHistogram, computeHistogram(image, x, y, radius, currentHistogram));
								final int g = max(0xFF & gradient.getRGB(x, y), (int) clamp(distance * 255F, 0F, 255F));
								
								gradient.setRGB(x, y, g * 20);
								
								if (threshold <= distance) {
									mask.setRGB(x, y, 0);
									System.arraycopy(currentHistogram, 0, referenceHistogram, 0, BIN_COUNT);
								}
							}
							
							for (int y = 0; y < height - 1; ++y) {
								gradient.setRGB(x, y, max(gradient.getRGB(x, y), gradient.getRGB(x, y + 1)));
							}
							
							monitor.setProgress(height + x);
						}
						
						monitor.close();
						imageView.refreshBuffer();
					}
					
				}.start();
				// Idea: find patches where the texture doesn't change too much
				
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
			Arrays.fill(result, 0);
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
	
	public static final void fill(final BufferedImage image, final Color color) {
		final Graphics2D g = image.createGraphics();
		
		g.setColor(color);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.dispose();
	}
	
	public static final float clamp(final float value, final float minimum, final float maximum) {
		return value < minimum ? minimum : maximum < value ? maximum : value;
	}
	
}
