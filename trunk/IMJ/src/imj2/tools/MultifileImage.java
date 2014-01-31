package imj2.tools;

import static imj2.core.IMJCoreTools.cache;
import static imj2.tools.IMJTools.predefinedChannelsFor;
import static java.lang.Integer.parseInt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import imj2.core.ConcreteImage2D;
import imj2.core.IMJCoreTools;
import imj2.core.Image;
import imj2.core.Image2D;
import imj2.core.LinearIntImage;
import imj2.core.SubsampledImage2D;
import imj2.core.TiledImage2D;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2013-11-05)
 */
public final class MultifileImage extends TiledImage2D {
	
	private final String root;
	
	private final ConnectionConfigurator connectionConfigurator;
	
	private final String idWithoutLOD;
	
	private final int lod;
	
	private final int width;
	
	private final int height;
	
	private final Channels channels;
	
	private transient ConcreteImage2D<LinearIntImage> tile;
	
	public MultifileImage(final String root, final String id,
			final ConnectionConfigurator connectionConfigurator) {
		super(id);
		
		final Matcher matcher = ID_PATTERN.matcher(id);
		
		if (!matcher.matches()) {
			throw new IllegalArgumentException();
		}
		
		this.root = root;
		this.connectionConfigurator = connectionConfigurator;
		this.idWithoutLOD = matcher.group(1);
		this.lod = parseInt(matcher.group(2));
		
		final String databaseId = root + "/metadata.xml";
		final Document database = cache(databaseId, new Callable<Document>() {
			
			@Override
			public final Document call() throws Exception {
				return setIdAttributes(parse(open(databaseId, connectionConfigurator)));
			}
			
		});
		final Element imageElement = database.getElementById(id);
		
		this.width = parseInt(imageElement.getAttribute("width"));
		this.height = parseInt(imageElement.getAttribute("height"));
		this.channels = predefinedChannelsFor(this.loadImage(id + "_0_0.jpg"));
		
		this.setOptimalTileDimensions(
				parseInt(imageElement.getAttribute("tileWidth")), parseInt(imageElement.getAttribute("tileHeight")));
		
		gc();
	}
	
	public final String getRoot() {
		return this.root;
	}
	
	public final ConnectionConfigurator getConnectionConfigurator() {
		return this.connectionConfigurator;
	}
	
	public final BufferedImage loadImage(final String id) {
		try {
			return ImageIO.read(open(this.getRoot()+ "/" + id, this.getConnectionConfigurator()));
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	@Override
	public final Image getSource() {
		return null;
	}
	
	@Override
	public final int getLOD() {
		return this.lod;
	}
	
	@Override
	public final Image2D getLODImage(final int lod) {
		final int thisLOD = this.getLOD();
		
		if (lod == thisLOD) {
			return this;
		}
		
		final String newRoot = this.getRoot();
		final String newId = this.idWithoutLOD + "_lod" + lod;
		final ConnectionConfigurator newConnectionConfigurator = this.getConnectionConfigurator();
		final String key = newRoot + "/" + newId;
		
		try {
			return cache(key, new Callable<Image2D>() {
				
				@Override
				public final Image2D call() throws Exception {
					return new MultifileImage(newRoot, newId, newConnectionConfigurator);
				}
				
			});
		} catch (final Exception exception) {
			if (thisLOD < lod) {
				debugPrint(exception);
				
				return new SubsampledImage2D(this).getLODImage(lod);
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
		return IMJCoreTools.newParallelViews(this, n);
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final ConcreteImage2D<LinearIntImage> updateTile() {
		final int tileX = this.getTileX();
		final int tileY = this.getTileY();
		
		this.tile = cache(Arrays.asList(this.getId(), tileX, tileY), new Callable<ConcreteImage2D<LinearIntImage>>() {
			
			@Override
			public final ConcreteImage2D<LinearIntImage> call() throws Exception {
				final String tileId = MultifileImage.this.getId() + "_" + tileY + "_" + tileX + ".jpg";
				final AwtBackedImage awtTile = new AwtBackedImage(tileId, MultifileImage.this.loadImage(tileId));
				final LinearIntImage data = new LinearIntImage(tileId, awtTile.getPixelCount(), awtTile.getChannels());
				final int tileWidth = awtTile.getWidth();
				final int tileHeight = awtTile.getHeight();
				
				awtTile.copyPixelValues(0, 0, tileWidth, tileHeight, data.getData());
				
				return new ConcreteImage2D<LinearIntImage>(data, tileWidth, tileHeight);
			}
			
		});
		
		return this.tile;
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
	
	@Override
	protected final void copyTilePixelValues(final int[] result) {
		this.tile.copyPixelValues(0, 0, this.getTileWidth(), this.getTileHeight(), result);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = 1577111610648812112L;
	
	public static final Pattern ID_PATTERN = Pattern.compile("(.*)_lod([0-9]+)");
	
	public static final Pattern PROTOCOL_PATTERN = Pattern.compile("((https?)|(s?ftp))://.+");
	
	public static final Document setIdAttributes(final Document database) {
		for (final Node node : XMLTools.getNodes(database, "//image")) {
			((Element) node).setIdAttribute("id", true);
		}
		
		return database;
	}
	
	public static final InputStream open(final String id, final ConnectionConfigurator connectionConfigurator) {
		final Matcher protocolMatcher = PROTOCOL_PATTERN.matcher(id);
		URLConnection connection = null;
		
		try {
			if (protocolMatcher.matches()) {
				Tools.debugPrint("url:", id);
				
				connection = new URI(id).toURL().openConnection();
				
				connectionConfigurator.configureConnection(connection);
				
				return new InstrumentedInputStream(connection.getInputStream());
			}
			
			return new InstrumentedInputStream(new FileInputStream(id));
		} catch (final Exception exception) {
			if (connection != null) {
				connectionConfigurator.connectionFailed(connection, exception);
			}
			
			throw unchecked(exception);
		}
	}
	
	public static final InputStream open(final String resource) {
		HTTPSAuthenticationForHost connectionConfigurator = null;
		
		try {
			final URL serverURL = new URI(resource).toURL();
			connectionConfigurator = new HTTPSAuthenticationForHost(serverURL.getHost());
		} catch (final Exception exception) {
			debugPrint(resource);
			debugPrint(exception);
		}
		
		return open(resource, connectionConfigurator);
	}
	
	public static final ConnectionConfigurator DEFAULT_CONNECTION_CONFIGURATOR = new ConnectionConfigurator() {
		
		@Override
		public final void configureConnection(final URLConnection connection) {
			// NOP
		}
		
		@Override
		public final void connectionFailed(final URLConnection connection, final Exception exception) {
			// NOP
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -46596027912129212L;
		
	};
	
}
