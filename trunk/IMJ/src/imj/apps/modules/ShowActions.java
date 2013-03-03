package imj.apps.modules;

import static imj.apps.modules.BigImageComponent.drawOutline;
import static imj.apps.modules.Plugin.fireUpdate;
import static imj.apps.modules.Sieve.getROI;
import static java.lang.Integer.parseInt;
import static java.lang.Math.pow;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj.IntList;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.af.AFConstants;
import net.sourceforge.aprog.af.AFMainFrame;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-28)
 */
public final class ShowActions {
	
	private ShowActions() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_TOGGLE_HISTOGRAM = "actions.toggleHistogram";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_TOGGLE_ANNOTATIONS = "actions.toggleAnnotations";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_SET_VIEW_FILTER = "actions.setViewFilter";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_APPLY_SIEVE = "actions.applySieve";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_RESET_ROI = "actions.resetROI";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_COPY_ROI_TO_LOD = "actions.copyROIToLOD";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_APPLY_MORPHOLOGICAL_OPERATION_TO_ROI = "actions.applyMorphologicalOperationToROI";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_USE_ANNOTATION_AS_ROI = "actions.useAnnotationAsROI";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_PICK_ANNOTATION_COLOR = "actions.pickAnnotationColor";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_CREATE_ANNOTATION_FROM_ROI = "actions.createAnnotationFromROI";
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ToggleHistogram extends AbstractAFAction {
		
		public ToggleHistogram(final Context context) {
			super(context, ACTIONS_TOGGLE_HISTOGRAM);
		}
		
		@Override
		public final void perform() {
			JDialog dialog = this.getContext().get("histogramDialog");
			
			if (dialog == null) {
				final AFMainFrame mainFrame = this.getContext().get(AFConstants.Variables.MAIN_FRAME);
				
				dialog = new JDialog(mainFrame, "Histogram");
				
				dialog.add(scrollable(new HistogramPanel(this.getContext())));
				
				dialog.pack();
				
				this.getContext().set("histogramDialog", dialog);
			}
			
			dialog.setVisible(!dialog.isVisible());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ToggleAnnotations extends AbstractAFAction {
		
		public ToggleAnnotations(final Context context) {
			super(context, ACTIONS_TOGGLE_ANNOTATIONS);
		}
		
		@Override
		public final void perform() {
			JDialog dialog = this.getContext().get("annotationsDialog");
			
			if (dialog == null) {
				final AFMainFrame mainFrame = this.getContext().get(AFConstants.Variables.MAIN_FRAME);
				
				dialog = new JDialog(mainFrame, "Annotations");
				
				dialog.add(scrollable(new AnnotationsPanel(this.getContext())));
				
				dialog.addComponentListener(new ComponentAdapter() {
					
					@Override
					public final void componentShown(final ComponentEvent event) {
						fireUpdate(ToggleAnnotations.this.getContext(), "sieve");
					}
					
					@Override
					public final void componentHidden(final ComponentEvent event) {
						fireUpdate(ToggleAnnotations.this.getContext(), "sieve");
					}
					
				});
				
				dialog.pack();
				
				this.getContext().set("annotationsDialog", dialog);
			}
			
			dialog.setVisible(!dialog.isVisible());
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class SetViewFilter extends AbstractAFAction {
		
		public SetViewFilter(final Context context) {
			super(context, ACTIONS_SET_VIEW_FILTER);
		}
		
		@Override
		public final void perform() {
			final ViewFilter[] filters = this.getContext().get("viewFilters");
			final JList input = new JList(filters);
			final int option = JOptionPane.showConfirmDialog(null, scrollable(input), "Select a filter", OK_CANCEL_OPTION);
			
			if (option != JOptionPane.OK_OPTION) {
				return;
			}
			
			final ViewFilter filter = (ViewFilter) input.getSelectedValue();
			
			if (filter != null) {
				filter.configureAndApply();
			} else {
				this.getContext().set("viewFilter", null);
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ApplySieve extends AbstractAFAction {
		
		public ApplySieve(final Context context) {
			super(context, ACTIONS_APPLY_SIEVE);
		}
		
		@Override
		public final void perform() {
			final Sieve[] sieves = this.getContext().get("sieves");
			final JList input = new JList(sieves);
			final int option = showConfirmDialog(null, scrollable(input), "Select a sieve", OK_CANCEL_OPTION);
			
			if (option != OK_OPTION) {
				return;
			}
			
			final Sieve sieve = (Sieve) input.getSelectedValue();
			
			sieve.configureAndApply();
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ApplyMorphologicalOperationToROI extends AbstractAFAction {
		
		private final ROIMorphologyPlugin plugin;
		
		public ApplyMorphologicalOperationToROI(final Context context) {
			super(context, ACTIONS_APPLY_MORPHOLOGICAL_OPERATION_TO_ROI);
			this.plugin = new ROIMorphologyPlugin(context);
		}
		
		@Override
		public final void perform() {
			this.plugin.configureAndApply();
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ResetROI extends AbstractAFAction {
		
		public ResetROI(final Context context) {
			super(context, ACTIONS_RESET_ROI);
		}
		
		@Override
		public final void perform() {
			final RegionOfInterest[] rois = this.getContext().get("rois");
			final int lod = this.getContext().get("lod");
			final RegionOfInterest roi = lod < rois.length ? rois[lod] : null;
			
			if (roi != null) {
				roi.reset();
				
				final BigImageComponent imageView = this.getContext().get("imageView");
				
				imageView.repaintAll();
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class CopyROIToLOD extends AbstractAFAction {
		
		public CopyROIToLOD(final Context context) {
			super(context, ACTIONS_COPY_ROI_TO_LOD);
		}
		
		@Override
		public final void perform() {
			final String destinationLodAsString = JOptionPane.showInputDialog("LOD:");
			
			if (destinationLodAsString == null || destinationLodAsString.isEmpty()) {
				return;
			}
			
			final int sourceLod = this.getContext().get("lod");
			final int destinationLod = parseInt(destinationLodAsString);
			final RegionOfInterest[] rois = this.getContext().get("rois");
			
			rois[sourceLod].copyTo(rois[destinationLod]);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class UseAnnotationAsROI extends AbstractAFAction {
		
		public UseAnnotationAsROI(final Context context) {
			super(context, ACTIONS_USE_ANNOTATION_AS_ROI);
		}
		
		@Override
		public final void perform() {
			final TreePath[] selectedAnnotations = this.getContext().get("selectedAnnotations");
			
			if (selectedAnnotations == null) {
				return;
			}
			
			for (final TreePath path : selectedAnnotations) {
				final Annotation annotation = cast(Annotation.class, path.getLastPathComponent());
				
				if (annotation == null) {
					continue;
				}
				
				final RegionOfInterest roi = getROI(this.getContext());
				
				if (roi == null) {
					continue;
				}
				
				final int rowCount = roi.getRowCount();
				final int columnCount = roi.getColumnCount();
				final BufferedImage buffer = new BufferedImage(columnCount, rowCount, BufferedImage.TYPE_BYTE_BINARY);
				final Graphics2D g = buffer.createGraphics();
				final int scale = this.getContext().get("scale");
				final int lod = this.getContext().get("lod");
				final double s = scale * pow(2.0, -lod);
				
				g.scale(s, s);
				g.setStroke(new BasicStroke((float) (3.0 / s)));
				
				for (final Region region : annotation.getRegions()) {
					drawOutline(region, g);
				}
				
				for (int y = 0; y < rowCount; ++y) {
					for (int x = 0; x < columnCount; ++x) {
						roi.set(y, x, (buffer.getRGB(x, y) & 0x00FFFFFF) != 0);
					}
				}
				
				g.dispose();
				
				fillContours(roi);
				
				fireUpdate(this.getContext(), "sieve");
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class PickAnnotationColor extends AbstractAFAction {
		
		public PickAnnotationColor(final Context context) {
			super(context, ACTIONS_PICK_ANNOTATION_COLOR);
		}
		
		@Override
		public final void perform() {
			final TreePath[] selectedAnnotations = this.getContext().get("selectedAnnotations");
			
			if (selectedAnnotations == null) {
				return;
			}
			
			Color newColor = null;
			
			for (final TreePath path : selectedAnnotations) {
				final Annotation annotation = cast(Annotation.class, path.getLastPathComponent());
				
				if (annotation == null) {
					continue;
				}
				
				if (newColor == null) {
					newColor = JColorChooser.showDialog(null, "Pick a color", annotation.getLineColor());
				}
				
				if (newColor == null) {
					break;
				}
				
				annotation.setLineColor(newColor);
			}
			
			if (newColor != null) {
				fireUpdate(this.getContext(), "sieve");
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class CreateAnnotationFromROI extends AbstractAFAction {
		
		public CreateAnnotationFromROI(final Context context) {
			super(context, ACTIONS_CREATE_ANNOTATION_FROM_ROI);
		}
		
		@Override
		public final void perform() {
			final RegionOfInterest roi = getROI(this.getContext());
			
			if (roi == null) {
				return;
			}
			
			final Annotations annotations = this.getContext().get("annotations");
			final Annotation annotation = annotations.new Annotation();
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			final int lod = this.getContext().get("lod");
			final float scale = (float) pow(2.0, lod);
			final Map<Point2D.Float, Point2D.Float> joints = new LinkedHashMap<Point2D.Float, Point2D.Float>();
			
			for (int rowIndex = 0; rowIndex < rowCount - 1; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount - 1; ++columnIndex) {
					connectEdges(roi, rowIndex, columnIndex, scale, joints);
				}
			}
			
			while (!joints.isEmpty()) {
				final Region region = annotation.new Region();
				final Point2D.Float start = joints.keySet().iterator().next();
				Point2D.Float edge = joints.remove(start);
				
				region.getShape().add(start);
				
				while (edge != null && !start.equals(edge)) {
					region.getShape().add(edge);
					edge = joints.remove(edge);
				}
				
				if (edge != null) {
					region.getShape().add(start);
				}
			}
			
			fireUpdate(this.getContext(), "annotations");
		}
		
	}
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_01_11 = 0 | 4 | 2 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_11_01 = 8 | 4 | 0 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_11_10 = 8 | 4 | 2 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_10_11 = 8 | 0 | 2 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_11_00 = 8 | 4 | 0 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_01_01 = 0 | 4 | 0 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_00_11 = 0 | 0 | 2 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_10_10 = 8 | 0 | 2 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_01_10 = 0 | 4 | 2 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_10_01 = 8 | 0 | 0 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_10_00 = 8 | 0 | 0 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_00_10 = 0 | 0 | 2 | 0;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_00_01 = 0 | 0 | 0 | 1;
	
	/**
	 * {@value}.
	 */
	private static final int NEIGHBORHOOD_PATTERN_01_00 = 0 | 4 | 0 | 0;
	
	/**
	 * Updates <code>joints</code> so that foreground has 8-connectivity, using a 2x2 window with top left corner at
	 * <code>(rowIndex, columnIndex)</code>.
	 * @param image
	 * <br>Not null
	 * @param rowIndex
	 * <br>Range: <code>[0 .. image.getRowCount() - 1]</code>
	 * @param columnIndex
	 * <br>Range: <code>[0 .. image.getColumnCount() - 1]</code>
	 * @param scale
	 * <br>Range: <code>[Float.MIN_VALUE .. Float.MAX_VALUE]</code>
	 * @param joints
	 * <br>Not null
	 * <br>Input-output
	 */
	public static final void connectEdges(final RegionOfInterest image, final int rowIndex, final int columnIndex,
			final float scale, final Map<Point2D.Float, Point2D.Float> joints) {
		final int neighborhood = (image.get(rowIndex, columnIndex) ? 8 : 0) | (image.get(rowIndex, columnIndex + 1) ? 4 : 0) |
				(image.get(rowIndex + 1, columnIndex) ? 2 : 0) | (image.get(rowIndex + 1, columnIndex + 1) ? 1 : 0);
		
		switch (neighborhood) {
		case NEIGHBORHOOD_PATTERN_01_11:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)),
					new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_11_01:
			joints.put(new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_11_10:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)),
					new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_10_11:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_11_00:
			joints.put(new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_01_01:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_00_11:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_10_10:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_01_10:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)),
					new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)));
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)),
					new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_10_01:
			joints.put(new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)));
			joints.put(new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_10_00:
			joints.put(new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_00_10:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)),
					new Point2D.Float(scale * (columnIndex + 0.5F), scale * (rowIndex + 1.0F)));
			break;
		case NEIGHBORHOOD_PATTERN_00_01:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)),
					new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 1.5F)));
			break;
		case NEIGHBORHOOD_PATTERN_01_00:
			joints.put(new Point2D.Float(scale * (columnIndex + 1.0F), scale * (rowIndex + 0.5F)),
					new Point2D.Float(scale * (columnIndex + 1.5F), scale * (rowIndex + 1.0F)));
			break;
		default:
			break;
		}
	}
	
	public static final void fillContours(final RegionOfInterest roi) {
		final int rowCount = roi.getRowCount();
		final int columnCount = roi.getColumnCount();
		final int lastRowIndex = rowCount - 1;
		final int lastColumnIndex = columnCount - 1;
		final RegionOfInterest done = new RegionOfInterest.UsingBitSet(rowCount, columnCount, false);
		final IntList todo = new IntList();
		
		for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
			todo.add(roi.getIndex(rowIndex, 0));
			todo.add(roi.getIndex(rowIndex, lastColumnIndex));
		}
		
		for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
			todo.add(roi.getIndex(0, columnIndex));
			todo.add(roi.getIndex(lastRowIndex, columnIndex));
		}
		
		while (!todo.isEmpty()) {
			final int pixel = todo.remove(0);
			
			if (!roi.get(pixel)) {
				roi.set(pixel);
				
				final int rowIndex = roi.getRowIndex(pixel);
				final int columnIndex = roi.getColumnIndex(pixel);
				
				if (0 < rowIndex) {
					final int neighbor = roi.getIndex(rowIndex - 1, columnIndex);
					
					if (!done.get(neighbor)) {
						done.set(neighbor);
						todo.add(neighbor);
					}
				}
				
				if (0 < columnIndex) {
					final int neighbor = roi.getIndex(rowIndex, columnIndex - 1);
					
					if (!done.get(neighbor)) {
						done.set(neighbor);
						todo.add(neighbor);
					}
				}
				
				if (columnIndex < lastColumnIndex) {
					final int neighbor = roi.getIndex(rowIndex, columnIndex + 1);
					
					if (!done.get(neighbor)) {
						done.set(neighbor);
						todo.add(neighbor);
					}
				}
				
				if (rowIndex < lastRowIndex) {
					final int neighbor = roi.getIndex(rowIndex + 1, columnIndex);
					
					if (!done.get(neighbor)) {
						done.set(neighbor);
						todo.add(neighbor);
					}
				}
			}
		}
		
		roi.invert();
	}
	
}
