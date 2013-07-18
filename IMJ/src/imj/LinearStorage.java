package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-01-31)
 */
public final class LinearStorage extends Image.Abstract {
	
	private final File file;
	
	private final boolean deleteFileOnExit;
	
	private final RandomAccessFile data;
	
	private int absoluteBufferStartIndex;
	
	private int absoluteBufferEndIndex;
	
	private MappedByteBuffer chunkBytes;
	
	private IntBuffer chunkInts;
	
	private LinearStorage(final int rowCount, final int columnCount, final int channelCount, final File file, final RandomAccessFile data) {
		super(rowCount, columnCount, channelCount);
		
		try {
			this.file = file;
			this.deleteFileOnExit = false;
			this.data = data;
			this.absoluteBufferStartIndex = HEADER_DATUM_COUNT;
			this.absoluteBufferEndIndex = HEADER_DATUM_COUNT + min(MAXIMUM_BUFFER_SIZE, rowCount * columnCount);
			this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
					(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public LinearStorage(final int rowCount, final int columnCount, final int channelCount) {
		this(rowCount, columnCount, channelCount, true);
	}
	
	public LinearStorage(final int rowCount, final int columnCount, final int channelCount, final boolean deleteFileOnExit) {
		super(rowCount, columnCount, channelCount);
		
		try {
			this.file = File.createTempFile("image", ".raw");
			this.deleteFileOnExit = deleteFileOnExit;
			
			if (deleteFileOnExit) {
				this.file.deleteOnExit();
			}
			
			this.data = new RandomAccessFile(this.file, "rw");
			
			this.data.writeInt(rowCount);
			this.data.writeInt(columnCount);
			this.data.writeInt(channelCount);
			
			this.absoluteBufferStartIndex = HEADER_DATUM_COUNT;
			this.absoluteBufferEndIndex = HEADER_DATUM_COUNT + min(MAXIMUM_BUFFER_SIZE, rowCount * columnCount);
			this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
					(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			this.chunkInts = this.chunkBytes.asIntBuffer();
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
			
			if (this.deleteFileOnExit) {
				this.file.delete();
			}
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
			this.absoluteBufferEndIndex = HEADER_DATUM_COUNT + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
		} else if (this.absoluteBufferEndIndex <= absoluteIndex) {
			this.absoluteBufferStartIndex = absoluteIndex - MAXIMUM_BUFFER_SIZE / 2;
			this.absoluteBufferEndIndex = HEADER_DATUM_COUNT + min(this.getPixelCount(), this.absoluteBufferStartIndex + MAXIMUM_BUFFER_SIZE);
			try {
				this.chunkBytes = this.data.getChannel().map(MapMode.READ_WRITE, this.absoluteBufferStartIndex * DATUM_SIZE,
						(this.absoluteBufferEndIndex - this.absoluteBufferStartIndex) * DATUM_SIZE);
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			this.chunkInts = this.chunkBytes.asIntBuffer();
		}
		
		return this.chunkInts.get(absoluteIndex - this.absoluteBufferStartIndex);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.getValue(index);
		
		this.chunkInts.put(index + HEADER_DATUM_COUNT - this.absoluteBufferStartIndex, value);
		
		return oldValue;
	}
	
	public static final long DATUM_SIZE = max(Integer.SIZE, Float.SIZE) / 8;
	
	public static final int HEADER_DATUM_COUNT = 3;
	
	public static final int MAXIMUM_BUFFER_SIZE = 268435456;
	
	public static final LinearStorage open(final File file) {
		try {
			final RandomAccessFile data = new RandomAccessFile(file, "rw");
			final int rowCount = data.readInt();
			final int columnCount = data.readInt();
			final int channelCount = data.readInt();
			final LinearStorage result = new LinearStorage(rowCount, columnCount, channelCount, file, data);
			
			return result;
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
}
