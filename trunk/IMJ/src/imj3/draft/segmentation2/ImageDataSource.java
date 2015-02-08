package imj3.draft.segmentation2;

import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class ImageDataSource<M extends ImageDataSource.Metadata, C extends ClassifierClass> extends DataSource.Abstract<M, C> {
	
	public ImageDataSource(final M metadata) {
		super(metadata);
	}
	
	@Override
	public final int size() {
		return this.getMetadata().sizeX() * this.getMetadata().sizeY();
	}
	
	private static final long serialVersionUID = -5424770105639516510L;
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static abstract class Metadata implements DataSource.Metadata {
		
		private final int patchSize;
		
		private final int patchSparsity;
		
		private final int stride;
		
		protected Metadata(final int patchSize, final int patchSparsity, final int stride) {
			this.patchSize = patchSize;
			this.patchSparsity = patchSparsity;
			this.stride = stride;
		}
		
		public final int getPatchSize() {
			return this.patchSize;
		}
		
		public final int getPatchSparsity() {
			return this.patchSparsity;
		}
		
		public final int getStride() {
			return this.stride;
		}
		
		public final int sizeX() {
			return this.getImageWidth() / this.getStride();
		}
		
		public final int sizeY() {
			return this.getImageHeight() / this.getStride();
		}
		
		public final int getPatchPixelCount() {
			final int n = this.getPatchSize() / this.getPatchSparsity();
			
			return n * n;
		}
		
		public abstract int getImageWidth();
		
		public abstract int getImageHeight();
		
		private static final long serialVersionUID = -6730325302037718679L;
		
	}
	
}