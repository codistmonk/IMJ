package imj3.draft;

import static imj3.tools.BioFormatsImage2D.newImageReader;
import static java.lang.Math.min;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugError;
import static multij.tools.Tools.debugPrint;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.ZipOutputStream;

import imj3.tools.IMJTools;
import imj3.tools.MultifileImage2D;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.MathTools.VectorStatistics;
import multij.tools.TicToc;

/**
 * @author codistmonk (creation 2015-12-12)
 */
public final class Tiff2Multifile {
	
	static float compressionQuality = 0.9F;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String fileName = arguments.get("file", "");
		final File outRoot = new File(arguments.get("out", "."));
		compressionQuality = Float.parseFloat(arguments.get("compressionQuality", "0.9"));
		// TODO Auto-generated method stub
		
		IMJTools.toneDownBioFormatsLogger();
		
		final File file = new File(fileName);
		final File outputFile = new File(outRoot, baseName(fileName) + ".zip");
		
		if (outputFile.isFile() && false) {
			debugError("Ignoring", file);
		} else {
			final String tileFormat = MultifileImage2D.TILE_FORMAT;
			final TicToc timer = new TicToc();
			
			debugPrint("Processing", file, new Date(timer.tic()));
			
			try (final ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outputFile))) {
				final IFormatReader reader = newImageReader(file.getPath());
				final int imageWidth = reader.getSizeX();
				final int imageHeight = reader.getSizeY();
				final int tileSize = 512;
				final int[] level = { 0 };
				final VectorStatistics channelStatistics = new VectorStatistics(reader.getSizeC());
				
				debugPrint(reader.getSizeC(), reader.getEffectiveSizeC(), reader.isRGB(), reader.isFalseColor(), reader.isInterleaved(), reader.getBitsPerPixel(), reader.getRGBChannelCount());
				debugPrint(imageWidth, imageHeight, reader.getOptimalTileWidth(), reader.getOptimalTileHeight());
				debugPrint(FormatTools.getPixelTypeString(reader.getPixelType()));
				
				if (reader.isInterleaved()) {
					debugPrint("Interleaved");
					
					final byte[] bytes = new byte[imageWidth * tileSize * reader.getBitsPerPixel() / 8 * reader.getSizeC()];
					
					for (int y = 0; y < imageHeight; y += tileSize) {
						reader.openBytes(0, bytes, 0, y, imageWidth, min(tileSize, imageHeight - y));
					}
				} else {
					debugPrint("Not interleaved");
					
					final byte[] buffer = new byte[imageWidth * tileSize * reader.getBitsPerPixel() / 8 * reader.getSizeC()];
					final ByteBuffer bytes = ByteBuffer.wrap(buffer);
					
					bytes.order(reader.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
					
					for (int y = 0; y < imageHeight; y += tileSize) {
						final int tileHeight = min(tileSize, imageHeight - y);
						
						reader.openBytes(0, buffer, 0, y, imageWidth, tileHeight);
						
						switch (reader.getPixelType()) {
						case FormatTools.INT8:
						case FormatTools.UINT8:
							for (int channel = 0; channel < reader.getSizeC(); ++channel) {
								for (int yy = 0; yy < tileHeight; ++yy) {
									for (int xx = 0; xx < imageWidth; ++xx) {
										channelStatistics.getStatistics()[channel].addValue(0xFF & bytes.get(xx + imageWidth * (yy + tileHeight * channel)));
									}
								}
							}
							
							break;
						case FormatTools.INT16:
						case FormatTools.UINT16:
							final ShortBuffer shorts = bytes.asShortBuffer();
							
							for (int channel = 0; channel < reader.getSizeC(); ++channel) {
								for (int yy = 0; yy < tileHeight; ++yy) {
									for (int xx = 0; xx < imageWidth; ++xx) {
										channelStatistics.getStatistics()[channel].addValue(0xFFFF & shorts.get(xx + imageWidth * (yy + tileHeight * channel)));
									}
								}
							}
							
							break;
						case FormatTools.INT32:
						case FormatTools.UINT32:
							final IntBuffer ints = bytes.asIntBuffer();
							
							for (int channel = 0; channel < reader.getSizeC(); ++channel) {
								for (int yy = 0; yy < tileHeight; ++yy) {
									for (int xx = 0; xx < imageWidth; ++xx) {
										channelStatistics.getStatistics()[channel].addValue(0xFFFFFFFFL & ints.get(xx + imageWidth * (yy + tileHeight * channel)));
									}
								}
							}
							
							break;
						case FormatTools.FLOAT:
							final FloatBuffer floats = bytes.asFloatBuffer();
							
							for (int channel = 0; channel < reader.getSizeC(); ++channel) {
								for (int yy = 0; yy < tileHeight; ++yy) {
									for (int xx = 0; xx < imageWidth; ++xx) {
										channelStatistics.getStatistics()[channel].addValue(floats.get(xx + imageWidth * (yy + tileHeight * channel)));
									}
								}
							}
							
							break;
						case FormatTools.DOUBLE:
							final DoubleBuffer doubles = bytes.asDoubleBuffer();
							
							for (int channel = 0; channel < reader.getSizeC(); ++channel) {
								for (int yy = 0; yy < tileHeight; ++yy) {
									for (int xx = 0; xx < imageWidth; ++xx) {
										channelStatistics.getStatistics()[channel].addValue(doubles.get(xx + imageWidth * (yy + tileHeight * channel)));
									}
								}
							}
							
							break;
						default:
							throw new IllegalArgumentException();
						}
					}
				}
				
				debugPrint("minima:", Arrays.toString(channelStatistics.getMinima()));
				debugPrint("means:", Arrays.toString(channelStatistics.getMeans()));
				debugPrint("maxima:", Arrays.toString(channelStatistics.getMaxima()));
				/*
				if (reader.getSizeC() <= 3) {
					
				} else {
					
				}
				
				switch (reader.getPixelType()) {
				case FormatTools.INT8:
				case FormatTools.UINT8:
					break;
				case FormatTools.INT16:
				case FormatTools.UINT16:
					break;
				case FormatTools.INT32:
				case FormatTools.UINT32:
					break;
				case FormatTools.FLOAT:
					break;
				case FormatTools.DOUBLE:
					break;
				default:
					throw new IllegalArgumentException();
				}
				*/
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
		}
	}
	
}
