package imj3.draft;

import static imj3.tools.CommonSwingTools.*;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static multij.swing.SwingTools.*;
import static multij.tools.Tools.*;

import imj3.core.Image2D;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import multij.swing.MouseHandler;
import multij.swing.SwingTools;

/**
 * @author codistmonk (creation 2015-07-18)
 */
public final class VisualPatchExtractor extends JPanel {
	
	private final Image2DComponent view;
	
	private final Map<String, List<Area>> regions;
	
	private final Map<String, Color> classColors;
	
	private final Point mouseLocation;
	
	private final JSpinner patchSizeSpinner;
	
	private final JSpinner patchStrideSpinner;
	
	private final JLabel patchView;
	
	private final JLabel patchClassesView;
	
	private final Rectangle patchBounds;
	
	private final Map<String, Double> patchClassRatios;
	
	private final JList<PatchInfo> patchList;
	
	public VisualPatchExtractor(final Image2DComponent view) {
		super(new BorderLayout());
		this.view = view;
		this.regions = new TreeMap<>();
		this.classColors = new HashMap<>();
		this.mouseLocation = new Point();
		this.patchSizeSpinner = new JSpinner(new SpinnerNumberModel(32, 1, 1024, 1));
		this.patchStrideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1024, 1));
		this.patchView = new JLabel("");
		this.patchClassesView = new JLabel("");
		this.patchBounds = new Rectangle();
		this.patchClassRatios = new HashMap<>();
		this.patchList = new JList<>(new DefaultListModel<>());
		
		SwingTools.setCheckAWT(false);
		
		this.setupView();
		this.setupControls();
		
		final JComponent controlBox = verticalBox(
				horizontalBox(new JLabel("patchSize"), Box.createHorizontalGlue(), this.getPatchSizeSpinner()),
				horizontalBox(new JLabel("patchStride"), Box.createHorizontalGlue(), this.getPatchStrideSpinner()),
				scrollable(center(this.getPatchView())),
				scrollable(center(this.patchClassesView)),
				scrollable(this.getPatchList()));
		
		this.add(horizontalSplit(controlBox, view), BorderLayout.CENTER);
		
		SwingTools.setCheckAWT(true);
	}
	
	public final JSpinner getPatchSizeSpinner() {
		return this.patchSizeSpinner;
	}
	
	public final JSpinner getPatchStrideSpinner() {
		return this.patchStrideSpinner;
	}
	
	public final JLabel getPatchView() {
		return this.patchView;
	}
	
	public final JLabel getPatchClassesView() {
		return this.patchClassesView;
	}
	
	public final Rectangle getPatchBounds() {
		return this.patchBounds;
	}
	
	public final Map<String, Double> getPatchClassRatios() {
		return this.patchClassRatios;
	}
	
	public final void updatePatchClasses() {
		final Rectangle patchBounds0 = this.getPatchBounds();
		final Map<String, Double> classRatios = SVGTools.normalize(
				SVGTools.getClassSurfaces(patchBounds0, getRegions()), patchBounds0.getWidth() * patchBounds0.getHeight());
		
		this.getPatchClassRatios().clear();
		this.getPatchClassRatios().putAll(classRatios);
		
		setClassesViewText(this.getPatchClassesView(), classRatios);
	}
	
	public final JList<PatchInfo> getPatchList() {
		return this.patchList;
	}
	
	public final int getPatchSize() {
		return ((SpinnerNumberModel) this.getPatchSizeSpinner().getModel()).getNumber().intValue();
	}
	
	public final VisualPatchExtractor setPatchSize(final int patchSize) {
		this.getPatchSizeSpinner().setValue(patchSize);
		
		return this;
	}
	
	public final int getPatchStride() {
		return ((SpinnerNumberModel) this.getPatchStrideSpinner().getModel()).getNumber().intValue();
	}
	
	public final VisualPatchExtractor setPatchStride(final int patchStride) {
		this.getPatchStrideSpinner().setValue(patchStride);
		
		return this;
	}
	
	public final Image2DComponent getView() {
		return this.view;
	}
	
	public final Map<String, List<Area>> getRegions() {
		return this.regions;
	}
	
	public final Map<String, Color> getClassColors() {
		return this.classColors;
	}
	
	public final BufferedImage getPatchAsBufferedImage() {
		return (BufferedImage) ((ImageIcon) this.getPatchView().getIcon()).getImage();
	}
	
	public final void updateFrameTitle() {
		final Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
		
		if (frame != null) {
			final Image2D image = this.getView().getImage();
			final double lod = -log(image.getScale()) / log(2.0);
			frame.setTitle(image.getId() + " LOD " + (int) lod);
		}
	}
	
	final void updatePatchListCellHeight() {
		final ListModel<PatchInfo> model = this.getPatchList().getModel();
		final int n = model.getSize();
		int h = 1;
		
		for (int i = 0; i < n; ++i) {
			h = max(h, 2 + model.getElementAt(i).getImage().getHeight(null));
		}
		
		this.getPatchList().setFixedCellHeight(h);
	}
	
	final void addPatchToList() {
		((DefaultListModel<PatchInfo>) this.getPatchList().getModel()).insertElementAt(
				new PatchInfo(
						new Rectangle(this.getPatchBounds()),
						new TreeMap<>(this.getPatchClassRatios()),
						this.getPatchAsBufferedImage()), 0);
	}
	
	final Point getMouseLocation() {
		return this.mouseLocation;
	}
	
	final void setPatchViewImage(final BufferedImage image) {
		this.getPatchView().setIcon(new ImageIcon(image));
	}
	
	private final void setupControls() {
		this.getPatchSizeSpinner().addChangeListener(e -> {
			this.updatePatchListCellHeight();
			this.getView().repaint();
		});
		this.getPatchSizeSpinner().setMaximumSize(this.getPatchSizeSpinner().getPreferredSize());
		
		this.getPatchStrideSpinner().addChangeListener(e -> this.getView().repaint());
		this.getPatchStrideSpinner().setMaximumSize(this.getPatchStrideSpinner().getPreferredSize());
		
		this.getPatchList().setCellRenderer(new ListCellRenderer<PatchInfo>() {
			
			private final JLabel patchRenderer = new JLabel();
			
			private final JLabel patchClassesRenderer = new JLabel();
			
			private final JComponent renderer = center(horizontalBox(this.patchRenderer, this.patchClassesRenderer));
			
			@Override
			public final Component getListCellRendererComponent(
					final JList<? extends PatchInfo> list, final PatchInfo value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				this.patchRenderer.setIcon(new ImageIcon(value.getImage()));
				
				setClassesViewText(this.patchClassesRenderer, value.getClassRatios());
				
				return this.renderer;
			}
			
		});
		this.getPatchList().getModel().addListDataListener(new ListDataListener() {
			
			@Override
			public final void intervalRemoved(final ListDataEvent event) {
				updatePatchListCellHeight();
			}
			
			@Override
			public final void intervalAdded(final ListDataEvent event) {
				updatePatchListCellHeight();
			}
			
			@Override
			public final void contentsChanged(final ListDataEvent event) {
				updatePatchListCellHeight();
			}
			
		});
	}
	
	private final void setupView() {
		final Image2DComponent component = this.getView();
		
		new MouseHandler() {
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				getMouseLocation().x = -1;
				component.repaint();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				getMouseLocation().setLocation(event.getX(), event.getY());
				component.repaint();
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				this.mouseMoved(event);
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				this.mouseMoved(event);
				updateFrameTitle();
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (!event.isPopupTrigger() && event.getClickCount() == 2) {
					addPatchToList();
				}
			}
			
			private static final long serialVersionUID = 4116418318352589515L;
			
		}.addTo(component);
		
		component.setOverlay(new Overlay() {
			
			private final Point tmp = new Point();
			
			@Override
			public final void update(final Graphics2D graphics, final Rectangle region) {
				{
					final AffineTransform transform = graphics.getTransform();
					
					graphics.setTransform(component.getView());
					
					for (final Map.Entry<String, List<Area>> entry : getRegions().entrySet()) {
						graphics.setColor(getClassColors().getOrDefault(entry.getKey(), new Color(0x60000000, true)));
						
						for (final Area r : entry.getValue()) {
							graphics.fill(r);
						}
					}
					
					graphics.setTransform(transform);
				}
				
				if (0 <= getMouseLocation().x) {
					try {
						final int size = getPatchSize();
						final int stride = getPatchStride();
						final AffineTransform view = component.getView();
						final Image2D image = component.getImage();
						final Image2D image0 = image.getScaledImage(1.0);
						final double imageScale = image.getScale();
						final int scaledPatchSize = (int) (size * view.getScaleX() / imageScale);
						
						view.inverseTransform(getMouseLocation(), this.tmp);
						
						if (0 <= this.tmp.x && this.tmp.x < image0.getWidth()
								&& 0 <= this.tmp.y && this.tmp.y < image0.getHeight()) {
							this.tmp.x = patchStart(this.tmp.x, stride, size, imageScale);
							this.tmp.y = patchStart(this.tmp.y, stride, size, imageScale);
							final int left = (int) (this.tmp.x * imageScale);
							final int top = (int) (this.tmp.y * imageScale);
							
							setPatchViewImage(new Patches.SubImage2D(image, left, top, size, size).toAwt());
							
							getPatchBounds().setBounds((int) (left / imageScale), (int) (top / imageScale),
									(int) (size / imageScale), (int) (size / imageScale));
							updatePatchClasses();
							
							view.transform(this.tmp, this.tmp);
							
							graphics.setColor(Color.RED);
							graphics.drawRect(this.tmp.x, this.tmp.y, scaledPatchSize, scaledPatchSize);
						}
					} catch (final NoninvertibleTransformException exception) {
						exception.printStackTrace();
					}
				}
				
				updateFrameTitle();
			}
			
			private static final long serialVersionUID = -6646527468467687096L;
			
		});
		
		this.view.setDropImageEnabled(true);
		
		final DropTarget dropImage = this.view.getDropTarget();
		
		this.view.setDropTarget(new DropTarget() {
			
			@Override
			public final synchronized void drop(final DropTargetDropEvent event) {
				final File file = SwingTools.getFiles(event).get(0);
				
				try {
					final Map<String, List<Area>> regions = SVGTools.getRegions(SVGTools.readXML(file));
					
					getRegions().clear();
					getRegions().putAll(regions);
					
					regions.keySet().forEach(k -> getClassColors().putIfAbsent(k, newRandomColor()));
					
					getView().repaint();
				} catch (final Exception exception) {
					debugError(exception);
					
					dropImage.drop(event);
				}
			}
			
			private static final long serialVersionUID = 3099468766376236640L;
			
		});
	}
	
	private static final long serialVersionUID = 2400491502237733629L;
	
	public static final int patchStart(final int coordinate, final int patchStride, final int patchSize, final double imageScale) {
		return (int) (adjust(coordinate, (int) (patchStride / imageScale)) - patchSize / imageScale / 2.0);
	}
	
	public static final int adjust(final int coordinate, final int patchStride) {
		return coordinate / patchStride * patchStride + patchStride / 2;
	}
	
	public static final void setClassesViewText(final JLabel classesView, final Map<String, Double> classRatios) {
		classesView.setText("<html><body>" + classRatios.entrySet().stream().map(
				e -> e.getKey() + ": " + String.format("%.2f&#37;<br>", 100.0 * e.getValue())).reduce("", String::concat) + "</body></html>");
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		SwingTools.useSystemLookAndFeel();
		
		final Image2D image;
		
		if (0 < commandLineArguments.length) {
			image = IMJTools.read(commandLineArguments[0]);
		} else {
			image = new AwtImage2D("", new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR));
		}
		
		final Image2DComponent component = new Image2DComponent(image);
		
		SwingTools.show(new VisualPatchExtractor(component), image.getId(), false);
	}
	
	/**
	 * @author codistmonk (creation 2015-07-21)
	 */
	public static final class PatchInfo implements Serializable {
		
		private final Rectangle bounds;
		
		private final Map<String, Double> classRatios;
		
		private final Image image;
		
		public PatchInfo(final Rectangle bounds, final Map<String, Double> classRatios,
				final Image image) {
			this.bounds = bounds;
			this.classRatios = classRatios;
			this.image = image;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		public final Map<String, Double> getClassRatios() {
			return this.classRatios;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
		private static final long serialVersionUID = 3361034415891899914L;
		
	}
	
}
