package imj2.draft;

import static imj2.tools.IMJTools.getFieldValue;
import static java.lang.Math.min;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;
import static multij.tools.Tools.intRange;
import static multij.tools.Tools.iterable;
import static multij.xml.XMLTools.parse;
import imj2.tools.IMJTools;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import loci.formats.ImageReader;
import loci.formats.tiff.TiffCompression;
import loci.formats.tiff.TiffParser;
import multij.tools.ConsoleMonitor;
import multij.tools.IllegalInstantiationException;
import multij.tools.RegexFilter;
import multij.tools.TicToc;
import multij.tools.Tools;
import multij.xml.XMLTools;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author codistmonk (creation 2014-05-24)
 */
public final class SVS2Zip {
	
	private SVS2Zip() {
		throw new IllegalInstantiationException();
	}
	
	public static final void main(final String[] commandLineArguments) throws Exception {
		final ConsoleMonitor monitor = new ConsoleMonitor(5000L);
		final boolean keepLabel = false;
		final boolean keepMacro = false;
		final int forcedTileSize = 512;
		
		((Logger) LoggerFactory.getLogger(TiffParser.class)).setLevel(Level.INFO);
		((Logger) LoggerFactory.getLogger(TiffCompression.class)).setLevel(Level.INFO);
		
		final File[] files = new File("E:/sysimit/data/Pilot_Series_Final").listFiles(
				RegexFilter.newSuffixFilter("_00[4567]A.svs"));
		
		for (final File file : files) {
			final String imageId = file.getPath();
			
			process(imageId, forcedTileSize, keepLabel, keepMacro, monitor);
		}
	}
	
