package imj3.draft;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.*;
import imj2.tools.BigBitSet;
import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.tools.GroundTruth2Bin;
import imj3.tools.IMJTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-06-05)
 */
public final class SVG2Bin {
	
	private SVG2Bin() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final int lod = arguments.get("lod", 4)[0];
		final int patchSize = arguments.get("patchSize", 32)[0];
		final String svgPath = arguments.get("svg", baseName(imagePath) + ".svg");
		final File svgFile = new File(svgPath);
		final File classesFile = new File(arguments.get("classes", new File(svgFile.getParentFile(), "classes.xml").getPath()));
		final Document svg = readXML(svgFile);
		final Document classes = readXML(classesFile);
		final String[] classIds = arguments.get("classIds", join(",",
				getNodes(classes, "//class").stream().map(node -> ((Element) node).getAttribute("id")).toArray())).split(",");
		final Image2D image = IMJTools.read(imagePath, lod);
		final long seed = Long.decode(arguments.get("seed", "0"));
		final boolean balance = arguments.get("balance", 1)[0] != 0;
		final long limit = Long.decode(arguments.get("limit", Long.toString(Long.MAX_VALUE)));
		final Random random = seed == -1L ? new Random() : new Random(seed);
		final List<Region> regions = new ArrayList<>();
		
		debugPrint("LOD:", lod, "imageWidth:", image.getWidth(), "imageWidth:", image.getHeight(), "imageChannels:", image.getChannels());
		debugPrint("classIds", Arrays.toString(classIds));
		
		for (final Node regionNode : getNodes(svg, "//path")) {
			final Element regionElement = (Element) regionNode;
			final int label = indexOf(regionElement.getAttribute("imj:classId"), classIds);
			final Area region = newRegion(regionElement);
			final double scale = pow(2.0, -lod);
			
			region.transform(AffineTransform.getScaleInstance(scale, scale));
			
			final Rectangle bounds = region.getBounds();
			final Canvas canvas = new Canvas().setFormat(bounds.width, bounds.height, BufferedImage.TYPE_BYTE_BINARY);
			
			{
				final Graphics2D graphics = canvas.getGraphics();
				final AffineTransform savedTransform = graphics.getTransform();
				
				graphics.translate(-bounds.x, -bounds.y);
				graphics.setColor(Color.WHITE);
				graphics.fill(region);
				graphics.setTransform(savedTransform);
			}
			
			regions.add(new Region(label, bounds, canvas.getImage()));
		}
		
		debugPrint("regionCount:", regions.size());
		
		final Map<Integer, Long> sizes = new HashMap<>();
		
		regions.forEach(region -> sizes.compute(region.getLabel(), (l, s) -> (s == null ? 0 : s) + region.getSize()));
		
		final Long totalSize = sizes.values().stream().reduce((x, y) -> x + y).get();
		final long classLimit = min(limit, balance ? sizes.values().stream().min(Long::compare).get() : Long.MAX_VALUE);
		final long selectionSize = sizes.values().stream().map(s -> min(s, classLimit)).reduce((x, y) -> x + y).get();
		
		debugPrint("classSizes:", sizes);
		debugPrint("totalSize:", totalSize, "selectionSize:", selectionSize, "classLimit:", classLimit);
		
		final BigBitSet selection = new BigBitSet(totalSize);
		
		for (long i = 0; i < selectionSize; ++i) {
			selection.set(i, true);
		}
		
		for (long i = 0; i < totalSize; ++i) {
			final long j = (random.nextLong() & (~0L >>> 1)) % totalSize;
			final boolean tmp = selection.get(i);
			
			selection.set(i, selection.get(j));
			selection.set(j, tmp);
		}
		
