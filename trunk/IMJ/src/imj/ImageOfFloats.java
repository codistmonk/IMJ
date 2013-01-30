package imj;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class ImageOfFloats extends Image.Abstract {
	
	private final float[] data;
	
	public ImageOfFloats(final int rowCount, final int columnCount) {
		super(rowCount, columnCount);
		this.data = new float[rowCount * columnCount];
	}
	
	@Override
	public final int getValue(final int index) {
		return (int) this.getFloatValue(index);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		return (int) this.setFloatValue(index, (int) value);
	}
	
	@Override
	public final float getFloatValue(final int index) {
		return this.data[index];
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		final float oldValue = this.data[index];
		
		this.data[index] = value;
		
		return oldValue;
	}
	
}

	