package imj.apps.modules;

import imj.Image.Abstract;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.util.BitSet;

/**
 * @author codistmonk (creation 2013-02-15)
 */
public abstract class RegionOfInterest extends Abstract {
	
	protected RegionOfInterest(final int rowCount, final int columnCount) {
		super(rowCount, columnCount, 1);
	}
	
	public abstract void invert();
	
	public abstract void reset();
	
	public abstract boolean get(int index);
	
	public abstract void set(int index);
	
	public abstract void set(int index, boolean value);
	
	public abstract boolean get(int rowIndex, int columnIndex);
	
	public abstract void set(int rowIndex, int columnIndex);
	
	public abstract void set(int rowIndex, int columnIndex, boolean value);
	
	public abstract void copyTo(final RegionOfInterest destination);
	
	public static final RegionOfInterest newInstance(final int rowCount, final int columnCount) {
		return new UsingBitSet(rowCount, columnCount);
//		return new UsingBufferedImage(rowCount, columnCount);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-26)
	 */
	public static final class UsingBitSet extends RegionOfInterest {
		
		private final BitSet data;
		
		public UsingBitSet(final int rowCount, final int columnCount) {
			this(rowCount, columnCount, true);
		}
		
		public UsingBitSet(final int rowCount, final int columnCount, final boolean initialState) {
			super(rowCount, columnCount);
			final int pixelCount = this.getPixelCount();
			this.data = new BitSet(pixelCount);
			
			if (initialState) {
				this.reset();
			}
		}
		
		@Override
		public final int getValue(final int index) {
			return this.get(index) ? Integer.MAX_VALUE : 0;
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
		
		@Override
		public final void invert() {
			this.data.flip(0, this.data.size());
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
			return this.get(this.getIndex(rowIndex, columnIndex));
		}
		
		public final void set(final int rowIndex, final int columnIndex) {
			this.set(this.getIndex(rowIndex, columnIndex));
		}
		
		public final void set(final int rowIndex, final int columnIndex, final boolean value) {
			this.set(this.getIndex(rowIndex, columnIndex), value);
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
	
	/**
	 * @author codistmonk (creation 2013-02-26)
	 */
	public static final class UsingBufferedImage extends RegionOfInterest {
		
		private final BufferedImage data;
		
		private final Graphics2D graphics;
		
		public UsingBufferedImage(final int rowCount, final int columnCount) {
			super(rowCount, columnCount);
			this.data = new BufferedImage(columnCount, rowCount, BufferedImage.TYPE_BYTE_BINARY);
			this.graphics = this.data.createGraphics();
			
			this.reset();
		}
		
		public final BufferedImage getData() {
			return this.data;
		}
		
		public final Graphics2D getGraphics() {
			return this.graphics;
		}
		
		@Override
		public final int getValue(final int index) {
			return this.get(index) ? Integer.MAX_VALUE : 0;
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
		
		@Override
		public final void invert() {
			new LookupOp(new ByteLookupTable(0, new byte[] { 1, 0 }), null).filter(this.getData(), this.getData());
		}
		
		public final void reset() {
			this.getGraphics().setColor(WHITE);
			this.getGraphics().fillRect(0, 0, this.getColumnCount(), this.getRowCount());
		}
		
		public final boolean get(final int index) {
			final int x = this.getColumnIndex(index);
			final int y = this.getRowIndex(index);
			
			return (this.getData().getRGB(x, y) & 0x00FFFFFF) != 0;
		}
		
		public final void set(final int index) {
			this.set(index, true);
		}
		
		public final void set(final int index, final boolean value) {
			final int x = this.getColumnIndex(index);
			final int y = this.getRowIndex(index);
			
			this.getData().setRGB(x, y, value ? Integer.MAX_VALUE : 0);
		}
		
		public final boolean get(final int rowIndex, final int columnIndex) {
			return (this.getData().getRGB(columnIndex, rowIndex) & 0x00FFFFFF) != 0;
		}
		
		public final void set(final int rowIndex, final int columnIndex) {
			this.getData().setRGB(columnIndex, rowIndex, Integer.MAX_VALUE);
		}
		
		public final void set(final int rowIndex, final int columnIndex, final boolean value) {
			this.getData().setRGB(columnIndex, rowIndex, value ? Integer.MAX_VALUE : 0);
		}
		
		public final void copyTo(final RegionOfInterest destination) {
			((UsingBufferedImage) destination).getGraphics().drawImage(this.getData(), 0, 0, destination.getColumnCount(), destination.getRowCount(), null);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			try {
				this.getGraphics().dispose();
			} finally {
				super.finalize();
			}
		}
		
		public static final Color WHITE = new Color(Integer.MAX_VALUE, false);
		
	}
	
}