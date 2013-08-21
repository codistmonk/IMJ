package imj2.tools;

import static imj2.tools.MultiThreadTools.WORKER_COUNT;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.list;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-08-21)
 */
public abstract class ParallelProcess2D extends MonopatchProcess implements Runnable, Cloneable {
	
	private int workerId;
	
	private final Image2D[] images;
	
	private Image2D image;
	
	private Rectangle tile;
	
	protected ParallelProcess2D(final Image2D image) {
		this.workerId = -1;
		this.images = image.newParallelViews(WORKER_COUNT);
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final ExecutorService executor = MultiThreadTools.getExecutor();
		final Collection<Rectangle> tiles = list(IMJTools.parallelTiles(imageWidth, imageHeight, WORKER_COUNT));
		
		debugPrint("tileCount:", tiles.size());
		
		final Collection<Future<?>> tasks = new ArrayList<Future<?>>(tiles.size());
		
		try {
			for (final Rectangle tile : tiles) {
				final ParallelProcess2D task = this.clone();
				task.tile = tile;
				tasks.add(executor.submit(task));
			}
		} catch (final CloneNotSupportedException exception) {
			throw unchecked(exception);
		}
		
		MultiThreadTools.wait(tasks);
	}
	
	public final int getWorkerId() {
		return this.workerId;
	}
	
	public final Image2D[] getImages() {
		return this.images;
	}
	
	public final Image2D getImage() {
		return this.image;
	}
	
	public final Rectangle getTile() {
		return this.tile;
	}
	
	@Override
	public final void run() {
		this.workerId = MultiThreadTools.getWorkerId();
		this.image = this.getImages()[this.getWorkerId()];
		final Rectangle tile = this.getTile();
		Tools.debugPrint(this.getWorkerId(), this.getTile());
		this.beforeProcessing();
		this.images[this.getWorkerId()].forEachPixelInBox(tile.x, tile.y, tile.width, tile.height, this);
	}
	
	@Override
	public final ParallelProcess2D clone() throws CloneNotSupportedException {
		return (ParallelProcess2D) super.clone();
	}
	
	protected void beforeProcessing() {
		// NOP
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -499024532331810275L;
	
}
