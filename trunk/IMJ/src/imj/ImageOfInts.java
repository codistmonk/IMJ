package imj;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class ImageOfInts extends Image.Abstract {
	
	private final int[] data;
	
	public ImageOfInts(final int rowCount, final int columnCount) {
		super(rowCount, columnCount);
		this.data = new int[rowCount * columnCount];
	}
	
	@Override
	public final int getValue(final int index) {
		return this.data[index];
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.data[index];
		
		this.data[index] = value;
		
		return oldValue;
	}
	
}

	