package imj3.draft.segmentation2;

import imj3.draft.machinelearning.ClassifierClass;
import imj3.draft.machinelearning.DataSource;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class ImageDataSource<C extends ClassifierClass> implements DataSource<C> {
	
	private final int patchSize;
	
	private final int patchSparsity;
	
	private final int stride;
	
	public ImageDataSource(final int patchSize) {
		this(patchSize, 1, 1);
	}
	
	public ImageDataSource(final int patchSize, final int patchSparsity, final int stride) {
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
	
	@Override
	public final int size() {
		return this.sizeX() * this.sizeY();
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
	
	private static final long serialVersionUID = -5424770105639516510L;
	
}