package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class LinearStorage extends Image.Abstract {
	
	private final File file;
	
	private final RandomAccessFile data;
	
	private int absoluteBufferStartIndex;
	
	private int absoluteBufferEndIndex;
	
	private MappedByteBuffer chunkBytes;
	
	private IntBuffer chunkInts;
	
	private FloatBuffer chunkFloats;
	
	private LinearStorage(final int rowCount, final int columnCount, final File file, final RandomAccessFile data) {
		super(rowCount, columnCount);
		
		try {
			this.file = file;
			this.data = data;
			this.absoluteBufferStartIndex = 2;
			this.absoluteBufferEndIndex = 2 + min(MAXIMUM_BUFFER_SIZE, rowCount * columnCount);
			this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
					(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public LinearStorage(final int rowCount, final int columnCount) {
		this(rowCount, columnCount, true);
	}
	
	public LinearStorage(final int rowCount, final int columnCount, final boolean deleteFileOnExit) {
		super(rowCount, columnCount);
		
		try {
			this.file = File.createTempFile("image", ".raw");
			if (deleteFileOnExit) {
				this.file.deleteOnExit();
			}
			this.data = new RandomAccessFile(this.file, "rw");
			this.absoluteBufferStartIndex = 2;
			this.absoluteBufferEndIndex = 2 + min(MAXIMUM_BUFFER_SIZE, rowCount * columnCount);
			this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
					(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
			
			this.data.writeInt(rowCount);
			this.data.writeInt(columnCount);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final File getFile() {
		return this.file;
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
		if (index < 0 || this.getPixelCount() <= index) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		
		final int absoluteIndex = index + HEADER_DATUM_COUNT;
		
		if (absoluteIndex < this.absoluteBufferStartIndex) {
			this.absoluteBufferStartIndex = max(2, absoluteIndex - MAXIMUM_BUFFER_SIZE / 2);
			this.absoluteBufferEndIndex = 2 + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
		} else if (this.absoluteBufferEndIndex <= absoluteIndex) {
			this.absoluteBufferStartIndex = absoluteIndex - MAXIMUM_BUFFER_SIZE / 2;
			this.absoluteBufferEndIndex = 2 + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
		}
		
		return this.chunkInts.get(absoluteIndex - this.absoluteBufferStartIndex);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.getValue(index);
		
		this.chunkInts.put(index + HEADER_DATUM_COUNT - this.absoluteBufferStartIndex, value);
		
		return oldValue;
	}
	
	@Override
	public final float getFloatValue(final int index) {
		if (index < 0 || this.getPixelCount() <= index) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		
		final int absoluteIndex = index + HEADER_DATUM_COUNT;
		
		if (absoluteIndex < this.absoluteBufferStartIndex) {
			this.absoluteBufferStartIndex = max(2, absoluteIndex - MAXIMUM_BUFFER_SIZE / 2);
			this.absoluteBufferEndIndex = 2 + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
		} else if (this.absoluteBufferEndIndex <= absoluteIndex) {
			this.absoluteBufferStartIndex = absoluteIndex - MAXIMUM_BUFFER_SIZE / 2;
			this.absoluteBufferEndIndex = 2 + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
			this.chunkFloats = this.chunkBytes.asFloatBuffer();
		}
		
		return this.chunkFloats.get(absoluteIndex - this.absoluteBufferStartIndex);
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		final int oldValue = this.getValue(index);
		
		this.chunkFloats.put(index + HEADER_DATUM_COUNT - this.absoluteBufferStartIndex, value);
		
		return oldValue;
	}
	
	public static final long DATUM_SIZE = max(Integer.SIZE, Float.SIZE) / 8;
	
	public static final int HEADER_DATUM_COUNT = 2;
	
	public static final int MAXIMUM_BUFFER_SIZE = 268435456;
	
	public static final LinearStorage open(final File file) {
		try {
			final RandomAccessFile data = new RandomAccessFile(file, "rw");
			final int rowCount = data.readInt();
			final int columnCount = data.readInt();
			final LinearStorage result = new LinearStorage(rowCount, columnCount, file, data);
			
			// FIXME Metadata are missing from file!
			result.getMetadata().put("channelCount", 3);
			
			return result;
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
}
