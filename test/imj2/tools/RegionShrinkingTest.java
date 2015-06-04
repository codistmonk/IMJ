package imj2.tools;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static multij.swing.SwingTools.show;
import static multij.tools.Tools.debugPrint;

import imj2.tools.Image2DComponent.Painter;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-01-22)
 */
public final class RegionShrinkingTest {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				@Override
				public void paint(final Graphics2D g, final SimpleImageView component, final int width, final int height) {
					final BufferedImage mask = getMask();
					
					if (mask != null) {
						final BufferedImage buffer = imageView.getBufferImage();
						final int w = buffer.getWidth();
						final int h = buffer.getHeight();
						
						if (mask.getWidth() == w && mask.getHeight() == h) {
							for (int y = 0; y < h; ++y) {
								for (int x = 0; x < w; ++x) {
									if ((mask.getRGB(x, y) & 0x00FFFFFF) == 0) {
										buffer.setRGB(x, y, 0);
									}
								}
							}
						}
					}
					
					final Point mouseLocation = getMouseLocation();
					
					if (mouseLocation != null) {
						final int r = getRadius();
						final int d = 2 * r + 1;
						imageView.getBufferGraphics().drawOval(mouseLocation.x - r, mouseLocation.y - r, d, d);
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 2849951641253810303L;
				
			};
			
			private Point mouseLocation;
			
			private int radius;
			
			private BufferedImage mask;
			
			{
				this.radius = 2;
				imageView.getPainters().add(this.painter);
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				this.mouseLocation = event.getPoint();
				final BufferedImage image = imageView.getImage();
				final int w = image.getWidth();
				final int h = image.getHeight();
				final long[] referenceHistogram = new long[64];
				long referencePixelCount = 0L;
				final int r = this.getRadius();
				final int centerX = this.getMouseLocation().x;
				final int centerY = this.getMouseLocation().y;
				final int topY = centerY - r;
				final int bottomY = centerY + r;
				
				for (int y = topY; y <= bottomY; ++y) {
					// (y - centerY)^2 + (x - centerX)^2 = r^2
					// x - centerX = ± sqrt(r^2 - (y - centerY)^2)
					// x = centerX ± sqrt((r - y + centerY)(r + y - centerY))
					// x = centerX ± sqrt((bottomY - y)(y - topY))
					final int xSpan = (int) sqrt((bottomY - y) * (y - topY));
					final int firstX = centerX - xSpan;
					final int lastX = centerX + xSpan;
					
					for (int x = firstX; x <= lastX; ++x) {
						if (0 <= x && x < w && 0 <= y && y < h) {
							count(image, x, y, referenceHistogram);
							++referencePixelCount;
						}
					}
				}
				
				final long[] imageHistogram = new long[64];
				long imagePixelCount = 0L;
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						count(image, x, y, imageHistogram);
						++imagePixelCount;
					}
				}
				
				final double scale = getScaleOfReferenceInTarget(referenceHistogram,
						referencePixelCount, imageHistogram, imagePixelCount);
				
				debugPrint(scale);
				
				this.mask = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
				
				if (true) {
					Direction direction = Direction.WEST;
					int stepCount = w - 1;
					int alternateStepCount = h - 1;
					int x = 0;
					int y = 0;
					
					while (0 < stepCount) {
						for (int i = 0; i < stepCount; ++i) {
							this.updateMask(image, x, y, referenceHistogram, referencePixelCount,
									imageHistogram, imagePixelCount, scale);
							x += direction.getDx();
							y += direction.getDy();
						}
						
						
						{
							final Direction[] directions = Direction.values();
							final int ordinal = (direction.ordinal() + 1) % directions.length;
							final int tmp = stepCount;
							stepCount = alternateStepCount;
							alternateStepCount = tmp - 1;
							direction = directions[ordinal];
							if (direction == Direction.EAST && stepCount == w - 2) {
								stepCount = w - 1;
							}
						}
					}
					
					this.updateMask(image, x, y, referenceHistogram, referencePixelCount,
							imageHistogram, imagePixelCount, scale);
				} else {
					for (int y = 0; y < h; ++y) {
						for (int x = 0; x < w; ++x) {
							updateMask(image, x, y, referenceHistogram, referencePixelCount,
									imageHistogram, imagePixelCount, scale);
						}
					}
				}
				
