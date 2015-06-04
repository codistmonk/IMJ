package imj.apps.modules;

import static java.awt.Color.BLACK;
import static multij.tools.Tools.unchecked;
import imj.Image.Abstract;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;

import multij.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-15)
 */
public abstract class RegionOfInterest extends Abstract {
	
	protected RegionOfInterest(final int rowCount, final int columnCount) {
		super(rowCount, columnCount, 1);
	}
	
	public abstract void invert();
	
	public abstract void reset(boolean value);
	
	public abstract boolean get(int index);
	
	public abstract void set(int index);
	
	public abstract void set(int index, boolean value);
	
	public abstract boolean get(int rowIndex, int columnIndex);
	
	public abstract void set(int rowIndex, int columnIndex);
	
	public abstract void set(int rowIndex, int columnIndex, boolean value);
	
	public abstract void copyTo(final RegionOfInterest destination);
	
	public static final RegionOfInterest newInstance(final int rowCount, final int columnCount) {
		return new UsingBitSet(rowCount, columnCount);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-26)
	 */
	public static final class UsingBitSet extends RegionOfInterest {
		
		private final BitSet data;
		
		public UsingBitSet(final int rowCount, final int columnCount, final ObjectInputStream data) {
			super(rowCount, columnCount);
			
			try {
				this.data = (BitSet) data.readObject();
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		public UsingBitSet(final int rowCount, final int columnCount) {
			this(rowCount, columnCount, true);
		}
		
		public UsingBitSet(final int rowCount, final int columnCount, final boolean initialState) {
			super(rowCount, columnCount);
			final int pixelCount = this.getPixelCount();
			this.data = new BitSet(pixelCount);
			
			if (initialState) {
				this.reset(true);
			}
		}
		
		public final void writeDataTo(final ObjectOutputStream output) {
			try {
				output.writeObject(this.data);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
		
		public final int getCardinality() {
			return this.data.cardinality();
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
		public final void invert() {
			this.data.flip(0, this.data.size());
		}
		
		@Override
		public final void reset(final boolean value) {
			this.data.set(0, this.data.size(), value);
		}
		
		@Override
		public final boolean get(final int index) {
			return this.data.get(index);
		}
		
		@Override
		public final void set(final int index) {
			this.data.set(index);
		}
		
		@Override
		public final void set(final int index, final boolean value) {
			this.data.set(index, value);
		}
		
		@Override
		public final boolean get(final int rowIndex, final int columnIndex) {
			return this.get(this.getIndex(rowIndex, columnIndex));
		}
		
		@Override
		public final void set(final int rowIndex, final int columnIndex) {
			this.set(this.getIndex(rowIndex, columnIndex));
		}
		
		@Override
		public final void set(final int rowIndex, final int columnIndex, final boolean value) {
			this.set(this.getIndex(rowIndex, columnIndex), value);
		}
		
		@Override
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
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 1598389445959512340L;
		
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
			
			this.reset(true);
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
		public final void invert() {
			new LookupOp(new ByteLookupTable(0, new byte[] { 1, 0 }), null).filter(this.getData(), this.getData());
		}
		
		@Override
		public final void reset(final boolean value) {
			this.getGraphics().setColor(value ? WHITE : BLACK);
			this.getGraphics().fillRect(0, 0, this.getColumnCount(), this.getRowCount());
		}
		
		@Override
		public final boolean get(final int index) {
			final int x = this.getColumnIndex(index);
			final int y = this.getRowIndex(index);
			
			return (this.getData().getRGB(x, y) & 0x00FFFFFF) != 0;
		}
		
		@Override
		public final void set(final int index) {
			this.set(index, true);
		}
		
		@Override
		public final void set(final int index, final boolean value) {
			final int x = this.getColumnIndex(index);
			final int y = this.getRowIndex(index);
			
			this.getData().setRGB(x, y, value ? Integer.MAX_VALUE : 0);
		}
		
		@Override
		public final boolean get(final int rowIndex, final int columnIndex) {
			return (this.getData().getRGB(columnIndex, rowIndex) & 0x00FFFFFF) != 0;
		}
		
		@Override
		public final void set(final int rowIndex, final int columnIndex) {
			this.getData().setRGB(columnIndex, rowIndex, Integer.MAX_VALUE);
		}
		
		@Override
		public final void set(final int rowIndex, final int columnIndex, final boolean value) {
			this.getData().setRGB(columnIndex, rowIndex, value ? Integer.MAX_VALUE : 0);
		}
		
		@Override
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
