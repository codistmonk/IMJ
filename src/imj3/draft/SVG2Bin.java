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
import imj3.tools.BinView;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import multij.primitivelists.LongList;
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
		final int lod = arguments.get1("lod", 4);
		final int patchSize = arguments.get1("patchSize", 32);
		final int[] patchContext = arguments.get("patchContext", 0);
		final boolean alterations = arguments.get1("alterations", 0) != 0;
		final String svgPath = arguments.get("svg", baseName(imagePath) + ".svg");
		final String predictionPath = arguments.get("prediction", "");
		final String softmaxPath = arguments.get("softmax", "");
		final File svgFile = new File(svgPath);
		final Document svg = readXML(svgFile);
		final Image2D image = IMJTools.read(imagePath, lod);
		final long seed = Long.decode(arguments.get("seed", "0"));
		final boolean balance = arguments.get1("balance", 1) != 0;
		final int limit = (int) min(max(0L, Long.decode(arguments.get("limit", Integer.toString(Integer.MAX_VALUE)))), Integer.MAX_VALUE);
		final Random random = seed == -1L ? new Random() : new Random(seed);
		final List<Region> regions = new ArrayList<>();
		final double scale = pow(2.0, -lod);
		final AffineTransform scaling = AffineTransform.getScaleInstance(scale, scale);
		final int negativeRegionLabel = arguments.get1("negativeRegionLabel", -1);
		final Area negativeRegion = negativeRegionLabel != -1 ? new Area(new Rectangle(image.getWidth(), image.getHeight())) : null;
		final double trainRatio = Double.parseDouble(arguments.get("trainRatio", Double.toString(GroundTruth2Bin.TRAIN_RATIO)));
		final String[] classIds = getClassIdsArray(arguments.get("classIds", ""), svg);
		final String outputPrefix = arguments.get("outputPrefix", baseName(svgPath));
		
		debugPrint("imagePath:", imagePath);
		debugPrint("LOD:", lod, "imageWidth:", image.getWidth(), "imageWidth:", image.getHeight(), "imageChannels:", image.getChannels());
		debugPrint("svgPath:", svgPath);
		debugPrint("classIds", Arrays.toString(classIds));
		
		collectRegions(svg, classIds, scaling, negativeRegion, negativeRegionLabel, regions);
		
		debugPrint("regionCount:", regions.size());
		
		final Map<Integer, Long> sizes = new HashMap<>();
		
		regions.forEach(region -> sizes.compute(region.getLabel(), (l, s) -> (s == null ? 0 : s) + region.getSize()));
		
		final Long totalSize = sizes.values().stream().reduce((x, y) -> x + y).get();
		final int classLimit = balance ? limit : Integer.MAX_VALUE;
		
		debugPrint("classSizes:", sizes);
		debugPrint("totalSize:", totalSize, "classLimit:", classLimit);
		
		{
			final Image2D prediction = predictionPath.isEmpty() ? null : IMJTools.read(predictionPath);
			final Image2D softmax = prediction == null || softmaxPath.isEmpty() ? null : IMJTools.read(predictionPath);
			final Map<Byte, ClassSampler> samplers = buildSamplers(image, prediction, softmax, regions, sizes, balance,
					classLimit, random);
			final List<byte[]> items = collectItems(image, samplers, patchSize, patchContext);
			
			if (alterations) {
				alter(items, patchSize, random);
			}
			
			Collections.shuffle(items, random);
			
			final String trainOutputPath = outputPrefix + (trainRatio == 1.0 ? ".bin" : "_train.bin");
			final String testOutputPath = outputPrefix + "_test.bin";
			
			writeBins(items, trainRatio, trainOutputPath, testOutputPath);
			
			if (arguments.get1("show", 0) != 0) {
				BinView.main("bin", trainOutputPath, "itemWidth", "" + patchSize);
				
				if (1.0 != trainRatio) {
					BinView.main("bin", testOutputPath, "itemWidth", "" + patchSize);
				}
				
				SwingTools.show(new Image2DComponent(image), "Image", false);
			}
		}
	}
	
	public static final void alter(final List<byte[]> items, final int patchSize, final Random random) {
		final int channelSize = patchSize * patchSize;
		
		for (final byte[] item : items) {
			if (random.nextBoolean()) {
				for (int i = 1; i < item.length; i += channelSize) {
					for (int y = 0; y < patchSize; ++y) {
						for (int x = 0; x < patchSize / 2; ++x) {
							swapb(item, i + x + patchSize * y, i + (patchSize - 1 - x) + patchSize * y);
						}
					}
				}
			}
			
			final int rotations = random.nextInt(4);
			
			for (int j = 0; j < rotations; ++j) {
				for (int i = 1; i < item.length; i += channelSize) {
					for (int y = 0; y < (patchSize + 1) / 2; ++y) {
						for (int x = 0; x < (patchSize + 1) / 2; ++x) {
							rotl(item,
									i + x + patchSize * y,
									i + (patchSize - 1 - y) + patchSize * x,
									i + (patchSize - 1 - x) + patchSize * (patchSize - 1 - y),
									i + y + patchSize * (patchSize - 1 - x));
						}
					}
				}
			}
		}
	}
	
	public static final void swapb(final byte[] values, final int i, final int j) {
		final byte tmp = values[i];
		values[i] = values[j];
		values[j] = tmp;
	}
	
	public static final void rotl(final byte[] values, final int i, final int j, final int k, final int l) {
		final byte tmp = values[i];
		values[i] = values[j];
		values[j] = values[k];
		values[k] = values[l];
		values[l] = tmp;
	}
	
	public static final List<byte[]> collectItems(final Image2D image, final Map<Byte, ClassSampler> samplers,
			final int patchSide, final int[] patchContext) {
		final List<byte[]> result = new ArrayList<>();
		
		for (final Map.Entry<Byte, ClassSampler> entry : samplers.entrySet()) {
			entry.getValue().pixels().forEach(pixel -> {
				result.add(getItem(image, image.getX(pixel), image.getY(pixel), patchSide, patchContext, entry.getKey(), null));
			});
		}
		
		return result;
	}
	
	public static final Map<Byte, ClassSampler> buildSamplers(final Image2D image, final Image2D prediction,
			final Image2D softmax, final List<Region> regions, final Map<Integer, Long> sizes, final boolean balance,
			final int classLimit, final Random random) {
		final Map<Byte, ClassSampler> result = new TreeMap<>();
		
		for (final Region region : regions) {
			final byte label = (byte) region.getLabel();
			final ClassSampler sampler = result.computeIfAbsent(label, __ -> new ClassSampler(random, classLimit, sizes.get(region.getLabel())));
			final Rectangle bounds = region.getBounds();
			final BufferedImage mask = region.getMask();
			final int width = mask.getWidth();
			final int height = mask.getHeight();
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if ((mask.getRGB(x, y) & 1) != 0) {
						short priority = 1;
						final int xInImage = bounds.x + x;
						final int yInImage = bounds.y + y;
						
						if (prediction != null) {
							final int xInPrediction = xInImage * prediction.getWidth() / image.getWidth();
							final int yInPrediction = yInImage * prediction.getHeight() / image.getHeight();
							
							if ((0xFFL & label) != (0xFFL & prediction.getPixelValue(xInPrediction, yInPrediction))) {
								priority = -1;
							}
							
							if (softmax != null) {
								priority *= 0xFFL & softmax.getPixelValue(xInPrediction, yInPrediction);
							}
						}
						
						sampler.prepare(priority, image.getPixel(xInImage, yInImage));
					}
				}
			}
		}
		
		if (balance && classLimit < Integer.MAX_VALUE) {
			result.values().forEach(ClassSampler::fill);
		}
		
		return result;
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
	
	@Deprecated
	public static final List<byte[]> generateItems(final Image2D image,
			final int patchSize, final int[] patchContext, final List<Region> regions,
			final long selectionSize, final int classCount,
			final BigBitSet[] selections) {
		final List<byte[]> result = new ArrayList<>((int) selectionSize);
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
						result.add(getItem(image, bounds.x + x, bounds.y + y, patchSize, patchContext, label, null));
					}
				}
			}
		}
		
		return result;
	}
	
	@Deprecated
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
	
	public static final float[] getPatch(final Image2D image, final int x, final int y, final int patchSize,
			final float[] result) {
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int planeSize = patchSize * patchSize;
		final float[] actualResult = result != null ? result : new float[channelCount * planeSize];
		final int top = y - patchSize / 2;
		final int bottom = min(top + patchSize, image.getHeight());
		final int left = x - patchSize / 2;
		final int right = min(left + patchSize, image.getWidth());
		
		for (int yy = max(0, top); yy < bottom; ++yy) {
			for (int xx = max(0, left); xx < right; ++xx) {
				final long pixelValue = image.getPixelValue(xx, yy);
				
				for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
					actualResult[planeSize * (channelCount - 1 - channelIndex) + (yy - top) * patchSize + (xx - left)] = channels.getChannelValue(pixelValue, channelIndex);
				}
			}
		}
		
		return actualResult;
	}
	
	public static final BufferedImage getPatch(final Image2D image, final int x, final int y, final int patchSize,
			final BufferedImage result) {
		final BufferedImage actualResult = result != null ? result : new BufferedImage(patchSize, patchSize, BufferedImage.TYPE_3BYTE_BGR);
		final int top = y - patchSize / 2;
		final int bottom = min(top + patchSize, image.getHeight());
		final int left = x - patchSize / 2;
		final int right = min(left + patchSize, image.getWidth());
		
		for (int yy = max(0, top); yy < bottom; ++yy) {
			for (int xx = max(0, left); xx < right; ++xx) {
				final long pixelValue = image.getPixelValue(xx, yy);
				
				actualResult.setRGB(xx - left, yy - top, (int) pixelValue);
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
	
	/**
	 * @author codistmonk (creation 2016-07-05)
	 */
	public static final class ClassSampler implements Serializable {
		
		private final Random random;
		
		private final TreeMap<Short, LongList> map;
		
		private final int limit;
		
		private final long totalPixelCount;
		
		private int size;
		
		private int i;
		
		public ClassSampler(final Random random, final int limit, final long totalPixelCount) {
			this.random = random;
			this.map = new TreeMap<>();
			this.limit = limit;
			this.totalPixelCount = totalPixelCount;
		}
		
		public final void prepare(final short priority, final long pixel) {
			if (this.size < this.limit) {
				this.map.computeIfAbsent(priority, p -> new LongList()).add(pixel);
				++this.size;
			} else {
				final Entry<Short, LongList> lastEntry = this.map.lastEntry();
				
				if (priority <= lastEntry.getKey()) {
					final LongList pixels = this.map.computeIfAbsent(priority, p -> new LongList());
					
					if (lastEntry.getKey().equals(priority)) {
						final long r = (0x7F_FF_FF_FF__FF_FF_FF_FFL & this.random.nextLong()) % this.totalPixelCount;
						
						if (r < pixels.size()) {
							pixels.set(this.i % pixels.size(), pixel);
							++this.i;
						}
					} else {
						pixels.add(pixel);
						
						final LongList lastPixels = lastEntry.getValue();
						
						lastPixels.remove(0);
						
						if (lastPixels.isEmpty()) {
							this.map.remove(lastEntry.getKey());
						}
					}
				}
			}
		}
		
		public final ClassSampler fill() {
			if (!this.map.isEmpty()) {
				final LongList pixels = this.map.firstEntry().getValue();
				
				for (; this.size < this.limit; ++this.size) {
					pixels.add(pixels.get(this.random.nextInt(pixels.size())));
				}
			}
			
			return this;
		}
		
		public final LongStream pixels() {
			return this.map.values().stream().flatMapToLong(l -> Arrays.stream(l.toArray()));
		}
		
		private static final long serialVersionUID = -8987894943252816228L;
		
	}
	
}
