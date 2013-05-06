package imj.apps.modules;

import static imj.apps.modules.BigImageComponent.drawRegionOutline;
import static imj.apps.modules.BigImageComponent.fillRegion;
import static imj.apps.modules.ShowActions.EdgeNeighborhood.computeNeighborhood;
import static imj.apps.modules.Sieve.getROI;
import static imj.apps.modules.ViewFilter.VIEW_FILTER;
import static java.awt.Color.GREEN;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Collections.sort;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.SwingUtilities.getAncestorOfClass;
import static net.sourceforge.aprog.af.AFTools.fireUpdate;
import static net.sourceforge.aprog.i18n.Messages.translate;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.MathTools.Statistics.square;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.list;
import imj.Image;
import imj.IntList;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.af.AFConstants;
import net.sourceforge.aprog.af.AFMainFrame;
import net.sourceforge.aprog.af.AbstractAFAction;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.i18n.Translator;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
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
	public static final String ACTIONS_EXPORT_VIEW = "actions.exportView";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_MOVE_LIST_ITEM_UP = "actions.moveListItemUp";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_MOVE_LIST_ITEM_DOWN = "actions.moveListItemDown";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_DELETE_LIST_ITEM = "actions.deleteListItem";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_EXPORT_ANNOTATIONS = "actions.exportAnnotations";
	
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
	public static final String ACTIONS_TOGGLE_ANNOTATION_VISIBILITY = "actions.toggleAnnotationVisibility";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_DELETE_ANNOTATION = "actions.deleteAnnotation";
	
	/**
	 * {@value}.
	 */
	public static final String ACTIONS_CREATE_ANNOTATION_FROM_ROI = "actions.createAnnotationFromROI";
	
	public static final void loadInto(final Context context) {
		for (final Class<?> cls : ShowActions.class.getClasses()) {
			if (!isAbstract(cls.getModifiers()) && AbstractAFAction.class.isAssignableFrom(cls)) {
				try {
					cls.getConstructor(Context.class).newInstance(context);
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			}
		}
	}
	
	public static final String extension(final String fileName) {
		final int lastDotIndex = fileName.lastIndexOf('.');
		
		return lastDotIndex < 0 ? "" : fileName.substring(lastDotIndex + 1);
	}
	
	public static final String baseName(final String fileName) {
		final int lastDotIndex = fileName.lastIndexOf('.');
		
		return lastDotIndex < 0 ? fileName : fileName.substring(0, lastDotIndex);
	}
	
	public static final String attribute(final String name, final Object value) {
		return " " + name + "=\"" + value + "\"";
	}
	
	public static final JList getList(final Object eventSource) {
		{
			final JList list = cast(JList.class, eventSource);
			
			if (list != null) {
				return list;
			}
		}
		
		JPopupMenu popup = cast(JPopupMenu.class, eventSource);
		
		if (popup == null) {
			final JMenuItem menu = cast(JMenuItem.class, eventSource);
			
			if (menu != null) {
				popup = (JPopupMenu) getAncestorOfClass(JPopupMenu.class, menu);
			}
		}
		
		if (popup != null) {
			final JList list = cast(JList.class, popup.getInvoker());
			
			if (list != null) {
				return list;
			}
			
			return (JList) getAncestorOfClass(JList.class, popup.getInvoker());
		}
		
		return null;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ExportView extends AbstractAFAction {
		
		public ExportView(final Context context) {
			super(context, ACTIONS_EXPORT_VIEW);
		}
		
		@Override
		public final void perform(final Object event) {
			final JFileChooser fileChooser = new JFileChooser();
			
			if (APPROVE_OPTION != fileChooser.showSaveDialog(null)) {
				return;
			}
			
			final Image source = ViewFilter.getCurrentImage(this.getContext());
			final BufferedImage destination = toBufferedImage(source, Sieve.getROI(this.getContext()));
			final Graphics2D g = destination.createGraphics();
			
			BigImageComponent.drawAnnotations(this.getContext(), g);
			
			g.dispose();
			
			final File file = fileChooser.getSelectedFile();
			String format = extension(file.getName());
			
			if ("".equals(format)) {
				format = "png";
			}
			
			try {
				ImageIO.write(destination, extension(file.getName()), file);
			} catch (final IOException exception) {
				exception.printStackTrace();
			}
		}
		
		public static final BufferedImage toBufferedImage(final Image source, final RegionOfInterest roi) {
			final int rowCount = source.getRowCount();
			final int columnCount = source.getColumnCount();
			final BufferedImage destination = new BufferedImage(columnCount, rowCount, TYPE_3BYTE_BGR);
			
			for (int y = 0, pixel = 0; y < rowCount; ++y) {
				for (int x = 0; x < columnCount; ++x, ++pixel) {
					if (roi == null || roi.get(pixel)) {
						destination.setRGB(x, y, source.getValue(pixel));
					}
				}
			}
			
			return destination;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class MoveListItemUp extends AbstractAFAction {
		
		public MoveListItemUp(final Context context) {
			super(context, ACTIONS_MOVE_LIST_ITEM_UP);
		}
		
		@Override
		public final void perform(final Object event) {
			final JList list = getList(((AWTEvent) event).getSource());
			final int index = list.getSelectedIndex();
			
			if (0 < index) {
				final DefaultListModel model = (DefaultListModel) list.getModel();
				final Object item = model.remove(index);
				
				model.add(index - 1, item);
				list.setSelectedValue(item, true);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class MoveListItemDown extends AbstractAFAction {
		
		public MoveListItemDown(final Context context) {
			super(context, ACTIONS_MOVE_LIST_ITEM_DOWN);
		}
		
		@Override
		public final void perform(final Object event) {
			final JList list = getList(((AWTEvent) event).getSource());
			final int index = list.getSelectedIndex();
			
			if (index < list.getModel().getSize()) {
				final DefaultListModel model = (DefaultListModel) list.getModel();
				final Object item = model.remove(index);
				
				model.add(index + 1, item);
				list.setSelectedValue(item, true);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class DeleteListItem extends AbstractAFAction {
		
		public DeleteListItem(final Context context) {
			super(context, ACTIONS_DELETE_LIST_ITEM);
		}
		
		@Override
		public final void perform(final Object event) {
			final JList list = getList(((AWTEvent) event).getSource());
			final int index = list.getSelectedIndex();
			
			if (0 <= index && askUserToConfirmElementDeletion()) {
				((DefaultListModel) list.getModel()).remove(index);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ExportAnnotations extends AbstractAFAction {
		
		public ExportAnnotations(final Context context) {
			super(context, ACTIONS_EXPORT_ANNOTATIONS);
		}
		
		@Override
		public final void perform(final Object object) {
			final String imageId = this.getContext().get("imageId");
			final JFileChooser fileChooser = new JFileChooser(imageId);
			
			fileChooser.setSelectedFile(new File(baseName(imageId) + ".xml"));
			
			if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(null)) {
				final Annotations annotations = this.getContext().get("annotations");
				final File xmlFile = fileChooser.getSelectedFile();
				PrintStream out = null;
				
				try {
					out = new PrintStream(xmlFile);
					
					Annotations.toXML(annotations, out);
				} catch (final FileNotFoundException exception) {
					exception.printStackTrace();
				} finally {
					if (out != null) {
						out.close();
					}
				}
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class ToggleHistogram extends AbstractAFAction {
		
		public ToggleHistogram(final Context context) {
			super(context, ACTIONS_TOGGLE_HISTOGRAM);
		}
		
		@Override
		public final void perform(final Object object) {
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
		public final void perform(final Object object) {
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
		public final void perform(final Object object) {
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
				this.getContext().set(VIEW_FILTER, null);
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
		public final void perform(final Object object) {
			final Sieve[] sieves = this.getContext().get("sieves");
			final JList input = new JList(sieves);
			final int option = showConfirmDialog(null, scrollable(input), "Select a sieve", OK_CANCEL_OPTION);
			
			if (option != OK_OPTION) {
				return;
			}
			
			final Sieve sieve = (Sieve) input.getSelectedValue();
			
			if (sieve != null) {
				sieve.configureAndApply();
			}
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
		public final void perform(final Object object) {
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
		public final void perform(final Object object) {
			final RegionOfInterest[] rois = this.getContext().get("rois");
			final int lod = this.getContext().get("lod");
			final RegionOfInterest roi = lod < rois.length ? rois[lod] : null;
			
			if (roi != null) {
				roi.reset(true);
				
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
		public final void perform(final Object object) {
			final String lods = JOptionPane.showInputDialog("LOD:");
			
			if (lods == null || lods.isEmpty()) {
				return;
			}
			
			final int sourceLod = this.getContext().get("lod");
			final CommandLineArgumentsParser parser = new CommandLineArgumentsParser("lods", lods);
			final RegionOfInterest[] rois = this.getContext().get("rois");
			
			for (final int destinationLod : parser.get("lods")) {
				rois[sourceLod].copyTo(rois[destinationLod]);
			}
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
		public final void perform(final Object object) {
			final RegionOfInterest roi = getROI(this.getContext());
			
			if (roi == null) {
				return;
			}
			
			final TreePath[] selectedAnnotations = this.getContext().get("selectedAnnotations");
			
			if (selectedAnnotations == null) {
				return;
			}
			
			final Annotations annotations = this.getContext().get("annotations");
			final int lod = this.getContext().get("lod");
			
			set(roi, lod, extractRegions(selectedAnnotations), collectAllRegions(annotations));
			
			fireUpdate(this.getContext(), "sieve");
		}
		
		public static final Collection<Region> collectAllRegions(final Annotations annotations) {
			final Collection<Region> result = new LinkedHashSet<Region>();
			
			for (final Annotation annotation : annotations.getAnnotations()) {
				result.addAll(annotation.getRegions());
			}
			
			return result;
		}
		
		public static final void set(final RegionOfInterest roi, final int lod,
				final Collection<Region> selectedRegions, final Collection<Region> allRegions) {
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			final BufferedImage buffer = new BufferedImage(columnCount, rowCount, BufferedImage.TYPE_BYTE_BINARY);
			final Graphics2D g = buffer.createGraphics();
			final double s = pow(2.0, -lod);
			final Collection<Region> regionsToExclude = new LinkedHashSet<Region>(allRegions);
			
			regionsToExclude.removeAll(selectedRegions);
			
			g.scale(s, s);
			g.setStroke(new BasicStroke((float) (3.0 / s)));
			
			if (regionsAreClosed(selectedRegions)) {
//				fillClosedRegions(roi, selectedRegions, buffer, g, s);
				fillClosedRegions(roi, allRegions, regionsToExclude, buffer, g, s);
			} else {
				debugPrint("Result may be incorrect because some regions are not closed");
				
				fillUnclosedRegions(roi, allRegions, regionsToExclude, buffer, g);
			}
			
			g.dispose();
		}
		
		private static final boolean regionsAreClosed(final Iterable<Region> selectedRegions) {
			boolean regionsAreClosed = true;
			
			for (final Region region : selectedRegions) {
				final List<Float> shape = region.getVertices();
				
				if (!shape.isEmpty() && region.getLength() / 3 < shape.get(0).distance(shape.get(shape.size() - 1))) {
					regionsAreClosed = false;
					break;
				}
			}
			return regionsAreClosed;
		}
		
		private static final Collection<Region> extractRegions(final TreePath[] selectedAnnotations) {
			final Collection<Region> selectedRegions = new LinkedHashSet<Region>();
			
			for (final TreePath path : selectedAnnotations) {
				final Annotation annotation = cast(Annotation.class, path.getLastPathComponent());
				
				if (annotation != null) {
					selectedRegions.addAll(annotation.getRegions());
				}
				
				final Region region = cast(Region.class, path.getLastPathComponent());
				
				if (region != null) {
					selectedRegions.add(region);
				}
			}
			return selectedRegions;
		}
		
		private static final void fillUnclosedRegions(final RegionOfInterest roi,
				final Iterable<Region> allRegions, final Collection<Region> regionsToExclude, final BufferedImage buffer,
				final Graphics2D g) {
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			
			for (final Region region : allRegions) {
				if (region.isNegative() != regionsToExclude.contains(region)) {
					drawRegionOutline(region, g);
				}
			}
			
			for (int y = 0; y < rowCount; ++y) {
				for (int x = 0; x < columnCount; ++x) {
					roi.set(y, x, (buffer.getRGB(x, y) & 0x00FFFFFF) != 0);
				}
			}
			
			fillContours(roi);
			
			for (final Region region : allRegions) {
				if (!region.isNegative() && !regionsToExclude.contains(region)) {
					drawRegionOutline(region, g);
				}
			}
			
			for (int y = 0; y < rowCount; ++y) {
				for (int x = 0; x < columnCount; ++x) {
					if ((buffer.getRGB(x, y) & 0x00FFFFFF) != 0) {
						roi.set(y, x);
					}
				}
			}
			
			fillContours(roi);
		}

		private static final void fillClosedRegions(final RegionOfInterest roi,
				final Iterable<Region> allRegions, final Collection<Region> regionsToExclude, final BufferedImage buffer,
				final Graphics2D g, final double s) {
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			final List<Region> sortedRegions = list(allRegions);
			
			sort(sortedRegions, DECREASING_AREA);
			
			roi.reset(false);
			
			for (final Region region : sortedRegions) {
				final boolean regionIsNegative = region.isNegative();
				final boolean regionIsExcluded = regionsToExclude.contains(region);
				
				if (regionIsNegative && regionIsExcluded) {
					continue;
				}
				
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, (int) (columnCount / s), (int) (rowCount / s));
				g.setColor(Color.WHITE);
				
				fillRegion(region, g);
				
				for (int y = 0; y < rowCount; ++y) {
					for (int x = 0; x < columnCount; ++x) {
						if ((buffer.getRGB(x, y) & 0x00FFFFFF) != 0) {
							roi.set(y, x, !regionIsNegative && !regionIsExcluded);
						}
					}
				}
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
		public final void perform(final Object object) {
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
	public static final class ToggleAnnotationVisibility extends AbstractAFAction {
		
		public ToggleAnnotationVisibility(final Context context) {
			super(context, ACTIONS_TOGGLE_ANNOTATION_VISIBILITY);
		}
		
		@Override
		public final void perform(final Object object) {
			final TreePath[] selectedAnnotations = this.getContext().get("selectedAnnotations");
			
			if (selectedAnnotations == null) {
				return;
			}
			
			for (final TreePath path : selectedAnnotations) {
				final Annotation annotation = cast(Annotation.class, path.getLastPathComponent());
				
				if (annotation == null) {
					continue;
				}
				
				annotation.setVisible(!annotation.isVisible());
			}
			
			fireUpdate(this.getContext(), "sieve");
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class DeleteAnnotation extends AbstractAFAction {
		
		public DeleteAnnotation(final Context context) {
			super(context, ACTIONS_DELETE_ANNOTATION);
		}
		
		@Override
		public final void perform(final Object object) {
			final TreePath[] selectedAnnotations = this.getContext().get("selectedAnnotations");
			
			if (selectedAnnotations == null) {
				return;
			}
			
			boolean confirmed = false;
			
			for (final TreePath path : selectedAnnotations) {
				final DefaultMutableTreeNode element = cast(DefaultMutableTreeNode.class, path.getLastPathComponent());
				final Annotation annotation = cast(Annotation.class, element);
				final Region region = cast(Region.class, element);
				
				if (annotation == null && region == null) {
					continue;
				}
				
				if (!confirmed) {
					confirmed = askUserToConfirmElementDeletion();
					
					if (!confirmed) {
						return;
					}
				}
				
				((DefaultMutableTreeNode) element.getParent()).remove(element);
			}
			
			fireUpdate(this.getContext(), "annotations");
		}
		
	}
	
	public static final boolean askUserToConfirmElementDeletion() {
		return askUserToConfirm("Delete selected elements?");
	}
	
	public static final boolean askUserToConfirm(final String messageTranslationKey) {
		JOptionPane.setDefaultLocale(Translator.getDefaultTranslator().getLocale());
		
		return JOptionPane.OK_OPTION == showConfirmDialog(null, translate(messageTranslationKey));
	}
	
	/**
	 * @author codistmonk (creation 2013-02-28)
	 */
	public static final class CreateAnnotationFromROI extends AbstractAFAction {
		
		public CreateAnnotationFromROI(final Context context) {
			super(context, ACTIONS_CREATE_ANNOTATION_FROM_ROI);
		}
		
		@Override
		public final void perform(final Object object) {
			final RegionOfInterest roi = getROI(this.getContext());
			
			if (roi == null) {
				return;
			}
			
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			final int lod = this.getContext().get("lod");
			final float scale = (float) pow(2.0, lod);
			final Map<Point2D.Float, Point2D.Float> joints = new LinkedHashMap<Point2D.Float, Point2D.Float>();
			
			for (int rowIndex = -1; rowIndex < rowCount; ++rowIndex) {
				for (int columnIndex = -1; columnIndex < columnCount; ++columnIndex) {
					connectEdges(roi, rowIndex, columnIndex, scale, joints);
				}
			}
			
			final Annotations annotations = this.getContext().get("annotations");
			final Annotation annotation = annotations.new Annotation();
			final List<Point2D.Float> vertices = new ArrayList<Point2D.Float>();
			
			annotation.setLineColor(GREEN);
			annotation.setVisible(true);
			
			while (!joints.isEmpty()) {
				final Region region = annotation.new Region();
				final Point2D.Float start = joints.keySet().iterator().next();
				float area = 0F;
				float length = 0F;
				Point2D.Float edge = joints.remove(start);
				Point2D.Float previousEdge = start;
				
				vertices.clear();
				vertices.add(start);
				
				while (edge != null && !start.equals(edge)) {
					vertices.add(edge);
					
					area += det(edge, previousEdge);
					length += scale;
					previousEdge = edge;
					edge = joints.remove(edge);
				}
				
				if (edge != null) {
					edge = start;
					
					vertices.add(edge);
					
					area += det(edge, previousEdge);
				}
				
				final int preferredSegmentLength = 5;
				
				if (3 * preferredSegmentLength <= vertices.size()) {
					int i = 0;
					
					for (final Point2D.Float vertex : vertices) {
						if (i == 0) {
							region.getVertices().add(vertex);
						}
						
						if (++i == preferredSegmentLength) {
							i = 0;
						}
					}
					
					region.getVertices().add(start);
				} else {
					region.getVertices().addAll(vertices);
				}
				
				region.setNegative(area < 0);
				region.setArea(abs(area));
				region.setAreaInSquareMicrons(region.getArea() * square(annotations.getMicronsPerPixel()));
				region.setLength(length);
				region.setLengthInMicrons(region.getLength() * annotations.getMicronsPerPixel());
			}
			
			fireUpdate(this.getContext(), "annotations");
			fireUpdate(this.getContext(), "sieve");
		}
		
	}
	
	public static final float det(final Point2D.Float v1, final Point2D.Float v2) {
		return v1.x * v2.y - v1.y * v2.x;
	}
	
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
		EdgeNeighborhood.values()[computeNeighborhood(image, rowIndex, columnIndex)]
				.updateJoints(columnIndex, rowIndex, scale, joints);
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
	
	public static final Comparator<Region> INCREASING_AREA = new Comparator<Region>() {
		
		@Override
		public final int compare(final Region r1, final Region r2) {
			return Double.compare(r1.getArea(), r2.getArea());
		}
		
	};
	
	public static final Comparator<Region> DECREASING_AREA = new Comparator<Region>() {
		
		@Override
		public final int compare(final Region r1, final Region r2) {
			return Double.compare(r2.getArea(), r1.getArea());
		}
		
	};
	
	/**
	 * @author codistmonk (creation 2013-03-04)
	 */
	public static enum EdgeNeighborhood {
		
		PATTERN_00_00 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				// NOP
			}
			
		}, PATTERN_00_01 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.5F, +1.0F, +1.0F, +1.5F, scale, joints);
			}
			
		}, PATTERN_00_10 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +1.5F, +0.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_00_11 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.5F, +1.0F, +0.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_01_00 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +0.5F, +1.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_01_01 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +0.5F, +1.0F, +1.5F, scale, joints);
			}
			
		}, PATTERN_01_10 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +0.5F, +0.5F, +1.0F, scale, joints);
				EdgeNeighborhood.addJoint(x, y, +1.0F, +1.5F, +1.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_01_11 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +0.5F, +0.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_10_00 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +0.5F, +1.0F, +1.0F, +0.5F, scale, joints);
			}
			
		}, PATTERN_10_01 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +0.5F, +1.0F, +1.0F, +1.5F, scale, joints);
				EdgeNeighborhood.addJoint(x, y, +1.5F, +1.0F, +1.0F, +0.5F, scale, joints);
			}
			
		}, PATTERN_10_10 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +1.5F, +1.0F, +0.5F, scale, joints);
			}
			
		}, PATTERN_10_11 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.5F, +1.0F, +1.0F, +0.5F, scale, joints);
			}
			
		}, PATTERN_11_00 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +0.5F, +1.0F, +1.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_11_01 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +0.5F, +1.0F, +1.0F, +1.5F, scale, joints);
			}
			
		}, PATTERN_11_10 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				EdgeNeighborhood.addJoint(x, y, +1.0F, +1.5F, +1.5F, +1.0F, scale, joints);
			}
			
		}, PATTERN_11_11 {
			
			@Override
			public final void updateJoints(final int x, final int y, final float scale, final Map<Float, Float> joints) {
				// NOP
			}
			
		};
		
		public abstract void updateJoints(int x, int y, float scale, Map<Point2D.Float, Point2D.Float> joints);
		
		public static final int computeNeighborhood(final RegionOfInterest image, final int rowIndex, final int columnIndex) {
			final int nextRowIndex = rowIndex + 1;
			final int nextColumnIndex = columnIndex + 1;
			final boolean rowIndexIsValid = 0 <= rowIndex;
			final boolean nextRowIndexIsValid = nextRowIndex < image.getRowCount();
			final boolean columnIndexIsValid = 0 <= columnIndex;
			final boolean nextColumnIndexIsValid = nextColumnIndex < image.getColumnCount();
			
			return (rowIndexIsValid && columnIndexIsValid && image.get(rowIndex, columnIndex) ? 8 : 0) |
					(rowIndexIsValid && nextColumnIndexIsValid && image.get(rowIndex, nextColumnIndex) ? 4 : 0) |
					(nextRowIndexIsValid && columnIndexIsValid && image.get(nextRowIndex, columnIndex) ? 2 : 0) |
					(nextRowIndexIsValid && nextColumnIndexIsValid && image.get(nextRowIndex, nextColumnIndex) ? 1 : 0);
		}
		
		public static final void addJoint(final int x, final int y,
				final float dx1, final float dy1, final float dx2, final float dy2,
				final float scale, final Map<Point2D.Float, Point2D.Float> joints) {
			joints.put(new Point2D.Float(scale * (x + dx1), scale * (y + dy1)),
					new Point2D.Float(scale * (x + dx2), scale * (y + dy2)));
		}
		
	}
	
}
