package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-02-23)
 */
public final class TiledParticleSegmentationTest {
	
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
					
					for (int tileY = 0; tileY < imageHeight; tileY += s) {
						final int tileLastY = min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX < imageWidth; tileX += s) {
							final int tileLastX = min(imageWidth - 1, tileX + s);
							final Point north = new Point((tileX + tileLastX) / 2, tileY);
							final Point west = new Point(tileX, (tileY + tileLastY) / 2);
							final Point east = new Point(tileLastX, west.y);
							final Point south = new Point(north.x, tileLastY);
							int northMaximumGradient = 0;
							int westMaximumGradient = 0;
							int eastMaximumGradient = 0;
							int southMaximumGradient = 0;
							
							for (int x = tileX + 1; x < tileLastX; ++x) {
								final int northGradient = getColorGradient(image, x, tileY);
								
								if (northMaximumGradient < northGradient) {
									northMaximumGradient = northGradient;
									north.x = x;
								}
								
								final int southGradient = getColorGradient(image, x, tileLastY);
								
								if (southMaximumGradient < southGradient) {
									southMaximumGradient = southGradient;
									south.x = x;
								}
							}
							
							for (int y = tileY + 1; y < tileLastY; ++y) {
								final int westGradient = getColorGradient(image, tileX, y);
								
								if (westMaximumGradient < westGradient) {
									westMaximumGradient = westGradient;
									west.y = y;
								}
								
								final int eastGradient = getColorGradient(image, tileLastX, y);
								
								if (eastMaximumGradient < eastGradient) {
									eastMaximumGradient = eastGradient;
									east.y = y;
								}
							}
							
							g.setColor(Color.RED);
							g.drawLine(north.x, north.y, south.x, south.y);
							g.drawLine(west.x, west.y, east.x, east.y);
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
	
}
