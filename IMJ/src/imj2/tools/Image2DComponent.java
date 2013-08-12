package imj2.tools;

import static imj2.tools.IMJTools.forEachPixelInRectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
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
	
	private BufferedImage buffer;
	
	private Graphics2D bufferGraphics;
	
	private final Rectangle scaledImageVisibleRectangle;
	
	private final JScrollBar horizontalScrollBar;
	
	private final JScrollBar verticalScrollBar;
	
	public Image2DComponent() {
		this.scaledImageVisibleRectangle = new Rectangle();
		this.horizontalScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
		this.verticalScrollBar = new JScrollBar(Adjustable.VERTICAL);
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
				switch (event.getKeyChar()) {
				case '*':
					Image2DComponent.this.setZoom(Image2DComponent.this.getZoom() * 2);
					break;
				case '/':
					Image2DComponent.this.setZoom(Image2DComponent.this.getZoom() / 2);
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
				// newC = oldC * newM / oldM
				// newV + newA / 2 = (oldV + oldA / 2) * newM / oldM
				// newV = (oldV + oldA / 2) * newM / oldM - newA / 2
				// newV = ((2 * oldV + oldA) * newM - newA * oldM) / (2 * oldM)
				horizontalScrollBar.setValue((int) (((2L * oldHV + oldHA) * newHM - (long) newHA * oldHM) / (2L * oldHM)));
				verticalScrollBar.setValue((int) (((2L * oldVV + oldVA) * newVM - (long) newVA * oldVM) / (2L * oldVM)));
			}
			
		});
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
		return this.scaledImage.getZoom();
	}
	
	public final void setZoom(final int zoom) {
		if (0 < zoom && zoom != this.getZoom()) {
			this.scaledImage.setZoom(zoom);
			
			this.repaint();
		}
	}
	
	public final void setBuffer() {
		final int width = min(this.getScaledImageWidth(), max(1, this.getUsableWidth()));
		final int height = min(this.getScaledImageHeight(), max(1, this.getUsableHeight()));
		final boolean createBuffer;
		
		if (this.buffer == null) {
			createBuffer = true;
		} else if (this.buffer.getWidth() != width || this.buffer.getHeight() != height) {
			this.bufferGraphics.dispose();
			this.bufferGraphics = null;
			createBuffer = true;
		} else {
			createBuffer = false;
		}
		
		if (createBuffer) {
			final BufferedImage oldBuffer = this.buffer;
			this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			this.bufferGraphics = this.buffer.createGraphics();
			
			this.setImageVisibleRectangle(new Rectangle(
					min(this.scaledImageVisibleRectangle.x, this.getScaledImageWidth() - width),
					min(this.scaledImageVisibleRectangle.y, this.getScaledImageHeight() - height),
					width, height), oldBuffer);
		}
	}
	
	public final void updateBuffer(final int left, final int top, final int width, final int height) {
		if (this.scaledImage != null && 0 < width && 0 < height) {
			this.copyImagePixelsToBuffer(left, top, width, height);
		}
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		this.setBuffer();
		
		final int centeringOffsetX = max(0, (this.getUsableWidth() - this.buffer.getWidth()) / 2);
		final int centeringOffsetY = max(0, (this.getUsableHeight() - this.buffer.getHeight()) / 2);
		
		g.drawImage(this.buffer, centeringOffsetX, centeringOffsetY, null);
	}
	
	final JScrollBar getHorizontalScrollBar() {
		return this.horizontalScrollBar;
	}
	
	final JScrollBar getVerticalScrollBar() {
		return this.verticalScrollBar;
	}
	
	final void updateBufferAccordingToScrollBars(final boolean forceRepaint) {
		if (this.buffer == null) {
			return;
		}
		
		final int width = min(this.getScaledImageWidth(), this.buffer.getWidth());
		final int height = min(this.getScaledImageHeight(), this.buffer.getHeight());
		final int left = width < this.getScaledImageWidth() ? min(this.getScaledImageWidth() - width, this.horizontalScrollBar.getValue()) : 0;
		final int top = height < this.getScaledImageHeight() ? min(this.getScaledImageHeight() - height, this.verticalScrollBar.getValue()) : 0;
		
		this.setImageVisibleRectangle(new Rectangle(left, top, width, height), forceRepaint ? null : this.buffer);
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
		forEachPixelInRectangle(this.scaledImage, left, top, width, height, new MonopatchProcess() {
			
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
	
	final void copyImagePixelToBuffer(final int xInScaledImage, final int yInScaledImage) {
		try {
			this.buffer.setRGB(xInScaledImage - this.scaledImageVisibleRectangle.x, yInScaledImage - this.scaledImageVisibleRectangle.y,
					this.scaledImage.getPixelValue(xInScaledImage, yInScaledImage));
		} catch (final Exception exception) {
			exception.printStackTrace();
			debugPrint(xInScaledImage, yInScaledImage, xInScaledImage - this.scaledImageVisibleRectangle.x, yInScaledImage - this.scaledImageVisibleRectangle.y);
			System.exit(-1);
		}
	}
	
	private final int getUsableHeight() {
		return this.getHeight() - this.horizontalScrollBar.getHeight();
	}
	
	private final int getUsableWidth() {
		return this.getWidth() - this.verticalScrollBar.getWidth();
	}
	
	private final void setImageVisibleRectangle(final Rectangle rectangle, final BufferedImage oldBuffer) {
		if (this.getScaledImageWidth() < rectangle.x + rectangle.width || this.getScaledImageHeight() < rectangle.y + rectangle.height ||
				this.buffer.getWidth() < rectangle.width || this.buffer.getHeight() < rectangle.height) {
			throw new IllegalArgumentException(rectangle + " " + new Rectangle(this.getScaledImageWidth(), this.getScaledImageHeight()) +
					" " + new Rectangle(this.buffer.getWidth(),  this.buffer.getHeight()));
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
				final int startX, endX, stepX, startY, endY, stepY;
				
				if (intersection.x - rectangle.x <= intersection.x - this.scaledImageVisibleRectangle.x) {
					startX = intersection.x;
					endX = intersectionRight;
					stepX = 1;
				} else {
					startX = intersectionRight - 1;
					endX = intersection.x - 1;
					stepX = -1;
				}
				
				if (intersection.y - rectangle.y <= intersection.y - this.scaledImageVisibleRectangle.y) {
					startY = intersection.y;
					endY = intersectionBottom;
					stepY = 1;
				} else {
					startY = intersectionBottom - 1;
					endY = intersection.y - 1;
					stepY = -1;
				}
				
				for (int y = startY; y != endY; y += stepY) {
					for (int x = startX; x != endX; x += stepX) {
						this.buffer.setRGB(x - rectangle.x, y - rectangle.y,
								oldBuffer.getRGB(x - this.scaledImageVisibleRectangle.x, y - this.scaledImageVisibleRectangle.y));
					}
				}
				
				this.scaledImageVisibleRectangle.setBounds(rectangle);
				
				// Update top
				this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, intersection.y - rectangle.y);
				// Update left
				this.updateBuffer(rectangle.x, intersection.y, intersection.x - rectangle.x, intersection.height);
				// Update right
				this.updateBuffer(intersectionRight, intersection.y, rectangle.x + rectangle.width - intersectionRight, intersection.height);
				// Update bottom
				this.updateBuffer(rectangle.x, intersectionBottom, rectangle.width, rectangle.y + rectangle.height - intersectionBottom);
			}
		}
		
		if (this.buffer.getWidth() < this.scaledImageVisibleRectangle.width || this.buffer.getHeight() < this.scaledImageVisibleRectangle.getHeight()) {
			throw new IllegalStateException();
		}
	}
	
	private final int getScaledImageWidth() {
		return this.scaledImage.getWidth();// * this.getZoom();
	}
	
	private final int getScaledImageHeight() {
		return this.scaledImage.getHeight();// * this.getZoom();
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
	public static final class ScaledImage2D extends TiledImage {
		
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
				
				if (this.getSource() instanceof TiledImage) {
					this.setOptimalTileWidth(((TiledImage) this.getSource()).getOptimalTileWidth() * zoom);
					this.setOptimalTileHeight(((TiledImage) this.getSource()).getOptimalTileHeight() * zoom);
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
		protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
			return this.getSource().getPixelValue(x / this.getZoom(), y / this.getZoom());
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
