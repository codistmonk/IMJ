package imj2.draft;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.join;

import imj2.core.Image2D;
import imj2.draft.PaletteBasedSegmentation.NearestNeighborRGBQuantizer;
import imj2.tools.ColorSeparationTest.RGBTransformer;
import imj2.tools.AwtBackedImage;
import imj2.tools.IMJTools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Pair;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class PaletteBasedHistograms {
	
	private PaletteBasedHistograms() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File paletteFile = new File(arguments.get("palette", "transformerSelectorModel.jo"));
		final ComboBoxModel<? extends RGBTransformer> palette = Tools.readObject(paletteFile.getPath());
		final Collection<Pair<String, Map<Integer, int[]>>> data = new ArrayList<>();
		final Map<Integer, Map<Integer, AtomicInteger>> segmentSizes = new HashMap<>();
		final TicToc timer = new TicToc();
		
		Tools.debugPrint(palette);
		final RGBTransformer quantizer = palette.getElementAt(1);
		Tools.debugPrint(quantizer);
		
		// temporary fix for a defective palette
		if (true) {
			final NearestNeighborRGBQuantizer nnq = (NearestNeighborRGBQuantizer) quantizer;
			
			nnq.getClusters().remove(0xFF008080);
			
			Tools.debugPrint(nnq.getClusters());
		}
		
		for (final File file : root.listFiles()) {
			final String fileName = file.getName();
			
			if (fileName.endsWith(".png")) {
				final String baseName = baseName(fileName);
				final String maskName = baseName + "_mask.png";
				final String labelsName = baseName + "_labels.png";
				final String segmentedName = baseName + "_segmented.png";
				final File maskFile = new File(file.getParentFile(), maskName);
				final File labelsFile = new File(file.getParentFile(), labelsName);
				final File segmentedFile = new File(file.getParentFile(), segmentedName);
				
				if (maskFile.exists()/* && !(labelsFile.exists() && segmentedFile.exists())*/) {
					Tools.debugPrint(file);
					
					timer.tic();
					
					final BufferedImage image = awtReadImage(file.getPath());
					final BufferedImage mask = awtReadImage(maskFile.getPath());
					
					forEachPixelIn(image, (x, y) -> {
						if (0xFFFFFF00 == image.getRGB(x, y)) {
							mask.setRGB(x, y, 0);
						}
						
						return true;
					});
					
					final int imageWidth = image.getWidth();
					final int imageHeight = image.getHeight();
					final BufferedImage labels;
					
					if (labelsFile.exists()) {
						labels = awtReadImage(labelsFile.getPath());
					} else {
						labels = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
						
						quantize(image, mask, quantizer, labels);
						smootheLabels(labels, mask, 3, 3);
						outlineSegments(labels, mask, image, 0xFF00FF00);
					}
					
					data.add(new Pair<>(labelsName, computeHistogram(labels, mask)));
					
					updateSegmentSizes(segmentSizes, labelsName, labels);
					
					try {
						if (!labelsFile.exists()) {
							Tools.debugPrint("Writing", labelsFile);
							ImageIO.write(labels, "png", labelsFile);
						}
						
						if (!segmentedFile.exists()) {
							Tools.debugPrint("Writing", segmentedFile);
							ImageIO.write(image, "png", segmentedFile);
						}
						
						Tools.debugPrint(timer.toc());
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		{
			final Collection<Integer> labels = new HashSet<>();
			
			data.forEach(p -> labels.addAll(p.getSecond().keySet()));
			
			Tools.debugPrint(labels.size(), labels);
			
			final File countsFile = new File(root, "counts.csv");
			
			Tools.debugPrint("Writing", countsFile);
			
			try (final PrintStream out = new PrintStream(countsFile)) {
				out.println("fileName," + join(",", labels.stream().map(Integer::toHexString).toArray()));
				
				data.forEach(p -> {
					out.print(p.getFirst());
					out.print(',');
					out.print(join(",", labels.stream().map(label -> {
						final int[] count = p.getSecond().get(label);
						return count == null ? 0 : count[0];
					}).toArray()));
					out.println();
				});
			} catch (final FileNotFoundException exception) {
				exception.printStackTrace();
			}
			
			final File segmentsFile = new File(root, "segments.txt");
			
			Tools.debugPrint("Writing", segmentsFile);
			
			try (final PrintStream out = new PrintStream(segmentsFile)) {
				labels.forEach(label -> {
					final Map<Integer, AtomicInteger> sizes = segmentSizes.get(label);
					
					if (sizes != null) {
						final List<Map.Entry<Integer, AtomicInteger>> list = new ArrayList<>(sizes.entrySet());
						
						Collections.sort(list, (e1, e2) ->
							Integer.compare(e2.getValue().get(), e1.getValue().get()));
						
						out.println(Integer.toHexString(label) + ":" + list);
					}
				});
			} catch (final FileNotFoundException exception) {
				exception.printStackTrace();
			}
		}
	}
	
	public static final void updateSegmentSizes(
			final Map<Integer, Map<Integer, AtomicInteger>> segmentSizes,
			final String labelsName, final BufferedImage labels) {
		IMJTools.forEachPixelInEachComponent4(new AwtBackedImage(labelsName, labels), new Image2D.Process() {
			
			private Integer label;
			
			private int pixelCount;
			
			@Override
			public final void pixel(final int x, final int y) {
				if (this.label == null) {
					this.label = labels.getRGB(x, y);
				}
				
				++this.pixelCount;
			}
			
			@Override
			public final void endOfPatch() {
				if (this.label != 0) {
					final int q = 4;
					final int quantizedPatchSize = (this.pixelCount + q - 1) / q * q;
					
					segmentSizes.computeIfAbsent(this.label, l -> new HashMap<>())
						.computeIfAbsent(quantizedPatchSize, k -> new AtomicInteger()).incrementAndGet();
				}
				
				this.label = null;
				this.pixelCount = 0;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4003095469554064893L;
			
		});
	}
	
	public static final void outlineSegments(final BufferedImage labels,
			final BufferedImage mask, final BufferedImage image, final int color) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		forEachPixelIn(labels, (x, y) -> {
			if ((mask.getRGB(x, y) & 1) != 0) {
				final int label = labels.getRGB(x, y);
				final int eastLabel = x + 1 < imageWidth ? labels.getRGB(x + 1, y) : label;
				final int southLabel = y + 1 < imageHeight ? labels.getRGB(x, y + 1) : label;
				
				if (label != eastLabel || label != southLabel) {
					image.setRGB(x, y, color);
				}
			}
			
			return true;
		});
	}
	
	public static final void quantize(final BufferedImage image,
			final BufferedImage mask, final RGBTransformer quantizer,
			final BufferedImage labels) {
		forEachPixelIn(image, (x, y) -> {
			if ((mask.getRGB(x, y) & 1) != 0) {
				labels.setRGB(x, y, quantizer.transform(image.getRGB(x, y)));
			}
			
			return true;
		});
	}
	
	public static final Map<Integer, int[]> computeHistogram(final BufferedImage image,
			final BufferedImage mask) {
		final Map<Integer, int[]> result = new HashMap<>();
		
		forEachPixelIn(image, (x, y) -> {
			if ((mask.getRGB(x, y) & 1) != 0) {
				++result.computeIfAbsent(image.getRGB(x, y), rgb -> new int[1])[0];
			}
			
			return true;
		});
		
		return result;
	}
	
	public static final BufferedImage smootheLabels(final BufferedImage labels,
			final BufferedImage mask, final int patchSize, final int threshold) {
		final int imageWidth = labels.getWidth(); 
		final int imageHeight = labels.getHeight();
		final BufferedImage tmp = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		labels.copyData(tmp.getRaster());
		
		forEachConvolutionPixelIn(tmp, patchSize, new Patch2DProcessor() {
			
			private final Map<Integer, int[]> histogram = new HashMap<>();
			
			private int x, y;
			
			private final int[] best = new int[2];
			
			@Override
			public final boolean beginPatch(final int x, final int y) {
				if ((mask.getRGB(x, y) & 1) != 0) {
					this.x = x;
					this.y = y;
					this.best[0] = tmp.getRGB(x, y);
					this.best[1] = 0;
					this.histogram.clear();
					
					return true;
				}
				
				return false;
			}
			
			@Override
			public final boolean pixel(final int x, final int y) {
				final int rgb = tmp.getRGB(x, y);
				
				if (rgb != 0) {
					++this.histogram.computeIfAbsent(rgb, v -> new int[1])[0];
				}
				
				return true;
			}
			
			@Override
			public final boolean endPatch() {
				this.histogram.forEach((k, v) -> {
					if (this.best[1] < v[0]) {
						this.best[0] = k;
						this.best[1] = v[0];
					}
				});
				
				if (this.histogram.get(tmp.getRGB(this.x, this.y))[0] < min(threshold, this.best[1])) {
					labels.setRGB(this.x, this.y, this.best[0]);
				}
				
				return true;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -5117333476493324315L;
			
		});
		
		return tmp;
	}
	
	public static final BufferedImage awtReadImage(final String path) {
		try {
			return ImageIO.read(new File(path));
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final void forEachPixelIn(final BufferedImage image, final Pixel2DProcessor process) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		for (int y = 0; y < imageHeight; ++y) {
			for (int x = 0; x < imageWidth; ++x) {
				if (!process.pixel(x, y)) {
					return;
				}
			}
		}
	}
	
	public static final void forEachConvolutionPixelIn(final BufferedImage image, final int patchSize,
			final Patch2DProcessor process) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int s = patchSize / 2;
		
		forEachPixelIn(image, (x, y) -> {
			if (process.beginPatch(x, y)) {
				final int left = max(0, x - s);
				final int right = min(left + patchSize, imageWidth) - 1;
				final int top = max(0, y - s);
				final int bottom = min(top + patchSize, imageHeight) - 1;
				
				loop:
				for (int yy = top; yy <= bottom; ++yy) {
					for (int xx = left; xx <= right; ++xx) {
						if (!process.pixel(xx, yy)) {
							break loop;
						}
					}
				}
				
				return process.endPatch();
			}
			
			return true;
		});
	}
	
	/**
	 * @author codistmonk (creation 2014-12-03)
	 */
	public static abstract interface Pixel2DProcessor extends Serializable {
		
		public abstract boolean pixel(int x, int y);
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-03)
	 */
	public static abstract interface Patch2DProcessor extends Serializable {
		
		public default boolean beginPatch(final int x, final int y) {
			return true;
		}
		
		public abstract boolean pixel(int x, int y);
		
		public default boolean endPatch() {
			return true;
		}
		
	}
	
}
