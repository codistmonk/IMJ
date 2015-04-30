package imj3.protocol.sftp;

import static java.util.Arrays.fill;
import static java.util.Collections.synchronizedMap;
import static net.sourceforge.aprog.swing.SwingTools.*;
import static net.sourceforge.aprog.tools.Tools.*;

import java.awt.GraphicsEnvironment;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * @author codistmonk (creation 2013-11-20)
 */
public final class Handler extends URLStreamHandler implements Serializable {
	
	@Override
	protected final URLConnection openConnection(final URL url) throws IOException {
		try {
			final ChannelSftp channel = getChannel(url);
			
			if (channel == null) {
				return null;
			}
			
			return new URLConnection(url) {
				
				@Override
				public final void connect() throws IOException {
					// NOP
				}
				
				@Override
				public final InputStream getInputStream() throws IOException {
					try {
						final String urlString = this.getURL().toString();
						final String src = urlString.substring(urlString.indexOf(this.getURL().getPath()));
						
						return channel.get(src);
					} catch (final SftpException exception) {
						debugError(this.getURL());
						
						throw new IOException(exception);
					}
				}
				
				@Override
				public final OutputStream getOutputStream() throws IOException {
					try {
						final String path = this.getURL().getFile();
						
						{
							final String[] pathElements = path.split("/");
							String directory = "";
							final int n = pathElements.length - 1;
							
							for (int i = 0; i < n; ++i) {
								directory += pathElements[i] + "/";
								
								try {
									channel.get(directory).close();
								} catch (final SftpException exception) {
									System.out.println("sftp: mkdir: " + directory);
									channel.mkdir(directory);
								}
							}
						}
						
						return channel.put(path);
					} catch (final SftpException exception) {
						debugError(this.getURL());
						
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
	
	private static final Map<String, Session> sessions = new HashMap<>();
	
	private static final Map<Pair<Thread, String>, ChannelSftp> channels = new HashMap<>();
	
	public static final List<Exception> closeAll() {
		final List<Exception> result = new ArrayList<>();
		
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
	
	public static final ChannelSftp getChannel(final URL url) throws JSchException, IOException {
		final String host = url.getHost();
		final Pair<Thread, String> key = new Pair<>(Thread.currentThread(), host);
		final Handler.Credentials credentials = Credentials.forHost(host);
		
		if (credentials == null) {
			return null;
		}
		
		final ChannelSftp result;
		
		synchronized (channels) {
			final ChannelSftp cachedChannel = channels.get(key);
			
			if (cachedChannel != null && cachedChannel.isConnected()) {
				result = cachedChannel;
			} else {
				final Session session;
				final Session cachedSession = sessions.get(key);
				
				if (cachedSession != null && cachedSession.isConnected()) {
					session = cachedSession;
				} else {
					session = new JSch().getSession(credentials.getLogin(), host);
					
					session.setConfig("StrictHostKeyChecking", "no");
					session.setPassword(credentials.getPassword());
					try {
						session.connect();
					} catch (final JSchException exception) {
						credentials.invalidate();
						throw exception;
					}
					
					if (session.isConnected()) {
						sessions.put(key.getSecond(), session);
					} else {
						throw new IOException("Failed to connect session for url: " + url);
					}
				}
				
				result = (ChannelSftp) session.openChannel("sftp");
				
				result.connect();
				
				if (result.isConnected()) {
					channels.put(key, result);
				} else {
					throw new IOException("Failed to connect channel for url: " + url);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-11-18)
	 */
	public static final class Credentials implements Serializable {
		
		private final String host;
		
		private transient String login;
		
		private transient byte[] password;
		
		private transient String privateKey;
		
		public Credentials(final String host) {
			this.host = host;
			
			final Preferences preferences = Preferences.userNodeForPackage(getCallerClass());
			final String userNameKey = "login for " + host;
			final String userName = preferences.get(userNameKey, System.getProperty("user.name"));
			
			if (GraphicsEnvironment.isHeadless()) {
				final Console console = System.console();
				
				if (console == null) {
					return;
				}
				
				String login = console.readLine("login(%s): ", userName);
				
				if (login == null) {
					return;
				}
				
				if (login.isEmpty()) {
					login = userName;
				}
				
				final char[] password = console.readPassword("password: ");
				
				if (password == null) {
					return;
				}
				
				this.set(login, password, preferences, userNameKey);
			} else {
				SwingTools.setCheckAWT(false);
				
				final JTextField loginField = new JTextField(userName);
				final JPasswordField passwordField = new JPasswordField();
				final JComponent credentialsComponent = verticalBox(
						horizontalBox(new JLabel("Login:"), loginField),
						horizontalBox(new JLabel("Password:"), passwordField)
				);
				
				SwingTools.setCheckAWT(true);
				
				passwordField.requestFocusInWindow();
				
				final int option = JOptionPane.showOptionDialog(null, credentialsComponent, host,
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				
				if (option == JOptionPane.OK_OPTION) {
					this.set(loginField.getText(), passwordField.getPassword(), preferences, userNameKey);
				}
			}
		}
		
		private final void set(final String login, final char[] password, final Preferences preferences, final String userNameKey) {
			this.login = login;
			this.password = getPassword(password);
			preferences.put(userNameKey, login);
		}
		
		public final void invalidate() {
			credentials.remove(this.host);
		}
		
		public final String getLogin() {
			return this.login;
		}
		
		public final byte[] getPassword() {
			return this.password;
		}
		
		private static final Map<String, Credentials> credentials = synchronizedMap(new HashMap<>());
		
		public static final Credentials forHost(final String host) {
			return credentials.computeIfAbsent(host, h -> {
				final Credentials candidate = new Credentials(host);
				
				return candidate.getLogin() != null ? candidate : null;
			});
		}
		
		public static final byte[] getPassword(final char[] passwordChars) {
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