package imj;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public abstract interface Image {
	
	public abstract Map<String, Object> getMetadata();
	
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
		
		protected Abstract(final int rowCount, final int columnCount) {
			this.metadata = new LinkedHashMap<String, Object>();
			this.rowCount = rowCount;
			this.columnCount = columnCount;
		}
		
		@Override
		public final Map<String, Object> getMetadata() {
			return this.metadata;
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
		public final int getValue(final int rowIndex, final int columnIndex) {
			return this.getValue(index(this, rowIndex, columnIndex));
		}
		
		@Override
		public final int setValue(final int rowIndex, final int columnIndex, final int value) {
			return this.setValue(index(this, rowIndex, columnIndex), value);
		}
		
		public static final int index(final Image image, final int rowIndex, final int columnIndex) {
			return rowIndex * image.getColumnCount() + columnIndex;
		}
		
	}
	
}