		{
			final List<byte[]> items = new ArrayList<>((int) selectionSize);
			long i = -1L;
			
			for (final Region region : regions) {
				final byte label = (byte) region.getLabel();
				final Rectangle bounds = region.getBounds();
				final BufferedImage mask = region.getMask();
				final int width = mask.getWidth();
				final int height = mask.getHeight();
				
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						if ((mask.getRGB(x, y) & 1) != 0 && selection.get(++i)) {
							items.add(newItem(image, bounds.x + x, bounds.y + y, patchSize, label));
						}
					}
				}
			}
			
			Collections.shuffle(items, random);
			
			final String trainOutputPath = baseName(svgPath) + "_train.bin";
			final String testOutputPath = baseName(svgPath) + "_test.bin";
			
			writeBins(items, GroundTruth2Bin.TRAIN_RATIO, trainOutputPath, testOutputPath);
			
			if (arguments.get("show", 0)[0] != 0) {
				GroundTruth2Bin.BinView.main(trainOutputPath);
				GroundTruth2Bin.BinView.main(testOutputPath);
			}
		}
	}
	
	public static final void writeBins(final List<byte[]> data, final double trainRatio, final String trainOutputPath, final String testOutputPath) {
		final int n = data.size();
		final int trainSize = (int) (n * trainRatio);
		
		try (final OutputStream output = new FileOutputStream(trainOutputPath)) {
			for (int i = 0; i < trainSize; ++i) {
				output.write(data.get(i));
			}
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		try (final OutputStream output = new FileOutputStream(testOutputPath)) {
			for (int i = trainSize; i < n; ++i) {
				output.write(data.get(i));
			}
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final byte[] newItem(final Image2D image, final int x, final int y, final int patchSize,
			final byte classIndex) {
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int planeSize = patchSize * patchSize;
		final byte[] result = new byte[1 + channelCount * planeSize];
		final int top = y - patchSize / 2;
		final int bottom = min(top + patchSize, image.getHeight());
		final int left = x - patchSize / 2;
		final int right = min(left + patchSize, image.getWidth());
		
		result[0] = classIndex;
		
		for (int yy = max(0, top); yy < bottom; ++yy) {
			for (int xx = max(0, left); xx < right; ++xx) {
				final long pixelValue = image.getPixelValue(xx, yy);
				
				for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
					result[1 + planeSize * channelIndex + (yy - top) * patchSize + (xx - left)] = (byte) channels.getChannelValue(pixelValue, channelIndex);
				}
			}
		}
		
		return result;
	}
	
	public static final Area newRegion(final Element svgPath) {
		final Path2D path = new Path2D.Double();
		
		path.setWindingRule(Path2D.WIND_EVEN_ODD);
		
		if (true) {
			String pathElement = "m";
			final double[] buffer = new double[4];
			int i = 2;
			SVGPathDataParserState state = SVGPathDataParserState.READ_COMMAND;
			
			try (final Scanner scanner = new Scanner(svgPath.getAttribute("d"))) {
				scanner.useLocale(Locale.ENGLISH);
				
				while (scanner.hasNext()) {
					switch (state) {
					case READ_COMMAND:
						scanner.useDelimiter("");
						final String command = scanner.next("[MmLlZz ]");
						switch (command) {
						case "M":
						case "m":
						case "L":
						case "l":
							pathElement = command;
						case " ":
							state = SVGPathDataParserState.READ_NUMBER;
							break;
						case "Z":
						case "z":
							path.closePath();
						}
						break;
					case READ_NUMBER:
						scanner.useDelimiter("[^0-9.]");
						buffer[i] = scanner.nextDouble();
						
						if (++i == 4) {
							switch (pathElement) {
							case "m":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
							case "M":
								path.moveTo(buffer[2], buffer[3]);
								break;
							case "l":
								buffer[2] += buffer[0];
								buffer[3] += buffer[1];
							case "L":
								path.lineTo(buffer[2], buffer[3]);
								break;
							default:
								throw new UnsupportedOperationException(pathElement);
							}
							
							System.arraycopy(buffer, 2, buffer, 0, 2);
							i = 2;
							state = SVGPathDataParserState.READ_COMMAND;
						}
					}
				}
			}
		}
		
		return new Area(path);
	}
	
	public static final <E> int indexOf(final E needle, final E... haystack) {
		final int n = haystack.length;
		
		for (int i = 0; i < n; ++i) {
			if (Tools.equals(needle, haystack[i])) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static final Document readXML(final File file) {
		try (final InputStream input = new FileInputStream(file)) {
			return parse(input);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2015-06-05)
	 */
	public static final class Region implements Serializable {
		
		private final int label;
		
		private final Rectangle bounds;
		
		private final BufferedImage mask;
		
		public Region(final int label, final Rectangle bounds, final BufferedImage mask) {
			this.label = label;
			this.bounds = bounds;
			this.mask = mask;
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		public final BufferedImage getMask() {
			return this.mask;
		}
		
		public final long getSize() {
			final int width = this.getMask().getWidth();
			final int height = this.getMask().getHeight();
			long result = 0L;
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if ((this.getMask().getRGB(x, y) & 1) != 0) {
						++result;
					}
				}
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 3168354082192346491L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-06-05)
	 */
	private static enum SVGPathDataParserState {
		
		READ_COMMAND, READ_NUMBER;
		
	}
	
}
