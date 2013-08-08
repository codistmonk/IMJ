package imj2.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj2.core.Image.Channels;
import imj2.core.Image.PredefinedChannels;
import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;

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

import javax.swing.JComponent;
import javax.swing.JScrollBar;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-08-04)
 */
public final class IMJTools {
	
	private IMJTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final int quantize(final int value, final int quantum) {
		return (value / quantum) * quantum;
	}
	
	public static final BufferedImage awtImage(final Image2D image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		final BufferedImage result = new BufferedImage(width, height, awtImageTypeFor(image));
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				result.setRGB(x, y, image.getPixelValue(x, y));
			}
		}
		
		return result;
	}
	
	public static final int awtImageTypeFor(final Image2D image) {
		switch (image.getChannels().getChannelCount()) {
		case 1:
			switch (image.getChannels().getChannelBitCount()) {
			case 1:
				return BufferedImage.TYPE_BYTE_BINARY;
			case 8:
				return BufferedImage.TYPE_BYTE_GRAY;
			case 16:
				return BufferedImage.TYPE_USHORT_GRAY;
			default:
				throw new IllegalArgumentException();
			}
		case 2:
			throw new IllegalArgumentException();
		case 3:
			return BufferedImage.TYPE_3BYTE_BGR;
		case 4:
			return BufferedImage.TYPE_INT_ARGB;
		default:
			throw new IllegalArgumentException();
		}
	}
		
	public static final Channels predefinedChannelsFor(final BufferedImage awtImage) {
		switch (awtImage.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
			return 1 == awtImage.getColorModel().getPixelSize() ?
					PredefinedChannels.C1_U1 : PredefinedChannels.C3_U8;
		case BufferedImage.TYPE_USHORT_GRAY:
			return PredefinedChannels.C1_U16;
		case BufferedImage.TYPE_BYTE_GRAY:
			return PredefinedChannels.C1_U8;
		case BufferedImage.TYPE_3BYTE_BGR:
			return PredefinedChannels.C3_U8;
		default:
			return PredefinedChannels.C4_U8;
		}
	}
	
	public static final Channels predefinedChannelsFor(final IFormatReader lociImage) {
		if (lociImage.isIndexed()) {
			return PredefinedChannels.C3_U8;
		}
		
		switch (lociImage.getRGBChannelCount()) {
		case 1:
			switch (FormatTools.getBytesPerPixel(lociImage.getPixelType()) * lociImage.getRGBChannelCount()) {
			case 1:
				return 1 == lociImage.getBitsPerPixel() ?
						PredefinedChannels.C1_U1 : PredefinedChannels.C1_U8;
			case 2:
				return PredefinedChannels.C1_U16;
			default:
				return PredefinedChannels.C1_S32;
			}
		case 2:
			return PredefinedChannels.C2_U16;
		case 3:
			return PredefinedChannels.C3_U8;
		case 4:
			return PredefinedChannels.C4_U8;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public static final void show(final Image2D image) {
		final Image2DComponent imageComponent = new Image2DComponent(image);
		final Dimension preferredSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
		
		preferredSize.width = min(preferredSize.width, image.getWidth());
		preferredSize.height = min(preferredSize.height, image.getHeight());
		imageComponent.setPreferredSize(preferredSize);
		
		SwingTools.show(imageComponent, image.getId(), true);
	}
	
	/**
	 * @author codistmonk (creation 2013-08-05)
	 */
	public static final class Image2DComponent extends JComponent {
		
		private Image2D image;
		
		private BufferedImage buffer;
		
		private Graphics2D bufferGraphics;
		
		private int bufferX;
		
		private int bufferY;
		
		private final JScrollBar horizontalScrollBar;
		
		private final JScrollBar verticalScrollBar;
		
		public Image2DComponent() {
			this.horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
			this.verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
			this.setDoubleBuffered(false);
			this.setLayout(new BorderLayout());
			this.add(this.horizontalScrollBar, BorderLayout.SOUTH);
			this.add(this.verticalScrollBar, BorderLayout.EAST);
			
			this.addComponentListener(new ComponentAdapter() {
				
				@Override
				public final void componentResized(final ComponentEvent event) {
					Image2DComponent.this.setScrollBarVisibleAmounts();
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
		}
		
		public final void setBuffer() {
			final int width = min(this.image.getWidth(), max(1, this.getWidth() - this.verticalScrollBar.getWidth()));
			final int height = min(this.image.getHeight(), max(1, this.getHeight() - this.horizontalScrollBar.getHeight()));
			final boolean createBuffer;
			
			if (this.buffer == null) {
				createBuffer = true;
			} else if (this.buffer.getWidth() != width || this.buffer.getHeight() != height) {
				this.bufferGraphics.dispose();
				createBuffer = true;
			} else {
				createBuffer = false;
			}
			
			if (createBuffer) {
				final BufferedImage oldBuffer = this.buffer;
				this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				this.bufferGraphics = this.buffer.createGraphics();
				
				if (oldBuffer != null) {
					this.bufferGraphics.drawImage(oldBuffer, 0, 0, null);
					
					final int oldWidth = oldBuffer.getWidth();
					final int oldHeight = oldBuffer.getHeight();
					
					this.update(oldWidth, 0, width - oldWidth, min(oldHeight, height));
					this.update(0, oldHeight, width, height - oldHeight);
				} else {
					this.update(0, 0, width, height);
				}
			}
		}
		
		public final void update(final int left, final int top, final int width, final int height) {
			if (this.image == null || width <= 0 || height <= 0) {
				return;
			}
			
			if (this.image.getWidth() < left + width + this.bufferX) {
				this.bufferX = this.image.getWidth() - left - width;
			}
			
			if (this.image.getHeight() < top + height + this.bufferY) {
				this.bufferY = this.image.getHeight() - top - height;
			}
			
			if (this.image instanceof TiledImage) {
				((TiledImage) this.image).forEachPixelInRectangle(left, top, width, height, new MonopatchProcess() {
					
					@Override
					public final void pixel(final int x, final int y) {
						Image2DComponent.this.copyImagePixelToBuffer(x, y);
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 1810623847473680066L;
					
				});
			} else {
				final int xEnd = min(this.image.getWidth(), left + width);
				final int yEnd = min(this.image.getHeight(), top + height);
				
				for (int y = top; y < yEnd; ++y) {
					for (int x = left; x < xEnd; ++x) {
						this.copyImagePixelToBuffer(x, y);
					}
				}
			}
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			this.setBuffer();
			this.updateBufferAccordingToScrollBars();
			
			final int dx = max(0, (this.getWidth() - this.verticalScrollBar.getWidth() - this.buffer.getWidth()) / 2);
			final int dy = max(0, (this.getHeight() - this.horizontalScrollBar.getHeight() - this.buffer.getHeight()) / 2);
			
			g.drawImage(this.buffer, dx, dy, null);
		}
		
		final void copyImagePixelToBuffer(final int x, final int y) {
			this.buffer.setRGB(x, y, this.image.getPixelValue(x + this.bufferX, y + this.bufferY));
		}
		
		final void updateBufferAccordingToScrollBars() {
			if (this.buffer == null) {
				return;
			}
			
//			debugPrint(this.horizontalScrollBar.getVisibleAmount(), this.horizontalScrollBar.getMaximum(), this.horizontalScrollBar.getValue(), this.getWidth(), this.image.getWidth(), this.buffer.getWidth());
			
			final int dx = this.horizontalScrollBar.getValue() - this.bufferX;
			final int dy = this.verticalScrollBar.getValue() - this.bufferY;
			this.bufferX += dx;
			this.bufferY += dy;
			final int sourceX, sourceY, targetX, targetY, stepX, stepY;
			
			if (0 <= dx) {
				sourceX = dx;
				targetX = 0;
				stepX = 1;
			} else {
				sourceX = this.buffer.getWidth() + dx;
				targetX = this.buffer.getWidth() - 1;
				stepX = -1;
			}
			
			if (0 <= dy) {
				sourceY = dy;
				targetY = 0;
				stepY = 1;
			} else {
				sourceY = this.buffer.getHeight() + dy;
				targetY = this.buffer.getHeight() - 1;
				stepY = -1;
			}
			
			for (int y0 = sourceY, y1 = targetY; 0 <= y0 && y0 < this.buffer.getHeight(); y0 += stepY, y1 += stepY) {
				for (int x0 = sourceX, x1 = targetX; 0 <= x0 && x0 < this.buffer.getWidth(); x0 += stepX, x1 += stepX) {
					this.buffer.setRGB(x1, y1, this.buffer.getRGB(x0, y0));
				}
			}
			
			if (0 <= dx) {
				if (0 <= dy) {
					this.update(this.buffer.getWidth() - dx, 0, dx, this.buffer.getHeight() - dy);
				} else {
					this.update(this.buffer.getWidth() - dx, -dy, dx, this.buffer.getHeight() + dy);
				}
			} else {
				if (0 <= dy) {
					this.update(0, 0, -dx, this.buffer.getHeight() - dy);
				} else {
					this.update(0, -dy, -dx, this.buffer.getHeight() + dy);
				}
			}
		}
		
		final void setScrollBarVisibleAmounts() {
			final int usableWidth = max(0, this.getWidth() - this.verticalScrollBar.getWidth());
			final int usableHeight = max(0, this.getHeight() - this.horizontalScrollBar.getHeight());
			
			if (this.horizontalScrollBar.getMaximum() <= this.horizontalScrollBar.getValue() + usableWidth) {
				this.horizontalScrollBar.setValue(max(0, this.horizontalScrollBar.getMaximum() - usableWidth));
			}
			
			this.horizontalScrollBar.setVisibleAmount(usableWidth);
			
			if (this.verticalScrollBar.getMaximum() <= this.verticalScrollBar.getValue() + usableHeight) {
				this.verticalScrollBar.setValue(max(0, this.verticalScrollBar.getMaximum() - usableHeight));
			}
			
			this.verticalScrollBar.setVisibleAmount(usableHeight);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4189273248039238064L;
		
	}
	
}
