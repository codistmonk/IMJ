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
	
	public final void reset() {
		this.data.set(0, this.data.size());
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
	
	public final void set(final int index, final boolean value) {
		this.data.set(index, value);
	}
	
	public final boolean get(final int rowIndex, final int columnIndex) {
		return this.data.get(this.getIndex(rowIndex, columnIndex));
	}
	
	public final void set(final int rowIndex, final int columnIndex) {
		this.data.set(this.getIndex(rowIndex, columnIndex));
	}
	
	public final void set(final int rowIndex, final int columnIndex, final boolean value) {
		this.data.set(this.getIndex(rowIndex, columnIndex), value);
	}
	
	public final void copyTo(final RegionOfInterest destination) {
		final int sourceRowCount = this.getRowCount();
		final int sourceColumnCount = this.getColumnCount();
		final int destinationRowCount = destination.getRowCount();
		final int destinationColumnCount = destination.getColumnCount();
		final boolean sourceIsSmallerThanDestination = sourceRowCount < destinationRowCount;
		
		if (sourceIsSmallerThanDestination) {
			for (int destinationRowIndex = 0; destinationRowIndex < destinationRowCount; ++destinationRowIndex) {
				final int sourceRowIndex = destinationRowIndex * sourceRowCount / destinationRowCount;
				
				for (int destinationColumnIndex = 0; destinationColumnIndex < destinationColumnCount; ++destinationColumnIndex) {
					final int sourceColumnIndex = destinationColumnIndex * sourceColumnCount / destinationColumnCount;
					
					destination.set(destinationRowIndex, destinationColumnIndex, this.get(sourceRowIndex, sourceColumnIndex));
				}
			}
		} else {
			for (int destinationRowIndex = 0; destinationRowIndex < destinationRowCount; ++destinationRowIndex) {
				for (int destinationColumnIndex = 0; destinationColumnIndex < destinationColumnCount; ++destinationColumnIndex) {
					destination.set(destinationRowIndex, destinationColumnIndex, false);
				}
			}
			
			for (int sourceRowIndex = 0; sourceRowIndex < sourceRowCount; ++sourceRowIndex) {
				final int destinationRowIndex = sourceRowIndex * destinationRowCount / sourceRowCount;
				
				for (int sourceColumnIndex = 0; sourceColumnIndex < sourceColumnCount; ++sourceColumnIndex) {
					final int destinationColumnIndex = sourceColumnIndex * destinationColumnCount / sourceColumnCount;
					
					if (this.get(sourceRowIndex, sourceColumnIndex)) {
						destination.set(destinationRowIndex, destinationColumnIndex);
					}
				}
			}
		}
	}
	
}