	public static final void process(final String imageId, final int forcedTileSize
			, final boolean keepLabel, final boolean keepMacro
			, final ConsoleMonitor monitor) throws Exception {
		final TicToc timer = new TicToc();
		
		debugPrint(imageId);
		
//		final String baseName = baseName(imageId);
		final String baseName = baseName(new File(imageId).getName());
		
		if (true) {
			try (final ImageReader image = new ImageReader()) {
				debugPrint("Processing...", new Date(timer.tic()));
				
				image.setId(imageId);
				
				final Document metadata = parse("<image/>");
				
				metadata.getDocumentElement().setAttribute("micronsPerPixel"
						, "" + Array.getFloat(getFieldValue(image.getReader(), "pixelSize"), 0));
				
				final String[] comments = ((String[]) getFieldValue(image.getReader(), "comments")).clone();
				
				for (final int svsIndex : intRange(image.getSeriesCount())) {
					image.setSeries(svsIndex);
					
					final int w = image.getSizeX();
					final int h = image.getSizeY();
					final int dx = forcedTileSize <= 0 ? image.getOptimalTileWidth() : min(forcedTileSize, w);
					final int dy = forcedTileSize <= 0 ? image.getOptimalTileHeight() : min(forcedTileSize, h);
					
					debugPrint(svsIndex, comments[svsIndex], w, h, dx, dy);
					
					final Element subimage = metadata.createElement("subimage");
					
					if (comments[svsIndex].startsWith("label ")) {
						if (keepLabel) {
							subimage.setAttribute("type", "svs_slide_label");
						} else {
							comments[svsIndex] = null;
							continue;
						}
					} else if (comments[svsIndex].startsWith("macro ")) {
						if (keepMacro) {
							subimage.setAttribute("type", "svs_slide_macro");
						} else {
							comments[svsIndex] = null;
							continue;
						}
					} else {
						subimage.setAttribute("type", "svs_slide_lod");
					}
					
					subimage.setAttribute("id", "" + svsIndex);
					subimage.setAttribute("width", "" + w);
					subimage.setAttribute("height", "" + h);
					subimage.setAttribute("tileWidth", "" + dx);
					subimage.setAttribute("tileHeight", "" + dy);
					
					metadata.getDocumentElement().appendChild(subimage);
				}
				
				final String outputFormat = "jpg";
				final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
						new FileOutputStream(baseName + ".zip"));
//							new FileOutputStream("tmp" + ".zip"));
				
				try (final ZipArchiveOutputStream output = new ZipArchiveOutputStream(bufferedOutputStream)
				; final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter(outputFormat)) {
					imageWriter.setCompressionQuality(0.9F).setOutput(output);
					output.setLevel(ZipOutputStream.STORED);
					
					output.putArchiveEntry(output.createArchiveEntry(new File(""), "metadata.xml"));
					XMLTools.write(metadata, output, 0);
					output.closeArchiveEntry();
					final int[] svsIndices = intRange(image.getSeriesCount());
					
					image.setSeries(0);
					
					for (int svsIndexIndex = 0; svsIndexIndex < svsIndices.length; ++svsIndexIndex) {
						final int svsIndex = svsIndices[svsIndexIndex];
						if (comments[svsIndex] == null) {
							continue;
						}
						
						final int higherW = image.getSizeX();
						final int higherH = image.getSizeY();
						
						image.setSeries(svsIndex);
						
						final int w = image.getSizeX();
						final int h = image.getSizeY();
						final int scaleUp = (int) min((double) higherW / w, (double) higherH / h);
						final int dx = forcedTileSize <= 0 ? image.getOptimalTileWidth() : min(forcedTileSize, w);
						final int dy = forcedTileSize <= 0 ? image.getOptimalTileHeight() : min(forcedTileSize, h);
						final int higherDx = min(higherW, dx * scaleUp);
						final int higherDy = min(higherH, dy * scaleUp);
						byte[] higherBuffer = null; // workaround for a defect in SVS reader
						
						final byte[] buffer = new byte[dx * dy * 3];
						BufferedImage awtImage = new BufferedImage(dx, dy, BufferedImage.TYPE_3BYTE_BGR);
						
						for (int tileY = 0; tileY < h; tileY += dy) {
							monitor.ping(svsIndex + " " + tileY + "/" + h + "\r");
							
							final int nextTileY = min(h, tileY + dy);
							final int tileH = nextTileY - tileY;
							
							for (int tileX = 0; tileX < w; tileX += dx) {
								final int nextTileX = min(w, tileX + dx);
								final int tileW = nextTileX - tileX;
								final int planeSize = tileW * tileH;
								
								if (tileW != awtImage.getWidth() || tileH != awtImage.getHeight()) {
									awtImage = new BufferedImage(tileW, tileH, BufferedImage.TYPE_3BYTE_BGR);
								}
								
								try {
									image.openBytes(0, buffer, tileX, tileY, tileW, tileH);
									
									for (int y = tileY, r = planeSize * 0, g = planeSize * 1, b = planeSize * 2; y < nextTileY; ++y) {
										for (int x = tileX; x < nextTileX; ++x, ++r, ++g, ++b) {
											final int red = buffer[r] & 0xFF;
											final int green = buffer[g] & 0xFF;
											final int blue = buffer[b] & 0xFF;
											final int rgb = IMJTools.a8r8g8b8(0xFF, red, green, blue);
											
											awtImage.setRGB(x - tileX, y - tileY, rgb);
										}
									}
								} catch (final Exception exception) {
									Tools.debugError(svsIndex, tileX, tileY, tileW, tileH);
									
									exception.printStackTrace();
									
									// At this point, if the call to openBytes() has failed
									// then we try to compute each pixel of the tile from patches
									// extracted from the higher resolution (previous) image
									
									if (higherBuffer == null) {
										higherBuffer = new byte[higherDx * higherDy * 3];
									}
									
									final int n = scaleUp * scaleUp;
									final int higherTileW = tileW * scaleUp;
									final int higherTileH = tileH * scaleUp;
									final int higherPlaneSize = higherTileW * higherTileH;
									final int higherTileX0 = tileX * scaleUp;
									final int higherTileY0 = tileY * scaleUp;
									
									image.setSeries(svsIndices[svsIndex - 1]);
									image.openBytes(0, higherBuffer, higherTileX0, higherTileY0, higherTileW, higherTileH);
									image.setSeries(svsIndices[svsIndex]);
									
									for (int higherTileY = higherTileY0, y = tileY; higherTileY < higherTileY0 + higherTileH; higherTileY += scaleUp, ++y) {
										for (int higherTileX = higherTileX0, x = tileX; higherTileX < higherTileX0 + higherTileW; higherTileX += scaleUp, ++x) {
											final int[] rgb = new int[3];
											
											for (int yy = higherTileY; yy < higherTileY + scaleUp; ++yy) {
												for (int xx = higherTileX; xx < higherTileX + scaleUp; ++xx) {
													final int r = higherTileY * higherTileW + higherTileX;
													final int g = r + higherPlaneSize;
													final int b = g + higherPlaneSize;
													rgb[0] += higherBuffer[r] & 0xFF;
													rgb[1] += higherBuffer[g] & 0xFF;
													rgb[2] += higherBuffer[b] & 0xFF;
												}
											}
											
											awtImage.setRGB(x, y, IMJTools.a8r8g8b8(0xFF, rgb[0] / n, rgb[1] / n, rgb[2] / n));
										}
									}
								}
								
								output.putArchiveEntry(output.createArchiveEntry(new File("")
										, new File(baseName).getName() + "_svs" + svsIndex
										+ "_" + tileX + "_" + tileY + "." + outputFormat));
								imageWriter.write(awtImage);
								output.closeArchiveEntry();
							}
						}
					}
				}
				
				monitor.pause();
				
				debugPrint("Processing done in", timer.toc(), "ms");
			}
		}
		
		{
			debugPrint("Testing...", new Date(timer.tic()));
			
			try (final ZipFile input = new ZipFile(baseName + ".zip")) {
				final Map<String, ZipArchiveEntry> map = new HashMap<>();
				
				for (final ZipArchiveEntry entry : iterable(input.getEntries())) {
					map.put(entry.getName(), entry);
				}
				
				final List<String> keys = new ArrayList<>(map.keySet());
				
				Collections.shuffle(keys);
				
				for (final String key : keys) {
					final ZipArchiveEntry entry = map.get(key);
					
					monitor.ping(entry.getName() + "\r");
					
					final BufferedImage image = ImageIO.read(input.getInputStream(entry));
				}
			}
			
			monitor.pause();
			
			debugPrint("Testing done in", timer.toc(), "ms");
		}
	}
	
}
