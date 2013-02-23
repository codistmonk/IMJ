package imj.apps.modules;

import imj.Image.Abstract;

import java.util.BitSet;

/**
 * @author codistmonk (creation 2013-02-15)
 */
public final class RegionOfInterest extends Abstract {
	
	private final BitSet data;
	
	public RegionOfInterest(final int rowCount, final int columnCount) {
		super(rowCount, columnCount, 1);
		final int pixelCount = this.getPixelCount();
		this.data = new BitSet(pixelCount);
		
		this.data.set(0, pixelCount);
	}
	
	@Override
	public final int getValue(final int index) {
		return this.get(index) ? 1 : 0;
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int result = this.getValue(index);
		
		this.set(index, value != 0);
		
		return result;
	}

	@Override
	public final float getFloatValue(final int index) {
		return this.getValue(index);
	}

	@Override
	public final float setFloatValue(final int index, final float value) {
		return this.setValue(index, (int) value);
	}
	
	public final void reset() {
		this.data.set(0, this.data.size());
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
