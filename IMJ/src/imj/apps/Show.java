package imj.apps;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.af.AFTools.newAboutItem;
import static net.sourceforge.aprog.af.AFTools.newPreferencesItem;
import static net.sourceforge.aprog.af.AFTools.newQuitItem;
import static net.sourceforge.aprog.i18n.Messages.setMessagesBase;
import static net.sourceforge.aprog.swing.SwingTools.checkAWT;
import static net.sourceforge.aprog.swing.SwingTools.menuBar;
import static net.sourceforge.aprog.swing.SwingTools.packAndCenter;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.useSystemLookAndFeel;
import static net.sourceforge.aprog.swing.SwingTools.I18N.menu;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getThisPackagePath;
import imj.Image;
import imj.ImageWrangler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.af.AFConstants;
import net.sourceforge.aprog.af.AFMainFrame;
import net.sourceforge.aprog.af.AFTools;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.af.MacOSXTools;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-13)
 */
public final class Show {
	
	private Show() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_NAME = "IMJ Show";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_VERSION = "0.1.0";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_COPYRIGHT = "(c) 2013 Codist Monk";
	
	/**
	 * {@value}.
	 */
	public static final String APPLICATION_ICON_PATH = "imj/apps/thumbnail.png";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_TOGGLE_HISTOGRAMS = "actions.toggleHistograms";
	
	public static final Context newContext() {
		final Context result = AFTools.newContext();
		
		result.set(AFConstants.Variables.APPLICATION_NAME, APPLICATION_NAME);
		result.set(AFConstants.Variables.APPLICATION_VERSION, APPLICATION_VERSION);
		result.set(AFConstants.Variables.APPLICATION_COPYRIGHT, APPLICATION_COPYRIGHT);
		result.set(AFConstants.Variables.APPLICATION_ICON_PATH, APPLICATION_ICON_PATH);
		
		new AbstractAFAction(result, AFConstants.Variables.ACTIONS_QUIT) {
			
			@Override
			public final void perform() {
				final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
				
				mainFrame.dispose();
			}
			
		};
		
		new AbstractAFAction(result, ACTIONS_TOGGLE_HISTOGRAMS) {
			
			@Override
			public final void perform() {
				JDialog histogramsDialog = result.get("histogramsDialog");
				
				if (histogramsDialog == null) {
					final AFMainFrame mainFrame = result.get(AFConstants.Variables.MAIN_FRAME);
					
					histogramsDialog = new JDialog(mainFrame, "Histograms");
					
					histogramsDialog.add(scrollable(new HistogramsPanel(result)));
					
					result.set("histogramsDialog", histogramsDialog);
				}
				
				histogramsDialog.setVisible(!histogramsDialog.isVisible());
			}
			
		};
		
		result.set(AFConstants.Variables.MAIN_MENU_BAR, menuBar(
				menu("Application",
						newAboutItem(result),
						null,
						newPreferencesItem(result),
						null,
						newQuitItem(result)),
				menu("Tools",
						newHistogramsItem(result))
		));
		
		result.set("image", null, Image.class);
		result.set("lod", null, Integer.class);
		result.set("xy", null, Point.class);
		result.set("rgb", null, String.class);
		result.set("hsb", null, String.class);
		
		final Variable<Point> xyVariable = result.getVariable("xy");
		
		xyVariable.addListener(new Listener<Point>() {
			
			private final float[] hsbBuffer = new float[4];
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Point, ?> event) {
				final Image image = result.get("image");
				
				if (image != null) {
					final Point xy = event.getNewValue();
					final int rgb = image.getValue(xy.y, xy.x);
					final int red = red(rgb);
					final int green = green(rgb);
					final int blue = blue(rgb);
					
					Color.RGBtoHSB(red, green, blue, this.hsbBuffer);
					
					result.set("rgb", "(" + red + " " + green + " " + blue + ")");
					
					final int hue = (int) (this.hsbBuffer[0] * 255);
					final int saturation = (int) (this.hsbBuffer[1] * 255);
					final int brightness = (int) (this.hsbBuffer[2] * 255);
					
					result.set("hsb", "(" + hue + " " + saturation + " " + brightness + ")");
				}
			}
			
		});
		
