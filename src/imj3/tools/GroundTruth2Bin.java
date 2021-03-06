package imj3.tools;

import static java.lang.Math.*;
import static multij.tools.Tools.*;

import imj2.tools.BigBitSet;

import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.processing.Pipeline;
import imj3.processing.Pipeline.ClassDescription;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2015-04-04)
 */
public final class GroundTruth2Bin {
	
	private GroundTruth2Bin() {
		throw new IllegalInstantiationException();
	}
	
	static final Random random = new Random(0L);
	
	public static final double TRAIN_RATIO = 5.0 / 6.0;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String pipelinePath = arguments.get("pipeline", "");
		// TODO later, retrieve training set from pipeline instead
		final String groundTruthPath = arguments.get("groundtruth", "");
		final String defaultImagePath;
		{
			final int i = groundTruthPath.indexOf("_groundtruth_");
			defaultImagePath = 0 <= i ? groundTruthPath.substring(0, i) + ".zip" : "";
		}
		final String imagePath = arguments.get("image", defaultImagePath);
		final int lod = arguments.get("lod", 4)[0];
		debugPrint(imagePath);
		final Image2D image = IMJTools.read(imagePath, lod);
		final String trainOutputPath = baseName(groundTruthPath) + "_train.bin";
		final String testOutputPath = baseName(groundTruthPath) + "_test.bin";
		final boolean show = arguments.get("show", 0)[0] != 0;
		final boolean force = arguments.get("force", 0)[0] != 0;
		final boolean binarize = arguments.get("binarize", 0)[0] != 0;
		
		if (!new File(trainOutputPath).exists() || force) {
			try {
				processVector(pipelinePath, image, groundTruthPath, lod, trainOutputPath, testOutputPath);
			} catch (final Exception exception) {
				debugError(exception);
				
				final int maximumExamples = arguments.get("maximumExamples", Integer.MAX_VALUE)[0];
				
				processBitmap(image, groundTruthPath, binarize, lod, maximumExamples, trainOutputPath, testOutputPath);
			}
		}
		
