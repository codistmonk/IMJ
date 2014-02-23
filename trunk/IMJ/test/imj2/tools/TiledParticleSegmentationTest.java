package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.instances;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

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
					final Point[] particles = instances(4, DefaultFactory.forClass(Point.class));
					final int[] maximumGradients = new int[particles.length];
					
					for (int tileY = 0; tileY < imageHeight; tileY += s) {
						final int tileLastY = min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX < imageWidth; tileX += s) {
							final int tileLastX = min(imageWidth - 1, tileX + s);
							
							particles[NORTH].setLocation((tileX + 1 + tileLastX) / 2, tileY);
							particles[WEST].setLocation(tileX, (tileY + 1 + tileLastY) / 2);
							particles[EAST].setLocation(tileLastX, particles[WEST].y);
							particles[SOUTH].setLocation(particles[NORTH].x, tileLastY);
							
							fill(maximumGradients, 0);
							
							for (int x = tileX + 1; x < tileLastX; ++x) {
								updateParticleX(NORTH, particles, maximumGradients, x,
										getColorGradient(image, x, tileY));
								updateParticleX(SOUTH, particles, maximumGradients, x,
										getColorGradient(image, x, tileLastY));
							}
							
							for (int y = tileY + 1; y < tileLastY; ++y) {
								updateParticleY(WEST, particles, maximumGradients, y,
										getColorGradient(image, tileX, y));
								updateParticleY(EAST, particles, maximumGradients, y,
										getColorGradient(image, tileLastX, y));
							}
							
							this.canvas.getGraphics().setColor(WHITE);
							this.canvas.getGraphics().drawLine(particles[NORTH].x, particles[NORTH].y, particles[SOUTH].x, particles[SOUTH].y);
							this.canvas.getGraphics().drawLine(particles[WEST].x, particles[WEST].y, particles[EAST].x, particles[EAST].y);
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
	
	public static final void updateParticleX(final int particleIndex, final Point[] particles,
			final int[] maximumGradients, final int x, final int gradient) {
		if (maximumGradients[particleIndex] < gradient) {
			maximumGradients[particleIndex] = gradient;
			particles[particleIndex].x = x;
		}
	}
	
	public static final void updateParticleY(final int particleIndex, final Point[] particles,
			final int[] maximumGradients, final int y, final int gradient) {
		if (maximumGradients[particleIndex] < gradient) {
			maximumGradients[particleIndex] = gradient;
			particles[particleIndex].y = y;
		}
	}
	
}
