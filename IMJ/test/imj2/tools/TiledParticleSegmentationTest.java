package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;
import imj.IntList;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-02-23)
 */
public final class TiledParticleSegmentationTest {
	
	/**
	 * {@value}.
	 */
	public static final int NORTH = 0;
	
	/**
	 * {@value}.
	 */
	public static final int WEST = 1;
	
	/**
	 * {@value}.
	 */
	public static final int EAST = 2;
	
	/**
	 * {@value}.
	 */
	public static final int SOUTH = 3;
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private int cellSize = 8;
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				private final Canvas segmentation;
				
				{
					this.segmentation = new Canvas();
					imageView.getPainters().add(this);
				}
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final BufferedImage image = imageView.getImage();
					final int imageWidth = image.getWidth();
					final int imageHeight = image.getHeight();
					
					this.segmentation.setFormat(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
					this.segmentation.clear(BLACK);
					
					final int s = getCellSize();
					
					for (int tileY = 0; tileY + 2 < imageHeight; tileY += s) {
						final int tileLastY = imageHeight <= tileY + s + 2 ? imageHeight - 1 : min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX + 2 < imageWidth; tileX += s) {
							final int tileLastX = imageWidth <= tileX + s + 2 ? imageWidth - 1 : min(imageWidth - 1, tileX + s);
							final int northY = tileY;
							final int westX = tileX;
							final int eastX = tileLastX;
							final int southY = tileLastY;
							final int northX = findMaximumGradientX(image, northY, westX + 1, eastX - 1);
							final int westY = findMaximumGradientY(image, westX, northY + 1, southY - 1);
							final int eastY = findMaximumGradientY(image, eastX, northY + 1, southY - 1);
							final int southX = findMaximumGradientX(image, southY, westX + 1, eastX - 1);
							
							this.segmentation.getGraphics().setColor(WHITE);
							this.segmentation.getGraphics().drawLine(northX, northY, southX, southY);
							this.segmentation.getGraphics().drawLine(westX, westY, eastX, eastY);
						}
					}
					
					for (int y = 0; y < imageHeight; ++y) {
						for (int x = 0; x < imageWidth; ++x) {
							if ((this.segmentation.getImage().getRGB(x, y) & 0x00FFFFFF) != 0) {
								imageView.getBufferImage().setRGB(x, y, RED.getRGB());
							}
						}
					}
					
					{
						for (int tileY = 0; tileY < imageHeight; tileY += s) {
							for (int tileX = 0; tileX < imageWidth; tileX += s) {
								final Color[] color = { Color.YELLOW };
								
								new SegmentProcessor() {
									
									private int red = 0;
									
									private int green = 0;
									
									private int blue = 0;
									
									@Override
									protected final void pixel(final int pixel, final int x, final int y) {
										final int rgb = image.getRGB(x, y);
										this.red += (rgb >> 16) & 0xFF;
										this.green += (rgb >> 8) & 0xFF;
										this.blue += (rgb >> 0) & 0xFF;
									}
									
									@Override
									protected final void afterPixels() {
										final int pixelCount = this.getPixelCount();
										
										if (0 < pixelCount) {
											this.red = (this.red / pixelCount) << 16;
											this.green = (this.green / pixelCount) << 8;
											this.blue = (this.blue / pixelCount) << 0;
										}
										
										color[0] = new Color(this.red | this.green | this.blue);
									}
									
									/**
									 * {@value}.
									 */
									private static final long serialVersionUID = 8855412064679395641L;
									
								}.process(this.segmentation.getImage(), tileX, tileY);
								
								new SegmentProcessor() {
									
									@Override
									protected final void pixel(final int pixel, final int x, final int y) {
										imageView.getBuffer().getImage().setRGB(x, y, color[0].getRGB());
									}
									
									/**
									 * {@value}.
									 */
									private static final long serialVersionUID = -1650602767179074395L;
									
								}.process(this.segmentation.getImage(), tileX, tileY);
								
//								g.setColor(color[0]);
//								g.drawOval(tileX - 1, tileY - 1, 2, 2);
							}
						}
					}
					
//					for (int tileY = 0; tileY < imageHeight; tileY += s) {
//						for (int tileX = 0; tileX < imageWidth; tileX += s) {
//							g.setColor(Color.YELLOW);
//							g.drawOval(tileX - 1, tileY - 1, 2, 2);
//						}
//					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -8170474943200742892L;
				
			};
			
			public final int getCellSize() {
				return this.cellSize;
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -6497489818537320168L;
			
		};
		
		show(imageView, this.getClass().getSimpleName(), true);
	}
	
	public static final int findMaximumGradientX(final BufferedImage image, final int y, final int firstX, final int lastX) {
		int maximumGradient = 0;
		int result = (firstX + lastX) / 2;
		
		for (int x = firstX; x <= lastX; ++x) {
			final int gradient = getColorGradient(image, x, y);
			
			if (maximumGradient < gradient) {
				maximumGradient = gradient;
				result = x;
			}
		}
		
		return result;
	}
	
	public static final int findMaximumGradientY(final BufferedImage image, final int x, final int firstY, final int lastY) {
		int maximumGradient = 0;
		int result = (firstY + lastY) / 2;
		
		for (int y = firstY; y <= lastY; ++y) {
			final int gradient = getColorGradient(image, x, y);
			
			if (maximumGradient < gradient) {
				maximumGradient = gradient;
				result = y;
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-02-24)
	 */
	public static abstract class SegmentProcessor implements Serializable {
		
		private int pixelCount;
		
		public final SegmentProcessor process(final BufferedImage segmentation, final int tileX, final int tileY) {
			final IntList todo = new IntList();
			final int w = segmentation.getWidth();
			final int h = segmentation.getHeight();
			final BitSet done = new BitSet(w * h);
			this.pixelCount = 0;
			
			todo.add(tileY * w + tileX);
			
			this.beforePixels();
			
			while (!todo.isEmpty()) {
				final int pixel = todo.remove(0);
				final int x = pixel % w;
				final int y = pixel / w;
				
				++this.pixelCount;
				done.set(pixel);
				
				this.pixel(pixel, x, y);
				
				if (0 < y && !done.get(pixel - w) && (segmentation.getRGB(x, y - 1) & 0x00FFFFFF) == 0) {
					todo.add(pixel - w);
				}
				
				if (0 < x && !done.get(pixel - 1) && (segmentation.getRGB(x - 1, y) & 0x00FFFFFF) == 0) {
					todo.add(pixel - 1);
				}
				
				if (x + 1 < w && !done.get(pixel + 1) && (segmentation.getRGB(x + 1, y) & 0x00FFFFFF) == 0) {
					todo.add(pixel + 1);
				}
				
				if (y + 1 < h && !done.get(pixel + w) && (segmentation.getRGB(x, y + 1) & 0x00FFFFFF) == 0) {
					todo.add(pixel + w);
				}
			}
			
			this.afterPixels();
			
			return this;
		}
		
		public final int getPixelCount() {
			return this.pixelCount;
		}
		
		protected void beforePixels() {
			// NOP
		}
		
		protected abstract void pixel(int pixel, int x, int y);
		
		protected void afterPixels() {
			// NOP
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5860299062780405885L;
		
	}
	
}
