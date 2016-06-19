package imj3.tools;

import static imj3.tools.BioFormatsImage2D.*;
import static imj3.tools.CommonTools.getFieldValue;
import static imj3.tools.MultifileImage2D.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.synchronizedList;
import static multij.tools.Tools.*;
import imj3.tools.CommonTools.FileProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.ConsoleMonitor;
import multij.tools.IllegalInstantiationException;
import multij.tools.RegexFilter;
import multij.tools.SystemProperties;
import multij.tools.TaskManager;
import multij.tools.TicToc;
import multij.tools.Tools;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2014-11-30)
 */
public final class SVS2Multifile {
	
	private SVS2Multifile() {
		throw new IllegalInstantiationException();
	}
	
	public static final int R = 0x00FF0000;
	
	public static final int G = 0x0000FF00;
	
	public static final int B = 0x000000FF;
	
	static float compressionQuality = 0.9F;
	
	static double levelCPULoad = 0.0;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String pathsAsString = arguments.get("files", "");
		final String[] paths = pathsAsString.split(":");
		final File inRoot = new File(arguments.get("in", ""));
		final RegexFilter filter = new RegexFilter(arguments.get("filter", ".*\\.svs"));
		final File outRoot = new File(arguments.get("out", "."));
		final int threads = max(1, arguments.get("threads", SystemProperties.getAvailableProcessorCount() / 2)[0]);
		final boolean includeHTMLViewer = arguments.get1("includeHTMLViewer", 1) != 0;
		final int tileSide = arguments.get1("tileSide", 512);
		final String defaultMicronsPerPixel = arguments.get("defaultMicronsPerPixel", "0.25");
		final String tileFormat = arguments.get("tileFormat", MultifileImage2D.TILE_FORMAT);
		
		compressionQuality = Float.parseFloat(arguments.get("compressionQuality", "0.9"));
		levelCPULoad = 1.0 / threads;
		
		IMJTools.toneDownBioFormatsLogger();
		
		final TaskManager tasks = new TaskManager((double) threads / SystemProperties.getAvailableProcessorCount());
		
		debugPrint(tasks.getWorkerCount());
		
		if (!pathsAsString.isEmpty()) {
			Arrays.stream(paths).forEach(p -> tasks.submit(() -> process(new File(p), null, tileSide, defaultMicronsPerPixel, tileFormat, outRoot, includeHTMLViewer)));
		} else {
			FileProcessor.deepForEachFileIn(inRoot, f -> tasks.submit(() -> process(f, filter, tileSide, defaultMicronsPerPixel, tileFormat, outRoot, includeHTMLViewer)));
		}
		
