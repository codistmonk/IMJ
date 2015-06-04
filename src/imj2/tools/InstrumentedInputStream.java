package imj2.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author codistmonk (creation 2013-12-01)
 */
public final class InstrumentedInputStream extends InputStream {
	
	private final InputStream delegate;
	
	public InstrumentedInputStream(final InputStream delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public final int read() throws IOException {
		final int result = this.delegate.read();
		
		if (-1 != result) {
			byteCount.incrementAndGet();
		}
		
		return result;
	}
	
	@Override
	public final int read(final byte[] b) throws IOException {
		return count(this.delegate.read(b));
	}
	
	@Override
	public final int read(final byte[] b, final int off, final int len) throws IOException {
		return count(this.delegate.read(b, off, len));
	}
	
	@Override
	public final long skip(final long n) throws IOException {
		return count(this.delegate.skip(n));
	}
	
	@Override
	public final int available() throws IOException {
		return this.delegate.available();
	}
	
	@Override
	public final void close() throws IOException {
		this.delegate.close();
	}
	
	@Override
	public final void mark(final int readlimit) {
		this.delegate.mark(readlimit);
	}
	
	@Override
	public final void reset() throws IOException {
		this.delegate.reset();
	}
	
	@Override
	public final boolean markSupported() {
		return this.delegate.markSupported();
	}
	
	public static final AtomicLong byteCount = new AtomicLong();
	
	public static final int count(final int byteCount) {
		if (-1 != byteCount) {
			InstrumentedInputStream.byteCount.addAndGet(byteCount);
		}
		
		return byteCount;
	}
	
	public static final long count(final long byteCount) {
		if (-1L != byteCount) {
			InstrumentedInputStream.byteCount.addAndGet(byteCount);
		}
		
		return byteCount;
	}
	
}
