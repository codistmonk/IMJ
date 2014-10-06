package imj2.pixel3d;

import static java.lang.Math.round;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.SimpleImageView;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-04-27)
 */
public final class Pixel3DDemo {
	
	private Pixel3DDemo() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final Statistics time = new Statistics();
		final SimpleImageView imageView = new SimpleImageView();
		
		final int w = 800;
		final int h = w;
		
		imageView.setImage(new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB));
		imageView.setPreferredSize(new Dimension(w, h));
		
		SwingTools.show(imageView, Pixel3DDemo.class.getName(), false);
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			private final Renderer renderer = new OrthographicRenderer().setCanvas(imageView.getBufferImage());
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final TicToc timer = new TicToc();
				
				timer.tic();
				
				imageView.getBuffer().clear(Color.BLACK);
				
				this.renderer.setCanvas(imageView.getBufferImage());
				this.renderer.clear();
				
				for (int ty = 0; ty < h; ty += 100) {
					for (int tx = 0; tx < w; tx += 100) {
						PolygonTools.render(new ARGBShader(this.renderer, 0x80FF0000),
								tx + 10.0, ty + 0.0, 0.0,
								tx + 100.0, ty + 40.0, 1.0,
								tx + 0.0, ty + 10.0, 0.0);
						PolygonTools.render(new ARGBShader(this.renderer, 0x8000FF00),
								tx + 90.0, ty + 0.0, 0.0,
								tx + 100.0, ty + 10.0, 0.0,
								tx + 0.0, ty + 80.0, 1.0);
						PolygonTools.render(new ARGBShader(this.renderer, 0x800000FF),
								tx + 45.0, ty + 80.0, 0.0,
								tx + 55.0, ty + 70.0, 0.0,
								tx + 20.0, ty + 0.0, 1.0);
					}
				}
				
				this.renderer.render();
				
				time.addValue(timer.toc());
				
				debugPrint("frameTime:", round(time.getMean()), "frameRate:", round(1000.0 / time.getMean()));
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3358934187100520107L;
			
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				for (int i = 0; i < 100; ++i) {
					if (i % 21 == 0) {
						time.reset();
					}
					
					imageView.refreshBuffer();
				}
			}
			
		});
	}
	
}
