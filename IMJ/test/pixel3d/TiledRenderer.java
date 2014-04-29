package pixel3d;

import static net.sourceforge.aprog.tools.Tools.instances;

import java.awt.image.BufferedImage;

import net.sourceforge.aprog.tools.Factory;

/**
 * @author codistmonk (creation 2014-04-29)
 */
public final class TiledRenderer implements Renderer {
	
	private BufferedImage canvas;
	
	private final Renderer[] subrenderers;
	
	public TiledRenderer(final Factory<? extends Renderer> subrendererFactory) {
		this.subrenderers = instances(4, subrendererFactory);
	}
	
	@Override
	public final TiledRenderer setCanvas(final BufferedImage canvas) {
		for (final Renderer subrenderer : this.subrenderers) {
			subrenderer.setCanvas(canvas);
		}
		
		this.canvas = canvas;
		
		return this;
	}
	
	@Override
	public final void clear() {
		for (final Renderer subrenderer : this.subrenderers) {
			subrenderer.clear();
		}
	}
	
	@Override
	public final TiledRenderer addPixel(final double x, final double y, final double z, final int argb) {
		final int x0 = this.canvas.getWidth() / 2;
		final int y0 = this.canvas.getHeight() / 2;
		
		if (y < y0) {
			if (x < x0) {
				this.subrenderers[0].addPixel(x, y, z, argb);
			} else {
				this.subrenderers[1].addPixel(x, y, z, argb);
			}
		} else {
			if (x < x0) {
				this.subrenderers[2].addPixel(x, y, z, argb);
			} else {
				this.subrenderers[3].addPixel(x, y, z, argb);
			}
		}
		
		return this;
	}
	
	@Override
	public final void render() {
		for (final Renderer subrenderer : this.subrenderers) {
			subrenderer.render();
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 2137387212914109831L;
	
}
