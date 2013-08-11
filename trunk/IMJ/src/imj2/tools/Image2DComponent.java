package imj2.tools;

import static imj2.tools.IMJTools.forEachPixelInRectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;

import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

/**
 * @author codistmonk (creation 2013-08-05)
 */
public final class Image2DComponent extends JComponent {
	
	private Image2D image;
	
	private BufferedImage buffer;
	
	private Graphics2D bufferGraphics;
	
	private final Rectangle imageVisibleRectangle;
	
	private final JScrollBar horizontalScrollBar;
	
	private final JScrollBar verticalScrollBar;
	
	public Image2DComponent() {
		this.imageVisibleRectangle = new Rectangle();
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
				Image2DComponent.this.updateBufferAccordingToScrollBars();
				Image2DComponent.this.repaint();
			}
			
		};
		
		this.horizontalScrollBar.addAdjustmentListener(bufferPositionAdjuster);
		this.verticalScrollBar.addAdjustmentListener(bufferPositionAdjuster);
		
		this.setBackground(Color.BLACK);
	}
	
	public Image2DComponent(final Image2D image) {
		this();
		this.image = image;
		this.horizontalScrollBar.setMaximum(image.getWidth());
		this.verticalScrollBar.setMaximum(image.getHeight());
		
		final Dimension preferredSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
		preferredSize.width = min(preferredSize.width, image.getWidth() + this.verticalScrollBar.getPreferredSize().width);
		preferredSize.height = min(preferredSize.height, image.getHeight() + this.horizontalScrollBar.getPreferredSize().height);
		
		this.setPreferredSize(preferredSize);
	}
	
	public final void setBuffer() {
		final int width = min(this.image.getWidth(), max(1, this.getUsableWidth()));
		final int height = min(this.image.getHeight(), max(1, this.getUsableHeight()));
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
					min(this.imageVisibleRectangle.x, this.image.getWidth() - width),
					min(this.imageVisibleRectangle.y, this.image.getHeight() - height),
					width, height), oldBuffer);
		}
	}
	
	private final void setImageVisibleRectangle(final Rectangle rectangle, final BufferedImage oldBuffer) {
		if (this.image.getWidth() < rectangle.x + rectangle.width || this.image.getHeight() < rectangle.y + rectangle.height ||
				this.buffer.getWidth() < rectangle.width || this.buffer.getHeight() < rectangle.height) {
			throw new IllegalArgumentException(rectangle + " " + new Rectangle(this.image.getWidth(), this.image.getHeight()) +
					" " + new Rectangle(this.buffer.getWidth(),  this.buffer.getHeight()));
		}
		
		if (oldBuffer == null) {
			this.imageVisibleRectangle.setBounds(rectangle);
			this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
		} else {
			final Rectangle intersection = this.imageVisibleRectangle.intersection(rectangle);
			
			if (intersection.isEmpty()) {
				this.imageVisibleRectangle.setBounds(rectangle);
				this.updateBuffer(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
			} else {
				final int intersectionRight = intersection.x + intersection.width;
				final int intersectionBottom = intersection.y + intersection.height;
				final int startX, endX, stepX, startY, endY, stepY;
				
				if (intersection.x - rectangle.x <= intersection.x - this.imageVisibleRectangle.x) {
					startX = intersection.x;
					endX = intersectionRight;
					stepX = 1;
				} else {
					startX = intersectionRight - 1;
					endX = intersection.x - 1;
					stepX = -1;
				}
				
				if (intersection.y - rectangle.y <= intersection.y - this.imageVisibleRectangle.y) {
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
								oldBuffer.getRGB(x - this.imageVisibleRectangle.x, y - this.imageVisibleRectangle.y));
					}
				}
				
				this.imageVisibleRectangle.setBounds(rectangle);
				
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
		
		if (this.buffer.getWidth() < this.imageVisibleRectangle.width || this.buffer.getHeight() < this.imageVisibleRectangle.getHeight()) {
			throw new IllegalStateException();
		}
	}
	
	public final void updateBuffer(final int left, final int top, final int width, final int height) {
		if (this.image != null && 0 < width && 0 < height) {
			this.copyImagePixelsToBuffer(top, left, width, height);
		}
	}
	
	@Override
	protected final void paintComponent(final Graphics g) {
		this.setBuffer();
		
		final int centeringOffsetX = max(0, (this.getUsableWidth() - this.buffer.getWidth()) / 2);
		final int centeringOffsetY = max(0, (this.getUsableHeight() - this.buffer.getHeight()) / 2);
		
		g.drawImage(this.buffer, centeringOffsetX, centeringOffsetY, null);
	}
	
	final void updateBufferAccordingToScrollBars() {
		if (this.buffer == null) {
			return;
		}
		
		final int width = min(this.image.getWidth(), this.buffer.getWidth());
		final int height = min(this.image.getHeight(), this.buffer.getHeight());
		final int left = width < this.image.getWidth() ? min(this.image.getWidth() - width, this.horizontalScrollBar.getValue()) : 0;
		final int top = height < this.image.getHeight() ? min(this.image.getHeight() - height, this.verticalScrollBar.getValue()) : 0;
		
		this.setImageVisibleRectangle(new Rectangle(left, top, width, height), this.buffer);
	}
	
	final void setScrollBarsVisibleAmounts() {
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
	
	final void copyImagePixelsToBuffer(final int top, final int left, final int width, final int height) {
		forEachPixelInRectangle(this.image, left, top, width, height, new MonopatchProcess() {
			
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
	
	final void copyImagePixelToBuffer(final int xInImage, final int yInImage) {
		this.buffer.setRGB(xInImage - this.imageVisibleRectangle.x, yInImage - this.imageVisibleRectangle.y,
				this.image.getPixelValue(xInImage, yInImage));
	}
	
	private final int getUsableHeight() {
		return this.getHeight() - this.horizontalScrollBar.getHeight();
	}
	
	private final int getUsableWidth() {
		return this.getWidth() - this.verticalScrollBar.getWidth();
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 4189273248039238064L;
	
}
