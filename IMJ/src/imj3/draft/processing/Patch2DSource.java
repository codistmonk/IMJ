package imj3.draft.processing;

import imj3.draft.machinelearning.DataSource;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class Patch2DSource extends DataSource.Abstract<DataSource> {
	
	private final int patchSize;
	
	private final int patchSparsity;
	
	private final int stride;
	
	protected Patch2DSource(final int patchSize, final int patchSparsity, final int stride) {
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
		return this.getBoundsWidth() / this.getStride();
	}
	
	public final int sizeY() {
		return this.getBoundsHeight() / this.getStride();
	}
	
	public final int getPatchPixelCount() {
		final int n = this.getPatchSize() / this.getPatchSparsity();
		
		return n * n;
	}
	
	public abstract int getBoundsWidth();
	
	public abstract int getBoundsHeight();
	
	@Override
	public final int size() {
		return this.sizeX() * this.sizeY();
	}
	
	private static final long serialVersionUID = -5424770105639516510L;
	
}