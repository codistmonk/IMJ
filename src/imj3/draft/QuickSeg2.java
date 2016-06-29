package imj3.draft;

import static imj3.core.IMJCoreTools.*;
import static imj3.tools.IMJTools.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static multij.swing.SwingTools.horizontalBox;
import static multij.swing.SwingTools.horizontalSplit;
import static multij.swing.SwingTools.scrollable;
import static multij.swing.SwingTools.show;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.getNumber;

import imj3.core.Channels;
import imj3.core.IMJCoreTools;
import imj3.core.IMJCoreTools.Reference;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools;
import imj3.tools.IMJTools.ComponentComembership;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;
import imj3.tools.Image2DComponent.TileOverlay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import multij.primitivelists.FloatList;
import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2016-06-03)
 */
public final class QuickSeg2 {
	
	private QuickSeg2() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		
		if (!imagePath.isEmpty()) {
			debugPrint("image:", imagePath);
			
			final int q = arguments.get("q", 5)[0];
			final int s = arguments.get("s", 16)[0];
			
			debugPrint("q:", q, "s:", s);
			
			final Image2D image = IMJTools.read(imagePath);
			final String outputPath = baseName(imagePath) + "_segments.zip";
			
			debugPrint("output:", outputPath);
			
			segmentFull(image, q, s, outputPath, Monitor.DEFAULT);
			
			return;
		}
		
		SwingTools.useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final Context context = new Context();
				final JPanel mainPanel = newMainPanel(context);
				final Window mainFrame = show(mainPanel, "Image");
				
