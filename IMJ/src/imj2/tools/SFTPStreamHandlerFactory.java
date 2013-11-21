package imj2.tools;

import static java.util.Arrays.fill;
import static java.util.Collections.synchronizedMap;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Pair;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-11-20)
 */
public final class SFTPStreamHandlerFactory implements URLStreamHandlerFactory, Serializable {
	
	@Override
	public final URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("sftp".equals(protocol)) {
			return new SFTPStreamHandler();
		}
		
		return null;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 6645843997774044192L;
	
	/**
	 * @author codistmonk (creation 2013-11-20)
	 */
	public static final class SFTPStreamHandler extends URLStreamHandler implements Serializable {
		
		@Override
		protected final URLConnection openConnection(final URL url) throws IOException {
			final Pair<String, byte[]> loginPassword = new Credentials(url.getHost()).getLoginPassword();
			
			if (loginPassword == null) {
				return null;
			}
			
			try {
				final String key = url.getHost();
				final ChannelSftp channel;
				
				synchronized (channels) {
					final ChannelSftp cachedChannel = channels.get(key);
					
					if (cachedChannel != null && cachedChannel.isConnected()) {
						channel = cachedChannel;
					} else {
						final Session session;
						final Session cachedSession = sessions.get(key);
						
						if (cachedSession != null && cachedSession.isConnected()) {
							session = cachedSession;
						} else {
							session = new JSch().getSession(loginPassword.getFirst(), url.getHost());
							
							session.setConfig("StrictHostKeyChecking", "no");
							session.setPassword(loginPassword.getSecond());
							session.connect();
							
							if (session.isConnected()) {
								sessions.put(key, session);
							} else {
								throw new IOException("Failed to connect session for url: " + url);
							}
						}
						
						channel = (ChannelSftp) session.openChannel("sftp");
						
						channel.connect();
						
						if (channel.isConnected()) {
							channels.put(key, channel);
						} else {
							throw new IOException("Failed to connect channel for url: " + url);
						}
					}
				}
				
				return new URLConnection(url) {
					
					@Override
					public final void connect() throws IOException {
						// NOP
					}
					
					@Override
					public final InputStream getInputStream() throws IOException {
						try {
							return channel.get(this.getURL().getFile());
						} catch (final SftpException exception) {
							debugPrint(this.getURL());
							throw new IOException(exception);
						}
					}
					
				};
			} catch (final JSchException exception) {
				exception.printStackTrace();
			}
			
			return null;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 6164151336427013720L;
		
		private static final Map<String, Session> sessions = new HashMap<String, Session>();
		
		private static final Map<String, ChannelSftp> channels = new HashMap<String, ChannelSftp>();
		
		public static final List<Exception> closeAll() {
			final List<Exception> result = new ArrayList<Exception>();
			
			synchronized (channels) {
				for (final ChannelSftp channel : channels.values()) {
					try {
						channel.exit();
					} catch (final Exception exception) {
						result.add(exception);
					}
				}
				
				for (final Session session : sessions.values()) {
					try {
						session.disconnect();
					} catch (final Exception exception) {
						result.add(exception);
					}
				}
			}
			
			return result;
		}
		
		/**
		 * @author codistmonk (creation 2013-11-18)
		 */
		public static final class Credentials implements Serializable {
			
			private transient Pair<String, byte[]> loginPassword;
			
			public Credentials(final String host) {
				this.loginPassword = authorizations.get(host);
				
				if (this.loginPassword == null) {
					SwingTools.setCheckAWT(false);
					final Preferences preferences = Preferences.userNodeForPackage(Tools.getCallerClass());
					final String userNameKey = "login for " + host;
					final String userName = preferences.get(userNameKey, System.getProperty("user.name"));
					final JTextField loginField = new JTextField(userName);
					final JPasswordField passwordField = new JPasswordField();
					final JComponent credentialsComponent = verticalBox(
							horizontalBox(new JLabel("Login:"), loginField),
							horizontalBox(new JLabel("Password:"), passwordField)
					);
					passwordField.requestFocusInWindow();
					final int option = JOptionPane.showOptionDialog(null, credentialsComponent, host,
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
					
					SwingTools.setCheckAWT(true);
					
					if (option == JOptionPane.OK_OPTION) {
						this.loginPassword = new Pair<String, byte[]>(loginField.getText(), getPassword(passwordField));
						preferences.put(userNameKey, this.loginPassword.getFirst());
						authorizations.put(host, this.loginPassword);
					}
				}
			}
			
			public final Pair<String, byte[]> getLoginPassword() {
				try {
					return this.loginPassword;
				} finally {
					this.loginPassword = null;
				}
			}
			
			private static final Map<String, Pair<String, byte[]>> authorizations = synchronizedMap(new HashMap<String, Pair<String, byte[]>>());
			
			public static final byte[] getPassword(final JPasswordField passwordField) {
				final char[] passwordChars = passwordField.getPassword();
				final byte[] result = new String(passwordChars).getBytes();
				
				fill(passwordChars, (char) 0);
				
				return result;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -435478800001672284L;
			
		}
		
	}
	
}
