package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.apps.modules.BigImageComponent.SOURCE_IMAGE;
import static imj.apps.modules.Plugin.onChange;
import static imj.apps.modules.ViewFilter.VIEW_FILTER;
import static java.awt.Color.BLACK;
import static java.awt.Color.YELLOW;
import static java.lang.Math.log;
import static java.lang.Math.round;
import static java.util.Arrays.fill;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.af.AFTools.menu;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
		this.histogram1Component = new Histogram1Component(context);
		this.histogram2Component = new Histogram2Component(context);
		
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
		
		// TODO avoid duplicate updates
		context.getVariable(SOURCE_IMAGE).addListener(updater);
		context.getVariable(VIEW_FILTER).addListener(updater);
		
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
		final Image image = ViewFilter.getCurrentImage(this.context);
		
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
	 * @author codistmonk (creation 2013-03-19)
	 */
	public static final class HistogramActions {
		
		private HistogramActions() {
			throw new IllegalInstantiationException();
		}
		
		/**
		 * {@value}.
		 */
		public static final String ACTIONS_SET_VALUE_SCALE_LINEAR = "actions.setValueScaleLinear";
		
		/**
		 * {@value}.
		 */
		public static final String ACTIONS_SET_VALUE_SCALE_LOGARITHMIC = "actions.setValueScaleLogarithmic";
		
		/**
		 * @author codistmonk (creation 2013-02-25)
		 */
		public static final class SetValueScaleLinear extends AbstractAFAction {
			
			public SetValueScaleLinear(final Context context) {
				super(context, ACTIONS_SET_VALUE_SCALE_LINEAR);
			}
			
			@Override
			public final void perform(final Object object) {
				this.getContext().set("valueScale", new ValueScale.Linear());
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-02-25)
		 */
		public static final class SetValueScaleLogarithmic extends AbstractAFAction {
			
			public SetValueScaleLogarithmic(final Context context) {
				super(context, ACTIONS_SET_VALUE_SCALE_LOGARITHMIC);
			}
			
			@Override
			public final void perform(final Object object) {
				this.getContext().set("valueScale", new ValueScale.Logarithmic());
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-03-19)
	 */
	public static abstract class ValueScale {
		
		private int valueMinimum;
		
		private int valueMaximum;
		
		private int valueAmplitude;
		
		private int displayMinimum;
		
		private int displayMaximum;
		
		private int displayAmplitude;
		
		public final void setBounds(final int valueMinimum, final int valueMaximum, final int displayMinimum, final int displayMaximum) {
			this.valueMinimum = valueMinimum;
			this.valueMaximum = valueMaximum;
			this.valueAmplitude = valueMaximum - valueMinimum;
			this.displayMinimum = displayMinimum;
			this.displayMaximum = displayMaximum;
			this.displayAmplitude = displayMaximum - displayMinimum;
			
//			debugPrint(this.getValueMinimum(), this.getValueMaximum(), this.getValueAmplitude());
//			debugPrint(this.getDisplayMinimum(), this.getDisplayMaximum(), this.getDisplayAmplitude());
			
			this.boundsSet();
		}
		
		public final int getValueMinimum() {
			return this.valueMinimum;
		}
		
		public final int getValueMaximum() {
			return this.valueMaximum;
		}
		
		public final int getValueAmplitude() {
			return this.valueAmplitude;
		}
		
		public final int getDisplayMinimum() {
			return this.displayMinimum;
		}
		
		public final int getDisplayMaximum() {
			return this.displayMaximum;
		}
		
		public final int getDisplayAmplitude() {
			return this.displayAmplitude;
		}
		
		public abstract int getDisplayValue(int value);
		
		protected void boundsSet() {
			// NOP
		}
		
		public static final ValueScale parseValueScale(final String valueScale) {
			final String s = valueScale.trim().toLowerCase(Locale.ENGLISH);
			
			if ("linear".equals(s)) {
				return new Linear();
			}
			
			if ("logarithmic".equals(s)) {
				return new Logarithmic();
			}
			
			throw new IllegalArgumentException("Invalid value scale: " + valueScale);
		}
		
		/**
		 * @author codistmonk (creation 2013-03-19)
		 */
		public static final class Linear extends ValueScale {
			
			@Override
			public final int getDisplayValue(final int value) {
				return (int) (this.getDisplayMinimum() + (long) this.getDisplayAmplitude() * (value - this.getValueMinimum()) / this.getValueAmplitude());
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2013-03-19)
		 */
		public static final class Logarithmic extends ValueScale {
			
			private double logScale;
			
			@Override
			public final int getDisplayValue(final int value) {
				return (int) round(this.getDisplayMinimum() +
						this.getDisplayAmplitude() * log(1 + value - this.getValueMinimum()) / this.logScale);
			}
			
			@Override
			protected final void boundsSet() {
				this.logScale = log(1 + this.getValueAmplitude());
				
				if (this.logScale <= 0.0) {
					this.logScale = 1.0;
				}
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-25)
	 */
	public static final class HistogramPopupHandler extends MouseAdapter {
		
		private final JPopupMenu popup;
		
		public HistogramPopupHandler(final Context context, final Component component) {
			this.popup = new JPopupMenu();
			
			new HistogramActions.SetValueScaleLinear(context);
			new HistogramActions.SetValueScaleLogarithmic(context);
			
			this.popup.add(
					menu("Value scale",
							item("Linear", context, HistogramActions.ACTIONS_SET_VALUE_SCALE_LINEAR),
							item("Logarithmic", context, HistogramActions.ACTIONS_SET_VALUE_SCALE_LOGARITHMIC)));
			
			onChange(context, "valueScale", component, "repaint");
		}
		
		@Override
		public final void mouseClicked(final MouseEvent event) {
			if (isRightMouseButton(event)) {
				this.popup.show(event.getComponent(), event.getX(), event.getY());
			}
		}
		
		public final void addTo(final Component component) {
			component.addMouseListener(this);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-25)
	 */
	public static final class Histogram1Component extends JComponent {
		
		private final Context context;
		
		private final int[] data;
		
		private int max;
		
		public Histogram1Component(final Context parentContext) {
			this.context = new Context();
			this.data = new int[256];
			
			this.context.set("parent", parentContext);
			this.context.set("valueScale", new ValueScale.Linear(), ValueScale.class);
			
			new HistogramPopupHandler(this.context, this).addTo(this);
		}
		
		public final void update(final Image image, final Channel channel) {
			final int imageRowCount = image.getRowCount();
			final int imageColumnCount = image.getColumnCount();
			final int pixelCount = imageRowCount * imageColumnCount;
			
			fill(this.data, 0);
			
			final Context parentContext = this.context.get("parent");
			RegionOfInterest roi = Sieve.getROI(parentContext);
			
			if (roi != null && (roi.getRowCount() != imageRowCount || roi.getColumnCount() != imageColumnCount)) {
				roi = null;
			}
			
			final FilteredImage f = cast(FilteredImage.class, image);
			
			if (f != null) {
				final int tileRowCount = 512;
				final int tileColumnCount = 512;
				final int lastTileRowIndex = imageRowCount / tileRowCount;
				final int lastTileColumnIndex = imageColumnCount / tileColumnCount;
				
				for (int tileRowIndex = 0; tileRowIndex <= lastTileRowIndex; ++tileRowIndex) {
					System.out.print(tileRowIndex + "/" + lastTileRowIndex + "\r");
					
					for (int tileColumnIndex = 0; tileColumnIndex <= lastTileColumnIndex; ++tileColumnIndex) {
						for (int rowIndexInTile = 0, rowIndex = tileRowIndex * tileRowCount; rowIndexInTile < tileRowCount && rowIndex < imageRowCount; ++rowIndexInTile, ++rowIndex) {
							for (int columnIndexInTile = 0, columnIndex = tileColumnIndex * tileColumnCount; columnIndexInTile < tileColumnCount && columnIndex < imageColumnCount; ++columnIndexInTile, ++columnIndex) {
								if (roi != null && roi.get(rowIndex, columnIndex)) {
									++this.data[channel.getValue(image.getValue(rowIndex, columnIndex))];
								}
							}
						}
					}
				}
			} else {
				for (int pixel = 0; pixel < pixelCount; ++pixel) {
					if (pixel % imageColumnCount == 0) {
						System.out.print(pixel + "/" + pixelCount + "\r");
					}
					
					if (roi == null || roi.get(pixel)) {
						++this.data[channel.getValue(image.getValue(pixel))];
					}
				}
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
			
			final ValueScale valueScale = this.context.get("valueScale");
			
			valueScale.setBounds(0, this.max, 0, viewport.height);
			
			for (int x = viewport.x; x < endX; ++x) {
				final int h = valueScale.getDisplayValue(this.getValue(this.getDatumIndex(x)));
				
				g.drawLine(x, lastY, x, lastY - h);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-25)
	 */
	public static final class Histogram2Component extends JComponent {
		
		private final Context context;
		
		private final int[][] data;
		
		private int maximum;
		
		private int nonZeroMinimum;
		
		private BufferedImage buffer;
		
		public Histogram2Component(final Context parentContext) {
			this.context = new Context();
			this.data = new int[256][256];
			this.setDoubleBuffered(false);
			
			this.context.set("parent", parentContext);
			this.context.set("valueScale", new ValueScale.Logarithmic(), ValueScale.class);
			
			new HistogramPopupHandler(this.context, this).addTo(this);
		}
		
		public final int getValue(final int datumColumnIndex, final int datumRowIndex) {
			return this.data[datumColumnIndex][datumRowIndex];
		}
		
		public final int getDatumColumnIndex(final int x) {
			return x * 256 / this.getWidth();
		}
		
		public final int getDatumRowIndex(final int y) {
			return 255 - y * 256 / this.getHeight();
		}
		
		public final void update(final Image image, final Channel channel0, final Channel channel1) {
			final int imageRowCount = image.getRowCount();
			final int imageColumnCount = image.getColumnCount();
			final int pixelCount = imageRowCount * imageColumnCount;
			
			for (final int[] subData : this.data) {
				fill(subData, 0);
			}
			
			final Context parentContext = this.context.get("parent");
			RegionOfInterest roi = Sieve.getROI(parentContext);
			
			if (roi != null && (roi.getRowCount() != imageRowCount || roi.getColumnCount() != imageColumnCount)) {
				roi = null;
			}
			
//			for (int pixel = 0; pixel < pixelCount; ++pixel) {
//				final int rgba = image.getValue(pixel);
//				
//				if (roi != null && roi.get(pixel)) {
//					++this.data[channel0.getValue(rgba)][channel1.getValue(rgba)];
//				}
//			}
			final FilteredImage f = cast(FilteredImage.class, image);
			
			if (f != null) {
				final int tileRowCount = 512;
				final int tileColumnCount = 512;
				final int lastTileRowIndex = imageRowCount / tileRowCount;
				final int lastTileColumnIndex = imageColumnCount / tileColumnCount;
				
				for (int tileRowIndex = 0; tileRowIndex <= lastTileRowIndex; ++tileRowIndex) {
					System.out.print(tileRowIndex + "/" + lastTileRowIndex + "\r");
					
					for (int tileColumnIndex = 0; tileColumnIndex <= lastTileColumnIndex; ++tileColumnIndex) {
						for (int rowIndexInTile = 0, rowIndex = tileRowIndex * tileRowCount; rowIndexInTile < tileRowCount && rowIndex < imageRowCount; ++rowIndexInTile, ++rowIndex) {
							for (int columnIndexInTile = 0, columnIndex = tileColumnIndex * tileColumnCount; columnIndexInTile < tileColumnCount && columnIndex < imageColumnCount; ++columnIndexInTile, ++columnIndex) {
								if (roi != null && roi.get(rowIndex, columnIndex)) {
									final int argb = image.getValue(rowIndex, columnIndex);
									
									++this.data[channel0.getValue(argb)][channel1.getValue(argb)];
								}
							}
						}
					}
				}
			} else {
				for (int pixel = 0; pixel < pixelCount; ++pixel) {
					if (pixel % imageColumnCount == 0) {
						System.out.print(pixel + "/" + pixelCount + "\r");
					}
					
					if (roi != null && roi.get(pixel)) {
						if (roi != null && roi.get(pixel)) {
							final int argb = image.getValue(pixel);
							
							++this.data[channel0.getValue(argb)][channel1.getValue(argb)];
						}
					}
				}
			}
			
			this.nonZeroMinimum = Integer.MAX_VALUE;
			this.maximum = Integer.MIN_VALUE;
			
			for (final int[] subData : this.data) {
				this.maximum = Math.max(this.maximum, max(subData));
				this.nonZeroMinimum = Math.min(this.nonZeroMinimum, nonZeroMin(subData));
			}
			
			this.repaint();
		}
		
		public final void updateBuffer() {
			final Rectangle viewport = this.getVisibleRect();
			
			if (this.buffer == null || this.buffer.getWidth() != viewport.width || this.buffer.getHeight() != viewport.height) {
				this.buffer = new BufferedImage(viewport.width, viewport.height, BufferedImage.TYPE_3BYTE_BGR);
			}
			
			final ValueScale valueScale = this.context.get("valueScale");
			final int endX = viewport.x + viewport.width;
			final int endY = viewport.y + viewport.height;
			final int displayMinimum = 16;
			
			valueScale.setBounds(this.nonZeroMinimum, this.maximum, displayMinimum, 255);
			
			for (int y = viewport.y; y < endY; ++y) {
				final int datumRowIndex = this.getDatumRowIndex(y);
				
				for (int x = viewport.x; x < endX; ++x) {
					final int datumColumnIndex = this.getDatumColumnIndex(x);
					final int value = this.getValue(datumColumnIndex, datumRowIndex);
					final int color = value == 0 ? 0 : valueScale.getDisplayValue(value);
					
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
