package imj3.draft;

import static imj3.core.Channels.Predefined.*;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static multij.tools.Tools.baseName;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.tools.TicToc;
import multij.tools.Tools;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class OldBatchSegmentation {
	
	private OldBatchSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		IMJTools.toneDownBioFormatsLogger();
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final TicToc timer = new TicToc();
		final AtomicInteger fileCount = new AtomicInteger();
		
		for (final File file : root.listFiles()) {
			final String fileName = file.getName();
			if (fileName.endsWith(".png")) {
				final String baseName = baseName(fileName);
				final String maskName = baseName + "_mask.png";
				final String resultName = baseName + "_segmented.png";
				final File maskFile = new File(file.getParent(), maskName);
				final File resultFile = new File(file.getParent(), resultName);
				
				if (maskFile.exists()/* && !resultFile.exists()*/) {
					Tools.debugPrint(file);
					
					fileCount.incrementAndGet();
					
					timer.tic();
					process(file, maskFile, resultFile);
					Tools.debugPrint(timer.toc());
					
					break;
				}
			}
		}
		
		Tools.debugPrint(fileCount, timer.getTotalTime());
	}
	
	public static final void process(final File file, final File maskFile, final File resultFile) {
		final AwtImage2D image = new AwtImage2D(file.getPath());
		final AwtImage2D result = new AwtImage2D(file.getPath() + "_segmented", image.getWidth(), image.getHeight());
		final AwtImage2D mask = new AwtImage2D(maskFile.getPath());
		final AtomicInteger selectedPixelCount = new AtomicInteger();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int windowSize = 11;
		final int m = windowSize * windowSize;
		final int maximumError = 32;
		final Map<Integer, AtomicInteger> histogram = new HashMap<>();
		
		Tools.debugPrint(imageWidth, imageHeight);
		
		image.forEachPixel((x, y) -> {
			if ((mask.getPixelValue(x, y) & 1) != 0) {
				selectedPixelCount.incrementAndGet();
				
				final long pixelRGB = image.getPixelValue(x, y);
				final int left = max(0, x - windowSize / 2);
				final int right = min(left + windowSize, imageWidth) - 1;
				final int top = max(0, y - windowSize / 2);
				final int bottom = min(top + windowSize, imageHeight) - 1;
				int r = 0;
				int g = 0;
				int b = 0;
				int n = 0;
				
				for (int yy = top; yy <= bottom; ++yy) {
					for (int xx = left; xx <= right; ++xx) {
						final long neighborRGB = image.getPixelValue(xx, yy);
						final int d = rgbRelativeD1(pixelRGB, neighborRGB);
						
						if (d <= maximumError) {
							r += red8(neighborRGB);
							g += green8(neighborRGB);
							b += blue8(neighborRGB);
							++n;
						}
					}
				}
				
				r /= n;
				g /= n;
				b /= n;
				final int d = 255 * n / m;
				
				{
					final int key = a8r8g8b8(0xFF, r, g, b);
					histogram.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
				}
				
				result.setPixelValue(x, y, a8r8g8b8(0xFF, r, g, b));
			}
			
			return true;
		});
		
		Tools.debugPrint(histogram.size());
		
		try {
			Tools.debugPrint(resultFile);
			ImageIO.write(result.getSource(), "png", resultFile);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final int rgbRelativeD1(final long rgb1, final long rgb2) {
		final int r1 = red8(rgb1);
		final int g1 = green8(rgb1);
		final int b1 = blue8(rgb1);
		final int maxD = max(r1, 255 - r1) + max(g1, 255 - g1) + max(b1, 255 - b1);
		final int r2 = red8(rgb2);
		final int g2 = green8(rgb2);
		final int b2 = blue8(rgb2);
		
		return 255 * (abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)) / maxD;
	}
	
}
