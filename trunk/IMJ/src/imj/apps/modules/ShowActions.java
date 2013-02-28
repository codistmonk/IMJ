package imj.apps.modules;

import static imj.apps.modules.Plugin.fireUpdate;
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
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

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
				
				final RegionOfInterest roi = Sieve.getROI(this.getContext());
				
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
					final Polygon shape = (Polygon) region.getShape();
					
					g.drawPolyline(shape.xpoints, shape.ypoints, shape.npoints);
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
