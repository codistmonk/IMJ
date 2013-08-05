package imj2.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import imj2.core.ConcreteImage2D;
import imj2.core.Image;
import imj2.core.Image.PredefinedChannels;
import imj2.core.Image2D;
import imj2.core.LinearBooleanImage;
import imj2.core.LinearIntImage;
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
	
	public static final ConcreteImage2D newImage(final String imageId, final BufferedImage awtImage) {
		final int width = awtImage.getWidth();
		final int height = awtImage.getHeight();
		final long pixelCount = (long) width * height;
		final Image source;
		
		switch (awtImage.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
			source = new LinearBooleanImage(imageId, pixelCount);
			break;
		case BufferedImage.TYPE_USHORT_GRAY:
		case BufferedImage.TYPE_BYTE_GRAY:
			source = new LinearIntImage(imageId, pixelCount, PredefinedChannels.C1);
			break;
		default:
			source = new LinearIntImage(imageId, pixelCount, PredefinedChannels.C4);
			break;
		}
		
		{
			long pixelIndex = 0L;
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x, ++pixelIndex) {
					source.setPixelValue(pixelIndex, awtImage.getRGB(x, y));
				}
			}
		}
		
		return new ConcreteImage2D(source, width, height);
	}
	
	public static final void show(final Image2D image) {
		SwingTools.show(new Image2DComponent(image), image.getId(), true);
	}
	
	/**
	 * @author codistmonk (creation 2013-08-05)
	 */
	public static final class Image2DComponent extends JComponent {
		
		private Image2D image;
		
		private BufferedImage buffer;
		
		private Graphics2D bufferGraphics;
		
		public Image2DComponent(final Image2D image) {
			this();
			this.image = image;
		}
		
		public Image2DComponent() {
			this.setDoubleBuffered(false);
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			this.setBuffer();
			
			g.drawImage(this.buffer, 0, 0, null);
		}
		
		@Override
		public final void paintImmediately(final int x, final int y, final int w, final int h) {
			Tools.debugPrint(x, y, w, h);
			super.paintImmediately(x, y, w, h);
		}

		@Override
		public final void paintImmediately(final Rectangle r) {
			Tools.debugPrint(r);
			super.paintImmediately(r);
		}

		public final void setBuffer() {
			final int width = max(1, this.getWidth());
			final int height = max(1, this.getHeight());
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
			
			final int xEnd = min(this.image.getWidth(), left + width);
			final int yEnd = min(this.image.getHeight(), top + height);
			
			for (int y = top; y < yEnd; ++y) {
				for (int x = left; x < xEnd; ++x) {
					this.buffer.setRGB(x, y, this.image.getPixelValue(x, y));
				}
			}
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4189273248039238064L;
		
	}
	
}
