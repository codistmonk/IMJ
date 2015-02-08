package imj3.draft.segmentation2;

import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.ClassifierClass;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class BufferedImageDataSource<M extends BufferedImageDataSource.Metadata, C extends ClassifierClass> extends ImageDataSource<M, C> {
	
	protected BufferedImageDataSource(final M metadata) {
		super(metadata);
	}
	
	@Override
	public final Iterator<Classification<C>> iterator() {
		final int imageWidth = this.getMetadata().getImageWidth();
		final int imageHeight = this.getMetadata().getImageHeight();
		final int stride = this.getMetadata().getStride();
		final int offset = stride / 2;
		
		return new Iterator<Classification<C>>() {
			
			private int x = offset;
			
			private int y = this.x;
			
			private final int[] patchData = new int[BufferedImageDataSource.this.getMetadata().getPatchPixelCount()];
			
			private final Object context = BufferedImageDataSource.this.newContext();
			
			@Override
			public final boolean hasNext() {
				return this.y < imageHeight && this.x < imageWidth;
			}
			
			@Override
			public final Classification<C> next() {
				BufferedImageDataSource.this.extractPatchValues(this.x, this.y, this.patchData);
				
				final Classification<C> result = BufferedImageDataSource.this.convert(this.x, this.y, this.patchData, this.context);
				
				if (imageWidth <= (this.x += stride)) {
					this.x = offset;
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
		final BufferedImage image = this.getMetadata().getImage();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		for (int yy = y0, i = 0; yy < y1; yy += step) {
			if (0 <= yy && yy < imageHeight) {
				for (int xx = x0; xx < x1; xx += step, ++i) {
					if (0 <= xx && xx < imageWidth) {
						result[i] = image.getRGB(xx, yy);
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
	public static abstract class Metadata extends ImageDataSource.Metadata {
		
		private final BufferedImage image;
		
		public Metadata(final BufferedImage image, final int patchSize,
				final int patchSparsity, final int stride) {
			super(patchSize, patchSparsity, stride);
			this.image = image;
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		@Override
		public final int getImageWidth() {
			return this.getImage().getWidth();
		}
		
		@Override
		public final int getImageHeight() {
			return this.getImage().getHeight();
		}
		
		private static final long serialVersionUID = 5722451664995251006L;
		
		/**
		 * @author codistmonk (creation 2015-02-08)
		 */
		public static final class Default extends Metadata {
			
			public Default(final BufferedImage image, final int patchSize,
					final int patchSparsity, final int stride) {
				super(image, patchSize, patchSparsity, stride);
			}
			
			private static final long serialVersionUID = 69449219027263879L;
			
		}
		
	}
	
}