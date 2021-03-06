package imj3.tools;

import static imj3.tools.CommonTools.getFieldValue;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static multij.tools.Tools.*;

import imj3.core.Channels;
import imj3.core.Image2D;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.TiffReader;

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
	
	private final Image2D[] scaledImages;
	
	private final int width0;
	
	private final int height0;
	
	private BioFormatsImage2D(final String id, final Map<String, Object> metadata, final int width0, final int height0, final double[] scales, final Image2D[] scaledImages, final int series) {
		this.id = id;
		this.metadata = metadata;
		this.reader = newImageReader(id);
		this.reader.setSeries(series);
		this.channels = predefinedChannelsFor(this.reader);
		this.tileHolder = new TileHolder();
		this.width0 = width0;
		this.height0 = height0;
		this.scales = scales;
		this.scaledImages = scaledImages;
	}
	
	public BioFormatsImage2D(final String id) {
		this.metadata = new HashMap<>();
		this.id = id;
		this.reader = newImageReader(id);
		this.channels = predefinedChannelsFor(this.reader);
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
				final double r = w / h;
				final double scale = min(w / this.width0, h / this.height0);
				
				if (1.0E-2 < abs(ratio - r) || scales.get(0) <= scale) {
					--series;
					break;
				}
				
				scales.add(0, scale);
			}
			
			this.scales = scales.stream().mapToDouble(Double::doubleValue).toArray();
			final int n = this.scales.length;
			this.scaledImages = new Image2D[n];
			this.scaledImages[0] = this;
			
			for (int i = 1; i < n; ++i) {
				this.scaledImages[i] = new BioFormatsImage2D(
						id, this.metadata, this.width0, this.height0, this.scales, this.scaledImages, i);
			}
			
			this.getReader().setSeries(0);
		}
		
		if (id.toLowerCase(Locale.ENGLISH).endsWith(".svs")) {
			try {
				this.metadata.put("micronsPerPixel", Array.get(
						getFieldValue(((ImageReader) this.reader).getReader(), "pixelSize"), 0));
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
		}
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
		final int series;
		
		if (0 <= i) {
			series = n - i;
		} else {
			final int right = min(n, -(i + 1));
			final int left = max(0, right - 1);
			
			if (abs(scale - this.scales[left]) <= abs(scale - this.scales[right])) {
				series = n - left;
			} else {
				series = n - right;
			}
		}
		
		return this.scaledImages[series];
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
	public final String getTileKey(final int tileX, final int tileY) {
		return Image2D.super.getTileKey(tileX, tileY) + "_series" + this.getReader().getSeries();
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
					final long pixelValue = getPixelValueFromBuffer(
							this.getReader(), buffer, tileWidth, tileHeight, this.getChannels().getChannelCount(), x, y);
					result.setPixelValue(x, y, pixelValue);
				}
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		return result;
	}
	
	@Override
	public final TileHolder getTileHolder() {
		return this.tileHolder;
	}
	
	private final byte[] getTileBuffer() {
		final int expectedBufferSize = this.getOptimalTileWidth() * this.getOptimalTileHeight() * getBytesPerPixel(this.getReader());
		
		if (this.tileBuffer == null) {
			this.tileBuffer = new byte[expectedBufferSize];
		}
		
		return this.tileBuffer;
	}
	
	private static final long serialVersionUID = 2586212892652688146L;
	
	public static final long getPixelValueFromBuffer(final IFormatReader reader, final byte[] buffer, final int bufferWidth, final int bufferHeight, final int channelCount, final int xInBuffer, final int yInBuffer) {
		final int bytesPerChannel = FormatTools.getBytesPerPixel(reader.getPixelType());
		long result = 0L;
		final boolean littleEndian = reader.isLittleEndian();
		
		if (reader.isIndexed()) {
			if (!reader.isInterleaved()) {
				throw new IllegalArgumentException();
			}
			
			final int pixelFirstByteIndex = (yInBuffer * bufferWidth + xInBuffer) * bytesPerChannel;
			
			try {
				switch (bytesPerChannel) {
				case 1:
					return packPixelValue(reader.get8BitLookupTable(),
							buffer[pixelFirstByteIndex] & 0x000000FF);
				case 2:
					return packPixelValue(reader.get16BitLookupTable(),
							((buffer[pixelFirstByteIndex] & 0x000000FF) << 8) | (buffer[pixelFirstByteIndex + 1] & 0x000000FF));
				default:
					throw new IllegalArgumentException();
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} else if (reader.isInterleaved()) {
			final int pixelFirstByteIndex = (yInBuffer * bufferWidth + xInBuffer) * bytesPerChannel * channelCount;
			
			for (int i = 0; i < channelCount; ++i) {
				result = (result << 8) | (buffer[pixelFirstByteIndex + i] & 0x000000FF);
			}
		} else {
			final int tileChannelByteCount = bufferWidth * bufferHeight * bytesPerChannel;
			final int pixelFirstByteIndex = (yInBuffer * bufferWidth + xInBuffer) * bytesPerChannel;
			
			for (int i = 0; i < channelCount; ++i) {
				for (int j = 0; j < bytesPerChannel; ++j) {
					if (littleEndian) {
						result = (result << 8) | (buffer[pixelFirstByteIndex + i * tileChannelByteCount + (bytesPerChannel - 1 - j)] & 0x000000FF);
					} else {
						result = (result << 8) | (buffer[pixelFirstByteIndex + i * tileChannelByteCount + j] & 0x000000FF);
					}
				}
			}
		}
		
		// XXX Is it always ok to assume RGBA and convert to ARGB if channelCount == 4?
		return channelCount == 4 ? (result >> 8) | (result << 24) : result;
	}
	
	public static final IFormatReader newImageReader(final String id) {
		IFormatReader reader = new ImageReader();
		
		try {
			reader.setId(id);
		} catch (final FileNotFoundException exception) {
			debugError(exception);
			
			if (exception.getMessage().contains("/null.tif")) {
				reader = new TiffReader();
				
				try {
					reader.setId(id);
				} catch (final Exception e) {
					throw unchecked(e);
				}
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		if ("portable gray map".equals(reader.getFormat().toLowerCase(Locale.ENGLISH))) {
			// XXX This fixes a defect in Bio-Formats PPM loading, but is it always OK?
//			reader.getCoreMetadata()[0].interleaved = true;
			// XXX method was removed from API, check if problem still exists, and fix if necessary
		}
		
		reader.setSeries(0);
		
		return reader;
	}
	
	public static final long packPixelValue(final byte[][] channelTables, final int colorIndex) {
		long result = 0;
		
		for (final byte[] channelTable : channelTables) {
			result = (result << 8) | (channelTable[colorIndex] & 0x000000FF);
		}
		
		return result;
	}
	
	public static final int packPixelValue(final short[][] channelTables, final int colorIndex) {
		int result = 0;
		
		for (final short[] channelTable : channelTables) {
			result = (result << 16) | (channelTable[colorIndex] & 0x0000FFFF);
		}
		
		return result;
	}
	
	public static final Channels predefinedChannelsFor(final IFormatReader lociImage) {
		if (lociImage.isIndexed()) {
			return Channels.Predefined.C3_U8;
		}
		
		switch (lociImage.getRGBChannelCount()) {
		case 1:
			switch (getBytesPerPixel(lociImage)) {
			case 1:
				return 1 == lociImage.getBitsPerPixel() ?
						Channels.Predefined.C1_U1 : Channels.Predefined.C1_U8;
			case 2:
				return Channels.Predefined.C1_U16;
			default:
				return Channels.Predefined.C1_S32;
			}
		case 2:
			return Channels.Predefined.C2_U16;
		case 3:
			switch (getBytesPerPixel(lociImage)) {
			case 3:
				return Channels.Predefined.C3_U8;
			case 6:
				return Channels.Predefined.C3_U16;
			default:
				return Channels.Predefined.C3_U16;
			}
		case 4:
			switch (getBytesPerPixel(lociImage)) {
			case 4:
				return Channels.Predefined.C4_U8;
			case 8:
				return Channels.Predefined.C4_U16;
			default:
				return Channels.Predefined.C4_U16;
			}
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public static final int getBytesPerPixel(final IFormatReader lociImage) {
		return FormatTools.getBytesPerPixel(lociImage.getPixelType()) * lociImage.getRGBChannelCount();
	}
	
}