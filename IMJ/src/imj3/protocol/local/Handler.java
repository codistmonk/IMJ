package imj3.protocol.local;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * @author codistmonk (creation 2015-04-29)
 */
public final class Handler extends URLStreamHandler implements Serializable {
	
	@Override
	public final URLConnection openConnection(final URL u) throws IOException {
		final TFile file = new TFile(removePrefix(u.toString(), "local://"));
		
		try {
			file.setWritable(true);
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		
		return new URLConnection(u) {
			
			@Override
			public final void connect() throws IOException {
				// NOP
			}
			
			@Override
			public final InputStream getInputStream() throws IOException {
				try {
					return new TFileInputStream(file);
				} catch (final FileNotFoundException exception) {
					throw new UncheckedIOException(exception);
				}
			}
			
			@Override
			public final OutputStream getOutputStream() throws IOException {
				try {
					return new TFileOutputStream(file);
				} catch (final FileNotFoundException exception) {
					throw new UncheckedIOException(exception);
				}
			}
			
		};
	}
	
	private static final long serialVersionUID = 7306015033326912617L;
	
	public static final String removePrefix(final String string, final String prefix) {
		return string.startsWith(prefix) ? string.substring(prefix.length()) : string;
	}
	
}