		return result;
	}
	
    public static final JMenuItem newHistogramsItem(final Context context) {
    	checkAWT();
    	
        return item("Histograms", context, ACTIONS_TOGGLE_HISTOGRAMS);
    }
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		if (commandLineArguments.length != 2) {
			System.out.println("Arguments: file <imageId>");
			
			return;
		}
		
		MacOSXTools.setupUI(APPLICATION_NAME, APPLICATION_ICON_PATH);
		useSystemLookAndFeel();
		setMessagesBase(getThisPackagePath() + "i18n/Messages");
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		
		ImageWrangler.INSTANCE.load(imageId);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final Context context = newContext();
				final AFMainFrame frame = AFMainFrame.newMainFrame(context);
				
				frame.setPreferredSize(new Dimension(800, 600));
				frame.setTitle(new File(imageId).getName());
				
				frame.add(scrollable(centered(new BigImageComponent(context, imageId))), BorderLayout.CENTER);
				frame.add(newStatusBar(context), BorderLayout.SOUTH);
				
				packAndCenter(frame).setVisible(true);
			}
			
		});
	}
	
	public static final String toStatusString(final Object object) {
		final Point point = cast(Point.class, object);
		
		if (point != null) {
			return "(" + point.x + " " + point.y + ")";
		}
		
		return "" + object;
	}
	
	public static final JPanel newStatusBar(final Context context) {
		final JPanel result = new JPanel();
		
		result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
		
		for (final String variableName : array("lod", "xy", "rgb", "hsb")) {
			final JLabel label = new JLabel(toStatusString(context.get(variableName)));
			
			result.add(new JLabel(variableName.toUpperCase(Locale.ENGLISH) + ":"));
			result.add(label);
			result.add(Box.createHorizontalStrut(10));
			
			final Variable<Object> variable = context.getVariable(variableName);
			
			variable.addListener(new Listener<Object>() {
				
				@Override
				public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
					label.setText(toStatusString(event.getNewValue()));
				}
				
			});
		}
		
		return result;
	}
	
	public static final JComponent centered(final Component component) {
		final JComponent result = new JPanel(new GridBagLayout());
		
		result.add(component);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-13)
	 */
	public static final class HistogramsPanel extends JPanel {
		
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
					HistogramsPanel.this.update();
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
	
	/**
	 * @author codistmonk (creation 2013-02-13)
	 */
	public static final class BigImageComponent extends JComponent {
		
		private final Context context;
		
		private final String imageId;
		
		private int lod;
		
		private Image image;
		
		private BufferedImage buffer1;
		
		private BufferedImage buffer2;
		
		private final Rectangle viewport;
		
		public BigImageComponent(final Context context, final String imageId) {
			this.context = context;
			this.imageId = imageId;
			this.viewport = new Rectangle();
			
			this.setLod(0);
			
			this.setFocusable(true);
			
			this.addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyTyped(final KeyEvent event) {
					if ('+' == event.getKeyChar()) {
						BigImageComponent.this.setLod(BigImageComponent.this.getLod() - 1);
					} else if ('-' == event.getKeyChar()) {
						BigImageComponent.this.setLod(BigImageComponent.this.getLod() + 1);
					}
				}
				
			});
			final MouseAdapter mouseHandler = new MouseAdapter() {
				
				private final Point viewportInitialLocation = new Point();
				
				private final Point mousePressedLocation = new Point();
				
				@Override
				public final void mouseMoved(final MouseEvent event) {
					context.set("xy", event.getPoint());
				}
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.viewportInitialLocation.setLocation(BigImageComponent.this.getVisibleRect().getLocation());
					this.mousePressedLocation.setLocation(event.getLocationOnScreen());
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					final Rectangle viewport = BigImageComponent.this.getVisibleRect();
					
					viewport.x = this.viewportInitialLocation.x - event.getXOnScreen() + this.mousePressedLocation.x;
					viewport.y = this.viewportInitialLocation.y - event.getYOnScreen() + this.mousePressedLocation.y;
					
					BigImageComponent.this.scrollRectToVisible(viewport);
				}
				
			};
			this.addMouseListener(mouseHandler);
			this.addMouseMotionListener(mouseHandler);
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			this.refreshBuffer();
			
			final Rectangle viewport = this.getVisibleRect();
			
			g.drawImage(this.buffer1, viewport.x, viewport.y, null);
		}
		
		public final String getImageId() {
			return this.imageId;
		}
		
		public final int getLod() {
			return this.lod;
		}
		
		public final void setLod(final int lod) {
			if (0 <= lod) {
				this.lod = lod;
				
				this.context.set("lod", lod);
				
				this.refreshImage();
			}
		}
		
		public final void refreshImage() {
			this.image = ImageWrangler.INSTANCE.load(this.getImageId(), this.getLod());
			
			this.context.set("image", this.image);
			
			final Rectangle viewport = this.getVisibleRect();
			final double scaleVariation = this.image.getColumnCount() / (double) this.getPreferredSize().getWidth();
			
			this.setPreferredSize(new Dimension(this.image.getColumnCount(), this.image.getRowCount()));
			
			this.buffer1 = null;
			this.buffer2 = null;
			this.viewport.setBounds(0, 0, 0, 0);
			
			if (!isInfinite(scaleVariation) && !isNaN(scaleVariation)) {
				if (1.0 < scaleVariation) {
					this.invalidate();
				}
				
				this.scrollRectToVisible(new Rectangle(
						(int) ((viewport.x + viewport.width / 2) * scaleVariation - viewport.width / 2),
						(int) ((viewport.y + viewport.height/ 2) * scaleVariation - viewport.height / 2),
						viewport.width, viewport.height));
				
				if (scaleVariation < 1.0) {
					this.invalidate();
				}
				
				this.repaint();
			}
		}
		
		public static final BufferedImage copyOf(final BufferedImage image, final int newWidth, final int newHeight) {
			if (image == null) {
				return new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
			}
			
			final BufferedImage result = new BufferedImage(newWidth, newHeight, image.getType());
			final Graphics2D g = result.createGraphics();
			
			g.drawImage(image, 0, 0, null);
			
			g.dispose();
			
			return result;
		}
		
		public final void refreshBuffer() {
			final Rectangle newViewport = this.getVisibleRect();
			
			if ((this.buffer1 == null || this.buffer1.getWidth() != newViewport.width || this.buffer1.getHeight() != newViewport.height) &&
					!newViewport.isEmpty()) {
				this.buffer1 = copyOf(this.buffer1, newViewport.width, newViewport.height);
				this.buffer2 = copyOf(this.buffer2, newViewport.width, newViewport.height);
			}
			
			if (this.buffer1 != null && this.image != null) {
				final int endY = newViewport.y + newViewport.height;
				final int endX = newViewport.x + newViewport.width;
				
				if (!this.viewport.isEmpty()) {
					final Graphics2D g = this.buffer2.createGraphics();
					g.drawImage(this.buffer1, this.viewport.x - newViewport.x, this.viewport.y - newViewport.y, null);
					g.dispose();
				}
				
				for (int y = newViewport.y; y < endY; ++y) {
					if (y < 0 || this.image.getRowCount() <= y) {
						continue;
					}
					
					for (int x = newViewport.x; x < endX; ++x) {
						if (y < 0 || this.image.getRowCount() <= y || this.viewport.contains(x, y)) {
							continue;
						}
						
						this.buffer2.setRGB(x - newViewport.x, y - newViewport.y, this.image.getValue(y, x));
					}
				}
				
				{
					final BufferedImage tmp = this.buffer1;
					this.buffer1 = this.buffer2;
					this.buffer2 = tmp;
				}
			}
			
			this.viewport.setBounds(newViewport);
		}
		
	}
	
}
