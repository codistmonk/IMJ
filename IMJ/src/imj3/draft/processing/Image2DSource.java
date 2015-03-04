package imj3.draft.processing;

import imj3.core.Image2D;
import imj3.draft.machinelearning.Datum;

import java.awt.Rectangle;
import java.util.Arrays;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public abstract class Image2DSource extends Patch2DSource {
	
	private final Image2D image;
	
	private final Rectangle bounds;
	
	protected Image2DSource(final Image2D image, final int patchSize,
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
	
	@Override
	public final PatchIterator iterator() {
		return this.new PatchIterator();
	}
	
	public final void extractPatchValues(final int x, final int y, final double[] result) {
		Arrays.fill(result, 0);
		
		final int s = this.getPatchSize();
		final int half = s / 2;
		final int x0 = x - half;
		final int x1 = x0 + s;
		final int y0 = y - half;
		final int y1 = y0 + s;
		final int step = this.getPatchSparsity();
		final int bufferWidth = s / step;
		final Image2D image = this.getImage();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int n = image.getChannels().getChannelCount();
		final double[] buffer = new double[n];
		
		for (int yy = y0, i = 0; yy < y1; yy += step) {
			if (0 <= yy && yy < imageHeight) {
				for (int xx = x0; xx < x1; xx += step, i += n) {
					if (0 <= xx && xx < imageWidth) {
						image.getPixelValue(xx, yy, buffer);
						
						System.arraycopy(buffer, 0, result, i, n);
					}
				}
			} else {
				i += bufferWidth * n;
			}
		}
	}
	
	protected abstract Object newContext();
	
	protected abstract Datum convert(int x, int y, double[] patchValues, Object context);
	
	/**
	 * @author codistmonk (creation 2015-03-03)
	 */
	public final class PatchIterator extends Iterator.Abstract<Iterator> {
		
		private final int stride = Image2DSource.this.getStride();
		
		private final int offset = this.stride / 2;
		
		private final Rectangle bounds = Image2DSource.this.getBounds();
		
		// bounds.x <= offset + k stride
		// <- (bounds.x - offset) / stride <= k
		// <- k = ceil((bounds.x - offset) / stride)
		private final int startX = this.offset + (this.bounds.x - this.offset + this.stride - 1) / this.stride * this.stride;
		
		private final int startY = this.offset + (this.bounds.y - this.offset + this.stride - 1) / this.stride * this.stride;
		
		private final int endX = this.bounds.x + this.bounds.width;
		
		private final int endY = this.bounds.y + this.bounds.height;
		
		private int x = this.startX;
		
		private int y = this.startY;
		
		private final double[] patchData = new double[Image2DSource.this.getPatchPixelCount() * Image2DSource.this.getImage().getChannels().getChannelCount()];
		
		private final Object context = Image2DSource.this.newContext();
		
		public final int getX() {
			return this.x;
		}
		
		public final int getY() {
			return this.y;
		}
		
		@Override
		public final boolean hasNext() {
			return this.y < this.endY && this.x < this.endX;
		}
		
		@Override
		public final Datum next() {
			Image2DSource.this.extractPatchValues(this.x, this.y, this.patchData);
			
			final Datum result = Image2DSource.this.convert(this.x, this.y, this.patchData, this.context);
			
			if (this.endX <= (this.x += this.stride)) {
				this.x = this.startX;
				this.y += this.stride;
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 4860177156580289698L;
		
	}
	
	private static final long serialVersionUID = -774979627942684978L;
	
}