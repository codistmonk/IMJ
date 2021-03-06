package imj3.draft;

import static imj3.draft.VisualSegmentation.extractCells;
import static imj3.draft.VisualSegmentation.newMaskFor;
import static multij.tools.Tools.getResourceAsStream;

import imj2.draft.PaletteBasedHistograms;
import imj2.draft.PaletteBasedHistograms.Patch2DProcessor;

import imj3.draft.VisualSegmentation.PaletteRoot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-15)
 */
public final class BatchSegmentation {
	
	private BatchSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File outputRoot = new File(root, arguments.get("out", "results_ga_" + new SimpleDateFormat("YYYYMMDDHHmm", Locale.ENGLISH).format(new Date())));
		final File referenceRoot = new File(root, arguments.get("reference", ""));
		final PaletteRoot palette = VisualSegmentation.fromXML(getResourceAsStream(arguments.get("palette", "")));
		final TicToc timer = new TicToc();
		
		outputRoot.mkdir();
		
		for (final File file : root.listFiles()) {
			final String fileName = file.getName();
			
			if (fileName.endsWith("lod6.jpg")) {
				Tools.debugPrint(fileName);
				
				final String baseName = Tools.baseName(fileName);
				final File segmentedFile = new File(outputRoot, baseName + "_segmented.png");
				final File segmentsFile = new File(outputRoot, baseName + "_segments.png");
				final BufferedImage segments;
				
				timer.tic();
				
				if (!segmentsFile.exists()) {
					final BufferedImage image = ImageIO.read(file);
					final BufferedImage mask = newMaskFor(image);
					final Canvas labels = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					segments = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					
					extractCells(file, image, mask, labels, segments, palette);
					VisualSegmentation.outlineSegments(segments, labels.getImage(), mask, image);
					
					Tools.debugPrint("Writing", segmentedFile);
					ImageIO.write(image, "png", segmentedFile);
					Tools.debugPrint("Writing", segmentsFile);
					ImageIO.write(segments, "png", segmentsFile);
				} else {
					segments = ImageIO.read(segmentsFile);
				}
				
				final File referenceFile = new File(referenceRoot,
						referenceRoot.getName() + "_" + baseName.substring(0, baseName.length() - "_lod6".length()) + ".png");
				
				if (referenceFile.exists()) {
					Tools.debugPrint(referenceFile);
					
					final BufferedImage reference = ImageIO.read(referenceFile);
					final AtomicLong tp = new AtomicLong();
					final AtomicLong fp = new AtomicLong();
					final AtomicLong tn = new AtomicLong();
					final AtomicLong fn = new AtomicLong();
					
					PaletteBasedHistograms.forEachPixelIn(reference, (x, y) -> {
						final int xx = x * segments.getWidth() / reference.getWidth();
						final int yy = y * segments.getHeight() / reference.getHeight();
						final boolean prediction = segments.getRGB(xx, yy) != 0;
						final boolean truth = (reference.getRGB(x, y) & 0x00FFFFFF) != 0;
						
						if (prediction) {
							if (truth) {
								tp.incrementAndGet();
							} else {
								fp.incrementAndGet();
							}
						} else {
							if (truth) {
								fn.incrementAndGet();
							} else {
								tn.incrementAndGet();
							}
						}
						
						return true;
					});
					
					final double f1 = 2.0 * tp.get() / (2.0 * tp.get() + fp.get() + fn.get());
					final double f1b;
					{
						final double tpb = tp.get() + fn.get();
						final double fpb = fp.get() + tn.get();
						final double tnb = 0.0;
						final double fnb = 0.0;
						f1b = 2.0 * tpb / (2.0 * tpb + fpb + fnb);
					}
					Tools.debugPrint(tp, fp, tn, fn, f1, f1b, tp.get() + fp.get() + tn.get() + fn.get(), segments.getWidth() * segments.getHeight(), reference.getWidth() * reference.getHeight());
					try (final PrintStream csv = new PrintStream(new File(outputRoot, baseName + ".csv"))) {
						csv.println("TP,FP,TN,FN,F1,F1_0");
						csv.println(Tools.join(",", tp, fp, tn, fn, f1, f1b));
					}
				}
				
				Tools.debugPrint(timer.toc());
			}
		}
	}
	
}
