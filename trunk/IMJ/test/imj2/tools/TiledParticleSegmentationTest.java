package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.instances;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
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
				
				{
					imageView.getPainters().add(this);
				}
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final BufferedImage image = imageView.getImage();
					final int imageWidth = image.getWidth();
					final int imageHeight = image.getHeight();
					final int s = getCellSize();
					final Point[] particles = instances(4, DefaultFactory.forClass(Point.class));
					final int[] maximumGradients = new int[particles.length];
					
					for (int tileY = 0; tileY < imageHeight; tileY += s) {
						final int tileLastY = min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX < imageWidth; tileX += s) {
							final int tileLastX = min(imageWidth - 1, tileX + s);
							
							particles[NORTH].setLocation((tileX + tileLastX) / 2, tileY);
							particles[WEST].setLocation(tileX, (tileY + tileLastY) / 2);
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
							
							g.setColor(Color.RED);
							g.drawLine(particles[NORTH].x, particles[NORTH].y, particles[SOUTH].x, particles[SOUTH].y);
							g.drawLine(particles[WEST].x, particles[WEST].y, particles[EAST].x, particles[EAST].y);
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
