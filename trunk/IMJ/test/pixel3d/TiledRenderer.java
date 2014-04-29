package pixel3d;

import static net.sourceforge.aprog.tools.Tools.instances;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import net.sourceforge.aprog.tools.Factory;

/**
 * @author codistmonk (creation 2014-04-29)
 */
public final class TiledRenderer implements Renderer {
	
	private BufferedImage canvas;
	
	private final Renderer[] subrenderers;
	
	private final Executor executor;
	
	public TiledRenderer(final Factory<? extends Renderer> subrendererFactory) {
		final int n = 4;
		this.subrenderers = instances(n, subrendererFactory);
		this.executor = new Executor(n);
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
			this.executor.submit(new Runnable() {
				
				@Override
				public final void run() {
					subrenderer.render();
				}
				
			});
		}
		
		this.executor.join();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 2137387212914109831L;
	
	/**
	 * @author codistmonk (creation 2014-04-29)
	 */
	public static final class Executor {
		
		private final Collection<Thread> threads;
		
		public Executor(final int threadCount) {
			this.threads = new ArrayList<Thread>(threadCount);
		}
		
		public final void submit(final Runnable task) {
			final Thread thread = new Thread(task);
			
			this.threads.add(thread);
			
			thread.start();
		}
		
		public final void join() {
			for (final Thread thread : this.threads) {
				try {
					thread.join();
				} catch (final InterruptedException exception) {
					exception.printStackTrace();
				}
			}
		}
		
	}
	
}
