package imj2.tools;

import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.intRange;
import static net.sourceforge.aprog.tools.Tools.invoke;
import static net.sourceforge.aprog.tools.Tools.iterable;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.in.SVSReader;
import loci.formats.tiff.TiffParser;
import net.sourceforge.aprog.tools.ConsoleMonitor;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;
import ome.xml.model.Image;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
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
		final TicToc timer = new TicToc();
//		final String imageId = "../Libraries/images/svs/SYS08_A10_7414-005.svs";
//		final String imageId = "../Libraries/images/svs/16088.svs";
		final String imageId = "../Libraries/images/svs/40267.svs";
		final String baseName = baseName(new File(imageId).getName());
		final ConsoleMonitor monitor = new ConsoleMonitor(5000L);
		
		((Logger) LoggerFactory.getLogger(TiffParser.class)).setLevel(Level.INFO);
		
		if (true) {
			try (final ImageReader image = new ImageReader()) {
				debugPrint("Processing...", new Date(timer.tic()));
				
				image.setId(imageId);
				
				final Document metadata = parse("<image/>");
				
				{
					final SVSReader svs = (SVSReader) image.getReader();
					final Field pixelSize = SVSReader.class.getDeclaredField("pixelSize");
					
					pixelSize.setAccessible(true);
					metadata.getDocumentElement().setAttribute("micronsPerPixel"
							, "" + Array.getFloat(pixelSize.get(svs), 0));
				}
				
				for (final int svsIndex : intRange(image.getSeriesCount())) {
					image.setSeries(svsIndex);
					
					final int w = image.getSizeX();
					final int h = image.getSizeY();
					final int dx = image.getOptimalTileWidth();
					final int dy = image.getOptimalTileHeight();
					final Element subimage = metadata.createElement("subimage");
					
					subimage.setAttribute("id", "" + svsIndex);
					subimage.setAttribute("width", "" + w);
					subimage.setAttribute("height", "" + h);
					subimage.setAttribute("tileWidth", "" + dx);
					subimage.setAttribute("tileHeight", "" + dy);
					
					metadata.getDocumentElement().appendChild(subimage);
				}
				
				try (final ZipOutputStream output = new ZipOutputStream(
						new BufferedOutputStream(new FileOutputStream(baseName + ".zip")))) {
					
					output.putNextEntry(new ZipEntry("metadata.xml"));
					XMLTools.write(metadata, output, 0);
					
					for (final int svsIndex : intRange(image.getSeriesCount())) {
						image.setSeries(svsIndex);
						
						final int w = image.getSizeX();
						final int h = image.getSizeY();
						final int dx = image.getOptimalTileWidth();
						final int dy = image.getOptimalTileHeight();
						
						debugPrint(w, h, dx, dy);
						
						final int planeSize = dx * dy;
						final byte[] buffer = new byte[planeSize * 3];
						BufferedImage awtImage = new BufferedImage(dx, dy, BufferedImage.TYPE_3BYTE_BGR);
						
						for (int tileY = 0; tileY < h; tileY += dy) {
							monitor.ping(tileY + "/" + h + "\r");
							
							final int nextTileY = min(h, tileY + dy);
							final int tileH = nextTileY - tileY;
							
							for (int tileX = 0; tileX < w; tileX += dx) {
								final int nextTileX = min(w, tileX + dx);
								final int tileW = nextTileX - tileX;
								
								image.openBytes(0, buffer, tileX, tileY, tileW, tileH);
								
								if (tileW != awtImage.getWidth() || tileH != awtImage.getHeight()) {
									awtImage = new BufferedImage(tileW, tileH, BufferedImage.TYPE_3BYTE_BGR);
								}
								
								for (int y = tileY, r = planeSize * 0, g = planeSize * 1, b = planeSize * 2; y < nextTileY; ++y) {
									for (int x = tileX; x < nextTileX; ++x, ++r, ++g, ++b) {
										final int red = buffer[r] & 0xFF;
										final int green = buffer[g] & 0xFF;
										final int blue = buffer[b] & 0xFF;
										final int rgb = IMJTools.a8r8g8b8(0xFF, red, green, blue);
										
										awtImage.setRGB(x - tileX, y - tileY, rgb);
									}
								}
								
								final String outputFormat = "jpg";
								
								output.putNextEntry(new ZipEntry(baseName + "_svs" + svsIndex
										+ "_" + tileX + "_" + tileY + "." + outputFormat));
								ImageIO.write(awtImage, outputFormat, output);
							}
						}
					}
				}
				
				monitor.pause();
				
				debugPrint("Processing done in", timer.toc(), "ms");
			}
		}
		
		if (true) {
			debugPrint("Testing...", new Date(timer.tic()));
			
			try (final ZipInputStream input = new ZipInputStream(
					new BufferedInputStream(new FileInputStream(baseName + ".zip")))) {
				for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
					monitor.ping(entry.getName() + "\r");
					
					final BufferedImage image = ImageIO.read(input);
				}
			}
			
			monitor.pause();
			
			debugPrint("Testing done in", timer.toc(), "ms");
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
