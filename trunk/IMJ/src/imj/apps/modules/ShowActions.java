package imj.apps.modules;

import static imj.apps.modules.BigImageComponent.drawRegionOutline;
import static imj.apps.modules.ShowActions.EdgeNeighborhood.computeNeighborhood;
import static imj.apps.modules.Sieve.getROI;
import static java.awt.Color.GREEN;
import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.util.Collections.sort;
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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
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
	
	public static final String baseName(final String fileName) {
		final int lastDotIndex = fileName.lastIndexOf('.');
		
		return lastDotIndex < 0 ? fileName : fileName.substring(0, lastDotIndex);
	}
	
	public static final String attribute(final String name, final Object value) {
		return " " + name + "=\"" + value + "\"";
	}
	
	public static final JList getList(final Object eventSource) {
		final JList list = cast(JList.class, eventSource);
		
		if (list != null) {
			return list;
		}
		
		final JPopupMenu popup = cast(JPopupMenu.class, eventSource);
		
		if (popup != null) {
			return (JList) getAncestorOfClass(JList.class, popup.getInvoker());
		}
		
		return null;
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
//			final JPopupMenu popup = (JPopupMenu) getAncestorOfClass(JPopupMenu.class,
//					(Component) ((ActionEvent) event).getSource());
//			final JList list = (JList) popup.getInvoker();
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
				final int lod = this.getContext().get("lod");
				final double s = pow(2.0, -lod);
				
				g.scale(s, s);
				g.setStroke(new BasicStroke((float) (3.0 / s)));
				
				boolean regionsAreClosed = true;
				
				for (final Region region : annotation.getRegions()) {
					final List<Float> shape = region.getVertices();
					
					if (!shape.isEmpty() && region.getLength() / 3 < shape.get(0).distance(shape.get(shape.size() - 1))) {
						regionsAreClosed = false;
						break;
					}
				}
				
				if (regionsAreClosed) {
					final List<Region> sortedRegions = new ArrayList<Region>(annotation.getRegions());
					
					sort(sortedRegions, DECREASING_AREA);
					
					final int pixelCount = roi.getPixelCount();
					final RegionOfInterest tmp = new RegionOfInterest.UsingBitSet(rowCount, columnCount, false);
					
					roi.reset();
					roi.invert();
					
					for (final Region region : sortedRegions) {
						g.setColor(Color.BLACK);
						g.fillRect(0, 0, (int) (columnCount / s), (int) (rowCount / s));
						g.setColor(Color.WHITE);
						
						drawRegionOutline(region, g);
						
						tmp.reset();
						tmp.invert();
						
						for (int y = 0; y < rowCount; ++y) {
							for (int x = 0; x < columnCount; ++x) {
								if ((buffer.getRGB(x, y) & 0x00FFFFFF) != 0) {
									tmp.set(y, x);
								}
							}
						}
						
						fillContours(tmp);
						
						for (int pixel = 0; pixel < pixelCount; ++pixel) {
							if (tmp.get(pixel)) {
								roi.set(pixel, !region.isNegative());
							}
						}
					}
				} else {
					debugPrint("Result may be incorrect because some regions are not closed");
					
					for (final Region region : annotation.getRegions()) {
						if (region.isNegative()) {
							drawRegionOutline(region, g);
						}
					}
					
					for (int y = 0; y < rowCount; ++y) {
						for (int x = 0; x < columnCount; ++x) {
							roi.set(y, x, (buffer.getRGB(x, y) & 0x00FFFFFF) != 0);
						}
					}
					
					fillContours(roi);
					
					for (final Region region : annotation.getRegions()) {
						if (!region.isNegative()) {
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
				
				g.dispose();
				
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
			
			for (int rowIndex = 0; rowIndex < rowCount - 1; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount - 1; ++columnIndex) {
					connectEdges(roi, rowIndex, columnIndex, scale, joints);
				}
			}
			
			final Annotations annotations = this.getContext().get("annotations");
			final Annotation annotation = annotations.new Annotation();
			
			annotation.setLineColor(GREEN);
			annotation.setVisible(true);
			
			while (!joints.isEmpty()) {
				final Region region = annotation.new Region();
				final Point2D.Float start = joints.keySet().iterator().next();
				float area = 0F;
				float length = 0F;
				Point2D.Float edge = joints.remove(start);
				
				region.getVertices().add(start);
				Point2D.Float previousEdge = start;
				boolean previousEdgeIsHorizontal = isEdgeHorizontal(start.x / scale, start.y / scale);
				int spin = 0;
				int segmentLength = 0;
				final int maximumSegmentLength = 5;
				
				while (edge != null && !start.equals(edge)) {
					final boolean edgeIsHorizontal = isEdgeHorizontal(edge.x / scale, edge.y / scale);
					
					if (previousEdgeIsHorizontal && !edgeIsHorizontal) {
						spin += (previousEdge.x < edge.x) == (edge.y < previousEdge.y) ? -1 : +1;
					} else if (!previousEdgeIsHorizontal && edgeIsHorizontal) {
						spin += (previousEdge.x < edge.x) == (previousEdge.y < edge.y) ? -1 : +1;
					}
					
					if (segmentLength < 1) {
						region.getVertices().add(edge);
						++segmentLength;
					} else if (maximumSegmentLength < segmentLength) {
						region.getVertices().add(edge);
						segmentLength = 1;
					} else {
						region.getVertices().set(region.getVertices().size() - 1, edge);
						++segmentLength;
					}
					area += det(previousEdge, edge);
					length += scale;
					previousEdge = edge;
					previousEdgeIsHorizontal = edgeIsHorizontal;
					edge = joints.remove(edge);
				}
				
				if (edge != null) {
					edge = start;
					final boolean edgeIsHorizontal = isEdgeHorizontal(edge.x / scale, edge.y / scale);
					
					if (previousEdgeIsHorizontal && !edgeIsHorizontal) {
						spin += (previousEdge.x < edge.x) == (edge.y < previousEdge.y) ? -1 : +1;
					} else if (!previousEdgeIsHorizontal && edgeIsHorizontal) {
						spin += (previousEdge.x < edge.x) == (previousEdge.y < edge.y) ? -1 : +1;
					}
					
					if (segmentLength < 1 || maximumSegmentLength < segmentLength) {
						region.getVertices().add(edge);
					} else {
						region.getVertices().set(region.getVertices().size() - 1, edge);
					}
					
					area += det(previousEdge, edge);
				}
				
				if ((abs(spin) % 4) != 0) {
					debugPrint("Possible defect detected");
					debugPrint("spin:", spin);
				}
				
				region.setNegative(spin < 0);
				region.setArea(abs(area));
				region.setAreaInSquareMicrons(region.getArea() * square(annotations.getMicronsPerPixel()));
				region.setLength(length);
				region.setLengthInMicrons(region.getLength() * annotations.getMicronsPerPixel());
			}
			
			fireUpdate(this.getContext(), "annotations");
		}
		
	}
	
	public static final float det(final Point2D.Float v1, final Point2D.Float v2) {
		return v1.x * v2.y - v1.y * v2.x;
	}
	
	public static final boolean isEdgeHorizontal(final float edgeX, final float edgeY) {
		return round(edgeX + 0.1F) == (int) floor(edgeX);
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
			return (image.get(rowIndex, columnIndex) ? 8 : 0) | (image.get(rowIndex, columnIndex + 1) ? 4 : 0) |
					(image.get(rowIndex + 1, columnIndex) ? 2 : 0) | (image.get(rowIndex + 1, columnIndex + 1) ? 1 : 0);
		}
		
		public static final void addJoint(final int x, final int y,
				final float dx1, final float dy1, final float dx2, final float dy2,
				final float scale, final Map<Point2D.Float, Point2D.Float> joints) {
			joints.put(new Point2D.Float(scale * (x + dx1), scale * (y + dy1)),
					new Point2D.Float(scale * (x + dx2), scale * (y + dy2)));
		}
		
	}
	
}
