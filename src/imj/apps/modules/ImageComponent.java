package imj.apps.modules;

import static java.lang.Math.sqrt;
import static javax.swing.SwingUtilities.convertPoint;
import static multij.tools.Tools.invoke;
import imj.Image;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import multij.context.Context;
import multij.events.EventManager.Event.Listener;
import multij.events.Variable;
import multij.events.Variable.ValueChangedEvent;
import multij.swing.SwingTools;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class ImageComponent extends JComponent {
	
	private final Context context;
	
	private final Image model;
	
	private final BufferedImage buffer;
	
	private final boolean adjust;
	
	public ImageComponent(final Context context, final Image model, final boolean adjust) {
		this.context = context;
		this.model = model;
		this.buffer = new BufferedImage(model.getColumnCount(), model.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
		this.adjust = adjust;
		
		{
			Variable<Double> variable = context.getVariable("SCALE");
			
			if (variable == null) {
				context.set("SCALE", 1.0);
				variable = context.getVariable("SCALE");
			}
			
			variable.addListener(new Variable.Listener<Double>() {
				
				@Override
				public final void valueChanged(final ValueChangedEvent<Double, ?> event) {
					ImageComponent.this.scaleChanged();
				}
				
			});
		}
		
		this.addAncestorListener(this.new ScrollSynchonizer());
		
		this.setPreferredSize(new Dimension(model.getColumnCount(), model.getRowCount()));
		
		this.modelChanged();
	}
	
	public final Context getContext() {
		return this.context;
	}
	
	public final Image getModel() {
		return this.model;
	}
	
	public final double getScale() {
		return this.context.get("SCALE");
	}
	
	final void scaleChanged() {
		this.setPreferredSize(new Dimension(
				this.scale(this.getModel().getColumnCount()), this.scale(this.getModel().getRowCount())));
		this.revalidate();
		this.repaint();
		
		final JScrollPane scrollPane = getAncestorJScrollPane(this);
		final JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
		final JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
		final double xRatio = getRatio(horizontalScrollBar);
		final double yRatio = getRatio(verticalScrollBar);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				setRatio(horizontalScrollBar, xRatio);
				setRatio(verticalScrollBar, yRatio);
			}
			
		});
	}
	
	public final void setScale(final double scale) {
		this.context.set("SCALE", scale);
	}
	
	public final int scale(final double value) {
		return (int) (value * this.getScale());
	}
	
	public final int unscale(final double value) {
		return (int) (value / this.getScale());
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		super.paintComponent(g);
		
		g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
		
		final Rectangle viewport = this.getVisibleRect();
		
		final double step = this.scale(1.0);
		
		if (2 < step) {
			final int firstY = (int) ((int) (viewport.y / step) * step);
			final int firstX = (int) ((int) (viewport.x / step) * step);
			final int yEnd = viewport.y + viewport.height;
			final int xEnd = viewport.x + viewport.width;
			
			// draw_grid:
			{
				g.setColor(Color.RED);
				
				final int lastX = xEnd - 1;
				final int lastY = yEnd - 1;
				
				for (int y = firstY; y < yEnd; y += step) {
					g.drawLine(viewport.x, y, lastX, y);
				}
				
				for (int x = firstX; x < xEnd; x += step) {
					g.drawLine(x, viewport.y, x, lastY);
				}
			}
			
			final int largestTextWidth = (int) g.getFontMetrics()
					.getStringBounds("999", g).getWidth();
			
			// draw_values:
			if (largestTextWidth < step) {
				for (double y = firstY; y < yEnd; y += step) {
					final int rowIndex = this.unscale(y);
					
					if (0 <= rowIndex && rowIndex < this.model.getRowCount()) {
						for (double x = firstX; x < xEnd; x += step) {
							final int columnIndex = this.unscale(x);
							
							if (0 <= columnIndex && columnIndex < this.model.getColumnCount()) {
								final String text = ""+ this.model.getValue(rowIndex, columnIndex);
								
								g.setColor(Color.BLACK);
								g.drawString(text, (int) (x + 1), (int) (y + step));
								g.drawString(text, (int) (x), (int) (y + step + 1));
								g.drawString(text, (int) (x - 1), (int) (y + step));
								g.drawString(text, (int) (x), (int) (y + step - 1));
								g.setColor(Color.YELLOW);
								g.drawString(text, (int) (x), (int) (y + step));
							}
						}
					}
				}
			}
		}
	}
	
	public static final BufferedImage awtImage(final Image image, final boolean adjust, final BufferedImage result) {
		final int rgb = new Color(1, 1, 1).getRGB();
		final int channelCount = image.getChannelCount();
		final boolean imageIsMonochannel = channelCount == 1;
		
		if (!adjust) {
			for (int y = 0; y < image.getRowCount(); ++y) {
				for (int x = 0; x < image.getColumnCount(); ++x) {
					result.setRGB(x, y, image.getValue(y, x) * (imageIsMonochannel ? rgb : 1));
				}
			}
		} else {
			final int n = image.getPixelCount();
			int oldMinimum = Integer.MAX_VALUE;
			int oldMaximum = Integer.MIN_VALUE;
			final int newMinimum = 0;
			final int newMaximum = 255;
			
			for (int i = 0; i < n; ++i) {
				final int value = image.getValue(i);
				
				if (value < oldMinimum) {
					oldMinimum = value;
				}
				
				if (oldMaximum < value) {
					oldMaximum = value;
				}
			}
			
			final int oldAmplitude = oldMaximum - oldMinimum;
			final int newAmplitude = newMaximum - newMinimum;
			
			for (int y = 0; y < image.getRowCount(); ++y) {
				for (int x = 0; x < image.getColumnCount(); ++x) {
					if (oldAmplitude == 0) {
						result.setRGB(x, y, newMaximum * rgb);
					} else {
						result.setRGB(x, y,
								(newMinimum + (image.getValue(y, x) - oldMinimum) * newAmplitude / oldAmplitude) * rgb);
					}
				}
			}
		}
		
		return result;
	}
	
	@Listener
	public final void modelChanged() {
		awtImage(this.getModel(), this.adjust, this.buffer);
		
		this.repaint();
	}
	
	/**
	 * @author codistmonk (creation 2013-01-24)
	 */
	private final class MouseHandler extends MouseAdapter {
		
		MouseHandler() {
			// NOP
		}
		
		@Override
		public final void mouseMoved(final MouseEvent event) {
			final Point xy = convertPoint((Component) event.getSource(), event.getPoint(), ImageComponent.this);
			final int x = ImageComponent.this.unscale(xy.x);
			final int y = ImageComponent.this.unscale(xy.y);
			final Image image = ImageComponent.this.getModel();
			final int channelCount = image.getChannelCount();
			final boolean imageIsMonochannel = channelCount == 1;
			
			if (0 <= y && y < image.getRowCount() && 0 <= x
					&& x < image.getColumnCount()) {
				final int value = image.getValue(y, x);
				
				invoke(ImageComponent.this.getRootPane().getParent(),
						"setTitle",
						"value(" + y + " " + x + ") = (" + (imageIsMonochannel ? value : "0x" + Integer.toHexString(value)) + ")"
								+ " scale = " + ImageComponent.this.getScale()
								* 100.0 + " %");
			}
		}
		
		@Override
		public final void mouseWheelMoved(final MouseWheelEvent event) {
			final double zoomFactor = 2.0;
			final double zoom;
			if (event.getWheelRotation() < 0) {
				zoom = zoomFactor;
			} else if (0 < event.getWheelRotation()) {
				zoom = 1.0 / zoomFactor;
			} else {
				zoom = 1.0;
			}
			
			ImageComponent.this.setScale(zoom * ImageComponent.this.getScale());
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-01-24)
	 */
	private final class ScrollSynchonizer implements AncestorListener {
		
		private final MouseAdapter mouseHandler;
		
		ScrollSynchonizer() {
			this.mouseHandler = ImageComponent.this.new MouseHandler();
		}
		
		@Override
		public final void ancestorRemoved(final AncestorEvent event) {
			final JScrollPane scrollPane = getAncestorJScrollPane(ImageComponent.this);
			
			if (scrollPane != null) {
				scrollPane.removeMouseListener(this.mouseHandler);
				scrollPane.removeMouseMotionListener(this.mouseHandler);
				scrollPane.removeMouseWheelListener(this.mouseHandler);
			}
		}
		
		@Override
		public final void ancestorMoved(final AncestorEvent event) {
			// NOP
		}
		
		@Override
		public final void ancestorAdded(final AncestorEvent event) {
			final JScrollPane scrollPane = getAncestorJScrollPane(ImageComponent.this);
			
			if (scrollPane != null) {
				final JScrollBar verticalScrollBar = scrollPane
						.getVerticalScrollBar();
				final JScrollBar horizontalScrollBar = scrollPane
						.getHorizontalScrollBar();
				final Context context = ImageComponent.this.getContext();
				
				final BoundedRangeModel verticalModel = context
						.get("VERTICAL_SCROLL_MODEL");
				final BoundedRangeModel horizontalModel = context
						.get("HORIZONTAL_SCROLL_MODEL");
				
				if (verticalModel == null) {
					context.set("VERTICAL_SCROLL_MODEL",
							verticalScrollBar.getModel());
				} else {
					verticalScrollBar.setModel(verticalModel);
				}
				
				if (horizontalModel == null) {
					context.set("HORIZONTAL_SCROLL_MODEL",
							horizontalScrollBar.getModel());
				} else {
					horizontalScrollBar.setModel(horizontalModel);
				}
				
				scrollPane.addMouseListener(this.mouseHandler);
				scrollPane.addMouseMotionListener(this.mouseHandler);
				scrollPane.addMouseWheelListener(this.mouseHandler);
			}
		}
		
	}
	
	public static final double getRatio(final JScrollBar scrollBar) {
		final int halfExtent = scrollBar.getVisibleAmount() / 2;
		final int minimum = scrollBar.getMinimum() + halfExtent;
		final int maximum = scrollBar.getMaximum() - halfExtent;
		
		return (double) (scrollBar.getValue() + halfExtent - minimum)
				/ (maximum - minimum);
	}
	
	public static final void setRatio(final JScrollBar scrollBar, final double ratio) {
		final int halfExtent = scrollBar.getVisibleAmount() / 2;
		final int minimum = scrollBar.getMinimum() + halfExtent;
		final int maximum = scrollBar.getMaximum() - halfExtent;
		
		scrollBar
				.setValue((int) (minimum + ratio * (maximum - minimum) - halfExtent));
	}
	
	public static final JScrollPane getAncestorJScrollPane(final Component component) {
		return getAncestor(component, JScrollPane.class);
	}
	
	public static final <T> T getAncestor(final Component component, final Class<T> ancestorClass) {
		Component result = component;
		
		while (result != null
				&& !ancestorClass.isAssignableFrom(result.getClass())) {
			result = result.getParent();
		}
		
		return (T) result;
	}
	
	public static final JComponent centered(final Component component) {
		final JComponent result = new JPanel(new GridBagLayout());
		
		result.add(component);
		
		return result;
	}
	
	public static final void show(final String title, final Image... images) {
		final int rowCount = (int) sqrt(images.length);
		final int columnCount = images.length / rowCount;
		final JPanel panel = new JPanel(new GridLayout(rowCount, columnCount));
		final Context context = new Context();
		
		for (final Image image : images) {
			panel.add(new JScrollPane(centered(new ImageComponent(context, image, false))));
		}
		
		SwingTools.show(panel, title, !SwingUtilities.isEventDispatchThread());
	}
	
	public static final void showAdjusted(final String title, final Image... images) {
		final int rowCount = (int) sqrt(images.length);
		final int columnCount = images.length / rowCount;
		final JPanel panel = new JPanel(new GridLayout(rowCount, columnCount));
		final Context context = new Context();
		
		for (final Image image : images) {
			panel.add(new JScrollPane(centered(new ImageComponent(context, image, true))));
		}
		
		SwingTools.show(panel, title, true);
	}
	
}
