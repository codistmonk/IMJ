package imj2.tools;

import static java.lang.Math.max;
import static java.lang.Math.min;
import imj2.core.ConcreteImage2D;
import imj2.core.Image;
import imj2.core.Image.Channels;
import imj2.core.Image.PredefinedChannels;
import imj2.core.Image2D;
import imj2.core.LinearBooleanImage;
import imj2.core.LinearIntImage;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

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
