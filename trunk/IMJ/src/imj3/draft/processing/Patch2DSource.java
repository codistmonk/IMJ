package imj3.draft.processing;

import java.awt.Rectangle;

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
		return newSize(this.getBounds().x, this.getBounds().width, this.getPatchSize(), this.getStride());
	}
	
	public final int sizeY() {
		return newSize(this.getBounds().y, this.getBounds().height, this.getPatchSize(), this.getStride());
	}
	
	public final int getPatchPixelCount() {
		final int n = this.getPatchSize() / this.getPatchSparsity();
		
		return n * n;
	}
	
	public abstract Rectangle getBounds();
	
	@Override
	public final int size() {
		return this.sizeX() * this.sizeY();
	}
	
	private static final long serialVersionUID = -5424770105639516510L;
	
	public static final int newSize(final int boundary, final int size, final int patchSize, final int stride) {
		final int offset = stride / 2;
		// x = offset + k1 * stride
		// boundary <= x  <--  boundary <= offset + k1 * stride
		//                <--  k1 = ceiling((boundary - offset) / stride)
		//
		// x + k2 * stride < boundary + size  <--  k2 = ceiling((boundary + size - x) / stride) - 1
		// result = k2 + 1
		final int k1 = ceiling(boundary - offset, stride);
		final int x = offset + k1 * stride;
		
		return ceiling(boundary + size - x, stride);
	}
	
	public static final int ceiling(final int numerator, final int denominator) {
		return (numerator + denominator - 1) / denominator;
	}
	
}