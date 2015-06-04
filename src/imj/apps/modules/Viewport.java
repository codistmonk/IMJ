package imj.apps.modules;

import imj.Image;

import java.awt.Rectangle;

/**
 * @author codistmonk (creation 2013-04-11)
 */
public final class Viewport implements Image {
	
	private final Image source;
	
	private final Rectangle viewport;
	
	public Viewport(final Image source, final Rectangle viewport) {
		this.source = source;
		this.viewport = viewport;
	}
	
	public final Image getSource() {
		return this.source;
	}
	
	public final Rectangle getViewport() {
		return this.viewport;
	}
	
	public final int convertRowIndex(final int rowIndexInSource) {
		return rowIndexInSource - this.getViewport().y;
	}
	
	public final int convertColumnIndex(final int columnIndexInSource) {
		return columnIndexInSource - this.getViewport().x;
	}
	
	public final int convertIndex(final int indexInSource) {
		final int sourceColumnCount = this.getSource().getColumnCount();
		final int rowIndexInSource = indexInSource / sourceColumnCount;
		final int columnIndexInSource = indexInSource % sourceColumnCount;
		
		return this.convertRowIndex(rowIndexInSource) * this.getColumnCount() + this.convertColumnIndex(columnIndexInSource);
	}
	
	@Override
	public final int getDimensionCount() {
		return this.getSource().getDimensionCount();
	}
	
	@Override
	public final int getDimension(final int dimensionIndex) {
		return this.getSource().getDimension(dimensionIndex);
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
	public final int getRowCount() {
		return this.getViewport().height;
	}
	
	@Override
	public final int getColumnCount() {
		return this.getViewport().width;
	}
	
	@Override
	public final int getValue(final int rowIndex, final int columnIndex) {
		return this.getSource().getValue(this.getViewport().y + rowIndex, this.getViewport().x + columnIndex);
	}
	
	@Override
	public final int setValue(final int rowIndex, final int columnIndex, final int value) {
		return this.getSource().setValue(this.getViewport().y + rowIndex, this.getViewport().x + columnIndex, value);
	}
	
	@Override
	public final int getValue(final int index) {
		final int columnCount = this.getColumnCount();
		final int rowIndex = index / columnCount;
		final int columnIndex = index % columnCount;
		
		return this.getValue(rowIndex, columnIndex);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int columnCount = this.getColumnCount();
		final int rowIndex = index / columnCount;
		final int columnIndex = index % columnCount;
		
		return this.setValue(rowIndex, columnIndex, value);
	}
	
}
