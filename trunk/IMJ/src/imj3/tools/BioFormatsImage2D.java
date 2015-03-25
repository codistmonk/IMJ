package imj3.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loci.formats.IFormatReader;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-03-20)
 */
public final class BioFormatsImage2D implements Image2D {
	
	private final Map<String, Object> metadata;
	
	private final String id;
	
	private final IFormatReader reader;
	
	private final Channels channels;
	
	private byte[] tileBuffer;
	
	private final TileHolder tileHolder;
	
	private final double[] scales;
	
	private final int width0;
	
	private final int height0;
	
	public BioFormatsImage2D(final String id) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.reader = SVS2Multifile.newImageReader(id);
		this.channels = SVS2Multifile.predefinedChannelsFor(this.reader);
		this.tileHolder = new TileHolder();
		this.width0 = this.getWidth();
		this.height0 = this.getHeight();
		
		{
			final double ratio = (double) this.width0 / this.height0;
			final List<Double> scales = new ArrayList<>();
			
			scales.add(1.0);
			
			for (int series = 1; series < this.reader.getSeriesCount(); ++series) {
				this.reader.setSeries(series);
				final double w = this.getWidth();
				final double h = this.getHeight();
				final double r = (double) w / h;
				final double scale = min(w / this.width0, h / this.height0);
				
				if (1.0E-2 < abs(ratio - r) || scales.get(0) <= scale) {
					--series;
					break;
				}
				
				scales.add(0, scale);
			}
			
			this.scales = scales.stream().mapToDouble(Double::doubleValue).toArray();
			
			this.getReader().setSeries(0);
		}
		
		// TODO load metadata
	}
	
	public final IFormatReader getReader() {
		return this.reader;
	}
	
	@Override
	public final double getScale() {
		return min((double) this.getWidth() / this.width0, (double) this.getHeight() / this.height0);
	}

	@Override
	public final Image2D getScaledImage(final double scale) {
		final int n = this.scales.length - 1;
		final int i = Arrays.binarySearch(this.scales, scale);
		
		if (0 <= i) {
			this.getReader().setSeries(n - i);
		} else {
			final int right = min(n, -(i + 1));
			final int left = max(0, right - 1);
			
			if (abs(scale - this.scales[left]) <= abs(scale - this.scales[right])) {
				this.getReader().setSeries(n - left);
			} else {
				this.getReader().setSeries(n - right);
			}
		}
		
		return this;
	}

	@Override
	public final Map<String, Object> getMetadata() {
		return this.metadata;
	}
	
	@Override
	public final String getId() {
		return this.id;
	}
	
	@Override
	public final Channels getChannels() {
		return this.channels;
	}
	
	@Override
	public final int getWidth() {
		return this.getReader().getSizeX();
	}
	
	@Override
	public final int getHeight() {
		return this.getReader().getSizeY();
	}
	
	@Override
	public final int getOptimalTileWidth() {
		return this.getReader().getOptimalTileWidth();
	}
	
	@Override
	public final int getOptimalTileHeight() {
		return this.getReader().getOptimalTileHeight();
	}
	
	@Override
	public final synchronized Image2D getTile(final int tileX, final int tileY) {
		final int tileWidth = min(this.getWidth() - tileX, this.getOptimalTileWidth());
		final int tileHeight = min(this.getHeight() - tileY, this.getOptimalTileHeight());
		final Image2D result = new PackedImage2D(this.getTileKey(tileX, tileY), tileWidth, tileHeight, this.getChannels());
		
		try {
			final byte[] buffer = this.getTileBuffer();
			
			this.getReader().openBytes(0, buffer, tileX, tileY, tileWidth, tileHeight);
			
			for (int y = 0; y < tileHeight; ++y) {
				for (int x = 0; x < tileWidth; ++x) {
					final int pixelValue = SVS2Multifile.getPixelValueFromBuffer(
							this.getReader(), buffer, tileWidth, tileHeight, this.getChannels().getChannelCount(), x, y);
					result.setPixelValue(x, y, pixelValue);
				}
			}
		} catch (final Exception exception) {
			throw Tools.unchecked(exception);
		}
		
		return result;
	}
	
	@Override
	public final TileHolder getTileHolder() {
		return this.tileHolder;
	}
	
	private final byte[] getTileBuffer() {
		final int expectedBufferSize = this.getOptimalTileWidth() * this.getOptimalTileHeight() * SVS2Multifile.getBytesPerPixel(this.getReader());
		
		if (this.tileBuffer == null) {
			this.tileBuffer = new byte[expectedBufferSize];
		}
		
		return this.tileBuffer;
	}
	
	private static final long serialVersionUID = 2586212892652688146L;
	
}