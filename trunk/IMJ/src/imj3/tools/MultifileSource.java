package imj3.tools;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author codistmonk (creation 2015-03-23)
 */
public final class MultifileSource implements Serializable, Closeable {
	
	private final String path;
	
	public MultifileSource(final String path) {
		String tmp = path.replaceFirst("^file:", "local:").replace(File.separatorChar, '/');
		
		try {
			new URL(tmp);
		} catch (final MalformedURLException exception) {
			if (exception.getMessage().contains("protocol")) {
				tmp = "local:" + tmp;
			} else {
				exception.printStackTrace();
			}
		}
		
		this.path = tmp;
	}
	
	public final InputStream getInputStream(final String key) {
		try {
			return new URL(this.getPath(key)).openStream();
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public final OutputStream getOutputStream(final String key) {
		try {
			return new URL(this.getPath(key)).openConnection().getOutputStream();
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public final String getPath(final String key) {
		return this.getId() + "/" + key;
	}
	
	public final String getId() {
		return this.path;
	}
	
	@Override
	public final void close() throws IOException {
		// NOP
	}
	
	private static final long serialVersionUID = -537686043074775519L;
	
	public static final String JAVA_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";
	
	static {
		final String oldPackages = System.getProperty(JAVA_PROTOCOL_HANDLER_PKGS);
		
		System.setProperty(JAVA_PROTOCOL_HANDLER_PKGS, (oldPackages == null ? "" : (oldPackages + "|")) + "imj3.protocol");
	}
	
}
