package imj3.draft;

import static imj3.draft.SVGTools.*;
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
import imj3.tools.Image2DComponent;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.Tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
		final int[] patchContext = arguments.get("patchContext", 0);
		final String svgPath = arguments.get("svg", baseName(imagePath) + ".svg");
		final File svgFile = new File(svgPath);
		final Document svg = readXML(svgFile);
		final Image2D image = IMJTools.read(imagePath, lod);
		final long seed = Long.decode(arguments.get("seed", "0"));
		final boolean balance = arguments.get("balance", 1)[0] != 0;
		final long limit = Long.decode(arguments.get("limit", Long.toString(Long.MAX_VALUE)));
		final Random random = seed == -1L ? new Random() : new Random(seed);
		final List<Region> regions = new ArrayList<>();
		final double scale = pow(2.0, -lod);
		final AffineTransform scaling = AffineTransform.getScaleInstance(scale, scale);
		final int negativeRegionLabel = arguments.get("negativeRegionLabel", -1)[0];
		final Area negativeRegion = negativeRegionLabel != -1 ? new Area(new Rectangle(image.getWidth(), image.getHeight())) : null;
		final double trainRatio = Double.parseDouble(arguments.get("trainRatio", Double.toString(GroundTruth2Bin.TRAIN_RATIO)));
		final String[] classIds = getClassIdsArray(arguments.get("classIds", ""), svg);
		final String outputPrefix = arguments.get("outputPrefix", baseName(svgPath));
		
		debugPrint("iamgePath:", imagePath);
		debugPrint("LOD:", lod, "imageWidth:", image.getWidth(), "imageWidth:", image.getHeight(), "imageChannels:", image.getChannels());
		debugPrint("svgPath:", svgPath);
		debugPrint("classIds", Arrays.toString(classIds));
		
		collectRegions(svg, classIds, scaling, negativeRegion, negativeRegionLabel, regions);
		
		debugPrint("regionCount:", regions.size());
		
		final Map<Integer, Long> sizes = new HashMap<>();
		
		regions.forEach(region -> sizes.compute(region.getLabel(), (l, s) -> (s == null ? 0 : s) + region.getSize()));
		
		final Long totalSize = sizes.values().stream().reduce((x, y) -> x + y).get();
		final long classLimit = min(limit, balance ? sizes.values().stream().min(Long::compare).get() : Long.MAX_VALUE);
		final long selectionSize = sizes.values().stream().map(s -> min(s, classLimit)).reduce((x, y) -> x + y).get();
		
		debugPrint("classSizes:", sizes);
		debugPrint("totalSize:", totalSize, "selectionSize:", selectionSize, "classLimit:", classLimit);
		
		final int classCount = classIds.length;
		
		{
			final BigBitSet[] selections = generateSelections(classCount, sizes, classLimit, random);
			final List<byte[]> items = generateItems(image, patchSize, patchContext, regions, selectionSize, classCount, selections);
			
			Collections.shuffle(items, random);
			
			final String trainOutputPath = outputPrefix + (trainRatio == 1.0 ? ".bin" : "_train.bin");
			final String testOutputPath = outputPrefix + "_test.bin";
			
			writeBins(items, trainRatio, trainOutputPath, testOutputPath);
			
			if (arguments.get("show", 0)[0] != 0) {
				GroundTruth2Bin.BinView.main(trainOutputPath);
				GroundTruth2Bin.BinView.main(testOutputPath);
				
				SwingTools.show(new Image2DComponent(image), "Image", false);
			}
		}
	}
	
	public static final void collectRegions(final Document svg,
			final String[] classIds, final AffineTransform scaling,
			final Area negativeRegion, final int negativeRegionLabel, final List<Region> regions) {
		for (final Node regionNode : getNodes(svg, "//path|//polygon")) {
			final Element regionElement = (Element) regionNode;
			final int label = indexOf(regionElement.getAttribute("imj:classId"), classIds);
			
			if (label < 0) {
				continue;
			}
			
			final Area region = newRegion(regionElement);
			
			region.transform(scaling);
			
			if (negativeRegion != null) {
				negativeRegion.subtract(region);
			}
			
			addTo(regions, region, label);
		}
		
		if (negativeRegion != null) {
			addTo(regions, negativeRegion, negativeRegionLabel);
		}
	}
	
	public static final List<byte[]> generateItems(final Image2D image,
			final int patchSize, final int[] patchContext, final List<Region> regions,
			final long selectionSize, final int classCount,
			final BigBitSet[] selections) {
		final List<byte[]> items = new ArrayList<>((int) selectionSize);
		final int[] selectionIndices = new int[classCount];
		
		for (final Region region : regions) {
			final byte label = (byte) region.getLabel();
			final Rectangle bounds = region.getBounds();
			final BufferedImage mask = region.getMask();
			final int width = mask.getWidth();
			final int height = mask.getHeight();
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if ((mask.getRGB(x, y) & 1) != 0 && selections[label].get(++selectionIndices[label])) {
						items.add(getItem(image, bounds.x + x, bounds.y + y, patchSize, patchContext, label, null));
					}
				}
			}
		}
		return items;
	}
	
	public static final BigBitSet[] generateSelections(final int classCount,
			final Map<Integer, Long> sizes, final long classLimit, final Random random) {
		final BigBitSet[] result = new BigBitSet[classCount];
		
		for (int label = 0; label < classCount; ++label) {
			final long labelSize = sizes.getOrDefault(label, 0L);
			
			if (0L < labelSize) {
				final long n = min(labelSize, classLimit);
				final BigBitSet selection = new BigBitSet(labelSize);
				result[label] = selection;
				
				for (long i = 0L; i < n; ++i) {
					selection.set(i, true);
				}
				
				if (n < labelSize) {
					for (long i = 0; i < labelSize; ++i) {
						final long j = (random.nextLong() & (~0L >>> 1)) % labelSize;
						final boolean tmp = selection.get(i);
						
						selection.set(i, selection.get(j));
						selection.set(j, tmp);
					}
				}
			}
		}
		
		return result;
	}
	
	public static final void addTo(final List<Region> regions, final Area region, final int label) {
		final Rectangle bounds = region.getBounds();
		final Canvas canvas = new Canvas().setFormat(max(1, bounds.width), max(1, bounds.height), BufferedImage.TYPE_BYTE_BINARY);
		
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
	
	public static final void writeBins(final List<byte[]> data, final double trainRatio, final String trainOutputPath, final String testOutputPath) {
		final int n = data.size();
		final int trainSize = (int) (n * trainRatio);
		
		debugPrint("Writing", trainOutputPath);
		
		try (final OutputStream output = new FileOutputStream(trainOutputPath)) {
			for (int i = 0; i < trainSize; ++i) {
				output.write(data.get(i));
			}
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		if (1.0 != trainRatio) {
			debugPrint("Writing", testOutputPath);
			
			try (final OutputStream output = new FileOutputStream(testOutputPath)) {
				for (int i = trainSize; i < n; ++i) {
					output.write(data.get(i));
				}
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
		}
	}
	
	public static final byte[] getItem(final Image2D image, final int x, final int y, final int patchSize, final int[] patchContext,
			final byte classIndex, final byte[] result) {
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int planeSize = patchSize * patchSize;
		final int contextSize = channelCount * planeSize;
		final int n = patchContext.length;
		final byte[] actualResult = result != null ? result : new byte[1 + contextSize * n];
		final byte[] partialResult = new byte[1 + contextSize];
		final double scale = image.getScale();
		
		for (int i = 0; i < n; ++i) {
			final int deltaLOD = patchContext[i];
			final double k = pow(2.0, -deltaLOD);
			
			getItem(image.getScaledImage(scale * k), (int) (x * k), (int) (y * k), patchSize, classIndex, partialResult);
			
			System.arraycopy(partialResult, 1, actualResult, 1 + contextSize * i, contextSize);
		}
		
		actualResult[0] = classIndex;
		
		return actualResult;
	}
	
	public static final byte[] getItem(final Image2D image, final int x, final int y, final int patchSize,
			final byte classIndex, final byte[] result) {
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int planeSize = patchSize * patchSize;
		final byte[] actualResult = result != null ? result : new byte[1 + channelCount * planeSize];
		final int top = y - patchSize / 2;
		final int bottom = min(top + patchSize, image.getHeight());
		final int left = x - patchSize / 2;
		final int right = min(left + patchSize, image.getWidth());
		
		actualResult[0] = classIndex;
		
		for (int yy = max(0, top); yy < bottom; ++yy) {
			for (int xx = max(0, left); xx < right; ++xx) {
				final long pixelValue = image.getPixelValue(xx, yy);
				
				for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
					actualResult[1 + planeSize * (channelCount - 1 - channelIndex) + (yy - top) * patchSize + (xx - left)] = (byte) channels.getChannelValue(pixelValue, channelIndex);
				}
			}
		}
		
		return actualResult;
	}
	
	public static final <E> int indexOf(final E needle, @SuppressWarnings("unchecked") final E... haystack) {
		final int n = haystack.length;
		
		for (int i = 0; i < n; ++i) {
			if (Tools.equals(needle, haystack[i])) {
				return i;
			}
		}
		
		return -1;
	}
	
	public static final String[] getClassIdsArray(final String classIdsAsString, final Document svg) {
		final Collection<String> classIdsAsSet = new LinkedHashSet<>();
		Arrays.stream(classIdsAsString.split(",")).filter(not(String::isEmpty)).forEach(classIdsAsSet::add);
		
		if (classIdsAsSet.isEmpty() && svg != null) {
			for (final Node node : getNodes(svg, "//*")) {
				final Node attribute = node.getAttributes().getNamedItem("imj:classId");
				
				if (attribute != null) {
					classIdsAsSet.add(attribute.getNodeValue());
				}
			}
		}
		
		return classIdsAsSet.toArray(new String[classIdsAsSet.size()]);
	}
	
	public static <T> Predicate<T> not(final Predicate<T> t) {
	    return t.negate();
	}
	
}
