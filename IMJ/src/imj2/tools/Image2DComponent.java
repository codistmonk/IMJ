package imj2.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;

/**
 * @author codistmonk (creation 2013-08-05)
 */
public final class Image2DComponent extends JComponent {
	
	private ScaledImage2D scaledImage;
	
	private BufferedImage backBuffer;
	
	private Graphics2D backBufferGraphics;
	
	private BufferedImage frontBuffer;
	
	private Graphics2D frontBufferGraphics;
	
	private final Rectangle scaledImageVisibleRectangle;
	
	private final JScrollBar horizontalScrollBar;
	
	private final JScrollBar verticalScrollBar;
	
	private boolean multiThread;
	
	public Image2DComponent() {
		this.scaledImageVisibleRectangle = new Rectangle();
		this.horizontalScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
		this.verticalScrollBar = new JScrollBar(Adjustable.VERTICAL);
		this.multiThread = false;
		this.setDoubleBuffered(false);
		this.setLayout(new BorderLayout());
		this.add(horizontalBox(this.horizontalScrollBar, Box.createHorizontalStrut(this.verticalScrollBar.getPreferredSize().width)), BorderLayout.SOUTH);
		this.add(this.verticalScrollBar, BorderLayout.EAST);
		
		this.addComponentListener(new ComponentAdapter() {
			
			@Override
			public final void componentResized(final ComponentEvent event) {
				Image2DComponent.this.setScrollBarsVisibleAmounts();
			}
			
		});
		
		final AdjustmentListener bufferPositionAdjuster = new AdjustmentListener() {
			
			@Override
			public final void adjustmentValueChanged(final AdjustmentEvent event) {
				Image2DComponent.this.updateBufferAccordingToScrollBars(false);
				Image2DComponent.this.repaint();
			}
			
		};
		
		this.horizontalScrollBar.addAdjustmentListener(bufferPositionAdjuster);
		this.verticalScrollBar.addAdjustmentListener(bufferPositionAdjuster);
		
		this.setBackground(Color.BLACK);
		
		this.setFocusable(true);
		
		this.addKeyListener(new KeyAdapter() {
			
			@Override
			public final void keyTyped(final KeyEvent event) {
				final Image2D image = Image2DComponent.this.getImage();
				final int zoom = Image2DComponent.this.getZoom();
				
				switch (event.getKeyChar()) {
				case '*':
					Image2DComponent.this.setZoom(zoom * 2);
					break;
				case '/':
					Image2DComponent.this.setZoom(zoom / 2);
					break;
				case '+':
					final SubsampledImage2D subsampledImage = cast(SubsampledImage2D.class, image);
					
					if (subsampledImage != null) {
						Image2DComponent.this.setImage(subsampledImage.getSource());
						Image2DComponent.this.setZoom(zoom / 2);
					}
					
					break;
				case '-':
					if (1 < image.getWidth() && 1 < image.getHeight()) {
						Image2DComponent.this.setImage(new SubsampledImage2D(image));
						Image2DComponent.this.setZoom(zoom * 2);
					}
					
					break;
				default:
					return;
				}
				
				final JScrollBar horizontalScrollBar = Image2DComponent.this.getHorizontalScrollBar();
				final JScrollBar verticalScrollBar = Image2DComponent.this.getVerticalScrollBar();
				final int oldHV = horizontalScrollBar.getValue();
				final int oldHA = horizontalScrollBar.getVisibleAmount();
				final int oldHM = horizontalScrollBar.getMaximum();
				final int oldVV = verticalScrollBar.getValue();
				final int oldVA = verticalScrollBar.getVisibleAmount();
				final int oldVM = verticalScrollBar.getMaximum();
				
				Image2DComponent.this.setScrollBarsVisibleAmounts();
				Image2DComponent.this.updateBufferAccordingToScrollBars(true);
				
				final int newHA = horizontalScrollBar.getVisibleAmount();
				final int newHM = horizontalScrollBar.getMaximum();
				final int newVA = verticalScrollBar.getVisibleAmount();
				final int newVM = verticalScrollBar.getMaximum();
				
				// oldC / oldM = newC / newM
				// -> newC = oldC * newM / oldM
				// -> newV + newA / 2 = (oldV + oldA / 2) * newM / oldM
				// -> newV = (oldV + oldA / 2) * newM / oldM - newA / 2
				// -> newV = ((2 * oldV + oldA) * newM - newA * oldM) / (2 * oldM)
				horizontalScrollBar.setValue((int) (((2L * oldHV + oldHA) * newHM - (long) newHA * oldHM) / (2L * oldHM)));
				verticalScrollBar.setValue((int) (((2L * oldVV + oldVA) * newVM - (long) newVA * oldVM) / (2L * oldVM)));
			}
			
		});
		
		final MouseAdapter mouseHandler = new MouseAdapter() {
			
			private int horizontalScrollBarValue;
			
			private int verticalScrollBarValue;
			
			private int x;
			
			private int y;
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.horizontalScrollBarValue = Image2DComponent.this.getHorizontalScrollBar().getValue();
				this.verticalScrollBarValue = Image2DComponent.this.getVerticalScrollBar().getValue();
				this.x = event.getX();
				this.y = event.getY();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				Image2DComponent.this.getHorizontalScrollBar().setValue(this.horizontalScrollBarValue - (event.getX() - this.x));
				Image2DComponent.this.getVerticalScrollBar().setValue(this.verticalScrollBarValue - (event.getY() - this.y));
			}
			
		};
		
