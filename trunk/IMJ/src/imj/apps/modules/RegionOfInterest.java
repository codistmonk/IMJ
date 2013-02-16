package imj.apps.modules;

import java.util.BitSet;

/**
 * @author codistmonk (creation 2013-02-15)
 */
public final class RegionOfInterest {
	
	private final int rowCount;
	
	private final int columnCount;
	
	private final BitSet data;
	
	public RegionOfInterest(final int rowCount, final int columnCount) {
		this.rowCount = rowCount;
		this.columnCount = columnCount;
		final int pixelCount = rowCount * columnCount;
		this.data = new BitSet(pixelCount);
		
		this.data.set(0, pixelCount);
	}
	
	public final int getRowCount() {
		return this.rowCount;
	}
	
	public final int getColumnCount() {
		return this.columnCount;
	}
	
	public final int getIndex(final int rowIndex, final int columnIndex) {
		return rowIndex * this.getColumnCount() + columnIndex;
	}
	
	public final boolean get(final int index) {
		return this.data.get(index);
	}
	
	public final void set(final int index) {
		this.data.set(index);
	}
	
	public final boolean get(final int rowIndex, final int columnIndex) {
		return this.data.get(this.getIndex(rowIndex, columnIndex));
	}
	
	public final void set(final int rowIndex, final int columnIndex) {
		this.data.set(this.getIndex(rowIndex, columnIndex));
	}
	
}
