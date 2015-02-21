package imj3.tools;

import static imj2.tools.IMJTools.getFieldValue;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.synchronizedList;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj2.draft.AutoCloseableImageWriter;
import imj2.tools.Canvas;

import imj3.core.Channels;
import imj3.tools.CommonTools.FileProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;
import net.sourceforge.aprog.tools.TaskManager;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

/**
 * @author codistmonk (creation 2014-11-30)
 */
public final class SVS2Multifile {
	
	private SVS2Multifile() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File inRoot = new File(arguments.get("in", ""));
		final RegexFilter filter = new RegexFilter(arguments.get("filter", ".*\\.svs"));
		final File outRoot = new File(arguments.get("out", ""));
		
		IMJTools.toneDownBioFormatsLogger();
		
		FileProcessor.deepForEachFileIn(inRoot, new FileProcessor() {

			@Override
			public final void process(final File file) {
				if (filter.accept(file.getParentFile(), file.getName())) {
					if (file.getPath().contains("/old/")) {
						Tools.debugError("Ignoring", file);
					} else {
						Tools.debugPrint(file);
						
						final String tileFormat = "jpg";
						
						try (final ZipOutputStream output = new ZipOutputStream(new FileOutputStream(new File(outRoot, baseName(file.getName()) + ".zip")))) {
							final IFormatReader reader = newImageReader(file.getPath());
							final int imageWidth = reader.getSizeX();
							final int imageHeight = reader.getSizeY();
							final int tileSize = 512;
							
							{
								final String mpp = Array.get(getFieldValue(((ImageReader) reader).getReader(), "pixelSize"), 0).toString();
								
								output.putNextEntry(new ZipEntry("metadata.xml"));
								XMLTools.write(XMLTools.parse("<collection><image type=\"lod0\" format=\"" + tileFormat + "\" width=\"" + imageWidth
										+ "\" height=\"" + imageHeight + "\" tileWidth=\"" + tileSize + "\" tileHeight=\"" + tileSize
										+ "\" micronsPerPixel=\"" + mpp + "\"/></collection>"), output, 0);
								output.closeEntry();
							}
							
							Tools.debugPrint(imageWidth, imageHeight, predefinedChannelsFor(reader));
							
							final Collection<Exception> problems = synchronizedList(new ArrayList<>()); 
							final TicToc timer = new TicToc();
							
							Tools.debugPrint(new Date(timer.tic()));
							
							final Level level0 = new Level(reader, tileSize);
							
							while (level0.next0(problems, output, tileFormat)) {
								final int tileY = level0.getTileY();
								Tools.debugPrint(tileY, timer.toc(), 1000L * tileY * imageWidth / max(1L, timer.toc()));
							}
							
							for (final Exception problem : problems) {
								problem.printStackTrace();
							}
						} catch (final Exception exception) {
							exception.printStackTrace();
						}
					}
				}
			}
			
			private static final long serialVersionUID = 7631423500885984364L;
			
		});
	}
	
	/**
	 * @author codistmonk (creation 2015-02-21)
	 */
	public static final class Level implements Serializable {
		
		private final IFormatReader reader;
		
		private final Level previousLevel;
		
		private final byte[][] buffers;
		
		private final int tileSize, channelCount, imageWidth, imageHeight, bufferRowSize;
		
		private int bufferIndex, tileY, h;
		
		private Level nextLevel;
		
		public Level(final Level previousLevel) {
			this.reader = previousLevel.reader;
			this.previousLevel = previousLevel;
			this.tileSize = previousLevel.tileSize;
			this.channelCount = previousLevel.channelCount;
			this.imageWidth = previousLevel.imageWidth / 2;
			this.imageHeight = previousLevel.imageHeight / 2;
			this.bufferRowSize = this.imageWidth * getBytesPerPixel(this.reader);
			this.buffers = new byte[2][this.tileSize * this.bufferRowSize];
			
			if (2 <= this.imageWidth && 2 <= this.imageHeight) {
				this.nextLevel = new Level(this);
			}
		}
		
		public Level(final IFormatReader reader, final int tileSize) {
			this.reader = reader;
			this.previousLevel = null;
			this.tileSize = tileSize;
			this.channelCount = predefinedChannelsFor(reader).getChannelCount();
			this.imageWidth = reader.getSizeX();
			this.imageHeight = reader.getSizeY();
			this.bufferRowSize = this.imageWidth * getBytesPerPixel(reader);
			this.buffers = new byte[2][this.tileSize * this.bufferRowSize];
			
			if (2 <= this.imageWidth && 2 <= this.imageHeight) {
				this.nextLevel = new Level(this);
			}
		}
		
		public final boolean next0(final Collection<Exception> problems, final ZipOutputStream output, final String tileFormat) {
			this.h = min(this.tileSize, this.imageHeight - this.tileY);
			final byte[] buffer = this.buffers[this.bufferIndex];
			
			try {
				this.reader.openBytes(0, buffer, 0, this.tileY, this.imageWidth, this.h);
			} catch (final Exception exception) {
				problems.add(exception);
			}
			
			final int tileY0 = this.getTileY();
			final Map<Thread, Canvas> tiles = new HashMap<>();
			
			{
				final TaskManager tasks = new TaskManager(1.0);
				
				for (int tileX = 0; tileX < this.imageWidth && problems.isEmpty(); tileX += this.tileSize) {
					final int tileX0 = tileX;
					
					tasks.submit(new Runnable() {
						
						@Override
						public void run() {
							if (!problems.isEmpty()) {
								return;
							}
							
							final Canvas tile = tiles.computeIfAbsent(Thread.currentThread(), t -> new Canvas());
							final int w = min(Level.this.tileSize, Level.this.imageWidth - tileX0);
							
							tile.setFormat(w, Level.this.h, BufferedImage.TYPE_3BYTE_BGR);
							
							for (int y = 0; y < Level.this.h; ++y) {
								for (int x = 0; x < w; ++x) {
									tile.getImage().setRGB(x, y, getPixelValueFromBuffer(Level.this.reader, buffer, Level.this.imageWidth, Level.this.h, Level.this.channelCount, tileX0 + x, y));
								}
							}
							
							if (Level.this.nextLevel != null) {
								for (int y = 0; y < (Level.this.h & ~1); y += 2) {
									for (int x = 0; x < (w & ~1); x += 2) {
										final int rgb00 = tile.getImage().getRGB(x, y);
										final int rgb01 = tile.getImage().getRGB(x, y + 1);
										final int rgb10 = tile.getImage().getRGB(x + 1, y);
										final int rgb11 = tile.getImage().getRGB(x + 1, y + 1);
										final int r = ((rgb00 & 0x00FF0000) + (rgb01 & 0x00FF0000) + (rgb10 & 0x00FF0000) + (rgb11 & 0x00FF0000)) / 4; 
										final int g = ((rgb00 & 0x0000FF00) + (rgb01 & 0x0000FF00) + (rgb10 & 0x0000FF00) + (rgb11 & 0x0000FF00)) / 4; 
										final int b = ((rgb00 & 0x000000FF) + (rgb01 & 0x000000FF) + (rgb10 & 0x000000FF) + (rgb11 & 0x000000FF)) / 4;
										final int rgb = 0xFF000000 | r | g | b;
										
									}
								}
							}
							
							final String tileName = "tile_lod0_y" + tileY0 + "_x" + tileX0 + "." + tileFormat;
							final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
							
							try (final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter(tileFormat).setCompressionQuality(0.9F).setOutput(tmp)) {
								imageWriter.write(tile.getImage());
								
								synchronized (output) {
									output.putNextEntry(new ZipEntry(tileName));
									output.write(tmp.toByteArray());
									output.closeEntry();
								}
							} catch (final Exception exception) {
								exception.printStackTrace();
								
								problems.add(exception);
							}
						}
						
					});
				}
				
				tasks.join();
			}
			
			this.tileY += this.tileSize;
			this.bufferIndex = (this.bufferIndex + 1) & 1;
			
			return this.tileY < this.imageHeight && problems.isEmpty();
		}
		
		final int getTileY() {
			return this.tileY;
		}
		
		private static final long serialVersionUID = -5885557044465685071L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-21)
	 */
	public static final class LevelN implements Serializable {
		
		private int tileY;
		
		private final int[] buffer;
		
		private final int levelWidth, levelHeight, tileSize;
		
		private final LevelN nextLevel;
		
		public LevelN(final int tileSize, final int levelWidth, final int levelHeight) {
			this.buffer = new int[tileSize * levelWidth];
			this.levelWidth = levelWidth;
			this.levelHeight = levelHeight;
			this.tileSize = tileSize;
			this.nextLevel = 2 <= levelWidth && 2 <= levelHeight ? new LevelN(tileSize, levelWidth / 2, levelHeight / 2) : null;
		}
		
		public final void setRGB(final int xInLevel, final int yInLevel, final int rgb) {
			this.buffer[(yInLevel - this.tileY) * this.levelWidth + xInLevel] = rgb;
			
			if (yInLevel % this.tileSize == 0 || yInLevel == this.levelHeight - 1) {
				final boolean lastXReached = xInLevel == this.levelWidth - 1;
				
				if (xInLevel % this.tileSize == 0 || lastXReached) {
					final int h = min(this.tileSize, this.levelHeight - this.tileY);
					
					{
						final Canvas tile = new Canvas();
						
						for (int tileX = 0; tileX < this.levelWidth; tileX += this.tileSize) {
							final int w = min(this.tileSize, this.levelWidth - tileX);
							
							tile.setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR);
							
							for (int y = 0; y < h; ++y) {
								for (int x = 0; x < w; ++x) {
									tile.getImage().setRGB(x, y, this.buffer[y * this.levelWidth + tileX + x]);
								}
							}
							
							// TODO save tile
							
							if (this.nextLevel != null) {
								for (int y = 0; y < h; y += 2) {
									for (int x = 0; x < w; x += 2) {
										final int rgb00 = tile.getImage().getRGB(x, y);
										final int rgb01 = tile.getImage().getRGB(x, y + 1);
										final int rgb10 = tile.getImage().getRGB(x + 1, y);
										final int rgb11 = tile.getImage().getRGB(x + 1, y + 1);
										final int r = ((rgb00 & 0x00FF0000) + (rgb01 & 0x00FF0000) + (rgb10 & 0x00FF0000) + (rgb11 & 0x00FF0000)) / 4; 
										final int g = ((rgb00 & 0x0000FF00) + (rgb01 & 0x0000FF00) + (rgb10 & 0x0000FF00) + (rgb11 & 0x0000FF00)) / 4; 
										final int b = ((rgb00 & 0x000000FF) + (rgb01 & 0x000000FF) + (rgb10 & 0x000000FF) + (rgb11 & 0x000000FF)) / 4;
										final int nextRGB = 0xFF000000 | r | g | b;
										
										this.nextLevel.setRGB((tileX + x) / 2, (this.tileY + y) / 2, nextRGB);
									}
								}
							}
						}
					}
					
					if (lastXReached) {
						this.tileY += this.tileSize;
					}
				}
			}
		}
		
		private static final long serialVersionUID = -1013722312568926426L;
		
	}
	
	public static final int getPixelValueFromBuffer(final IFormatReader reader, final byte[] buffer, final int bufferWidth, final int bufferHeight, final int channelCount, final int xInBuffer, final int yInBuffer) {
		final int bytesPerChannel = FormatTools.getBytesPerPixel(reader.getPixelType());
		int result = 0;
		
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
				result = (result << 8) | (buffer[pixelFirstByteIndex + i * tileChannelByteCount] & 0x000000FF);
			}
		}
		
		// XXX Is it always ok to assume RGBA and convert to ARGB if channelCount == 4?
		return channelCount == 4 ? (result >> 8) | (result << 24) : result;
	}
	
	public static final IFormatReader newImageReader(final String id) {
		final IFormatReader reader = new ImageReader();
		
		try {
			reader.setId(id);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		if ("portable gray map".equals(reader.getFormat().toLowerCase(Locale.ENGLISH))) {
			// XXX This fixes a defect in Bio-Formats PPM loading, but is it always OK?
			reader.getCoreMetadata()[0].interleaved = true;
		}
		
		reader.setSeries(0);
		
		return reader;
	}
	
	public static final int packPixelValue(final byte[][] channelTables, final int colorIndex) {
		int result = 0;
		
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
			return Channels.Predefined.C3_U8;
		case 4:
			return Channels.Predefined.C4_U8;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public static final int getBytesPerPixel(final IFormatReader lociImage) {
		return FormatTools.getBytesPerPixel(lociImage.getPixelType()) * lociImage.getRGBChannelCount();
	}
	
}
