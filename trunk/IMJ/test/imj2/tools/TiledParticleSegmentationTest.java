package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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
				
				private final Canvas canvas;
				
				{
					this.canvas = new Canvas();
					imageView.getPainters().add(this);
				}
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final BufferedImage image = imageView.getImage();
					final int imageWidth = image.getWidth();
					final int imageHeight = image.getHeight();
					
					this.canvas.setFormat(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
					this.canvas.clear(BLACK);
					
					final int s = getCellSize();
					
					for (int tileY = 0; tileY < imageHeight; tileY += s) {
						final int tileLastY = min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX < imageWidth; tileX += s) {
							final int tileLastX = min(imageWidth - 1, tileX + s);
							final int northY = tileY;
							final int westX = tileX;
							final int eastX = tileLastX;
							final int southY = tileLastY;
							final int northX = findMaximumGradientX(image, northY, westX + 1, eastX - 1);
							final int westY = findMaximumGradientY(image, westX, northY + 1, southY - 1);
							final int eastY = findMaximumGradientY(image, eastX, northY + 1, southY - 1);
							final int southX = findMaximumGradientX(image, southY, westX + 1, eastX - 1);
							
							this.canvas.getGraphics().setColor(WHITE);
							this.canvas.getGraphics().drawLine(northX, northY, southX, southY);
							this.canvas.getGraphics().drawLine(westX, westY, eastX, eastY);
						}
					}
					
					for (int y = 0; y < imageHeight; ++y) {
						for (int x = 0; x < imageWidth; ++x) {
							if ((this.canvas.getImage().getRGB(x, y) & 0x00FFFFFF) != 0) {
								imageView.getBufferImage().setRGB(x, y, RED.getRGB());
							}
						}
					}
					
					for (int tileY = 0; tileY < imageHeight; tileY += s) {
						for (int tileX = 0; tileX < imageWidth; tileX += s) {
							g.setColor(Color.YELLOW);
							g.drawOval(tileX - 1, tileY - 1, 2, 2);
						}
					}
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
	
}
