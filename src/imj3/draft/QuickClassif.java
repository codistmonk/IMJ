package imj3.draft;

import static imj3.core.IMJCoreTools.*;
import static java.lang.Math.max;
import static multij.swing.SwingTools.horizontalBox;
import static multij.swing.SwingTools.show;
import static multij.swing.SwingTools.verticalBox;
import static multij.tools.Tools.array;

import imj3.core.IMJCoreTools;
import imj3.core.IMJCoreTools.Reference;
import imj3.core.Image2D;
import imj3.draft.QuickSeg2.Monitor;
import imj3.tools.AwtImage2D;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;
import imj3.tools.Image2DComponent.TileOverlay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;
import multij.tools.Canvas;
import multij.tools.IllegalInstantiationException;
import multij.tools.Pair;
import multij.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2016-06-03)
 */
public final class QuickClassif {
	
	private QuickClassif() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
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
	
	public static final JPanel newMainPanel(final Context context) {
		final JPanel result = new JPanel(new BorderLayout());
		
		context.setMainPanel(result);
		context.setqField(new JTextField("6"));
		context.setsField(new JTextField("32"));
		context.setFineCheckBox(new JCheckBox("Fine view"));
		context.setToolSelector(new JComboBox<>(array("Move", "Draw")));
		context.setAnnotationSelector(new JComboBox<>(array("<New>", "-", "Open...", "Save...")));
		context.setModelSelector(new JComboBox<>(array("<New>", "-", "Open...", "Save...")));
		
		context.setupClasses();
		
		final Box optionBox = horizontalBox(
				new JLabel("Q:"), context.getqField(),
				new JLabel("S:"), context.getsField(),
				context.getFineCheckBox());
		final Box annotationBox = horizontalBox(
				new JLabel("Tool:"), context.getToolSelector(),
				new JLabel("Class:"), context.getClassSelector(),
				new JLabel("Annotation:"), context.getAnnotationSelector(),
				new JLabel("Model:"), context.getModelSelector());
		final Box toolBox = verticalBox(optionBox, annotationBox);
		
		context.setImageComponent(new Image2DComponent(new AwtImage2D("", 256, 256)).setDropImageEnabled(true));
		
		result.add(toolBox, BorderLayout.NORTH);
		result.add(context.getImageComponent(), BorderLayout.CENTER);
		
		final ActionListener fieldActionListener = new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				context.clearKeys();
				context.repaint();
			}
			
		};
		
		context.getqField().addActionListener(fieldActionListener);
		context.getsField().addActionListener(fieldActionListener);
		context.getFineCheckBox().addActionListener(fieldActionListener);
		
		context.getToolSelector().addActionListener(new ActionListener() {
			
			@Override
			public final void actionPerformed(final ActionEvent event) {
				if ("Draw".equals(context.getToolSelector().getSelectedItem())) {
					context.getImageComponent().setDragEnabled(false);
					context.getImageComponent().setWheelZoomEnabled(false);
				} else {
					context.getImageComponent().setDragEnabled(true);
					context.getImageComponent().setWheelZoomEnabled(true);
				}
			}
			
		});
		
		new MouseHandler() {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				final int n = context.getRegions().size();
				
				if (context.isDrawing()) {
					Map<String, Object> region = context.getSelectedRegion();
					
					if (region == null) {
						region = new HashMap<>();
						
						region.put("geometry", new Area());
						region.put("color", context.getCurrentClassColor());
						
						context.getRegions().add(region);
						context.setSelectedRegionIndex(n);
					}
					
					final Rectangle brushBounds = context.getBrushBounds();
					final Area newGeometry = new Area(new Ellipse2D.Double(brushBounds.getX(), brushBounds.getY(), brushBounds.getWidth(), brushBounds.getHeight()));
					
					if (event.isShiftDown()) {
						((Area) region.get("geometry")).subtract(newGeometry);
					} else {
						((Area) region.get("geometry")).add(newGeometry);
					}
					
					context.repaint();
				} else {
					final int previous = context.getSelectedRegionIndex();
					final int start = context.getSelectedRegionIndex() + 1;
					final int end = start + n;
					
					context.setSelectedRegionIndex(-1);
					
					for (int i = start; i < end; ++i) {
						final int j = i % n;
						final Map<String, Object> region = context.getRegions().get(j);
						
						if (((Shape) region.get("geometry")).contains(event.getX(), event.getY())) {
							context.setSelectedRegionIndex(j);
							break;
						}
					}
					
					if (previous != context.getSelectedRegionIndex()) {
						context.repaint();
					}
				}
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				context.repaintIfDrawing();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (context.isDrawing()) {
					if (event.isShiftDown()) {
						if (event.getWheelRotation() < 0) {
							context.setPaintbrushSize(context.getPaintbrushSize() - 1F);
						} else {
							context.setPaintbrushSize(context.getPaintbrushSize() + 1F);
						}
					} else {
						if (event.getWheelRotation() < 0) {
							context.setPaintbrushSize(context.getPaintbrushSize() / 1.5F);
						} else {
							context.setPaintbrushSize(context.getPaintbrushSize() * 1.5F);
						}
					}
					
					context.repaint();
				}
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				if (context.isDrawing()) {
					context.getMouseLocation().setLocation(event.getX(), event.getY());
					context.repaint();
				}
			}
			
			private static final long serialVersionUID = 1283974492350448403L;
			
		}.addTo(context.getImageComponent());
		
		context.getImageComponent().setTileOverlay(new QuickClassifTileOverlay(context));
		context.getImageComponent().setOverlay(new QuickClassifOverlay(context));
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class QuickClassifOverlay implements Overlay {
		
		private final Context context;
		
		private final Image2DComponent imageComponent;
		
		private final JCheckBox fineCheckBox;
		
		private final Canvas canvas;
		
		public QuickClassifOverlay(final Context context) {
			this.context = context;
			this.imageComponent = context.getImageComponent();
			this.fineCheckBox = context.getFineCheckBox();
			this.canvas = new Canvas();
		}
		
		@Override
		public final void update(final Graphics2D graphics, final Rectangle region) {
			for (final Map<String, Object> r : this.context.getRegions()) {
				graphics.setColor((Color) r.get("color"));
				graphics.fill((Shape) r.get("geometry"));
			}
			
			{
				final Map<String, Object> r = this.context.getSelectedRegion();
				
				if (r != null) {
					graphics.setColor(Color.BLACK);
					graphics.draw((Shape) r.get("geometry"));
				}
			}
			
			if (this.context.isDrawing()) {
				final Rectangle brushBounds = this.context.getBrushBounds();
				
				graphics.setColor(Color.WHITE);
				graphics.drawOval(brushBounds.x, brushBounds.y, brushBounds.width, brushBounds.height);
			}
			
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
		
		private static final long serialVersionUID = 1046682725999495091L;
		
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class QuickClassifTileOverlay implements TileOverlay {
		
		private final Context context;
		
		private final JTextField qField;
		
		private final JTextField sField;
		
		private final JCheckBox fineCheckBox;
		
		private final Image2DComponent imageComponent;

		public QuickClassifTileOverlay(final Context context) {
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
									final Monitor monitor = QuickClassifTileOverlay.this.context.newKeyMonitor(key);
									final int q = Integer.decode(QuickClassifTileOverlay.this.qField.getText());
									final int s = Integer.decode(QuickClassifTileOverlay.this.sField.getText());
									final Image2D tile = image.getTile(tileXY.x, tileXY.y);
									final BufferedImage segments = QuickSeg2.segment(tile, q, s, 0, monitor);
									
									if (segments == null) {
										return null;
									}
									
									cache(image.getTileKey(tileXY.x, tileXY.y) + "_segments", () -> segments, true);
									
									if (QuickClassifTileOverlay.this.fineCheckBox.isSelected() || max(tile.getWidth(), tile.getHeight()) < s) {
										return null;
									}
									
									return QuickSeg2.outline(segments, monitor);
								}
								
							});
							
							QuickClassifTileOverlay.this.context.getHash().incrementAndGet();
							QuickClassifTileOverlay.this.imageComponent.repaint();
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
		
		private static final long serialVersionUID = -3312425399419835765L;
		
	}

	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static final class Context implements Serializable {
		
		private final ExecutorService executor = Executors.newFixedThreadPool(4);
		
		private final Collection<String> keys = new HashSet<>();
		
		private final AtomicInteger hash = new AtomicInteger(1);
		
		private final Collection<RepaintListener> repaintListeners = new ArrayList<>();
		
		private final Point mouseLocation = new Point();
		
		private float paintbrushSize = 10F;
		
		private final List<Map<String, Object>> regions = new ArrayList<>();
		
		private int selectedRegionIndex = -1;
		
		private Document classes;
		
		private JPanel mainPanel;
		
		private JTextField qField;
		
		private JTextField sField;
		
		private JCheckBox fineCheckBox;
		
		private JComboBox<String> toolSelector;
		
		private JComboBox<Pair<String, String>> classSelector;
		
		private JComboBox<String> annotationSelector;
		
		private JComboBox<String> modelSelector;
		
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
		
		public final Point getMouseLocation() {
			return this.mouseLocation;
		}
		
		public final float getPaintbrushSize() {
			return this.paintbrushSize;
		}
		
		public final void setPaintbrushSize(final float paintbrushSize) {
			this.paintbrushSize = max(1F, paintbrushSize);
		}
		
		public final Rectangle getBrushBounds() {
			final int s = (int) this.getPaintbrushSize();
			
			return new Rectangle(this.getMouseLocation().x - s / 2, this.getMouseLocation().y - s / 2, s, s);
		}
		
		public final int getSelectedRegionIndex() {
			return this.selectedRegionIndex;
		}
		
		public final void setSelectedRegionIndex(final int selectedRegionIndex) {
			this.selectedRegionIndex = selectedRegionIndex;
		}
		
		public final Map<String, Object> getSelectedRegion() {
			return this.getSelectedRegionIndex() < 0 ? null : this.getRegions().get(this.getSelectedRegionIndex());
		}
		
		public final List<Map<String, Object>> getRegions() {
			return this.regions;
		}
		
		public final Document getClasses() {
			return this.classes;
		}
		
		public final void setupClasses() {
			try {
				this.classes = SVGTools.readXML(new File("../WSIData/classes.xml"));
				
				final Collection<Pair<String, String>> classHandles = new ArrayList<>();
				
				XMLTools.getElements(this.classes, "//class").forEach(classElement -> {
					classHandles.add(new Pair<>(classElement.getAttribute("id"), classElement.getAttribute("name")));
				});
				
				this.classSelector = new JComboBox<>(classHandles.toArray(new Pair[classHandles.size()]));
				
				this.classSelector.addActionListener(new ActionListener() {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final Map<String, Object> r = getSelectedRegion();
						
						if (r != null) {
							r.put("color", getCurrentClassColor());
							repaint();
						}
					}
					
				});
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
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
		
		public final JComboBox<String> getToolSelector() {
			return this.toolSelector;
		}
		
		public final void setToolSelector(final JComboBox<String> toolSelector) {
			this.toolSelector = toolSelector;
		}
		
		public final JComboBox<Pair<String, String>> getClassSelector() {
			return this.classSelector;
		}
		
		public final JComboBox<String> getAnnotationSelector() {
			return this.annotationSelector;
		}
		
		public final void setAnnotationSelector(final JComboBox<String> annotationSelector) {
			this.annotationSelector = annotationSelector;
		}
		
		public final JComboBox<String> getModelSelector() {
			return this.modelSelector;
		}
		
		public final void setModelSelector(final JComboBox<String> modelSelector) {
			this.modelSelector = modelSelector;
		}
		
		public final Image2DComponent getImageComponent() {
			return this.imageComponent;
		}
		
		public final void setImageComponent(final Image2DComponent imageComponent) {
			this.imageComponent = imageComponent;
		}
		
		public final void repaint() {
			this.getHash().incrementAndGet();
			this.getImageComponent().repaint();
		}
		
		public final void repaintIfDrawing() {
			if (this.isDrawing()) {
				this.repaint();
			}
		}
		
		public final boolean isDrawing() {
			return "Draw".equals(this.getToolSelector().getSelectedItem());
		}
		
		public final Color getCurrentClassColor() {
			try {
				final String classId = ((Pair<String, String>) this.getClassSelector().getSelectedItem()).getFirst();
				final Element classElement = XMLTools.getElement(this.getClasses(), "//class[@id='" + classId + "']");
				
				return new Color(0x80000000 | (0x00FFFFFF & Long.decode(classElement.getAttribute("preferredColor")).intValue()));
			} catch (final Exception exception) {
				return Color.BLACK;
			}
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
		
		private static final long serialVersionUID = 982030101247784588L;
		
		/**
		 * @author codistmonk (creation 2016-06-06)
		 */
		public static abstract interface RepaintListener extends Serializable {
			
			public abstract void repainted();
			
		}
		
	}
	
}
