package imj;

import static imj.ImageComponent.showAdjusted;
import static javax.imageio.ImageIO.read;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools.StatisticsSelector;
import imj.ImageOfBufferedImage.Feature;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import net.sourceforge.aprog.swing.SwingTools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public final class DensityTest {
	
	@Test
	public final void test1() throws IOException {
		final TicToc timer = new TicToc();
//		final String imageId = "bsds/train/12003.jpg";
//		final String imageId = "lib/images/16088-2.png";
		final String imageId = "test/imj/12003.jpg";
		final Image image = new ImageOfBufferedImage(read(new File(imageId)), Feature.HUE);
		
		debugPrint(image);
		
//		showHistogram(image);
		showAdjusted(imageId, image);
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
