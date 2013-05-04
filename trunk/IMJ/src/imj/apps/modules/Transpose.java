package imj.apps.modules;

import imj.Image;

/**
 * @author codistmonk (creation 2013-05-02)
 */
public final class Transpose implements Image {
	
	private final Image source;
	
	public Transpose(final Image source) {
		this.source = source;
	}
	
	public final Image getSource() {
		return this.source;
	}
	
	@Override
	public final int getPixelCount() {
		return this.getSource().getPixelCount();
	}
	
	@Override
	public final int getChannelCount() {
		return this.getSource().getChannelCount();
	}
	
	@Override
	public final int getDimensionCount() {
		return this.getSource().getDimensionCount();
	}
	
	@Override
	public final int getDimension(final int dimensionIndex) {
		return this.getSource().getDimension(dimensionIndex <= 1 ? dimensionIndex ^ 1 : dimensionIndex);
	}
	
	@Override
	public final int getRowCount() {
		return this.getSource().getColumnCount();
	}
	
	@Override
	public final int getColumnCount() {
		return this.getSource().getRowCount();
	}
	
	@Override
	public final int getValue(final int rowIndex, final int columnIndex) {
		return this.getSource().getValue(columnIndex, rowIndex);
	}
	
	@Override
	public final int setValue(final int rowIndex, final int columnIndex, final int value) {
		return this.getSource().setValue(columnIndex, rowIndex, value);
	}
	
	@Override
	public final int getValue(final int index) {
		final int sourceColumnCount = this.getSource().getColumnCount();
		final int rowIndexInSource = index / sourceColumnCount;
		final int columnIndexInSource = index % sourceColumnCount;
		
		return this.getValue(columnIndexInSource, rowIndexInSource);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int columnCount = this.getColumnCount();
		
		return this.setValue(index / columnCount, index % columnCount, value);
	}
	
}