		if (show) {
			BinView.main("bin", trainOutputPath);
			BinView.main("bin", testOutputPath);
		}
	}
	
	public static final void processBitmap(final Image2D image, final String groundTruthPath, final boolean binarize, final int lod,
			final int maximumExamples, final String trainOutputPath, final String testOutputPath) throws IOException {
		final Image2D groundtruth = IMJTools.read(groundTruthPath, lod);
		final int patchSize = 32;
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Map<Integer, AtomicLong> counts = new HashMap<>();
		
		groundtruth.forEachPixel((x, y) -> {
			counts.computeIfAbsent(getKey(groundtruth, x, y, binarize), v -> new AtomicLong()).incrementAndGet();
			
			return true;
		});
		
		debugPrint(counts);
		
		final long n = min(counts.values().stream().mapToLong(AtomicLong::get).min().getAsLong(), maximumExamples / counts.size());
		final Map<Integer, BigBitSet> selections = new HashMap<>();
		
		debugPrint(n);
		
		counts.forEach((k, v) -> {
			final long m = v.get();
			final BigBitSet selection = new BigBitSet(m);
			
			for (long i = 0; i < n; ++i) {
				selection.set(i, true);
			}
			
			if (m != n) {
//				shuffle:
				for (long i = 0; i < m; ++i) {
					final long j = abs(random.nextLong()) % m;
					final boolean tmp = selection.get(i);
					
					selection.set(i, selection.get(j));
					selection.set(j, tmp);
				}
			}
			
			selections.put(k, selection);
		});
		
		final List<byte[]> data = new ArrayList<>((int) n * counts.size());
		final Map<Integer, AtomicLong> indices = new HashMap<>();
		final int keyMask = ~((~0) << (binarize ? 1 : groundtruth.getChannels().getValueBitCount()));
		
		groundtruth.forEachPixel((x, y) -> {
			final int key = getKey(groundtruth, x, y, binarize);
			final BigBitSet selection = selections.get(key);
			final long i = indices.computeIfAbsent(key, k -> new AtomicLong(-1L)).incrementAndGet();
			
			if (selection.get(i)) {
				final int top = y - patchSize / 2;
				final int bottom = min(top + patchSize, imageHeight);
				final int left = x - patchSize / 2;
				final int right = min(left + patchSize, imageWidth);
				
				data.add(newItem(image, patchSize, top, bottom, left, right, (byte) (key & keyMask)));
			}
			
			return true;
		});
		
		debugPrint(indices);
		
		writeBins(data, TRAIN_RATIO, trainOutputPath, testOutputPath);
	}
	
	public static final int getKey(final Image2D groundtruth, final int x, final int y, final boolean binarize) {
		final int key = (int) groundtruth.getPixelValue(x, y) & 0x00FFFFFF;
		
		if (binarize) {
			return key == 0 ? 0 : 0x00FFFFFF;
		}
		
		return key;
	}
	
	public static final int checkInt(final long value) {
		if (value < Integer.MIN_VALUE || Integer.MAX_VALUE < value) {
			throw new IllegalArgumentException();
		}
		
		return (int) value;
	}
	
	public static final void processVector(final String pipelinePath, final Image2D image, final String groundTruthPath, final int lod,
			final String trainOutputPath, final String testOutputPath) throws IOException {
		final Pipeline pipeline = XMLSerializable.objectFromXML(new File(pipelinePath));
		final Map<Integer, Area> regions;
		
		try (final MultifileSource source = new MultifileSource(groundTruthPath);
				final InputStream input = source.getInputStream("annotations.xml")) {
			final Document xml = XMLTools.parse(input);
			
			regions = XMLSerializable.objectFromXML(xml.getDocumentElement(), new HashMap<>());
		}
		
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int patchSize = 32;
		final int planeSize = patchSize * patchSize;
		final byte[] buffer = new byte[1 + planeSize * channelCount];
		final List<byte[]> data = new ArrayList<>();
		final Map<String, Long> counts = new LinkedHashMap<>();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final double scale = pow(2.0, -lod);
		
		debugPrint(imageWidth, imageHeight, channels);
		
		for (final ClassDescription classDescription : pipeline.getClassDescriptions()) {
			final Area region = regions.get(classDescription.getLabel());
			
			region.transform(AffineTransform.getScaleInstance(scale, scale));
			
			final Rectangle regionBounds = region.getBounds();
			final int regionWidth = regionBounds.width;
			final int regionHeight = regionBounds.height;
			final BufferedImage mask = new BufferedImage(regionWidth, regionHeight, BufferedImage.TYPE_BYTE_BINARY);
			
			debugPrint(classDescription, regionBounds);
			
			{
				final Graphics2D graphics = mask.createGraphics();
				
				graphics.setColor(Color.WHITE);
				graphics.translate(-regionBounds.x, -regionBounds.y);
				graphics.fill(region);
				graphics.dispose();
			}
			
			final byte classIndex = (byte) pipeline.getClassDescriptions().indexOf(classDescription);
			buffer[0] = classIndex;
			long count = 0L;
			
			for (int y = 0; y < regionHeight; ++y) {
				final int top = regionBounds.y + y - patchSize / 2;
				final int bottom = min(top + patchSize, imageHeight);
				
				for (int x = 0; x < regionWidth; ++x) {
					if ((mask.getRGB(x, y) & 0x00FFFFFF) != 0) {
						final int left = regionBounds.x + x - patchSize / 2;
						final int right = min(left + patchSize, imageWidth);
						
						data.add(newItem(image, patchSize, top, bottom, left, right, classIndex));
						
						++count;
					}
				}
			}
			
			counts.put(classDescription.toString(), count);
		}
		
		writeBins(data, TRAIN_RATIO, trainOutputPath, testOutputPath);
		
		debugPrint(counts);
	}
	
	public static final void writeBins(final List<byte[]> data, final double trainRatio, final String trainOutputPath, final String testOutputPath)
			throws IOException {
		Collections.shuffle(data, random);
		
		final int n = data.size();
		final int trainSize = (int) (n * trainRatio);
		
		try (final OutputStream output = new FileOutputStream(trainOutputPath)) {
			for (int i = 0; i < trainSize; ++i) {
				output.write(data.get(i));
			}
		}
		
		try (final OutputStream output = new FileOutputStream(testOutputPath)) {
			for (int i = trainSize; i < n; ++i) {
				output.write(data.get(i));
			}
		}
	}
	
	public static final byte[] newItem(final Image2D image, final int patchSize,
			final int top, final int bottom, final int left, final int right, final byte classIndex) {
		final Channels channels = image.getChannels();
		final int channelCount = channels.getChannelCount();
		final int planeSize = patchSize * patchSize;
		final byte[] result = new byte[1 + channelCount * planeSize];
		
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
	
	public static final byte[] read(final String path) {
		try (final InputStream input = new FileInputStream(path);
				final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			write(input, output);
			
			return output.toByteArray();
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
}
