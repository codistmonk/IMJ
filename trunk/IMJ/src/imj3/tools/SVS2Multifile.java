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
							final int channelCount = predefinedChannelsFor(reader).getChannelCount();
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
							
							final int bufferRowSize = imageWidth * getBytesPerPixel(reader);
							final byte[] buffer = new byte[tileSize * bufferRowSize];
							final Map<Thread, Canvas> tiles = new HashMap<>();
							final TicToc timer = new TicToc();
							
							Tools.debugPrint(new Date(timer.tic()));
							
							for (int tileY = 0; tileY < imageHeight; tileY += tileSize) {
								final int tileY0 = tileY;
								Tools.debugPrint(tileY, timer.toc(), 1000L * tileY * imageWidth / max(1L, timer.toc()));
								
								final int h = min(tileSize, imageHeight - tileY);
								
								reader.openBytes(0, buffer, 0, tileY, imageWidth, h);
								
								final TaskManager tasks = new TaskManager(1.0);
								final Collection<Exception> problems = synchronizedList(new ArrayList<>()); 
								
								Tools.debugPrint("Threads:", tasks.getWorkerCount());
								
								for (int tileX = 0; tileX < imageWidth; tileX += tileSize) {
									final int tileX0 = tileX;
									
									tasks.submit(new Runnable() {
										
										@Override
										public void run() {
											if (!problems.isEmpty()) {
												return;
											}
											
											final Canvas tile = tiles.computeIfAbsent(Thread.currentThread(), t -> new Canvas());
											final int w = min(tileSize, imageWidth - tileX0);
											
											tile.setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR);
											
											for (int y = 0; y < h; ++y) {
												for (int x = 0; x < w; ++x) {
													tile.getImage().setRGB(x, y, getPixelValueFromBuffer(reader, buffer, imageWidth, h, channelCount, tileX0 + x, y));
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
								
								if (!problems.isEmpty()) {
									break;
								}
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
