package imj.apps.modules;

import static imj.IMJTools.argb;
import static java.awt.Color.BLACK;
import static java.awt.Color.YELLOW;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-25)
 */
public final class HistogramPanel extends JPanel {
	
	private final Context context;
	
	private final JTextField channelsTextField;
	
	private final JLabel statusLabel;
	
	private final Histogram1Component histogram1Component;
	
	private final Histogram2Component histogram2Component;
	
	public HistogramPanel(final Context context) {
		super(new BorderLayout());
		this.context = context;
		this.channelsTextField = new JTextField("brightness");
		this.statusLabel = new JLabel();
		this.histogram1Component = new Histogram1Component();
		this.histogram2Component = new Histogram2Component();
		
		this.histogram1Component.addMouseMotionListener(new MouseAdapter() {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				HistogramPanel.this.setStatusText("");
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final Histogram1Component h1 = HistogramPanel.this.getHistogram1Component();
				final int datumIndex = h1.getDatumIndex(event.getX());
				
				HistogramPanel.this.setStatusText("(" + datumIndex + " " + h1.getValue(datumIndex) + ")");
			}
			
		});
		
		this.histogram2Component.addMouseMotionListener(new MouseAdapter() {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				HistogramPanel.this.setStatusText("");
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final Histogram2Component h2 = HistogramPanel.this.getHistogram2Component();
				final int datumColumnIndex = h2.getDatumColumnIndex(event.getX());
				final int datumRowIndex = h2.getDatumRowIndex(event.getY());
				
				HistogramPanel.this.setStatusText("(" + datumColumnIndex + " " + datumRowIndex + " " +
						h2.getValue(datumColumnIndex, datumRowIndex) + ")");
			}
			
		});
		
		this.add(horizontalBox(new JLabel("Channels:"), this.channelsTextField), BorderLayout.NORTH);
		this.add(this.statusLabel, BorderLayout.SOUTH);
		
		this.channelsTextField.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				HistogramPanel.this.update();
			}
			
		});
		
		final Listener<Object> updater = new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				if (HistogramPanel.this.isShowing()) {
					HistogramPanel.this.update();
				}
			}
			
		};
		
		context.getVariable("image").addListener(updater);
		context.getVariable("sieve").addListener(updater);
		
		this.setPreferredSize(new Dimension(256, 256));
		
		this.update();
	}
	
	public final Histogram1Component getHistogram1Component() {
		return this.histogram1Component;
	}
	
	public final Histogram2Component getHistogram2Component() {
		return this.histogram2Component;
	}
	
	final void setStatusText(final String text) {
		this.statusLabel.setText(text);
	}
	
	final void update() {
		final Image image = this.context.get("image");
		
		if (image != null) {
			final String[] channelAsStrings = this.channelsTextField.getText().split("\\s+");
			final int n = channelAsStrings.length;
			
			debugPrint("Counting...");
			
			if (n == 1) {
				final Channel channel = ViewFilter.parseChannel(channelAsStrings[0].toUpperCase(Locale.ENGLISH));
				
				this.histogram1Component.update(image, channel);
				
				this.remove(this.histogram2Component);
				this.add(this.histogram1Component, BorderLayout.CENTER);
			} else {
				final Channel channel0 = ViewFilter.parseChannel(channelAsStrings[0].toUpperCase(Locale.ENGLISH));
				final Channel channel1 = ViewFilter.parseChannel(channelAsStrings[1].toUpperCase(Locale.ENGLISH));
				
				this.histogram2Component.update(image, channel0, channel1);
				
				this.remove(this.histogram1Component);
				this.add(this.histogram2Component, BorderLayout.CENTER);
			}
			
			debugPrint("Done");
			
			this.validate();
			this.invalidate();
			this.repaint();
		}
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
	
	public static final int nonZeroMin(final int... values) {
		int result = Integer.MAX_VALUE;
		
		for (final int value : values) {
			if (0 < value && value < result) {
				result = value;
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-25)
	 */
	public static final class Histogram1Component extends JComponent {
		
		private final int[] data;
		
		private int max;
		
		public Histogram1Component() {
			this.data = new int[256];
			this.setPreferredSize(new Dimension(256, 128));
		}
		
		public final void update(final Image image, final Channel channel) {
			final int pixelCount = image.getRowCount() * image.getColumnCount();
			
			fill(this.data, 0);
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				++this.data[channel.getValue(image.getValue(pixel))];
			}
			
			this.max = max(this.data);
			
			this.repaint();
		}
		
		public final int getValue(final int datumIndex) {
			return this.data[datumIndex];
		}
		
		public final int getDatumIndex(final int x) {
			return x * 256 / this.getWidth();
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			final Rectangle viewport = this.getVisibleRect();
			final int endX = viewport.x + viewport.width;
			final int lastY = viewport.y + viewport.height - 1;
			
			g.setColor(BLACK);
			g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);
			
			g.setColor(YELLOW);
			
			for (int x = viewport.x; x < endX; ++x) {
				final int h = this.getValue(this.getDatumIndex(x)) * viewport.height / this.max;
				
				g.drawLine(x, lastY, x, lastY - h);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-25)
	 */
	public static final class Histogram2Component extends JComponent {
		
		private final int[][] data;
		
		private int max;
		
		private int nonZeroMin;
		
		private BufferedImage buffer;
		
		public Histogram2Component() {
			this.data = new int[256][256];
			this.setDoubleBuffered(false);
			this.setPreferredSize(new Dimension(256, 256));
		}
		
		public final int getValue(final int datumColumnIndex, final int datumRowIndex) {
			return this.data[datumRowIndex][datumColumnIndex];
		}
		
		public final int getDatumRowIndex(final int y) {
			return 255 - y * 256 / this.getHeight();
		}
		
		public final int getDatumColumnIndex(final int x) {
			return x * 256 / this.getWidth();
		}
		
		public final void update(final Image image, final Channel channel0, final Channel channel1) {
			final int pixelCount = image.getRowCount() * image.getColumnCount();
			
			for (final int[] subData : this.data) {
				fill(subData, 0);
			}
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				final int rgba = image.getValue(pixel);
				
				++this.data[channel0.getValue(rgba)][channel1.getValue(rgba)];
			}
			
			this.nonZeroMin = Integer.MAX_VALUE;
			this.max = Integer.MIN_VALUE;
			
			for (final int[] subData : this.data) {
				this.max = Math.max(this.max, max(subData));
				this.nonZeroMin = Math.min(this.nonZeroMin, nonZeroMin(subData));
			}
			
			this.repaint();
		}
		
		public final void updateBuffer() {
			final Rectangle viewport = this.getVisibleRect();
			
			if (this.buffer == null || this.buffer.getWidth() != viewport.width || this.buffer.getHeight() != viewport.height) {
				this.buffer = new BufferedImage(viewport.width, viewport.height, BufferedImage.TYPE_3BYTE_BGR);
			}
			
			final int endX = viewport.x + viewport.width;
			final int endY = viewport.y + viewport.height;
			final int colorMin = 96;
			final int colorAmplitude = 255 - colorMin;
			final int countAmplitude = this.max - this.nonZeroMin;
			
			for (int y = viewport.y; y < endY; ++y) {
				final int datumRowIndex = 255 - y * 256 / viewport.height;
				
				for (int x = viewport.x; x < endX; ++x) {
					final int datumColumnIndex = x * 256 / viewport.width;
					final int count = this.getValue(datumColumnIndex, datumRowIndex);
					final int color = count == 0 ? 0 : colorMin + (count - this.nonZeroMin) * colorAmplitude / countAmplitude;
					
					this.buffer.setRGB(x, y, argb(255, color, color, 0));
				}
			}
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			this.updateBuffer();
			
			g.drawImage(this.buffer, 0, 0, null);
		}
		
	}
	
}
