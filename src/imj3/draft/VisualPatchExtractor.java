package imj3.draft;

import static imj3.tools.CommonSwingTools.center;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static multij.swing.SwingTools.*;

import imj3.core.Image2D;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;

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
	
	private final Point mouseLocation;
	
	private final JSpinner patchSizeSpinner;
	
	private final JSpinner patchStrideSpinner;
	
	private final JLabel patchView;
	
	private final JList<Image> patchList;
	
	public VisualPatchExtractor(final Image2DComponent view) {
		super(new BorderLayout());
		this.view = view;
		this.mouseLocation = new Point();
		this.patchSizeSpinner = new JSpinner(new SpinnerNumberModel(32, 1, 1024, 1));
		this.patchStrideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1024, 1));
		this.patchView = new JLabel("");
		this.patchList = new JList<>(new DefaultListModel<>());
		
		this.setupView();
		this.setupControls();
		
		SwingTools.setCheckAWT(false);
		
		final JComponent controlBox = verticalBox(
				horizontalBox(new JLabel("patchSize"), Box.createHorizontalGlue(), this.getPatchSizeSpinner()),
				horizontalBox(new JLabel("patchStride"), Box.createHorizontalGlue(), this.getPatchStrideSpinner()),
				scrollable(center(this.getPatchView())),
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
	
	public final JList<Image> getPatchList() {
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
		final ListModel<Image> model = this.getPatchList().getModel();
		final int n = model.getSize();
		int h = 1;
		
		for (int i = 0; i < n; ++i) {
			h = max(h, 2 + model.getElementAt(i).getHeight(null));
		}
		
		this.getPatchList().setFixedCellHeight(h);
	}
	
	final void addPatchToList() {
		((DefaultListModel<Image>) this.getPatchList().getModel()).insertElementAt(this.getPatchAsBufferedImage(), 0);
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
		
		this.getPatchList().setCellRenderer(new ListCellRenderer<Image>() {
			
			private final JLabel patchRenderer = new JLabel();
			
			private final JComponent renderer = center(this.patchRenderer);
			
			@Override
			public final Component getListCellRendererComponent(
					final JList<? extends Image> list, final Image value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				this.patchRenderer.setIcon(new ImageIcon(value));
				
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
							
							setPatchViewImage(new Patches.SubImage2D(
									image, (int) (this.tmp.x * imageScale), (int) (this.tmp.y * imageScale), size, size).toAwt());
							
							view.transform(this.tmp, this.tmp);
							
							graphics.setColor(Color.RED);
							graphics.drawRect(this.tmp.x, this.tmp.y, scaledPatchSize, scaledPatchSize);
						}
					} catch (final NoninvertibleTransformException exception) {
						exception.printStackTrace();
					}
				}
			}
			
			private static final long serialVersionUID = -6646527468467687096L;
			
		});
	}
	
	private static final long serialVersionUID = 2400491502237733629L;
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final Image2DComponent component = new Image2DComponent(IMJTools.read(commandLineArguments[0]));
		
		SwingTools.show(new VisualPatchExtractor(component), commandLineArguments[0], false);
	}
	
	public static final int patchStart(final int coordinate, final int patchStride, final int patchSize, final double imageScale) {
		return (int) (adjust(coordinate, (int) (patchStride / imageScale)) - patchSize / imageScale / 2.0);
	}
	
	public static final int adjust(final int coordinate, final int patchStride) {
		return coordinate / patchStride * patchStride + patchStride / 2;
	}
	
}
