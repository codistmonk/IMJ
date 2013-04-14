package imj;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public abstract interface Image {
	
	public abstract int getPixelCount();
	
	public abstract int getChannelCount();
	
	public abstract int getRowCount();
	
	public abstract int getColumnCount();
	
	public abstract int getValue(int rowIndex, int columnIndex);
	
	public abstract int setValue(int rowIndex, int columnIndex, final int value);
	
	public abstract int getValue(int index);
	
	public abstract int setValue(int index, final int value);
	
	/**
	 * @author codistmonk (creation 2013-01-24)
	 */
	public static abstract class Abstract implements Image {
		
		private final Map<String, Object> metadata;
		
		private final int rowCount;
		
		private final int columnCount;
		
		private final int channelCount;
		
		private final int pixelCount;
		
		protected Abstract(final int rowCount, final int columnCount, final int channelCount) {
			this.metadata = new LinkedHashMap<String, Object>();
			this.rowCount = rowCount;
			this.columnCount = columnCount;
			this.channelCount = channelCount;
			this.pixelCount = rowCount * columnCount;
		}
		
		@Override
		public final int getPixelCount() {
			return this.pixelCount;
		}
		
		@Override
		public final int getRowCount() {
			return this.rowCount;
		}
		
		@Override
		public final int getColumnCount() {
			return this.columnCount;
		}
		
		@Override
		public final int getChannelCount() {
			return this.channelCount;
		}
		
		@Override
		public final int getValue(final int rowIndex, final int columnIndex) {
			return this.getValue(index(this, rowIndex, columnIndex));
		}
		
		@Override
		public final int setValue(final int rowIndex, final int columnIndex, final int value) {
			return this.setValue(index(this, rowIndex, columnIndex), value);
		}
		
		public final int getIndex(final int rowIndex, final int columnIndex) {
			return index(this, rowIndex, columnIndex);
		}
		
		public final int getRowIndex(final int index) {
			return index / this.getColumnCount();
		}
		
		public final int getColumnIndex(final int index) {
			return index % this.getColumnCount();
		}
		
		public static final int index(final Image image, final int rowIndex, final int columnIndex) {
			return rowIndex * image.getColumnCount() + columnIndex;
		}
		
	}
	
}