		tasks.join();
	}
	
	public static final void process(final File file, final FilenameFilter filter,
			final int tileSide, final String defaultMicronsPerPixels, final String tileFormat, final File outRoot, final boolean includeHTMLViewer) {
		final String fileName = file.getName();
		
		if (!fileName.isEmpty() && (filter == null || filter.accept(file.getParentFile(), fileName))) {
			final File outputFile = new File(outRoot, baseName(fileName) + ".zip");
			
			if (file.getPath().contains("/old/") || outputFile.isFile()) {
				debugError("Ignoring", file);
			} else {
				final TicToc timer = new TicToc();
				
				debugPrint("Processing", file, new Date(timer.tic()));
				
				try (final ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outputFile))) {
					final IFormatReader reader = newImageReader(file.getPath());
					final int imageWidth = reader.getSizeX();
					final int imageHeight = reader.getSizeY();
					final int[] level = { 0 };
					
					{
						String mpp = defaultMicronsPerPixels;
						
						try {
							mpp = Array.get(getFieldValue(((ImageReader) reader).getReader(), "pixelSize"), 0).toString();
						} catch (final Exception exception) {
							debugError(exception);
						}
						
						final Document xml = newMetadata(imageWidth, imageHeight, tileSide, tileSide, tileFormat, mpp, level);
						
						output.putNextEntry(new ZipEntry("metadata.xml"));
						XMLTools.write(xml, output, 0);
						output.closeEntry();
					}
					
					if (includeHTMLViewer) {
						includeHTMLViewer(output, imageWidth, imageHeight, tileSide, level[0] - 1, tileFormat);
					}
					
					debugPrint(fileName, imageWidth, imageHeight, predefinedChannelsFor(reader));
					
					final Collection<Exception> problems = synchronizedList(new ArrayList<>());
					final ConsoleMonitor monitor = new ConsoleMonitor(30_000L);
					final Level0 level0 = new Level0(reader, tileSide, tileFormat, output, problems);
					
					while (level0.next()) {
						if (monitor.ping()) {
							final int tileY = level0.getTileY();
							
							debugPrint(fileName, "tileY:", tileY, "time(s):", timer.toc() / 1_000L,
									"rate(px/s):", 1_000L * tileY * imageWidth / max(1L, timer.toc()));
						}
					}
					
					timer.toc();
					
					debugPrint(fileName, "done in", timer.getTotalTime(), "ms");
					
					for (final Exception problem : problems) {
						problem.printStackTrace();
					}
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
		}
	}
	
	public static final void includeHTMLViewer(final ZipOutputStream output,
			final int imageWidth, final int imageHeight, final int tileSize,
			final int lastLOD, final String tileFormat) throws IOException {
		try (final ZipInputStream template = new ZipInputStream(getResourceAsStream("lib/openseadragon/template.zip"))) {
			for (ZipEntry entry = template.getNextEntry(); entry != null; entry = template.getNextEntry()) {
				output.putNextEntry(new ZipEntry(entry.getName()));
				Tools.write(template, output);
				output.closeEntry();
			}
		}
		
		{
			output.putNextEntry(new ZipEntry("index_files/imj_metadata.js"));
			
			final PrintStream out = new PrintStream(output);
			
			out.println("var tilePrefix = \"" + TILE_PREFIX +"\";");
			out.println("var imageWidth = " + imageWidth +";");
			out.println("var imageHeight = " + imageHeight +";");
			out.println("var tileSize = " + tileSize +";");
			out.println("var lastLOD = " + lastLOD +";");
			out.println("var tileFormat = \"" + tileFormat +"\";");
			
			output.closeEntry();
		}
	}
	
	public static final Document newMetadata(final int imageWidth, final int imageHeight,
			final int tileSize, final String tileFormat, final String micronsPerPixel) {
		return newMetadata(imageWidth, imageHeight, tileSize, tileSize, tileFormat, micronsPerPixel, new int[1]);
	}
	
	public static final Document newMetadata(final int imageWidth, final int imageHeight,
			final int optimalTileWidth, final int optimalTileHeight, final String tileFormat, final String micronsPerPixel, final int[] level) {
		final Document result = document(() ->
			element("group", () -> {
				final int[] w = { imageWidth };
				final int[] h = { imageHeight };
				
				while (0 < w[0] && 0 < h[0]) {
					element("image", () -> {
						attribute("tilePrefix", TILE_PREFIX);
						attribute("type", "lod" + level[0]);
						attribute("width", w[0]);
						attribute("height", h[0]);
						attribute("tileFormat", tileFormat);
						attribute("tileWidth", optimalTileWidth);
						attribute("tileHeight", optimalTileHeight);
						
						if (0 == level[0]) {
							attribute("micronsPerPixel", micronsPerPixel);
						}
					});
					
					++level[0];
					w[0] /= 2;
					h[0] /= 2;
				}
			})
		);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-21)
	 */
	public static final class Level0 implements Serializable {
		
		private final IFormatReader reader;
		
		private final int tileSize, channelCount, imageWidth, imageHeight, bufferRowSize;
		
		private final byte[] buffer;
		
		private final String tileFormat;
		
		private final ZipOutputStream output;
		
		private final Collection<Exception> problems;
		
		private final LevelN nextLevel;
		
		private int tileY;
		
		public Level0(final IFormatReader reader, final int tileSize, final String tileFormat,
				final ZipOutputStream output, final Collection<Exception> problems) {
			this.reader = reader;
			this.tileSize = tileSize;
			this.channelCount = predefinedChannelsFor(reader).getChannelCount();
			this.imageWidth = reader.getSizeX();
			this.imageHeight = reader.getSizeY();
			this.bufferRowSize = this.imageWidth * getBytesPerPixel(reader);
			this.buffer = new byte[this.tileSize * this.bufferRowSize];
			this.tileFormat = tileFormat;
			this.output = output;
			this.problems = problems;
			this.nextLevel = 2 <= this.imageWidth && 2 <= this.imageHeight ? new LevelN(
					1, channelCount, tileSize, this.imageWidth / 2, this.imageHeight / 2, tileFormat, output, problems) : null;
		}
		
		public final boolean next() {
			final IFormatReader reader = this.reader;
			final int channelCount = this.channelCount;
			final int h = min(this.tileSize, this.imageHeight - this.tileY);
			final byte[] buffer = this.buffer;
			final int tileSize = this.tileSize;
			final int imageWidth = this.imageWidth;
			final String tileFormat = this.tileFormat;
			final ZipOutputStream output = this.output;
			final Collection<Exception> problems = this.problems;
			final LevelN nextLevel = this.nextLevel;
			final int tileY = this.getTileY();
			
			try {
				reader.openBytes(0, buffer, 0, tileY, imageWidth, h);
			} catch (final Exception exception) {
				problems.add(exception);
			}
			
			{
				final Map<Thread, Canvas> tiles = new HashMap<>();
				final TaskManager tasks = new TaskManager(levelCPULoad);
				
				for (int tileX = 0; tileX < imageWidth && problems.isEmpty(); tileX += tileSize) {
					final int tileX0 = tileX;
					
					tasks.submit(new Runnable() {
						
						@Override
						public void run() {
							if (!problems.isEmpty()) {
								return;
							}
							
							final Canvas tile = tiles.computeIfAbsent(Thread.currentThread(), t -> new Canvas());
							final int w = min(tileSize, imageWidth - tileX0);
							
							if (channelCount == 1) {
								tile.setFormat(w, h, BufferedImage.TYPE_BYTE_GRAY);
							} else {
								tile.setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR);
							}
							
							final int[] pixelValue = { 0 };
							
							for (int y = 0; y < h; ++y) {
								for (int x = 0; x < w; ++x) {
									pixelValue[0] = getPixelValueFromBuffer(
											reader, buffer, imageWidth, h, channelCount, tileX0 + x, y);
									
									if (channelCount == 1) {
										tile.getImage().getRaster().setPixel(x, y, pixelValue);
									} else {
										tile.getImage().setRGB(x, y, pixelValue[0]);
									}
								}
							}
							
							if (nextLevel != null) {
								for (int y = 0; y < (h & ~1); y += 2) {
									for (int x = 0; x < (w & ~1); x += 2) {
										if (channelCount == 1) {
											final int v00 = tile.getImage().getRaster().getPixel(x, y, pixelValue)[0];
											final int v01 = tile.getImage().getRaster().getPixel(x, y + 1, pixelValue)[0];
											final int v10 = tile.getImage().getRaster().getPixel(x + 1, y, pixelValue)[0];
											final int v11 = tile.getImage().getRaster().getPixel(x + 1, y + 1, pixelValue)[0];
											final int v = (v00 + v01 + v10 + v11) / 4; 
											
											nextLevel.setRGB((tileX0 + x) / 2, (tileY + y) / 2, v);
										} else {
											final int rgb00 = tile.getImage().getRGB(x, y);
											final int rgb01 = tile.getImage().getRGB(x, y + 1);
											final int rgb10 = tile.getImage().getRGB(x + 1, y);
											final int rgb11 = tile.getImage().getRGB(x + 1, y + 1);
											final int r = (((rgb00 & R) + (rgb01 & R) + (rgb10 & R) + (rgb11 & R)) / 4) & R; 
											final int g = (((rgb00 & G) + (rgb01 & G) + (rgb10 & G) + (rgb11 & G)) / 4) & G; 
											final int b = (((rgb00 & B) + (rgb01 & B) + (rgb10 & B) + (rgb11 & B)) / 4) & B;
											final int rgb = 0xFF000000 | r | g | b;
											
											nextLevel.setRGB((tileX0 + x) / 2, (tileY + y) / 2, rgb);
										}
									}
								}
							}
							
							final String tileName = TILE_PREFIX + "lod0" + "_y" + tileY + "_x" + tileX0 + "." + tileFormat;
							final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
							
							try (final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter(tileFormat)
									.setCompressionQuality(compressionQuality).setOutput(tmp)) {
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
				
				if (nextLevel != null) {
					nextLevel.next();
				}
			}
			
			this.tileY += tileSize;
			
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
		
		private final int n;
		
		private final int channelCount;
		
		private int tileY;
		
		private final int[] buffer;
		
		private final int levelWidth, levelHeight, tileSize;
		
		private final String tileFormat;
		
		private final ZipOutputStream output;
		
		private final Collection<Exception> problems;
		
		private final LevelN nextLevel;
		
		private boolean bufferDone;
		
		public LevelN(final int n, final int channelCount, final int tileSize, final int levelWidth, final int levelHeight,
				final String tileFormat, final ZipOutputStream output, final Collection<Exception> problems) {
			this.n = n;
			this.channelCount = channelCount;
			this.buffer = new int[tileSize * levelWidth];
			this.tileSize = tileSize;
			this.levelWidth = levelWidth;
			this.levelHeight = levelHeight;
			this.tileFormat = tileFormat;
			this.output = output;
			this.problems = problems;
			this.nextLevel = 2 <= levelWidth && 2 <= levelHeight ? new LevelN(
					n + 1, channelCount, tileSize, levelWidth / 2, levelHeight / 2, tileFormat, output, problems) : null;
		}
		
		public final void setRGB(final int xInLevel, final int yInLevel, final int rgb) {
			this.buffer[(yInLevel - this.tileY) * this.levelWidth + xInLevel] = rgb;
			
			if ((yInLevel % this.tileSize == this.tileSize - 1 || yInLevel == this.levelHeight - 1)
					&& xInLevel == this.levelWidth - 1) {
				this.bufferDone = true;
			}
		}
		
		public final void next() {
			if (this.bufferDone) {
				final int n = this.n;
				final int tileY = this.tileY;
				final int[] buffer = this.buffer;
				final int tileSize = this.tileSize;
				final int levelWidth = this.levelWidth;
				final int levelHeight = this.levelHeight;
				final String tileFormat = this.tileFormat;
				final ZipOutputStream output = this.output;
				final Collection<Exception> problems = this.problems;
				final LevelN nextLevel = this.nextLevel;
				
				final int h = min(tileSize, levelHeight - tileY);
				final TaskManager tasks = new TaskManager(levelCPULoad);
				
				for (int tileX = 0; tileX < levelWidth; tileX += tileSize) {
					final int tileX0 = tileX;
					
					tasks.submit(new Runnable() {
						
						@Override
						public final void run() {
							final Canvas tile = new Canvas();
							final int w = min(tileSize, levelWidth - tileX0);
							
							if (channelCount == 1) {
								tile.setFormat(w, h, BufferedImage.TYPE_BYTE_GRAY);
							} else {
								tile.setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR);
							}
							
							final int[] pixelValue = { 0 };
							
							for (int y = 0; y < h; ++y) {
								for (int x = 0; x < w; ++x) {
									pixelValue[0] = buffer[y * levelWidth + tileX0 + x];
									
									if (channelCount == 1) {
										tile.getImage().getRaster().setPixel(x, y, pixelValue);
									} else {
										tile.getImage().setRGB(x, y, pixelValue[0]);
									}
								}
							}
							
							final String tileName = TILE_PREFIX + "lod" + n + "_y" + tileY + "_x" + tileX0 + "." + tileFormat;
							final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
							
							try (final AutoCloseableImageWriter imageWriter = new AutoCloseableImageWriter(tileFormat)
									.setCompressionQuality(compressionQuality).setOutput(tmp)) {
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
							
							if (nextLevel != null) {
								for (int y = 0; y < (h & ~1); y += 2) {
									for (int x = 0; x < (w & ~1); x += 2) {
										if (channelCount == 1) {
											final int v00 = tile.getImage().getRaster().getPixel(x, y, pixelValue)[0];
											final int v01 = tile.getImage().getRaster().getPixel(x, y + 1, pixelValue)[0];
											final int v10 = tile.getImage().getRaster().getPixel(x + 1, y, pixelValue)[0];
											final int v11 = tile.getImage().getRaster().getPixel(x + 1, y + 1, pixelValue)[0];
											final int v = (v00 + v01 + v10 + v11) / 4; 
											
											nextLevel.setRGB((tileX0 + x) / 2, (tileY + y) / 2, v);
										} else {
											final int rgb00 = buffer[y * levelWidth + tileX0 + x];
											final int rgb01 = buffer[y * levelWidth + tileX0 + x + 1];
											final int rgb10 = buffer[(y + 1) * levelWidth + tileX0 + x];
											final int rgb11 = buffer[(y + 1) * levelWidth + tileX0 + x + 1];
											final int r = (((rgb00 & R) + (rgb01 & R) + (rgb10 & R) + (rgb11 & R)) / 4) & R; 
											final int g = (((rgb00 & G) + (rgb01 & G) + (rgb10 & G) + (rgb11 & G)) / 4) & G; 
											final int b = (((rgb00 & B) + (rgb01 & B) + (rgb10 & B) + (rgb11 & B)) / 4) & B;
											final int nextRGB = 0xFF000000 | r | g | b;
											
											nextLevel.setRGB((tileX0 + x) / 2, (tileY + y) / 2, nextRGB);
										}
									}
								}
							}
						}
						
					});
				}
				
				tasks.join();
				
				this.bufferDone = false;
				this.tileY += tileSize;
			}
			
			if (this.nextLevel != null) {
				this.nextLevel.next();
			}
		}
		
		private static final long serialVersionUID = -1013722312568926426L;
		
	}
	
	private static final Map<Thread, List<Node>> stacks = Collections.synchronizedMap(new HashMap<>());
	
	public static final Document document(final Runnable contents) {
		final List<Node> stack = stacks.computeIfAbsent(Thread.currentThread(), t -> new ArrayList<>());
		final Document result = XMLTools.newDocument();
		
		stack.add(0, result);
		
		try {
			contents.run();
		} finally {
			stack.remove(0);
		}
		
		return result;
	}
	
	public static final Element element(final String tagName, final Runnable contents) {
		final List<Node> stack = stacks.get(Thread.currentThread());
		final Node parent = stack.get(0);
		final Document document = parent instanceof Document ? (Document) parent : parent.getOwnerDocument();
		final Element result = document.createElement(tagName);
		
		parent.appendChild(result);
		stack.add(0, result);
		
		try {
			contents.run();
		} finally {
			stack.remove(0);
		}
		
		return result;
	}
	
	public static final void attribute(final String name, final Object value) {
		final List<Node> stack = stacks.get(Thread.currentThread());
		final Element element = (Element) stack.get(0);
		
		element.setAttribute(name, "" + value);
	}
	
	public static final void text(final String text) {
		final List<Node> stack = stacks.get(Thread.currentThread());
		final Element element = (Element) stack.get(0);
		
		element.appendChild(element.getOwnerDocument().createTextNode(text));
	}
	
}
