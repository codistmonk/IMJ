package imj.apps.modules;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import imj.Image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class HistogramsPanel extends JPanel {
	
	private final Context context;
	
	private final int[] redCounts;
	
	private final int[] greenCounts;
	
	private final int[] blueCounts;
	
	private final int[] hueCounts;
	
	private final float[] hsbBuffer;
	
	private final BufferedImage redGreenBlueBrightnessHistogramImage;
	
	private final BufferedImage hueHistogramImage;
	
	public HistogramsPanel(final Context context) {
		super(new GridLayout(2, 1));
		this.context = context;
		this.redCounts = new int[256];
		this.greenCounts = new int[256];
		this.blueCounts = new int[256];
		this.hueCounts = new int[256];
		this.hsbBuffer = new float[3];
		this.redGreenBlueBrightnessHistogramImage = new BufferedImage(256, 200, BufferedImage.TYPE_3BYTE_BGR);
		this.hueHistogramImage = new BufferedImage(256, 200, BufferedImage.TYPE_3BYTE_BGR);
		
		this.add(new JLabel(new ImageIcon(this.redGreenBlueBrightnessHistogramImage)));
		this.add(new JLabel(new ImageIcon(this.hueHistogramImage)));
		
		final Variable<Image> imageVariable = context.getVariable("image");
		
		imageVariable.addListener(new Listener<Image>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Image, ?> event) {
				if (HistogramsPanel.this.isShowing()) {
					HistogramsPanel.this.update();
				}
			}
			
		});
		
		this.update();
	}
	
	final void update() {
		final Image image = this.context.get("image");
		
		if (image != null) {
			final int pixelCount = image.getRowCount() * image.getColumnCount();
			
			this.resetCounts();
			
			debugPrint("Counting...");
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				this.count(image.getValue(pixel));
			}
			
			debugPrint("Done");
			
			debugPrint("Updating histogram images...");
			
			this.updateHistogramImages();
			
			debugPrint("Done");
		}
	}
	
	private final void resetCounts() {
		Arrays.fill(this.redCounts, 0);
		Arrays.fill(this.greenCounts, 0);
		Arrays.fill(this.blueCounts, 0);
		Arrays.fill(this.hueCounts, 0);
	}
	
	private final void count(final int rgb) {
		final int red = red(rgb);
		final int green = green(rgb);
		final int blue = blue(rgb);
		
		++this.redCounts[red];
		++this.greenCounts[green];
		++this.blueCounts[blue];
		
		Color.RGBtoHSB(red, green, blue, this.hsbBuffer);
		
		++this.hueCounts[(int) (this.hsbBuffer[0] * 255)];
	}
	
	private final void updateHistogramImages() {
		{
			final Graphics2D g = this.redGreenBlueBrightnessHistogramImage.createGraphics();
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 256, 200);
			
			final int maxCount = max(max(this.redCounts), max(this.greenCounts), max(this.blueCounts));
			
			for (int x = 0; x < 255; ++x) {
				final int red = this.redCounts[x];
				final int green = this.greenCounts[x];
				final int blue = this.blueCounts[x];
				final int brightness = max(red, green, blue);
				
				g.setColor(Color.WHITE);
				g.fillRect(x + 0, 200 - 1 - brightness * 200 / maxCount, 1, brightness);
			}
			
			for (int x = 0; x < 255; ++x) {
				final int red = this.redCounts[x];
				final int nextRed = this.redCounts[x + 1];
				final int green = this.greenCounts[x];
				final int nextGreen = this.greenCounts[x + 1];
				final int blue = this.blueCounts[x];
				final int nextBlue = this.blueCounts[x + 1];
				
				g.setColor(Color.RED);
				g.drawLine(x + 0, 200 - 1 - red * 200 / maxCount, x + 1, 200 - 1 - nextRed * 200 / maxCount);
				g.setColor(Color.GREEN);
				g.drawLine(x + 0, 200 - 1 - green * 200 / maxCount, x + 1, 200 - 1 - nextGreen * 200 / maxCount);
				g.setColor(Color.BLUE);
				g.drawLine(x + 0, 200 - 1 - blue * 200 / maxCount, x + 1, 200 - 1 - nextBlue * 200 / maxCount);
			}
			
			g.dispose();
		}
		
		{
			final Graphics2D g = this.hueHistogramImage.createGraphics();
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 256, 200);
			
			final int maxCount = max(this.hueCounts);
			
			if (0 < maxCount) {
				g.setColor(Color.WHITE);
				
				for (int x = 0; x < 256; ++x) {
					g.drawLine(x, 200 - 1, x, 200 - 1 - this.hueCounts[x] * 200 / maxCount);
				}
			}
			
			g.dispose();
		}
		
		this.repaint();
	}
	
	public static final int max(final int... values) {
		int result = values[0];
		
		for (final int value : values) {
			if (result < value) {
				result = value;
			}
		}
		
		return result;
	}
	
}
