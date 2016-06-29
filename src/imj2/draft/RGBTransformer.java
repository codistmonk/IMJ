package imj2.draft;

import java.awt.image.BufferedImage;
import java.io.Serializable;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-30)
 */
public interface RGBTransformer extends Serializable {
	
	public abstract int transform(int rgb);
	
	/**
	 * @author codistmonk (creation 2014-04-30)
	 */
	public static enum Predefined implements RGBTransformer {
		
		ID {
			
			@Override
			public final int transform(final int rgb) {
				return rgb;
			}
			
		};
		
	}
	
	/**
	 * @author codistmonk (creation 2014-05-02)
	 */
	public static final class Tools {
		
		private Tools() {
			throw new IllegalInstantiationException();
		}
		
		public static final void filter(final BufferedImage image, final RGBTransformer transformer, final BufferedImage target) {
			final int w = target.getWidth();
			final int h = target.getHeight();
			
			for (int y = 0; y < h; ++y) {
				for (int x = 0; x < w; ++x) {
					if (0 < (transformer.transform(image.getRGB(x, y)) & 0x00FFFFFF)) {
						target.setRGB(x, y, 0xFFFFFFFF);
					}
				}
			}
		}
		
		public static final void transform(final BufferedImage image, final RGBTransformer transformer, final BufferedImage target) {
			final int w = target.getWidth();
			final int h = target.getHeight();
			
			for (int y = 0; y < h; ++y) {
				for (int x = 0; x < w; ++x) {
					target.setRGB(x, y, transformer.transform(image.getRGB(x, y)));
				}
			}
		}
		
		public static final void drawSegmentContours(final BufferedImage labels, final int color, final BufferedImage target) {
			final int w = target.getWidth();
			final int h = target.getHeight();
			final int right = w - 1;
			final int bottom = h - 1;
			
			for (int x = 0; x < w; ++x) {
				target.setRGB(x, 0, color);
				target.setRGB(x, bottom, color);
			}
			
			for (int y = 0; y < h; ++y) {
				target.setRGB(0, y, color);
				target.setRGB(right, y, color);
			}
			
			for (int y = 1; y < bottom; ++y) {
				for (int x = 1; x < right; ++x) {
					final int label = labels.getRGB(x, y);
					
					if (labels.getRGB(x, y - 1) < label
							|| labels.getRGB(x - 1, y) < label
							|| labels.getRGB(x + 1, y) < label
							|| labels.getRGB(x, y + 1) < label) {
						target.setRGB(x, y, color);
					}
				}
			}
		}
		
	}
	
}