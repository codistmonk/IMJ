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
import java.nio.channels.FileChannel.MapMode;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class LinearStorage extends Image.Abstract {
	
	private final File file;
	
	private final RandomAccessFile data;
	
	private final ByteBuffer chunkBytes;
	
	private final IntBuffer chunkInts;
	
	private final FloatBuffer chunkFloats;
	
	public LinearStorage(final int rowCount, final int columnCount) {
		super(rowCount, columnCount);
		
		try {
			this.file = File.createTempFile("tiling", ".raw");
			this.file.deleteOnExit();
			this.data = new RandomAccessFile(this.file, "rw");
			this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, 0L, (HEADER_DATUM_COUNT + rowCount * columnCount) * DATUM_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
			
			this.data.writeInt(rowCount);
			this.data.writeInt(columnCount);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final void close() {
		try {
			this.data.close();
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	protected final void finalize() throws Throwable {
		try {
			this.close();
			this.file.delete();
		} finally {
			super.finalize();
		}
	}
	
	@Override
	public final int getValue(final int index) {
		return this.chunkInts.get(HEADER_DATUM_COUNT + index);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.getValue(index);
		
		this.chunkInts.put(HEADER_DATUM_COUNT + index, value);
		
		return oldValue;
	}
	
	@Override
	public final float getFloatValue(final int index) {
		return this.chunkFloats.get(HEADER_DATUM_COUNT + index);
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		final int oldValue = this.getValue(index);
		
		this.chunkFloats.put(HEADER_DATUM_COUNT + index, value);
		
		return oldValue;
	}
	
	public static final int DATUM_SIZE = max(Integer.SIZE, Float.SIZE) / 8;
	
	public static final int HEADER_DATUM_COUNT = 2;
	
}
