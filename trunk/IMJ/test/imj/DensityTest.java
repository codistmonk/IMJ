package imj;

import static imj.apps.modules.ImageComponent.showAdjusted;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;
import static java.lang.Math.abs;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageOfBufferedImage.Feature;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.ProgressMonitor;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public final class DensityTest {
	
	@Test
	public final void test1() throws IOException {
		final TicToc timer = new TicToc();
//		final String imageId = "lib/images/16088-2.png";
		final String imageId = "test/imj/12003.jpg";
		debugPrint("Loading image:", new Date(timer.tic()));
		final Image image = new ImageOfBufferedImage(read(new File(imageId)), Feature.MAX_RGB);
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		debugPrint(image);
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		final Image density = new ImageOfFloats(rowCount, columnCount);
		final Image protoDensity = new ImageOfFloats(rowCount, columnCount);
		final int pixelCount = rowCount * columnCount;
		final ProgressMonitor progressMonitor = new ProgressMonitor(null, "Computing density", null, 0, columnCount);
		
		debugPrint("Precomputing densities:", new Date(timer.tic()));
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int row = pixel / columnCount;
			final int column = pixel % columnCount;
//			protoDensity.setFloatValue(pixel, 255.0F / (1 + squareDistance(0, 0, row, column)));
			protoDensity.setValue(pixel, floatToRawIntBits(255.0F / (1 + squareDistance(0, 0, row, column))));
		}
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		debugPrint("Computing densities:", new Date(timer.tic()));
		for (int out = 0; out < pixelCount; ++out) {
			final int outRow = out / columnCount;
			final int outColumn = out % columnCount;
			float value = +0.0F;
			
			for (int in = 0; in < pixelCount && !progressMonitor.isCanceled(); ++in) {
				final int inRow = in / columnCount;
				final int inColumn = in % columnCount;
				
//				value += protoDensity.getFloatValue(abs(outRow - inRow), abs(outColumn - inColumn)) * image.getFloatValue(in);
				value += intBitsToFloat(protoDensity.getValue(abs(outRow - inRow), abs(outColumn - inColumn))) * intBitsToFloat(image.getValue(in));
			}
			
			density.setValue(out, floatToRawIntBits(intBitsToFloat(density.getValue(out)) + value / 255.0F));
			
			if (outColumn == 0) {
				progressMonitor.setProgress(outRow);
			}
		}
		progressMonitor.close();
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
		
		normalize(density, density, +0.0F, +255.0F);
		
//		showHistogram(image);
		showAdjusted(imageId, image, density);
	}
	
	public static final Image normalize(final Image image, final Image result, final float newMinimum, final float newMaximum) {
		float oldMinimum = Float.POSITIVE_INFINITY;
		float oldMaximum = Float.NEGATIVE_INFINITY;
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final float value = intBitsToFloat(image.getValue(pixel));
			oldMinimum = Math.min(oldMinimum, value);
			oldMaximum = Math.max(oldMaximum, value);
		}
		
		final float oldAmplitude = oldMaximum - oldMinimum;
		final float newAmplitude = newMaximum - newMinimum;
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result.setValue(pixel, floatToRawIntBits(newMinimum + (image.getValue(pixel) - oldMinimum) * newAmplitude / oldAmplitude));
		}
		
		return result;
	}
	
	public static final int squareDistance(final int x1, final int y1, final int x2, final int y2) {
		return square(x2 - x1) + square(y2 - y1);
	}
	
	public static final int square(final int x) {
		return x * x;
	}
	
	public static final void showHistogram(final Image image) {
		final int[] histogram = histogram(image);
		final int hMax = max(histogram);
		final int n = histogram.length;
		final int h = 512;
		final BufferedImage histogramImage = new BufferedImage(n, h, BufferedImage.TYPE_3BYTE_BGR);
		final Graphics2D g = histogramImage.createGraphics();
		
		for (int i = 0; i < n; ++i) {
			g.setColor(new Color(Color.HSBtoRGB((float) i / (n - 1), 1.0F, 0.5F)));
			g.drawLine(i, h - 1, i, h - 1 - histogram[i] * (h - 1) / hMax);
		}
		
		g.dispose();
		
		SwingTools.show(histogramImage, "Hue histogram", true);
	}
	
	public static final int max(final int... values) {
		int result = Integer.MIN_VALUE;
		
		for (final int value : values) {
			result = Math.max(result, value);
		}
		
		return result;
	}
	
	public static final int[] histogram(final Image image) {
		final int minimum = 0;
		final int maximum = IMJTools.getMaximum(image);
		final int amplitude = maximum - minimum + 1;
		final int[] result = new int[amplitude];
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			++result[image.getValue(pixel) - minimum];
		}
		
		return result;
	}
	
}
