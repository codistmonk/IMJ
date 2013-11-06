package imj2.tools;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Tools;

import imj2.core.ConcreteImage2D;
import imj2.core.Image2D;

/**
 * @author codistmonk (creation 2013-11-05)
 */
public final class MultifileImage extends TiledImage2D {
	
	private final Options options;
	
	private final String idWithoutLOD;
	
	private final int lod;
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private transient Image2D tile;
	
	/**
	 * Equivalent to <code>MultifileImage(id, null)</code>.
	 * 
	 * @param id
	 * <br>Must not be null
	 */
	public MultifileImage(final String id) {
		this(id, null);
	}
	
	/**
	 * @param id
	 * <br>Must not be null
	 * @param options
	 * <br>Maybe null
	 */
	public MultifileImage(final String id, final Options options) {
		super(id);
		this.options = options;
		
		final Matcher matcher = Pattern.compile("(.*)_lod([0-9]+)").matcher(id);
		
		if (matcher.matches()) {
			this.idWithoutLOD = matcher.group(1);
			this.lod = Integer.parseInt(matcher.group(2));
		} else {
			this.idWithoutLOD = id;
			this.lod = 0;
		}
		
		try {
			final BufferedImage tile00 = this.loadImage(id + "_0_0.jpg");
			this.channels = IMJTools.predefinedChannelsFor(tile00);
			
			int tileY = 0;
			
			while (this.canRead(id + "_" + (tileY + tile00.getHeight()) + "_0.jpg")) {
				tileY += tile00.getHeight();
			}
			
			int tileX = 0;
			
			while (this.canRead(id + "_0_" + (tileX + tile00.getWidth()) + ".jpg")) {
				tileX += tile00.getWidth();
			}
			
			final BufferedImage tileMN = this.loadImage(id + "_" + tileY + "_" + tileX + ".jpg");
			
			this.width = tileX + tileMN.getWidth();
			this.height = tileY + tileMN.getHeight();
			
			this.setOptimalTileDimensions(tile00.getWidth(), tile00.getHeight());
			
			Tools.gc();
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final Options getOptions() {
		return this.options;
	}
	
	public final boolean canRead(final String id) {
		final Matcher protocolMatcher = PROTOCOL_PATTERN.matcher(id);
		
		try {
			if (protocolMatcher.matches()) {
				final URLConnection connection = new URI(id).toURL().openConnection();
				
				if (this.getOptions() != null) {
					this.getOptions().configureConnection(connection);
				}
				
				connection.connect();
				
				InputStream input = null;
				
				try {
					input = connection.getInputStream();
					
					return input != null;
				} catch (final IOException exception) {
					return false;
				} finally {
					if (input != null) {
						input.close();
					}
				}
			} else {
				return new File(id).canRead();
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final BufferedImage loadImage(final String id) {
		final Matcher protocolMatcher = PROTOCOL_PATTERN.matcher(id);
		
		try {
			if (protocolMatcher.matches()) {
				Tools.debugPrint("url:", id);
				
				final URLConnection connection = new URI(id).toURL().openConnection();
				
				if (this.getOptions() != null) {
					this.getOptions().configureConnection(connection);
				}
				
				connection.connect();
				
				return ImageIO.read(connection.getInputStream());
			} else {
				return ImageIO.read(new File(id));
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public final int getLOD() {
		return this.lod;
	}
	
	public final Image2D getLODImage(final int lod) {
		if (lod == this.getLOD()) {
			return this;
		}
		
		try {
			return new MultifileImage(this.idWithoutLOD + "_lod" + lod, this.getOptions());
		} catch (final Exception exception) {
			if (lod == 0) {
				return new MultifileImage(this.idWithoutLOD, this.getOptions());
			}
			
			throw unchecked(exception);
		}
	}
	
	@Override
	public final int getWidth() {
		return this.width;
	}
	
	@Override
	public final int getHeight() {
		return this.height;
	}
	
	@Override
	public final Image2D[] newParallelViews(final int n) {
		return ConcreteImage2D.newParallelViews(this, n);
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@Override
	protected final int getPixelValueFromTile(final int x, final int y, final int xInTile, final int yInTile) {
		return this.tile.getPixelValue(xInTile, yInTile);
	}
	
	@Override
	protected final void setTilePixelValue(final int x, final int y, final int xInTile, final int yInTile,
			final int value) {
		this.tile.setPixelValue(xInTile, yInTile, value);
	}
	
	@Override
	protected final boolean makeNewTile() {
		return this.tile == null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void updateTile() {
		final int tileX = this.getTileX();
		final int tileY = this.getTileY();
		this.tile = IMJTools.cache(Arrays.asList(this.getId(), this.getTileX(), this.getTileY()), new Callable<Image2D>() {
			
			@Override
			public final Image2D call() throws Exception {
				final String tileId = MultifileImage.this.getId() + "_" + tileY + "_" + tileX + ".jpg";
				
				return new AwtBackedImage(tileId, MultifileImage.this.loadImage(tileId));
			}
			
		});
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1577111610648812112L;
	
	public static final Pattern PROTOCOL_PATTERN = Pattern.compile("((https?)|(s?ftp))://.+");
	
	/**
	 * @author codistmonk (creation 2013-11-06)
	 */
	public static abstract interface Options extends Serializable {
		
		public abstract void configureConnection(URLConnection connection);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-11-06)
	 */
	public static final class AuthenticationForHost implements Options {
		
		private final String expectedHostname;
		
		public AuthenticationForHost(final String expectedHostname) {
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
					return AuthenticationForHost.this.getExpectedHostname().equals(hostname);
				}
				
			});
			
			configureCredentials(connection);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -1406984364320366938L;
		
		private static final Map<String, String> authorizations = new HashMap<String, String>();
		
		public static final void configureCredentials(final URLConnection connection) {
			String authorization = authorizations.get(connection.getURL().getHost());
			
			if (authorization == null) {
				SwingTools.setCheckAWT(false);
				
				final JTextField loginField = new JTextField();
				final JPasswordField passwordField = new JPasswordField();
				final JComponent credentialsComponent = verticalBox(
						horizontalBox(new JLabel("Login:"), loginField),
						horizontalBox(new JLabel("Password:"), passwordField)
				);
				final int option = JOptionPane.showOptionDialog(null, credentialsComponent, connection.getURL().toString(),
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				
				SwingTools.setCheckAWT(true);
				
				if (option == JOptionPane.OK_OPTION) {
					final byte[] login = loginField.getText().getBytes();
					final byte[] separator = ":".getBytes();
					final byte[] password = getPassword(passwordField);
					final byte[] credentials = new byte[login.length + separator.length + password.length];
					
					arraycopy(login, 0, credentials, 0, login.length);
					arraycopy(separator, 0, credentials, login.length, separator.length);
					arraycopy(password, 0, credentials, login.length + separator.length, password.length);
					
					authorization = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(credentials);
					authorizations.put(connection.getURL().getHost(), authorization);
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
	
}
