package imj3.draft;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-07-16)
 */
public final class Patches {
	
	private Patches() {
		throw new IllegalInstantiationException();
	}
	
	public static final Iterable<Image2D> in(final Image2D image, final int patchStride, final int patchSize) {
		return new Iterable<Image2D>() {
			
			@Override
			public final Iterator<Image2D> iterator() {
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				final int halfStride = patchStride / 2;
				final int halfSize = patchSize / 2;
				
				return new Iterator<Image2D>() {
					
					private int x = halfStride, y = halfStride;
					
					@Override
					public final boolean hasNext() {
						return this.y < imageHeight;
					}
					
					@Override
					public final Image2D next() {
						final Image2D result = new SubImage2D(image, this.x - halfSize, this.y - halfSize, patchSize, patchSize);
						
						this.x += patchStride;
						
						if (imageWidth <= this.x) {
							this.x = halfStride;
							this.y += patchStride;
						}
						
						return result;
					}
					
				};
			}
			
		};
	}
	
	/**
	 * @author codistmonk (creation 2015-07-16)
	 */
	public static final class SubImage2D implements Image2D {
		
		private final Image2D source;
		
		private final int left, top, width, height;
		
		public SubImage2D(final Image2D source, final int left, final int top, final int width, final int height) {
			this.source = source;
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}
		
		public final Image2D getSource() {
			return this.source;
		}
		
		@Override
		public final BufferedImage toAwt() {
			final BufferedImage result = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			for (int y = 0; y < this.getHeight(); ++y) {
				for (int x = 0; x < this.getWidth(); ++x) {
					result.setRGB(x, y, (int) this.getPixelValue(x, y));
				}
			}
			
			return result;
		}
		
		@Override
		public final String getId() {
			return this.getSource().getId() + "[" + this.left + " " + this.top + " " + this.getWidth() + " " + this.getHeight() + "]";
		}
		
		@Override
		public final Channels getChannels() {
			return this.getSource().getChannels();
		}
		
		@Override
		public final int getWidth() {
			return this.width;
		}
		
		@Override
		public final int getHeight() {
			return this.height;
		}
		
		@Override
		public final Map<String, Object> getMetadata() {
			return null; // TODO
		}
		
		@Override
		public final long getPixelValue(final int x, final int y) {
			final int xInImage = this.left + x;
			final int yInImage = this.top + y;
			
			return 0 <= xInImage && xInImage < this.getSource().getWidth() &&
					0 <= yInImage && yInImage < this.getSource().getHeight() ?
							this.getSource().getPixelValue(xInImage, yInImage) : 0L;
		}
		
		private static final long serialVersionUID = 7109783632958311001L;
		
	}
	
}
