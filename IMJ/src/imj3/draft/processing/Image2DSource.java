package imj3.draft.processing;

import imj3.core.Image2D;
import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.ClassifierClass;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class Image2DSource<M extends Image2DSource.Metadata, C extends ClassifierClass> extends Patch2DSource<M, C> {
	
	protected Image2DSource(final M metadata) {
		super(metadata);
	}
	
	@Override
	public final Iterator<Classification<C>> iterator() {
		final int stride = this.getMetadata().getStride();
		final int offset = stride / 2;
		final Rectangle bounds = this.getMetadata().getBounds();
		// bounds.x <= offset + k stride
		// <- (bounds.x - offset) / stride <= k
		// <- k = ceil((bounds.x - offset) / stride)
		final int startX = offset + (bounds.x - offset + stride - 1) / stride * stride;
		final int startY = offset + (bounds.y - offset + stride - 1) / stride * stride;
		final int endX = bounds.x + bounds.width;
		final int endY = bounds.y + bounds.height;
		
		return new Iterator<Classification<C>>() {
			
			private int x = startX;
			
			private int y = startY;
			
			private final int[] patchData = new int[Image2DSource.this.getMetadata().getPatchPixelCount()];
			
			private final Object context = Image2DSource.this.newContext();
			
			@Override
			public final boolean hasNext() {
				return this.y < endY && this.x < endX;
			}
			
			@Override
			public final Classification<C> next() {
				Image2DSource.this.extractPatchValues(this.x, this.y, this.patchData);
				
				final Classification<C> result = Image2DSource.this.convert(this.x, this.y, this.patchData, this.context);
				
				if (endX <= (this.x += stride)) {
					this.x = startX;
					this.y += stride;
				}
				
				return result;
			}
			
		};
	}
	
	public final void extractPatchValues(final int x, final int y, final int[] result) {
		Arrays.fill(result, 0);
		final int s = this.getMetadata().getPatchSize();
		final int half = s / 2;
		final int x0 = x - half;
		final int x1 = x0 + s;
		final int y0 = y - half;
		final int y1 = y0 + s;
		final int step = this.getMetadata().getPatchSparsity();
		final int bufferWidth = s / step;
		final Image2D image = this.getMetadata().getImage();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		for (int yy = y0, i = 0; yy < y1; yy += step) {
			if (0 <= yy && yy < imageHeight) {
				for (int xx = x0; xx < x1; xx += step, ++i) {
					if (0 <= xx && xx < imageWidth) {
						result[i] = (int) image.getPixelValue(xx, yy);
					}
				}
			} else {
				i += bufferWidth;
			}
		}
	}
	
	protected abstract Object newContext();
	
	protected abstract Classification<C> convert(int x, int y, int[] patchValues, Object context);
	
	private static final long serialVersionUID = -774979627942684978L;
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static abstract class Metadata extends Patch2DSource.Metadata {
		
		private final Image2D image;
		
		private final Rectangle bounds;
		
		public Metadata(final Image2D image, final int patchSize,
				final int patchSparsity, final int stride) {
			super(patchSize, patchSparsity, stride);
			this.image = image;
			this.bounds = new Rectangle(image.getWidth(), image.getHeight());
		}
		
		public final Image2D getImage() {
			return this.image;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		@Override
		public final int getBoundsWidth() {
			return this.getBounds().width;
		}
		
		@Override
		public final int getBoundsHeight() {
			return this.getBounds().height;
		}
		
		private static final long serialVersionUID = 5722451664995251006L;
		
		/**
		 * @author codistmonk (creation 2015-02-08)
		 */
		public static final class Default extends Metadata {
			
			public Default(final Image2D image, final int patchSize,
					final int patchSparsity, final int stride) {
				super(image, patchSize, patchSparsity, stride);
			}
			
			private static final long serialVersionUID = 69449219027263879L;
			
		}
		
	}
	
}