				debugPrint(Arrays.toString(referenceHistogram));
				debugPrint(Arrays.toString(imageHistogram));
				
				imageView.refreshBuffer();
			}
			
			private final void updateMask(final BufferedImage image, final int x, final int y,
					final long[] referenceHistogram, final long referencePixelCount,
					final long[] imageHistogram, final long imagePixelCount, final double scale) {
				final int colorIndex = getColorIndex(image, x, y);
				
				if (referenceHistogram[colorIndex] * imagePixelCount * scale < imageHistogram[colorIndex] * referencePixelCount) {
					this.mask.setRGB(x, y, 0);
					--imageHistogram[colorIndex];
				} else {
					this.mask.setRGB(x, y, 0xFFFFFFFF);
				}
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				this.mouseLocation = event.getPoint();
				imageView.refreshBuffer();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (event.getWheelRotation() < 0 && this.getRadius() < 64) {
					this.radius = this.getRadius() + 2;
				} else if (0 < event.getWheelRotation() && 2 < this.getRadius()) {
					this.radius = this.getRadius() - 2;
				}
				
				imageView.refreshBuffer();
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			final int getRadius() {
				return this.radius;
			}
			
			final Point getMouseLocation() {
				return this.mouseLocation;
			}
			
			final BufferedImage getMask() {
				return this.mask;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -3985769750853247131L;
			
		};
		
		show(imageView, "Simple Image View", true);
	}
	
	public static final double getScaleOfReferenceInTarget(
			final long[] referenceHistogram, long referencePixelCount,
			final long[] targetHistogram, long targetPixelCount) {
		double result = 1.0;
		
		for (int i = 0; i < referenceHistogram.length; ++i) {
			final double referenceBinValue = (double) referenceHistogram[i] / referencePixelCount;
			
			if (0.0 != referenceBinValue) {
				final double targetBinValue = (double) targetHistogram[i] / targetPixelCount;
				result = min(result, targetBinValue / referenceBinValue);
			}
		}
		
		return result;
	}
	
	public static final int getColorIndex(final BufferedImage image, int x, int y) {
		final int rgb = image.getRGB(x, y);
		final int red = (rgb & 0x00C00000) >> 18;
		final int green = (rgb & 0x0000C000) >> 12;
		final int blue = (rgb & 0x000000C0) >> 6;
		final int colorIndex = red | green | blue;
		
		return colorIndex;
	}
	
	public static final void count(final BufferedImage image, int x, int y, final long[] histogram) {
		++histogram[getColorIndex(image, x, y)];
	}
	
	/**
	 * @author codistmonk (creation 2014-01-22)
	 */
	public static enum Direction {
		
		WEST {
			
			@Override
			public final int getDx() {
				return 1;
			}
			
			@Override
			public final int getDy() {
				return 0;
			}
			
		}, SOUTH {
			
			@Override
			public final int getDx() {
				return 0;
			}
			
			@Override
			public final int getDy() {
				return 1;
			}
			
		}, EAST {
			
			@Override
			public final int getDx() {
				return -1;
			}
			
			@Override
			public final int getDy() {
				return 0;
			}
			
		}, NORTH {
			
			@Override
			public final int getDx() {
				return 0;
			}
			
			@Override
			public final int getDy() {
				return -1;
			}
			
		};
		
		public abstract int getDx();
		
		public abstract int getDy();
		
	}
	
	/**
	 * @author codistmonk (creation 2014-01-22)
	 */
	public static abstract class AutoMouseAdapter extends MouseAdapter implements Serializable {
		
		private final Component component;
		
		public AutoMouseAdapter(final Component component) {
			this.component = component;
			component.addMouseListener(this);
			component.addMouseMotionListener(this);
			component.addMouseWheelListener(this);
		}
		
		public final void removeFromComponent() {
			this.component.removeMouseListener(this);
			this.component.removeMouseMotionListener(this);
			this.component.removeMouseWheelListener(this);
			
			this.cleanup();
		}
		
		protected void cleanup() {
			// NOP
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8003165485002210600L;
		
	}
	
}
