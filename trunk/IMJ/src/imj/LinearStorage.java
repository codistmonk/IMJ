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
public final class LinearStorage extends Image.Abstract {
	
	private final File file;
	
	private final RandomAccessFile data;
	
	private final ByteBuffer chunkBytes;
	
	private final IntBuffer chunkInts;
	
	private final FloatBuffer chunkFloats;
	
	private int chunkIndex;
	
	public LinearStorage(final int rowCount, final int columnCount) {
		super(rowCount, columnCount);
		try {
			this.file = File.createTempFile("tiling", ".raw");
			this.file.deleteOnExit();
			this.data = new RandomAccessFile(this.file, "rw");
			this.chunkBytes = ByteBuffer.allocate(OPTIMAL_CHUNK_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
			final int chunkCount = ceilingOfRatio(this.getPixelCount(), OPTIMAL_CHUNK_DATUM_COUNT);
			
			for (int i = 0; i < chunkCount; ++i) {
				this.data.write(this.chunkBytes.array());
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final int getValue(final int index) {
		this.loadAppropriateChunk(index);
		
		return this.chunkInts.get(index % OPTIMAL_CHUNK_DATUM_COUNT);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.getValue(index);
		
		this.chunkInts.put(index % OPTIMAL_CHUNK_DATUM_COUNT, value);
		
		return oldValue;
	}
	
	@Override
	public final float getFloatValue(final int index) {
		this.loadAppropriateChunk(index);
		
		return this.chunkFloats.get(index % OPTIMAL_CHUNK_DATUM_COUNT);
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		final int oldValue = this.getValue(index);
		
		this.chunkFloats.put(index % OPTIMAL_CHUNK_DATUM_COUNT, value);
		
		return oldValue;
	}
	
	public final void flush() {
		try {
			this.seek(this.chunkIndex);
			this.data.write(this.chunkBytes.array());
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	private final void seek(final int chunkIndex) {
		try {
			this.data.seek(chunkIndex * OPTIMAL_CHUNK_SIZE);
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
	private final void loadAppropriateChunk(final int index) {
		final int chunkIndex = index / OPTIMAL_CHUNK_DATUM_COUNT;
		
		if (chunkIndex != this.chunkIndex) {
			this.flush();
			
			this.seek(chunkIndex);
			
			try {
				this.data.readFully(this.chunkBytes.array());
				this.chunkIndex = chunkIndex;
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
		}
	}
	
	public static final int DATUM_SIZE = max(Integer.SIZE, Float.SIZE) / 8;
	
	public static final int OPTIMAL_CHUNK_DATUM_COUNT = 16384;
	
	public static final int OPTIMAL_CHUNK_SIZE = OPTIMAL_CHUNK_DATUM_COUNT * DATUM_SIZE;
	
}
