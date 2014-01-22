package imj2.tools;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;
import static java.util.Collections.synchronizedMap;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.cast;

import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-11-06)
 */
public final class HTTPSAuthenticationForHost implements ConnectionConfigurator {
	
	private final String expectedHostname;
	
	public HTTPSAuthenticationForHost(final String expectedHostname) {
		this.expectedHostname = expectedHostname;
	}
	
	public final String getExpectedHostname() {
		return this.expectedHostname;
	}
	
	@Override
	public final void configureConnection(final URLConnection connection) {
		final HttpsURLConnection https = cast(HttpsURLConnection.class, connection);
		
		if (https == null) {
			return;
		}
		
		https.setHostnameVerifier(new HostnameVerifier() {
			
			@Override
			public final boolean verify(final String hostname, final SSLSession session) {
				return HTTPSAuthenticationForHost.this.getExpectedHostname().equals(hostname);
			}
			
		});
		
		configureCredentials(connection);
	}
	
	@Override
	public final void connectionFailed(final URLConnection connection, final Exception exception) {
		authorizations.remove(connection.getURL().getHost());
		
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -1406984364320366938L;
	
	private static final Map<String, String> authorizations = synchronizedMap(new HashMap<String, String>());
	
	public static final void configureCredentials(final URLConnection connection) {
		final String host = connection.getURL().getHost();
		String authorization = authorizations.get(host);
		
		if (authorization == null) {
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
			final int option = JOptionPane.showOptionDialog(null, credentialsComponent, connection.getURL().toString(),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
			
			SwingTools.setCheckAWT(true);
			
			if (option == JOptionPane.OK_OPTION) {
				final String loginText = loginField.getText();
				final byte[] login = loginText.getBytes();
				final byte[] separator = ":".getBytes();
				final byte[] password = getPassword(passwordField);
				final byte[] credentials = new byte[login.length + separator.length + password.length];
				
				preferences.put(userNameKey, loginText);
				
				arraycopy(login, 0, credentials, 0, login.length);
				arraycopy(separator, 0, credentials, login.length, separator.length);
				arraycopy(password, 0, credentials, login.length + separator.length, password.length);
				
				authorization = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(credentials);
				authorizations.put(host, authorization);
			}
		}
		
		connection.setRequestProperty("Authorization", authorization);
	}
	
	public static final byte[] getPassword(final JPasswordField passwordField) {
		final char[] passwordChars = passwordField.getPassword();
		final byte[] result = new String(passwordChars).getBytes();
		
		fill(passwordChars, (char) 0);
		
		return result;
	}
	
}
