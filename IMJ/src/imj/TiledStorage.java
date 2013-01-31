package imj;

import static imj.IMJTools.ceilingOfRatio;
import static java.lang.Math.max;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class TiledStorage extends Image.Abstract {
	
	private final File file;
	
	private final RandomAccessFile data;
	
	private final int optimalTileRowCount;
	
	private final int optimalTileColumnCount;
	
	private final int optimalTilePixelCount;
	
	private final int rowTileCount;
	
	private final int columnTileCount;
	
	private final ByteBuffer tileBytes;
	
	private final IntBuffer tileInts;
	
	private final FloatBuffer tileFloats;
	
	private int rowTileIndex;
	
	private int columnTileIndex;
	
	public TiledStorage(final int rowCount, final int columnCount) {
		super(rowCount, columnCount);
		
		try {
			this.file = File.createTempFile("tiling", ".raw");
			this.file.deleteOnExit();
			this.data = new RandomAccessFile(this.file, "rw");
			this.optimalTileRowCount = 256;
			this.optimalTileColumnCount = 256;
			this.optimalTilePixelCount = this.optimalTileRowCount * this.optimalTileColumnCount;
			this.rowTileCount = ceilingOfRatio(rowCount, this.optimalTileRowCount);
			this.columnTileCount = ceilingOfRatio(columnCount, this.optimalTileColumnCount);
			this.tileBytes = ByteBuffer.allocate(this.optimalTileRowCount * this.optimalTileColumnCount * DATUM_SIZE);
			this.tileInts = this.tileBytes.asIntBuffer();
			this.tileFloats = this.tileBytes.asFloatBuffer();
			
			final int tileCount = this.getRowTileCount() * this.getColumnTileCount();
			
			for (int i = 0; i < tileCount; ++i) {
				this.data.write(this.tileBytes.array());
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	protected final void finalize() throws Throwable {
		try {
			this.file.delete();
		} finally {
			super.finalize();
		}
	}
	
	public final int getOptimalTileRowCount() {
		return this.optimalTileRowCount;
	}
	
	public final int getOptimalTileColumnCount() {
		return this.optimalTileColumnCount;
	}
	
	public final int getOptimalTilePixelCount() {
		return this.optimalTilePixelCount;
	}
	
	public final int getRowTileCount() {
		return this.rowTileCount;
	}
	
	public final int getColumnTileCount() {
		return this.columnTileCount;
	}
	
	public final int getRowTileIndex(final int rowIndex) {
		return rowIndex / this.getOptimalTileRowCount();
	}
	
	public final int getColumnTileIndex(final int columnIndex) {
		return columnIndex / this.getOptimalTileColumnCount();
	}
	
	public final int getRowIndexInTile(final int rowIndex) {
		return rowIndex % this.getOptimalTileRowCount();
	}
	
	public final int getColumnIndexInTile(final int columnIndex) {
		return columnIndex % this.getOptimalTileColumnCount();
	}
	
	@Override
	public final int getValue(final int index) {
		final int rowIndex = this.getRowIndex(index);
		final int columnIndex = this.getColumnIndex(index);
		
		this.loadAppropriateTile(rowIndex, columnIndex);
		
		return this.tileInts.get(this.getIndexInTile(rowIndex, columnIndex));
	}
	
	public final int getIndexInTile(final int rowIndex, final int columnIndex) {
		return (this.getRowIndexInTile(rowIndex) * this.getOptimalTileColumnCount() +
				this.getColumnIndexInTile(columnIndex));
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int rowIndex = this.getRowIndex(index);
		final int columnIndex = this.getColumnIndex(index);
		final int oldValue = this.getValue(index);
		
		this.tileInts.put(this.getIndexInTile(rowIndex, columnIndex), value);
		
		return oldValue;
	}
	
	@Override
	public float getFloatValue(int index) {
		final int rowIndex = this.getRowIndex(index);
		final int columnIndex = this.getColumnIndex(index);
		
		this.loadAppropriateTile(rowIndex, columnIndex);
		
		return this.tileFloats.get(this.getIndexInTile(rowIndex, columnIndex));
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		final int rowIndex = this.getRowIndex(index);
		final int columnIndex = this.getColumnIndex(index);
		final float oldValue = this.getFloatValue(index);
		
		this.tileFloats.put(this.getIndexInTile(rowIndex, columnIndex), value);
		
		return oldValue;
	}
	
	public final void flush() {
		try {
			this.seek(this.rowTileIndex , this.columnTileIndex);
			this.data.write(this.tileBytes.array());
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	private final void seek(final int rowTileIndex, final int columnTileIndex) {
		try {
			this.data.seek((rowTileIndex * this.getColumnTileCount() + columnTileIndex) *
					this.getOptimalTilePixelCount() * DATUM_SIZE);
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	private final void loadAppropriateTile(final int rowIndex, final int columnIndex) {
		final int rowTileIndex = this.getRowTileIndex(rowIndex);
		final int columnTileIndex = this.getColumnTileIndex(columnIndex);
		
		if (rowTileIndex != this.rowTileIndex || columnTileIndex != this.columnTileIndex) {
			try {
				this.flush();
				
				this.seek(rowTileIndex, columnTileIndex);
				this.data.readFully(this.tileBytes.array());
				
				this.rowTileIndex = rowTileIndex;
				this.columnTileIndex = columnTileIndex;
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
	}
	
	public static final int DATUM_SIZE = max(Integer.SIZE, Float.SIZE) / 8;
	
}
