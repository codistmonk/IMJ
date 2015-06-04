package imj3.draft.segmentation;

import static multij.tools.Tools.last;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

import multij.tools.Canvas;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class ImageComponent extends JComponent {
	
	private final BufferedImage image;
	
	private final List<Layer> layers;
	
	public ImageComponent(final BufferedImage image) {
		this.image = image;
		this.layers = new ArrayList<>();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		this.addLayer().getPainters().add(new Painter.Abstract() {
			
			@Override
			public final void paint(final Canvas canvas) {
				canvas.getGraphics().drawImage(ImageComponent.this.getImage(), 0, 0, null);
			}
			
			private static final long serialVersionUID = 7401374809131989838L;
			
		});
		
		this.setMinimumSize(new Dimension(imageWidth, imageHeight));
		this.setMaximumSize(new Dimension(imageWidth, imageHeight));
		this.setPreferredSize(new Dimension(imageWidth, imageHeight));
		this.setSize(new Dimension(imageWidth, imageHeight));
	}
	
	public final List<Layer> getLayers() {
		return this.layers;
	}
	
	public final Layer addLayer() {
		final Layer result = this.getLayers().isEmpty() ? this.new Layer(this.getImage().getWidth(), this.getImage().getHeight())
			: this.new Layer(last(this.getLayers()));
		
		this.getLayers().add(result);
		
		return result;
	}
	
	public final BufferedImage getImage() {
		return this.image;
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		final Layer layer = last(this.getLayers());
		final Canvas buffer = layer.getCanvas();
		
		layer.update();
		
		// XXX Fix for Java 8 defect on some machines
		buffer.getGraphics().drawImage(this.getImage(), 0, 0, 1, 1, 0, 0, 1, 1, null);
		
		g.drawImage(buffer.getImage(), 0, 0, null);
	}
	
	private static final long serialVersionUID = 1260599901446126551L;
	
	public static final Color CLEAR = new Color(0, true);
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static abstract interface Painter extends Serializable {
		
		public abstract void paint(Canvas canvas);
		
		public abstract AtomicBoolean getActive();
		
		public abstract AtomicBoolean getUpdateNeeded();
		
		/**
		 * @author codistmonk (creation 2015-02-18)
		 */
		public static abstract class Abstract implements Painter {
			
			private final AtomicBoolean active;
			
			private final AtomicBoolean updateNeeded;
			
			protected Abstract() {
				this(new AtomicBoolean(true), new AtomicBoolean(true));
			}
			
			protected Abstract(final AtomicBoolean active, final AtomicBoolean updateNeeded) {
				this.active = active;
				this.updateNeeded = updateNeeded;
			}
			
			@Override
			public final AtomicBoolean getActive() {
				return this.active;
			}
			
			@Override
			public final AtomicBoolean getUpdateNeeded() {
				return this.updateNeeded;
			}
			
			private static final long serialVersionUID = -6462246646444465973L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public final class Layer implements Serializable {
		
		private final Layer previous;
		
		private final Canvas canvas;
		
		private final List<ImageComponent.Painter> painters;
		
		public Layer(final Layer previous) {
			this(previous, previous.getCanvas().getWidth(), previous.getCanvas().getHeight());
		}
		
		public Layer(final int width, final int height) {
			this(null, width, height);
		}
		
		private Layer(final Layer previous, final int width, final int height) {
			this.previous = previous;
			this.canvas = new Canvas().setFormat(width, height, BufferedImage.TYPE_INT_ARGB);
			this.painters = new ArrayList<>();
		}
		
		public final Layer getPrevious() {
			return this.previous;
		}
		
		public final boolean update() {
			boolean result = this.getPrevious() != null && this.getPrevious().update();
			
			for (final ImageComponent.Painter painter : this.getPainters()) {
				result |= painter.getUpdateNeeded().getAndSet(false);
			}
			
			if (result) {
				if (this.getPrevious() != null) {
					this.getCanvas().getGraphics().drawImage(this.getPrevious().getCanvas().getImage(), 0, 0, null);
				}
				
				this.getPainters().forEach(painter -> {
					if (painter.getActive().get()) {
						painter.paint(this.getCanvas());
					}
				});
			}
			
			return result;
		}
		
		public final Canvas getCanvas() {
			return this.canvas;
		}
		
		public final List<ImageComponent.Painter> getPainters() {
			return this.painters;
		}
		
		private static final long serialVersionUID = 6101324389175368308L;
		
	}
	
}