		this.addMouseListener(mouseHandler);
		this.addMouseMotionListener(mouseHandler);
		this.addMouseMotionListener(mouseHandler);
	}
	
	public Image2DComponent(final Image2D image) {
		this();
		this.scaledImage = new ScaledImage2D(image);
		this.horizontalScrollBar.setMaximum(image.getWidth());
		this.verticalScrollBar.setMaximum(image.getHeight());
		
		final Dimension preferredSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
		preferredSize.width = min(preferredSize.width / 2, image.getWidth() + this.verticalScrollBar.getPreferredSize().width);
		preferredSize.height = min(preferredSize.height / 2, image.getHeight() + this.horizontalScrollBar.getPreferredSize().height);
		
		this.setPreferredSize(preferredSize);
	}
	
	public final int getZoom() {
		return this.getScaledImage().getZoom();
	}
	
	public final void setZoom(final int zoom) {
		if (0 < zoom && zoom != this.getZoom()) {
			this.getScaledImage().setZoom(zoom);
			
			this.repaint();
		}
	}
	
	public final Image2D getImage() {
		return this.getScaledImage() == null ? null : this.getScaledImage().getSource();
	}
	
	public final void setImage(final Image2D image) {
		this.scaledImage = new ScaledImage2D(image);
	}
	
	public final boolean isMultiThread() {
		return this.multiThread;
	}
	
	public final void setMultiThread(final boolean multiThread) {
		this.multiThread = multiThread;
	}
	
	public final void setBuffer() {
		final int width = min(this.getScaledImageWidth(), max(1, this.getUsableWidth()));
		final int height = min(this.getScaledImageHeight(), max(1, this.getUsableHeight()));
		final boolean createBuffer;
		
		if (this.frontBuffer == null) {
			createBuffer = true;
		} else if (this.frontBuffer.getWidth() != width || this.frontBuffer.getHeight() != height) {
			this.frontBufferGraphics.dispose();
			this.frontBufferGraphics = null;
			this.backBufferGraphics.dispose();
			this.backBufferGraphics = null;
			createBuffer = true;
		} else {
			createBuffer = false;
		}
		
		if (createBuffer) {
			final BufferedImage oldBuffer = this.frontBuffer;
			this.frontBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			this.frontBufferGraphics = this.frontBuffer.createGraphics();
			this.backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			this.backBufferGraphics = this.backBuffer.createGraphics();
			
			this.setScaledImageVisibleRectangle(new Rectangle(
					min(this.scaledImageVisibleRectangle.x, this.getScaledImageWidth() - width),
					min(this.scaledImageVisibleRectangle.y, this.getScaledImageHeight() - height),
					width, height), oldBuffer);
		}
	}
	
	public final void updateBuffer(final int left, final int top, final int width, final int height) {
		if (this.getScaledImage() != null && 0 < width && 0 < height) {
			this.copyImagePixelsToBuffer(left, top, width, height);
		}
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		this.setBuffer();
		
		final int centeringOffsetX = max(0, (this.getUsableWidth() - this.frontBuffer.getWidth()) / 2);
		final int centeringOffsetY = max(0, (this.getUsableHeight() - this.frontBuffer.getHeight()) / 2);
		
		g.drawImage(this.frontBuffer, centeringOffsetX, centeringOffsetY, null);
	}
	
	final JScrollBar getHorizontalScrollBar() {
		return this.horizontalScrollBar;
	}
	
	final JScrollBar getVerticalScrollBar() {
		return this.verticalScrollBar;
	}
	
	final void updateBufferAccordingToScrollBars(final boolean forceRepaint) {
		if (this.frontBuffer == null) {
			return;
		}
		
		final int width = min(this.getScaledImageWidth(), this.frontBuffer.getWidth());
		final int height = min(this.getScaledImageHeight(), this.frontBuffer.getHeight());
		final int left = width < this.getScaledImageWidth() ? min(this.getScaledImageWidth() - width, this.horizontalScrollBar.getValue()) : 0;
		final int top = height < this.getScaledImageHeight() ? min(this.getScaledImageHeight() - height, this.verticalScrollBar.getValue()) : 0;
		
		this.setScaledImageVisibleRectangle(new Rectangle(left, top, width, height), forceRepaint ? null : this.frontBuffer);
	}
	
	final void setScrollBarsVisibleAmounts() {
		this.horizontalScrollBar.setMaximum(this.getScaledImageWidth());
		this.verticalScrollBar.setMaximum(this.getScaledImageHeight());
		
		final int usableWidth = max(0, this.getUsableWidth());
		final int usableHeight = max(0, this.getUsableHeight());
		
		if (this.horizontalScrollBar.getMaximum() <= this.horizontalScrollBar.getValue() + usableWidth) {
			this.horizontalScrollBar.setValue(max(0, this.horizontalScrollBar.getMaximum() - usableWidth));
		}
		
		this.horizontalScrollBar.setVisibleAmount(usableWidth);
		
		if (this.verticalScrollBar.getMaximum() <= this.verticalScrollBar.getValue() + usableHeight) {
			this.verticalScrollBar.setValue(max(0, this.verticalScrollBar.getMaximum() - usableHeight));
		}
		
		this.verticalScrollBar.setVisibleAmount(usableHeight);
	}
	
	final void copyImagePixelsToBuffer(final int left, final int top, final int width, final int height) {
		if (this.isMultiThread()) {
			new ParallelProcess2D(this.getScaledImage(), left, top, width, height) {
				
				@Override
				public final void pixel(final int x, final int y) {
					Image2DComponent.this.copyImagePixelToBuffer(x, y);
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 7757156523330629112L;
				
			};
		} else {
			this.getScaledImage().forEachPixelInBox(left, top, width, height, new MonopatchProcess() {
				
				@Override
				public final void pixel(final int x, final int y) {
					Image2DComponent.this.copyImagePixelToBuffer(x, y);
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 1810623847473680066L;
				
			});
		}
	}
	
	final void copyImagePixelToBuffer(final int xInScaledImage, final int yInScaledImage) {
		try {
			this.frontBuffer.setRGB(xInScaledImage - this.scaledImageVisibleRectangle.x, yInScaledImage - this.scaledImageVisibleRectangle.y,
					this.getScaledImage().getPixelValue(xInScaledImage, yInScaledImage));
		} catch (final Exception exception) {
			exception.printStackTrace();
			debugPrint(xInScaledImage, yInScaledImage, xInScaledImage - this.scaledImageVisibleRectangle.x, yInScaledImage - this.scaledImageVisibleRectangle.y);
			System.exit(-1);
		}
	}
	
	final ScaledImage2D getScaledImage() {
		return this.scaledImage;
	}
	
	public final void setScaledImage(ScaledImage2D scaledImage) {
		this.scaledImage = scaledImage;
	}

	private final int getUsableHeight() {
		return this.getHeight() - this.horizontalScrollBar.getHeight();
	}
	
	private final int getUsableWidth() {
		return this.getWidth() - this.verticalScrollBar.getWidth();
	}
	
	private final void setScaledImageVisibleRectangle(final Rectangle rectangle, final BufferedImage oldBuffer) {
		if (this.getScaledImageWidth() < rectangle.x + rectangle.width || this.getScaledImageHeight() < rectangle.y + rectangle.height ||
				this.frontBuffer.getWidth() < rectangle.width || this.frontBuffer.getHeight() < rectangle.height) {
			throw new IllegalArgumentException(rectangle + " " + new Rectangle(this.getScaledImageWidth(), this.getScaledImageHeight()) +
					" " + new Rectangle(this.frontBuffer.getWidth(),  this.frontBuffer.getHeight()));
		}
		
		if (oldBuffer == null) {
			this.scaledImageVisibleRectangle.setBounds(rectangle);
			this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
		} else {
			final Rectangle intersection = this.scaledImageVisibleRectangle.intersection(rectangle);
			
			if (intersection.isEmpty()) {
				this.scaledImageVisibleRectangle.setBounds(rectangle);
				this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
			} else {
				final int intersectionRight = intersection.x + intersection.width;
				final int intersectionBottom = intersection.y + intersection.height;
				
				this.backBufferGraphics.drawImage(oldBuffer,
						intersection.x - rectangle.x, intersection.y - rectangle.y,
						intersectionRight - rectangle.x, intersectionBottom - rectangle.y,
						intersection.x - this.scaledImageVisibleRectangle.x, intersection.y - this.scaledImageVisibleRectangle.y,
						intersectionRight - this.scaledImageVisibleRectangle.x, intersectionBottom - this.scaledImageVisibleRectangle.y
						, null);
				this.swapBuffers();
				
				this.scaledImageVisibleRectangle.setBounds(rectangle);
				
				final boolean multiThread = this.isMultiThread();
				
				this.setMultiThread(false);
				
				// Update top
				this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, intersection.y - rectangle.y);
				// Update left
				this.updateBuffer(rectangle.x, intersection.y, intersection.x - rectangle.x, intersection.height);
				// Update right
				this.updateBuffer(intersectionRight, intersection.y, rectangle.x + rectangle.width - intersectionRight, intersection.height);
				// Update bottom
				this.updateBuffer(rectangle.x, intersectionBottom, rectangle.width, rectangle.y + rectangle.height - intersectionBottom);
				
				this.setMultiThread(multiThread);
			}
		}
		
		if (this.frontBuffer.getWidth() < this.scaledImageVisibleRectangle.width || this.frontBuffer.getHeight() < this.scaledImageVisibleRectangle.getHeight()) {
			throw new IllegalStateException();
		}
	}
	
	private final void swapBuffers() {
		final BufferedImage tmpBuffer = this.frontBuffer;
		final Graphics2D tmpGraphics = this.frontBufferGraphics;
		this.frontBuffer = this.backBuffer;
		this.frontBufferGraphics = this.backBufferGraphics;
		this.backBuffer = tmpBuffer;
		this.backBufferGraphics = tmpGraphics;
	}
	
	private final int getScaledImageWidth() {
		return this.getScaledImage().getWidth();
	}
	
	private final int getScaledImageHeight() {
		return this.getScaledImage().getHeight();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 4189273248039238064L;
	
	public static final void show(final Image2D image) {
		final Component[] component = { null };
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				
				@Override
				public final void run() {
					component[0] = new Image2DComponent(image);
				}
				
			});
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		SwingTools.show(component[0], image.getId(), true);
	}
	
	/**
	 * @author codistmonk (creation 2013-08-12)
	 */
	public static final class ScaledImage2D extends TiledImage2D {
		
		private final Image2D source;
		
		private int zoom;
		
		public ScaledImage2D(final Image2D source) {
			super(source.getId());
			this.source = source;
			this.setZoom(1);
		}
		
		public final int getZoom() {
			return this.zoom;
		}
		
		public final void setZoom(final int zoom) {
			if (0 < zoom) {
				this.zoom = zoom;
				
				if (this.getSource() instanceof TiledImage2D) {
					this.setOptimalTileWidth(((TiledImage2D) this.getSource()).getOptimalTileWidth() * zoom);
					this.setOptimalTileHeight(((TiledImage2D) this.getSource()).getOptimalTileHeight() * zoom);
				} else {
					this.setOptimalTileWidth(this.getSource().getWidth() * zoom);
					this.setOptimalTileHeight(this.getSource().getHeight() * zoom);
				}
			}
		}
		
		public final Image2D getSource() {
			return this.source;
		}
		
		@Override
		public final int getWidth() {
			return this.getSource().getWidth() * this.getZoom();
		}
		
		@Override
		public final int getHeight() {
			return this.getSource().getHeight() * this.getZoom();
		}
		
		@Override
		public final Channels getChannels() {
			return this.getSource().getChannels();
		}
		
		@Override
		public final ScaledImage2D[] newParallelViews(final int n) {
			final ScaledImage2D[] result = new ScaledImage2D[n];
			
			result[0] = this;
			
			if (1 < n) {
				final Image2D[] sources = this.getSource().newParallelViews(n);
				
				for (int i = 1; i < n; ++i) {
					result[i] = new ScaledImage2D(sources[i]);
				}
			}
			
			return result;
		}
		
		@Override
		protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
			return this.getSource().getPixelValue(x / this.getZoom(), y / this.getZoom());
		}
		
		@Override
		protected final void setTilePixelValue(final int x, final int y, final int xInTile, final int yInTile,
				final int value) {
			this.getSource().setPixelValue(x / this.getZoom(), y / this.getZoom(), value);
		}
		
		@Override
		protected final boolean makeNewTile() {
			return false;
		}
		
		@Override
		protected final void updateTile() {
			// NOP
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -7082323074031564968L;
		
	}
	
}
