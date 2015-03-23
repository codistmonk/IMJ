package imj3.tools;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * @author codistmonk (creation 2015-03-23)
 */
public final class MultifileSource implements Serializable, Closeable {
	
	private final TFile file;
	
	public MultifileSource(final String path) {
		this.file = new TFile(path);
	}
	
	public final InputStream getInputStream(final String key) {
		try {
			return new TFileInputStream(this.getPath(key));
		} catch (final FileNotFoundException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public final OutputStream getOutputStream(final String key) {
		try {
			return new TFileOutputStream(this.getPath(key));
		} catch (final FileNotFoundException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public final String getPath(final String key) {
		return this.getId() + "/" + key;
	}
	
	public final String getId() {
		return this.file.getPath();
	}
	
	@Override
	public final void close() throws IOException {
		// NOP
	}
	
	private static final long serialVersionUID = -537686043074775519L;
	
}