				mainFrame.addWindowListener(new WindowAdapter() {
					
					@Override
					public final void windowClosing(final WindowEvent event) {
						context.destroy();
					}
					
				});
			}
			
		});
	}
	
	public static final void segmentFull(final Image2D image, final int q, final int s, final String outputPath, final Monitor monitor) {
		final int w0 = image.getWidth();
		final int h0 = image.getHeight();
		final String tileFormat = "png";
		
		createZipImage(outputPath, image.getWidth(), image.getHeight(), image.getOptimalTileWidth(), image.getOptimalTileHeight(), tileFormat, (Double) image.getMetadata().get("micronsPerPixel"), new TileGenerator() {
			
			private ZipOutputStream output;
			
			private String type;
			
			private Image2D im;
			
			@Override
			public final ZipOutputStream getOutput() {
				return this.output;
			}
			
			@Override
			public final void setOutput(final ZipOutputStream output) {
				this.output = output;
			}
			
			@Override
			public final void setElement(final Element imageElement) {
				this.type = imageElement.getAttribute("type");
				final int w = getNumber(imageElement, "@width").intValue();
				final int h = getNumber(imageElement, "@height").intValue();
				this.im = image.getScaledImage(max((double) w / w0, (double) h / h0));
			}
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				final BufferedImage segments = segment(this.im.getTile(tileX, tileY), q, s, 0xFF000000, monitor);
				
				if (tileX == 0) {
					debugPrint(this.im.getId(), this.type, tileX, tileY);
				}
				
				try {
					this.output.putNextEntry(new ZipEntry("tiles/tile_" + this.type + "_y" + tileY + "_x" + tileX + "." + tileFormat));
					ImageIO.write(segments, tileFormat, this.output);
					this.output.closeEntry();
				} catch (final IOException exception) {
					throw new UncheckedIOException(exception);
				}
				
				return segments != null;
			}
			
			private static final long serialVersionUID = -349954077441100567L;
			
		});
	}
	
	public static final BufferedImage segment(final Image2D image, final int q, final int s, final int id0, final Monitor monitor) {
		final int w = image.getWidth();
		final int h = image.getHeight();
		final BufferedImage transformedMiddle = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final BufferedImage segments = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Channels channels = image.getChannels();
		final int qMask = ((~0) << q) & 0xFF;
		
		for (int y = 0; y < h; ++y) {
			if (monitor.isCancelRequested()) {
				return null;
			}
			
			for (int x = 0; x < w; ++x) {
				final long value = image.getPixelValue(x, y);
				final int r = (int) channels.getChannelValue(value, 0);
				final int g = (int) channels.getChannelValue(value, 1);
				final int b = (int) channels.getChannelValue(value, 2);
				long newMiddleValue = channels.setChannelValue(0L, 0, qMask & r);
				newMiddleValue = channels.setChannelValue(newMiddleValue, 1, qMask & g);
				newMiddleValue = channels.setChannelValue(newMiddleValue, 2, qMask & b);
				
				transformedMiddle.setRGB(x, y, (int) newMiddleValue);
			}
		}
		
		final int[] id = { id0 };
		
		forEachTile(w, h, s, s, new Pixel2DProcessor() {
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				if (monitor.isCancelRequested()) {
					id[0] = -1;
					return false;
				}
				
				final int tw = min(s, w - tileX);
				final int th = min(s, h - tileY);
				
				forEachPixelInEachComponent4(image, new Rectangle(tileX, tileY, tw, th), new ComponentComembership() {
					
					@Override
					public final boolean test(final Image2D image, final long pixel, final long otherPixel) {
						final int x = image.getX(pixel);
						final int y = image.getY(pixel);
						final int otherX = image.getX(otherPixel);
						final int otherY = image.getY(otherPixel);
						
						return transformedMiddle.getRGB(x, y) == transformedMiddle.getRGB(otherX, otherY);
					}
					
					private static final long serialVersionUID = 6632151904983956288L;
					
				}, new Pixel2DProcessor() {
					
					@Override
					public final boolean pixel(final int x, final int y) {
						segments.setRGB(x, y, id[0]);
						
						return true;
					}
					
					@Override
					public final void endOfPatch() {
						++id[0];
					}
					
					private static final long serialVersionUID = -6435721813416466409L;
					
				});
				
				return true;
			}
			
			private static final long serialVersionUID = 13671048674063813L;
			
		});
		
		return id[0] == -1 ? null : segments;
	}
	
	public static final Image2D outline(final BufferedImage segments, final Monitor monitor) {
		final int w = segments.getWidth();
		final int h = segments.getHeight();
		final AwtImage2D result = new AwtImage2D("", w, h);
		
		for (int y = 0; y < h; ++y) {
			if (monitor.isCancelRequested()) {
				return null;
			}
			
			for (int x = 0; x < w; ++x) {
				final long center = segments.getRGB(x, y);
				boolean mark = false;
				
				if (0 < y) {
					mark = mark || center != segments.getRGB(x, y - 1);
				}
				
				if (0 < x) {
					mark = mark || center != segments.getRGB(x - 1, y);
				}
				
				mark = mark || x == 0 || x == w - 1 || y == 0 || y == h - 1;
				
				if (mark) {
					result.setPixelValue(x, y, 0xFF00FF00L);
				}
			}
		}
		
		return result;
	}
	
	public static final JPanel newMainPanel(final Context context) {
		final JPanel result = new JPanel(new BorderLayout());
		
		context.setMainPanel(result);
		context.setqField(new JTextField("6"));
		context.setsField(new JTextField("32"));
		context.setFineCheckBox(new JCheckBox("Fine view"));
		
		final JButton convolutionalHistogramButton = new JButton("ConvHisto...");
		
		final Box optionsBox = horizontalBox(
				new JLabel("Q:"), context.getqField(),
				new JLabel("S:"), context.getsField(),
				context.getFineCheckBox(),
				convolutionalHistogramButton);
		context.setImageComponent(new Image2DComponent(new AwtImage2D("", 256, 256)).setDropImageEnabled(true));
		
		result.add(optionsBox, BorderLayout.NORTH);
		result.add(context.getImageComponent(), BorderLayout.CENTER);
		
		final ActionListener fieldActionListener = new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				context.clearKeys();
				context.getHash().incrementAndGet();
				context.getImageComponent().repaint();
			}
			
		};
		
		context.getqField().addActionListener(fieldActionListener);
		context.getsField().addActionListener(fieldActionListener);
		context.getFineCheckBox().addActionListener(fieldActionListener);
		
		convolutionalHistogramButton.addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				final ConvolutionalHistogramPanel convhistoPanel = new ConvolutionalHistogramPanel(context);
				final Window window = show(convhistoPanel, "Convolutional histogram");
				
				window.addWindowListener(new WindowAdapter() {
					
					@Override
					public final void windowClosing(WindowEvent event) {
						context.getRepaintListeners().remove(convhistoPanel);
					}
					
				});
			}
			
		});
		
		context.getImageComponent().setTileOverlay(new QuickSegTileOverlay(context));
		context.getImageComponent().setOverlay(new QuickSegOverlay(context));
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class ConvolutionalHistogramPanel extends JPanel implements Context.RepaintListener {
		
		private final Context context;
		
		private final Canvas canvas;
		
		private final JTextArea kernelEditor;
		
		private final JComponent histoView;
		
		private final int[] counts;
		
		private ConvolveOp op;
		
		public ConvolutionalHistogramPanel(final Context context) {
			super(new BorderLayout());
			this.context = context;
			this.canvas = new Canvas();
			this.kernelEditor = new JTextArea("1");
			this.histoView = new JComponent() {

				{
					this.setPreferredSize(new Dimension(256, 256));
					this.setMinimumSize(new Dimension(256, 256));
				}
				
				@Override
				public final void paintComponent(final Graphics graphics) {
					graphics.setColor(Color.WHITE);
					graphics.fillRect(0, 0, 256, 256);
					
					final OptionalInt s = Arrays.stream(getCounts()).max();
					
					if (s.isPresent() && 0 < s.getAsInt()) {
						graphics.setColor(Color.BLACK);
						
						for (int i = 0; i < 256; ++i) {
							graphics.drawLine(i, 255, i, 255 - 255 * getCounts()[i] / s.getAsInt());
						}
					}
				}
				
				private static final long serialVersionUID = 619396278275341879L;
				
			};
			this.counts = new int[256];
			this.op = new ConvolveOp(new Kernel(1, 1, new float[] { 1F }));
			
			context.getRepaintListeners().add(this);
			
			this.add(horizontalSplit(scrollable(this.kernelEditor), scrollable(this.histoView)), BorderLayout.CENTER);
			
			this.kernelEditor.addKeyListener(new KeyAdapter() {
				
				@Override
				public final void keyPressed(final KeyEvent event) {
					if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_ENTER) {
						final FloatList data = new FloatList();
						int rowCount = 0;
						
						try (final Scanner scanner = new Scanner(ConvolutionalHistogramPanel.this.getKernelEditor().getText())) {
							scanner.useLocale(Locale.ENGLISH);
							
							while (scanner.hasNext()) {
								try (final Scanner lineScanner = new Scanner(scanner.nextLine())) {
									lineScanner.useLocale(Locale.ENGLISH);
									
									while (lineScanner.hasNext()) {
										data.add(lineScanner.nextFloat());
									}
									
									++rowCount;
								}
							}
						} catch (final Exception exception) {
							exception.printStackTrace();
							return;
						}
						
						ConvolutionalHistogramPanel.this.setOp(new Kernel(data.size() / rowCount, rowCount, data.toArray()));
						repainted();
					}
				}
				
			});
		}
		
		@Override
		public final void repainted() {
			final BufferedImage source = this.context.getImageComponent().getCanvasImage();
			final int w = source.getWidth();
			final int h = source.getHeight();
			this.canvas.setFormat(w, h, source.getType());
			this.op.filter(source, this.canvas.getImage());
			
			Arrays.fill(this.counts, 0);
			
			for (int y = 0; y < h; ++y) {
				for (int x = 0; x < w; ++x) {
					final Color c = new Color(this.canvas.getImage().getRGB(x, y));
					
					++this.counts[(c.getRed() + c.getGreen() + c.getBlue()) / 3];
				}
			}
			
			this.repaint();
		}
		
		final JTextArea getKernelEditor() {
			return this.kernelEditor;
		}
		
		final int[] getCounts() {
			return this.counts;
		}
		
		final ConvolveOp getOp() {
			return this.op;
		}
		
		final void setOp(final Kernel kernel) {
			this.setOp(new ConvolveOp(kernel));
		}
		
		final void setOp(final ConvolveOp op) {
			this.op = op;
		}
		
		private static final long serialVersionUID = -5126774744460467675L;
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class QuickSegOverlay implements Overlay {
		
		private final Image2DComponent imageComponent;
		
		private final JCheckBox fineCheckBox;
		
		private final Canvas canvas;
		
		public QuickSegOverlay(final Context context) {
			this.imageComponent = context.getImageComponent();
			this.fineCheckBox = context.getFineCheckBox();
			this.canvas = new Canvas();
		}
		
		@Override
		public final void update(final Graphics2D graphics, final Rectangle region) {
			if (region.isEmpty() || !this.fineCheckBox.isSelected()) {
				return;
			}
			
			this.canvas.setFormat(region.width, region.height).clear(new Color(0, true));
			
			final Point2D p0 = new Point2D.Double(0.0, 0.0);
			final Point2D p1 = new Point2D.Double(1.0, 1.0);
			
			try {
				this.imageComponent.getView().inverseTransform(p0, p0);
				this.imageComponent.getView().inverseTransform(p1, p1);
				
				final double dx = p1.getX() - p0.getX();
				final double dy = p1.getY() - p0.getY();
				final int w = region.width;
				final int h = region.height;
				final Image2D image = this.imageComponent.getImage();
				final double scale = image.getScale();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						final int xIm = (int) ((p0.getX() + x * dx) * scale);
						final int yIm = (int) ((p0.getY() + y * dy) * scale);
						
						if (0 <= xIm && xIm < image.getWidth() && 0 <= yIm && yIm < image.getHeight()) {
							final int tileX = image.getTileXContaining(xIm);
							final int tileY = image.getTileYContaining(yIm);
							final String key = image.getTileKey(tileX, tileY) + "_segments";
							final Reference<BufferedImage> segments = getCached(key);
							
							if (segments != null && segments.hasObject()) {
								final int centerId = segments.getObject().getRGB(xIm - tileX, yIm - tileY);
								final int north = (int) ((p0.getY() + (y - 1) * dy) * scale);
								final int west = (int) ((p0.getX() + (x - 1) * dx) * scale);
								
								boolean mark = false;
								
								if (0 <= north && north < yIm) {
									final int northTileY = image.getTileYContaining(north);
									final String northKey = image.getTileKey(tileX, northTileY) + "_segments";
									final Reference<BufferedImage> northSegments = getCached(northKey);
									
									if (northSegments != null && northSegments.hasObject()) {
										mark = mark || centerId != northSegments.getObject().getRGB(xIm - tileX, north - northTileY);
									}
								}
								
								if (0 <= west && west < xIm) {
									final int westTileX = image.getTileXContaining(west);
									final String westKey = image.getTileKey(westTileX, tileY) + "_segments";
									final Reference<BufferedImage> westSegments = getCached(westKey);
									
									if (westSegments != null && westSegments.hasObject()) {
										mark = mark || centerId != westSegments.getObject().getRGB(west - westTileX, yIm - tileY);
									}
								}
								
								if (mark) {
									this.canvas.getImage().setRGB(x, y, 0xFF00FF00);
								}
							}
						}
					}
				}
				
				graphics.drawImage(this.canvas.getImage(), 0, 0, null);
			} catch (final NoninvertibleTransformException exception) {
				exception.printStackTrace();
			}
		}
		
		private static final long serialVersionUID = -6245612608729813604L;
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static abstract interface Monitor extends Serializable {
		
		public abstract boolean isCancelRequested();
		
		public static final Monitor DEFAULT = () -> false;
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class QuickSegTileOverlay implements TileOverlay {
		
		private final Context context;
		
		private final JTextField qField;
		
		private final JTextField sField;
		
		private final JCheckBox fineCheckBox;
		
		private final Image2DComponent imageComponent;

		public QuickSegTileOverlay(final Context context) {
			this.context = context;
			this.qField = context.getqField();
			this.sField = context.getsField();
			this.fineCheckBox = context.getFineCheckBox();
			this.imageComponent = context.getImageComponent();
		}

		@Override
		public final int hashCode() {
			return this.context.getHash().get();
		}

		@Override
		public final void update(final Graphics2D graphics, final Point tileXY, final Rectangle region) {
			final Image2D image = this.imageComponent.getImage();
			final String key = image.getTileKey(tileXY.x, tileXY.y) + "_outlines";
			final Reference<Image2D> cached = getCached(key);
			
			if (cached == null) {
//				graphics.setColor(Color.RED);
//				graphics.draw(region);
				
				if (this.context.getKeys().add(key)) {
					this.context.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							cache(key, new Supplier<Image2D>() {
								
								@Override
								public final Image2D get() {
									final Monitor monitor = QuickSegTileOverlay.this.context.newKeyMonitor(key);
									final int q = Integer.decode(QuickSegTileOverlay.this.qField.getText());
									final int s = Integer.decode(QuickSegTileOverlay.this.sField.getText());
									final Image2D tile = image.getTile(tileXY.x, tileXY.y);
									final BufferedImage segments = segment(tile, q, s, 0, monitor);
									
									if (segments == null) {
										return null;
									}
									
									cache(image.getTileKey(tileXY.x, tileXY.y) + "_segments", () -> segments, true);
									
									if (QuickSegTileOverlay.this.fineCheckBox.isSelected() || max(tile.getWidth(), tile.getHeight()) < s) {
										return null;
									}
									
									return outline(segments, monitor);
								}
								
							});
							
							QuickSegTileOverlay.this.context.getHash().incrementAndGet();
							QuickSegTileOverlay.this.imageComponent.repaint();
						}
						
					});
				}
			} else if (cached.hasObject()) {
				graphics.drawImage((Image) cached.getObject().toAwt(), region.x, region.y, region.width, region.height, null);
			} else if (!this.fineCheckBox.isSelected()) {
//				graphics.setColor(Color.RED);
//				graphics.draw(region);
			}
			
			this.context.getRepaintListeners().forEach(Context.RepaintListener::repainted);
		}
		
		private static final long serialVersionUID = -4573576871706331668L;
		
	}

	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class Context implements Serializable {
		
		private final ExecutorService executor = Executors.newFixedThreadPool(4);
		
		private final Collection<String> keys = new HashSet<>();
		
		private final AtomicInteger hash = new AtomicInteger(1);
		
		private final Collection<RepaintListener> repaintListeners = new ArrayList<>();
		
		private JPanel mainPanel;
		
		private JTextField qField;
		
		private JTextField sField;
		
		private JCheckBox fineCheckBox;
		
		private Image2DComponent imageComponent;
		
		public final ExecutorService getExecutor() {
			return this.executor;
		}
		
		public final Collection<String> getKeys() {
			return this.keys;
		}
		
		public final AtomicInteger getHash() {
			return this.hash;
		}
		
		public final Collection<RepaintListener> getRepaintListeners() {
			return this.repaintListeners;
		}
		
		public final JPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final void setMainPanel(final JPanel mainPanel) {
			this.mainPanel = mainPanel;
		}
		
		public final JTextField getqField() {
			return this.qField;
		}
		
		public final void setqField(final JTextField qField) {
			this.qField = qField;
		}
		
		public final JTextField getsField() {
			return this.sField;
		}
		
		public final void setsField(final JTextField sField) {
			this.sField = sField;
		}
		
		public final JCheckBox getFineCheckBox() {
			return this.fineCheckBox;
		}
		
		public final void setFineCheckBox(final JCheckBox fineCheckBox) {
			this.fineCheckBox = fineCheckBox;
		}
		
		public final Image2DComponent getImageComponent() {
			return this.imageComponent;
		}
		
		public final void setImageComponent(final Image2DComponent imageComponent) {
			this.imageComponent = imageComponent;
		}
		
		public final void clearKeys() {
			this.getKeys().forEach(IMJCoreTools::uncache);
			this.getKeys().clear();
		}
		
		public final Monitor newKeyMonitor(final String key) {
			return () -> !this.getKeys().contains(key);
		}
		
		public final void destroy() {
			this.getExecutor().shutdown();
			this.getKeys().clear();
		}
		
		private static final long serialVersionUID = -5610744496161785513L;
		
		/**
		 * @author codistmonk (creation 2016-06-06)
		 */
		public static abstract interface RepaintListener extends Serializable {
			
			public abstract void repainted();
			
		}
		
	}
	
}
