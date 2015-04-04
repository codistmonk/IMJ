package imj3.processing;

import static imj3.tools.CommonSwingTools.limitHeight;
import static imj3.tools.CommonSwingTools.setModel;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.append;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.join;

import de.schlichtherle.truezip.fs.FsEntryNotFoundException;

import imj3.core.Image2D;
import imj3.processing.Pipeline.Algorithm;
import imj3.processing.Pipeline.ClassDescription;
import imj3.processing.Pipeline.ComputationStatus;
import imj3.processing.Pipeline.SupervisedAlgorithm;
import imj3.processing.Pipeline.TrainingField;
import imj3.processing.Pipeline.UnsupervisedAlgorithm;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;
import imj3.tools.MultiThreadTools;
import imj3.tools.MultifileImage2D;
import imj3.tools.MultifileSource;
import imj3.tools.SVS2Multifile;
import imj3.tools.XMLSerializable;
import imj3.tools.CommonSwingTools.Instantiator;
import imj3.tools.CommonSwingTools.HighlightComposite;
import imj3.tools.CommonSwingTools.UserObject;
import imj3.tools.CommonTools;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.MouseHandler;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Canvas;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2015-02-13)
 */
public final class VisualAnalysis {
	
	private VisualAnalysis() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(VisualAnalysis.class);
	
	public static final String IMAGE_PATH = "image.path";
	
	public static final String GROUND_TRUTH = "groundtruth";
	
	public static final String PIPELINE = "pipeline";
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingTools.useSystemLookAndFeel();
		
		final Context context = new Context();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				SwingTools.show(new MainPanel(context), VisualAnalysis.class.getSimpleName(), false);
				
				context.setImage(new File(preferences.get(IMAGE_PATH, "")));
				context.setGroundTruth(preferences.get(GROUND_TRUTH, "gt"));
				context.setPipeline(new File(preferences.get(PIPELINE, "pipeline.xml")));
			}
			
		});
	}
	
	public static final Component label(final String text, final Component... components) {
		return horizontalBox(append(array((Component) new JLabel(text)), components));
	}
	
	public static final <C extends JComponent> C centerX(final C component) {
		component.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		return component;
	}
	
	public static final <C extends JComponent> C left(final C component) {
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		return component;
	}
	
	public static final JTextField textView(final String text) {
		final JTextField result = new JTextField(text);
		
		result.setEditable(false);
		
		return result;
	}
	
	public static final JButton button(final String type) {
		final JButton result = new JButton(buttonIcon(type));
		final int size = max(result.getIcon().getIconWidth(), result.getIcon().getIconHeight());
		
		result.setPreferredSize(new Dimension(size + 2, size + 2));
		
		return result;
	}
	
	public static final ImageIcon buttonIcon(final String type) {
		return new ImageIcon(Tools.getResourceURL("lib/tango/" + type + ".png"));
	}
	
	/**
	 * @author codistmonk (creation 2015-02-19)
	 */
	public static final class FileSelector extends JButton {
		
		private final List<File> files = new ArrayList<>();
		
		private File selectedFile;
		
		private ActionListener fileListener;
		
		{
			this.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					FileSelector.this.showPopup();
				}
				
			});
			
			this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
			
			this.setHorizontalAlignment(SwingConstants.LEFT);
		}
		
		public final ActionListener getFileListener() {
			return this.fileListener;
		}
		
		public final void setFileListener(final ActionListener fileListener) {
			this.fileListener = fileListener;
		}
		
		public final File getSelectedFile() {
			return this.selectedFile;
		}
		
		public final FileSelector setFile(final File file) {
			this.files.remove(file);
			this.files.add(0, file);
			
			final boolean changed = !file.equals(this.getSelectedFile());
			
			if (changed) {
				this.selectedFile = file;
				this.setText(file.getName());
				
				if (this.fileListener != null) {
					this.fileListener.actionPerformed(new ActionEvent(this, -1, null));
				}
			}
			
			return this;
		}
		
		final void showPopup() {
			final JPopupMenu popup = new JPopupMenu();
			
			for (final File file : this.files) {
				popup.add(new JMenuItem(new AbstractAction(file.getPath()) {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						FileSelector.this.setFile(file);
					}
					
					private static final long serialVersionUID = 8311454620470586686L;
					
				}));
			}
			
			popup.show(this, 0, 0);
		}
		
		private static final long serialVersionUID = 7227165282556980768L;
		
	}
	
	public static final File save(final String preferenceKey, final String title, final Component parent) {
		final JFileChooser fileChooser = new JFileChooser(new File(preferences.get(preferenceKey, "")).getParentFile());
		
		fileChooser.setDialogTitle(title);
		
		if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(parent)) {
			return fileChooser.getSelectedFile();
		}
		
		return null;
	}
	
	public static final File open(final String preferenceKey, final String title, final Component parent) {
		final JFileChooser fileChooser = new JFileChooser(new File(preferences.get(preferenceKey, "")).getParentFile());
		
		fileChooser.setDialogTitle(title);
		
		if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(parent)) {
			return fileChooser.getSelectedFile();
		}
		
		return null;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class MainPanel extends JPanel {
		
		private final Context context;
		
		private final FileSelector imageSelector;
		
		private final JCheckBox imageVisibilitySelector;
		
		private final FileSelector groundTruthSelector;
		
		private final JCheckBox groundTruthVisibilitySelector;
		
		private final FileSelector pipelineSelector;
		
		private final JTextField trainingSummaryView;
		
		private final JTextField classificationSummaryView;
		
		private final JCheckBox classificationVisibilitySelector;
		
		private final JTree tree;
		
		private final JSplitPane mainSplitPane;
		
		private final StatusPanel statusPanel;
		
		private Image2DComponent imageComponent;
		
		private final Map<Integer, Area> groundTruthRegions;
		
		private Pipeline pipeline;
		
		private final Point mouse;
		
		private int brushSize;
		
		public MainPanel(final Context context) {
			super(new BorderLayout());
			
			this.context = context;
			this.imageSelector = new FileSelector();
			this.imageVisibilitySelector = new JCheckBox("", true);
			this.groundTruthSelector = new FileSelector();
			this.groundTruthVisibilitySelector = new JCheckBox();
			this.pipelineSelector = new FileSelector();
			this.trainingSummaryView = textView("-");
			this.classificationSummaryView = textView("-");
			this.classificationVisibilitySelector = new JCheckBox();
			this.tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("No pipeline")));
			this.mouse = new Point();
			this.brushSize = 1;
			this.groundTruthRegions = new HashMap<>();
			
			this.tree.addTreeSelectionListener(new TreeSelectionListener() {
				
				@Override
				public final void valueChanged(final TreeSelectionEvent event) {
					final Image2DComponent imageComponent = MainPanel.this.getImageComponent();
					
					if (imageComponent != null) {
						imageComponent.repaint();
					}
				}
				
			});
			
			final int padding = this.imageVisibilitySelector.getPreferredSize().width;
			final JButton openImageButton = button("open");
			final JButton newGroundTruthButton = button("new");
			final JButton saveGroundTruthButton = button("save");
			final JButton reloadGroundTruthButton = button("refresh");
			final JButton newPipelineButton = button("new");
			final JButton openPipelineButton = button("open");
			final JButton runPipelineButton = button("process");
			final JButton savePipelineButton = button("save");
			final JButton reloadPipelineButton = button("refresh");
			final JButton runTrainingButton = button("process");
			final JButton showTrainingResultsButton = button("results");
			final JButton runClassificationButton = button("process");
			final JButton showClassificationResultsButton = button("results");
			
			this.mainSplitPane = horizontalSplit(scrollable(verticalBox(
					limitHeight(horizontalBox(this.imageVisibilitySelector, openImageButton,
							label(" Image: ", this.imageSelector))),
					limitHeight(horizontalBox(this.groundTruthVisibilitySelector, newGroundTruthButton, saveGroundTruthButton, reloadGroundTruthButton,
							label(" Ground truth: ", this.groundTruthSelector))),
					limitHeight(horizontalBox(Box.createHorizontalStrut(padding), newPipelineButton, openPipelineButton, runPipelineButton, savePipelineButton, reloadPipelineButton,
							label(" Pipeline: ", this.pipelineSelector))),
					limitHeight(horizontalBox(Box.createHorizontalStrut(padding), runTrainingButton, showTrainingResultsButton,
							label(" Training: ", this.trainingSummaryView))),
					limitHeight(horizontalBox(this.classificationVisibilitySelector, runClassificationButton, showClassificationResultsButton,
							label(" Classification: ", this.classificationSummaryView))),
					scrollable(this.tree))), new JLabel("Drop file here"));
			
			this.mainSplitPane.setResizeWeight(0.25);
			this.add(this.mainSplitPane, BorderLayout.CENTER);
			
			this.statusPanel = new StatusPanel();
			this.add(this.statusPanel, BorderLayout.SOUTH);
			
			openImageButton.addActionListener(e -> {
				final File file = open(IMAGE_PATH, "Open image", MainPanel.this);
				
				if (file != null) {
					context.setImage(file);
				}
			});
			this.imageSelector.setFileListener(e -> context.setImage(MainPanel.this.getImageSelector().getSelectedFile()));
			
			this.groundTruthSelector.setFileListener(e -> context.refreshGroundTruthAndClassification());
			newGroundTruthButton.addActionListener(e -> context.saveGroundTruth(JOptionPane.showInputDialog("Ground truth name:")));
			saveGroundTruthButton.addActionListener(e -> context.saveGroundTruth(false));
			
			this.pipelineSelector.setFileListener(e -> context.setPipeline(MainPanel.this.getPipelineSelector().getSelectedFile()));
			newPipelineButton.addActionListener(e -> context.savePipeline(save(PIPELINE, "New pipeline", MainPanel.this)));
			openPipelineButton.addActionListener(e -> context.setPipeline(open(PIPELINE, "Open pipeline", MainPanel.this)));
			savePipelineButton.addActionListener(e -> context.savePipeline());
			
			runTrainingButton.addActionListener(e -> {
				final Pipeline pipeline = MainPanel.this.getPipeline();
				
				if (pipeline != null) {
					switch (pipeline.getComputationStatus()) {
					case COMPUTING:
						runTrainingButton.setIcon(buttonIcon("warning"));
						pipeline.setComputationStatus(ComputationStatus.CANCELED);
					case CANCELED:
						return;
					case IDLE:
						break;
					}
					
					runTrainingButton.setIcon(buttonIcon("stop"));
					MainPanel.this.getTrainingSummaryView().setText("-");
					
					new SwingWorker<Void, Void>() {
						
						@Override
						protected final Void doInBackground() throws Exception {
							context.saveGroundTruth(true);
							context.savePipeline();
							
							pipeline.train(context.getGroundTruthName());
							
							context.savePipeline();
							
							return null;
						}
						
						@Override
						protected final void done() {
							Tools.debugPrint();
							
							pipeline.setComputationStatus(ComputationStatus.IDLE);
							runTrainingButton.setIcon(buttonIcon("process"));
							
							final double trainingSeconds = pipeline.getTrainingMilliseconds() / 1_000.0;
							final double f1 = Pipeline.f1(pipeline.getTrainingConfusionMatrix());
							
							MainPanel.this.getTrainingSummaryView().setText(format(Locale.ENGLISH,
									"seconds=%.3f, F1=%.3f", trainingSeconds, f1));
							
							Tools.debugPrint();
						}
						
					}.execute();
				}
			});
			showTrainingResultsButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Pipeline pipeline = MainPanel.this.getPipeline();
					
					showTable(pipeline.getTrainingConfusionMatrix(), pipeline.getClassLabels(), "Training results");
				}
				
			});
			
			runClassificationButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Pipeline pipeline = MainPanel.this.getPipeline();
					
					if (pipeline != null) {
						switch (pipeline.getComputationStatus()) {
						case COMPUTING:
							runClassificationButton.setIcon(buttonIcon("warning"));
							pipeline.setComputationStatus(ComputationStatus.CANCELED);
						case CANCELED:
							return;
						case IDLE:
							break;
						}
						
						runClassificationButton.setIcon(buttonIcon("stop"));
						MainPanel.this.getClassificationSummaryView().setText("-");
						
						new SwingWorker<Void, Void>() {
							
							@Override
							protected final Void doInBackground() throws Exception {
								context.saveGroundTruth(true);
								context.savePipeline();
								
								pipeline.classify(context.getImage(),
										MainPanel.this.getImageComponent().getVisibleRect(),
										context.getGroundTruthName().isEmpty() ? null : new AwtImage2D(null, context.getGroundTruth().getImage()),
												new AwtImage2D(null, context.getClassification().getImage()));
								
								context.saveClassification();
								
								return null;
							}
							
							@Override
							protected final void done() {
								Tools.debugPrint();
								
								pipeline.setComputationStatus(ComputationStatus.IDLE);
								runClassificationButton.setIcon(buttonIcon("process"));
								
								final double classificationSeconds = pipeline.getClassificationMilliseconds() / 1_000.0;
								final double f1 = Pipeline.f1(pipeline.getClassificationConfusionMatrix());
								
								MainPanel.this.getClassificationSummaryView().setText(format(Locale.ENGLISH,
										"seconds=%.3f, F1=%.3f", classificationSeconds, f1));
								
								MainPanel.this.getImageComponent().repaint();
								
								Tools.debugPrint();
							}
							
						}.execute();
					}
				}
				
			});
			showClassificationResultsButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Pipeline pipeline = MainPanel.this.getPipeline();
					
					showTable(pipeline.getClassificationConfusionMatrix(), pipeline.getClassLabels(), "Classification results");
				}
				
			});
			
			this.imageVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Image2DComponent imageComponent = MainPanel.this.getImageComponent();
					
					imageComponent.setImageEnabled(!imageComponent.isImageEnabled());
					imageComponent.repaint();
				}
				
			});
			
			this.groundTruthVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					MainPanel.this.getImageComponent().repaint();
				}
				
			});
			
			this.classificationVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					MainPanel.this.getImageComponent().repaint();
				}
				
			});
			
			this.setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
//					System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
					final File file = SwingTools.getFiles(event).get(0);
					
					if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
						context.setPipeline(file);
					} else {
						context.setImage(file);
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 5442000733451223725L;
				
			});
			
			this.setPreferredSize(new Dimension(800, 600));
			
			this.tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
				
				@Override
				public final void valueChanged(final TreeSelectionEvent event) {
					final DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
					final UserObject userObject = cast(UserObject.class, node.getUserObject());
					final Object object = userObject == null ? null : userObject.getUIScaffold().getObject();
					final boolean painting = (object instanceof ClassDescription) || "classes".equals(node.getUserObject());
					
					getImageComponent().setWheelZoomEnabled(!painting);
					getImageComponent().setDragEnabled(!painting);
				}
				
			});
			
			context.setMainPanel(this);
		}
		
		public final StatusPanel getStatusPanel() {
			return this.statusPanel;
		}
		
		public final int getBrushSize() {
			return this.brushSize;
		}
		
		public final void setBrushSize(final int brushSize) {
			this.brushSize = max(1, min(brushSize, 128));
		}
		
		public final Point getMouse() {
			return this.mouse;
		}
		
		public final Context getContext() {
			return this.context;
		}
		
		public final Pipeline getPipeline() {
			return this.pipeline;
		}
		
		public final void setPipeline(final Pipeline pipeline) {
			this.pipeline = pipeline;
			
			setModel(this.tree, pipeline, "Pipeline", new Instantiator() {
				
				@SuppressWarnings("unchecked")
				@Override
				public final <T> T newInstanceOf(final Class<T> cls) {
					if (TrainingField.class.equals(cls)) {
						final TrainingField result = (TrainingField) CommonTools.newInstanceOf(cls);
						final Image2D image = MainPanel.this.getContext().getImage();
						
						if (image != null) {
							final double imageScale = image.getScale();
							
							result.setImagePath(MainPanel.this.getContext().getImageFile().getPath());
							final Rectangle bounds = result.getBounds();
							bounds.setBounds(MainPanel.this.getImageComponent().getVisibleRect());
							
							final Point2D p = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
							final AffineTransform view = getImageComponent().getView();
							
							try {
								view.inverseTransform(p, p);
							} catch (final NoninvertibleTransformException exception) {
								exception.printStackTrace();
							}
							
							bounds.setBounds(
									(int) (p.getX() - 128.0 / imageScale),
									(int) (p.getY() - 128.0 / imageScale),
									(int) (256 / imageScale), (int) (256 / imageScale));
						}
						
						return (T) result;
					}
					
					if (Algorithm.class.equals(cls)) {
						final Class<? extends Algorithm> choice = (Class<? extends Algorithm>) JOptionPane.showInputDialog(
								MainPanel.this.getTree(),
								"Learning type:",
								"New algorithm",
								JOptionPane.PLAIN_MESSAGE,
								null,
								array(UnsupervisedAlgorithm.class, SupervisedAlgorithm.class),
								UnsupervisedAlgorithm.class);
						
						if (choice != null) {
							return (T) CommonTools.newInstanceOf(choice, pipeline);
						}
						
						return null;
					}
					
					return CommonTools.newInstanceOf(cls);
				}
				
				private static final long serialVersionUID = -3863907586014633958L;
				
			});
		}
		
		public final Image2DComponent getImageComponent() {
			return this.imageComponent;
		}
		
		public final void setImage(final String path) {
			this.imageComponent = new Image2DComponent(IMJTools.read(path, 0));
			
			this.getImageComponent().setOverlay(new Overlay() {
				
				@Override
				public final void update(final Graphics2D g, final Rectangle region) {
					final Composite savedComposite = g.getComposite();
					boolean useDashing = false;
					
					g.setComposite(HighlightComposite.INSTANCE);
					
					try {
						g.drawOval(0, 0, 1, 1);
						g.drawOval(0, 0, 1, 1);
					} catch (final InternalError error) {
						// XXX On Linux, "at sun.java2d.xr.XRSurfaceData.getRaster(XRSurfaceData.java:72)" called from "sun.java2d.pipe.GeneralCompositePipe.renderPathTile(GeneralCompositePipe.java:100)"
						if ("not implemented yet".equals(error.getMessage())) {
							g.setComposite(savedComposite);
							useDashing = true;
						} else {
							throw error;
						}
					}
					
					final Image2D image = MainPanel.this.getImageComponent().getImage();
					final float dashSize = 4F;
					
					if (MainPanel.this.getGroundTruthVisibilitySelector().isSelected()) {
						final float imageScale = (float) image.getScale();
						final Stroke dash0 = newDash0(dashSize / imageScale);
						final Stroke dash1 = newDash1(dashSize / imageScale);
						final AffineTransform savedTransform = g.getTransform();
						
						g.setTransform(MainPanel.this.getImageComponent().getView());
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3F));
						
						for (final Map.Entry<Integer, Area> entry : MainPanel.this.groundTruthRegions.entrySet()) {
							g.setColor(new Color(entry.getKey(), true));
							g.fill(entry.getValue());
						}
						
						final Rectangle trainingBounds = MainPanel.this.getTrainingBounds();
						
						if (trainingBounds != null) {
							if (useDashing) {
								g.setStroke(dash0);
								g.setColor(Color.BLACK);
								g.draw(trainingBounds);
								g.setStroke(dash1);
								g.setColor(Color.WHITE);
							} else {
								g.setStroke(new BasicStroke());
								g.setComposite(HighlightComposite.INSTANCE);
							}
							
							g.draw(trainingBounds);
							
//							final int size = 12;
//							final int x = trainingBounds.x;
//							final int y = trainingBounds.y;
//							final int w = trainingBounds.width;
//							final int halfW = w / 2;
//							final int h = trainingBounds.height;
//							final int halfH = h / 2;
//							
//							fillDisk(g, x, y, size);
//							fillDisk(g, x + halfW, y, size);
//							fillDisk(g, x + w, y, size);
//							fillDisk(g, x, y + halfH, size);
//							fillDisk(g, x + w, y + halfH, size);
//							fillDisk(g, x, y + h, size);
//							fillDisk(g, x + halfW, y + h, size);
//							fillDisk(g, x + w, y + h, size);
						}
						
						g.setTransform(savedTransform);
					}
					
					final Point m = MainPanel.this.getMouse();
					
					if (0 < m.x && MainPanel.this.getBrushColor() != null) {
						final Stroke dash0 = newDash0(dashSize);
						final Stroke dash1 = newDash1(dashSize);
						final int s = MainPanel.this.getBrushSize();
						
						if (useDashing) {
							g.setStroke(dash0);
							g.setColor(Color.BLACK);
							g.drawOval(m.x - s / 2, m.y - s / 2, s, s);
							g.setStroke(dash1);
							g.setColor(Color.WHITE);
						} else {
							g.setStroke(new BasicStroke());
							g.setComposite(HighlightComposite.INSTANCE);
						}
						
						g.drawOval(m.x - s / 2, m.y - s / 2, s, s);
					}
					
					g.setComposite(savedComposite);
				}
				
				private static final long serialVersionUID = -1285261310324357936L;
				
			});

			new MouseHandler() {
				
				private boolean dragging;
				
				private Transform transform;
				
				@Override
				public final void mousePressed(final MouseEvent event) {
					this.transform = Transform.get(MainPanel.this.getTrainingBounds(), event.getX(), event.getY());
					
					this.mouseMoved(event);
					this.mouseDragged(event);
				}
				
				@Override
				public final void mouseClicked(final MouseEvent event) {
					if (this.transform != null && event.getClickCount() == 2) {
						MainPanel.this.getTrainingBounds().setBounds(
								MainPanel.this.getImageComponent().getVisibleRect());
						MainPanel.this.getImageComponent().repaint();
					}
					this.updateStatus(event);
				}
				
				@Override
				public final void mouseReleased(final MouseEvent event) {
					this.dragging = false;
					this.updateStatus(event);
				}
				
				@Override
				public final void mouseMoved(final MouseEvent event) {
					MainPanel.this.getMouse().setLocation(event.getX(), event.getY());
					MainPanel.this.getImageComponent().repaint();
					this.updateStatus(event);
				}
				
				private final void updateStatus(final MouseEvent event) {
					final AffineTransform view = MainPanel.this.getImageComponent().getView();
					
					MainPanel.this.getStatusPanel().setMessage("scale: ", String.format("%.3f", view.getScaleX()));
					
					try {
						final Point p = event.getPoint();
						view.inverseTransform(p, p);
						MainPanel.this.getStatusPanel().setMessage("(x y): ", String.format("(%d %d)", p.x, p.y));
					} catch (final NoninvertibleTransformException exception) {
						exception.printStackTrace();
					}
					
				}
				
				@Override
				public final void mouseExited(final MouseEvent event) {
					if (!this.dragging) {
						MainPanel.this.getMouse().x = -1;
						MainPanel.this.getImageComponent().repaint();
						
						// XXX this mysteriously fixes a mysterious GUI defect on Linux (overlay is shifted to the left when mouse exits to the left and goes over the split pane separator)
						SwingUtilities.invokeLater(MainPanel.this.getImageComponent()::repaint);
					}
					this.updateStatus(event);
				}
				
				@Override
				public final void mouseWheelMoved(final MouseWheelEvent event) {
					if (event.getWheelRotation() < 0) {
						MainPanel.this.setBrushSize(MainPanel.this.getBrushSize() + 1);
					} else {
						MainPanel.this.setBrushSize(MainPanel.this.getBrushSize() - 1);
					}
					
					MainPanel.this.getImageComponent().repaint();
					this.updateStatus(event);
				}
				
				@Override
				public final void mouseDragged(final MouseEvent event) {
					final Image2D image = MainPanel.this.getImageComponent().getImage();
					
					if (image != null) {
						final Color brushColor = MainPanel.this.getBrushColor();
						
						if (brushColor != null) {
							this.dragging = true;
							
							final Point m = MainPanel.this.getMouse();
							final int x = event.getX();
							final int y = event.getY();
							final int rgb = brushColor.getRGB();
							final double imageScale = image.getScale();
							final int brushSize = (int) (MainPanel.this.getBrushSize() / imageScale);
							final double r = brushSize / 2.0;
							final double strokeLength = m.distance(x, y) / imageScale;
							final Point2D p1 = new Point2D.Double(m.x, m.y);
							final Point2D p2 = new Point2D.Double(x, y);
							
							try {
								final AffineTransform view = MainPanel.this.getImageComponent().getView();
								view.inverseTransform(p1, p1);
								view.inverseTransform(p2, p2);
							} catch (final NoninvertibleTransformException exception) {
								exception.printStackTrace();
							}
							
							final Shape strokeShape = new RoundRectangle2D.Double(-r, -r, strokeLength + brushSize, brushSize, brushSize, brushSize);
							final AffineTransform transform = new AffineTransform();
							transform.rotate(p2.getX() - p1.getX(), p2.getY() - p1.getY(), p1.getX(), p1.getY());
							transform.translate(p1.getX(), p1.getY());
							final Area stroke = new Area(strokeShape).createTransformedArea(transform);
							
							for (final Map.Entry<Integer, Area> entry : MainPanel.this.groundTruthRegions.entrySet()) {
								if (entry.getKey() != rgb) {
									entry.getValue().subtract(stroke);
								}
							}
							
							if (rgb != 0) {
								MainPanel.this.groundTruthRegions.computeIfAbsent(rgb, l -> new Area()).add(stroke);
							}
							
							MainPanel.this.getImageComponent().repaint();
						} else if (this.transform != null) {
							this.transform.updateBounds(MainPanel.this.getTrainingBounds(),
									event.getX(), event.getY(), image.getWidth(), image.getHeight());
							
							MainPanel.this.getTree().repaint();
						}
					}
					
					this.mouseMoved(event);
				}
				
				private static final long serialVersionUID = 1137846082170903999L;
				
			}.addTo(this.getImageComponent());
			
			this.setContents(this.getImageComponent());
		}
		
		@SuppressWarnings("null")
		public final Color getBrushColor() {
			if (MainPanel.this.getGroundTruthVisibilitySelector().isSelected()
					&& !MainPanel.this.getContext().getGroundTruthName().isEmpty()) {
				final TreePath selectionPath = MainPanel.this.getTree().getSelectionPath();
				final DefaultMutableTreeNode node = selectionPath == null ? null : cast(DefaultMutableTreeNode.class, selectionPath.getLastPathComponent());
				final UserObject userObject = node == null ? null : cast(UserObject.class, node.getUserObject());
				final ClassDescription classDescription = userObject == null ? null : cast(ClassDescription.class, userObject.getUIScaffold().getObject());
				final boolean nodeIsClasses = node != null && "classes".equals(node.getUserObject());
				
				if (classDescription != null || nodeIsClasses) {
					return new Color(nodeIsClasses ? 0 : classDescription.getLabel(), true);
				}
			}
			
			return null;
		}
		
		public final Rectangle getTrainingBounds() {
			if (!MainPanel.this.getContext().getGroundTruthName().isEmpty()) {
				final TreePath selectionPath = MainPanel.this.getTree().getSelectionPath();
				final DefaultMutableTreeNode node = selectionPath == null ? null : cast(DefaultMutableTreeNode.class, selectionPath.getLastPathComponent());
				final UserObject userObject = node == null ? null : cast(UserObject.class, node.getUserObject());
				final TrainingField trainingField = userObject == null ? null : cast(TrainingField.class, userObject.getUIScaffold().getObject());
				
				if (trainingField != null
						&& MainPanel.this.getContext().getImageFile().equals(new File(trainingField.getImagePath()))) {
					return trainingField.getBounds();
				}
			}
			
			return null;
		}
		
		public final FileSelector getImageSelector() {
			return this.imageSelector;
		}
		
		public final JTree getTree() {
			return this.tree;
		}
		
		public final JCheckBox getImageVisibilitySelector() {
			return this.imageVisibilitySelector;
		}
		
		public final FileSelector getGroundTruthSelector() {
			return this.groundTruthSelector;
		}
		
		public final JCheckBox getGroundTruthVisibilitySelector() {
			return this.groundTruthVisibilitySelector;
		}
		
		public final FileSelector getPipelineSelector() {
			return this.pipelineSelector;
		}
		
		public final JTextField getTrainingSummaryView() {
			return this.trainingSummaryView;
		}
		
		public final JTextField getClassificationSummaryView() {
			return this.classificationSummaryView;
		}
		
		public final JCheckBox getClassificationVisibilitySelector() {
			return this.classificationVisibilitySelector;
		}
		
		public final void setContents(final Component component) {
//			this.mainSplitPane.setRightComponent(scrollable(component));
			this.mainSplitPane.setRightComponent(component);
		}
		
		private static final long serialVersionUID = 2173077945563031333L;
		
//		public static final BasicStroke DASH0 = new BasicStroke(2F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1F, new float[] { 2F, 2F }, 0F);
//		
//		public static final BasicStroke DASH1 = new BasicStroke(2F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1F, new float[] { 2F, 2F }, 2F);
		
		public static final BasicStroke newDash0(final float width) {
			return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1F, new float[] { width, width }, 0F);
		}
		
		public static final BasicStroke newDash1(final float width) {
			return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1F, new float[] { width, width }, width);
		}
		
		public static final int IMAGE_SELECTOR_RESERVED_SLOTS = 2;
		
		public static final void fillDisk(final Graphics g, final int x, final int y, final int size) {
			g.fillOval(x - size / 2, y - size / 2, size, size);
		}
		
		public static final <K, V> void showTable(final Map<K, Map<K, V>> labeledTable,
				final Map<String, K> namedLabels, final String title) {
			final StringBuilder html = new StringBuilder("<html><body><table>");
			
			html.append("<tr><td></td>");
			html.append(join("", namedLabels.keySet().stream().map(k -> "<td>" + k + "</td>").toArray()));
			html.append("</tr>");
			
			for (final Map.Entry<String, K> expected : namedLabels.entrySet()) {
				final String expectedName = expected.getKey();
				final K expectedLabel = expected.getValue();
				final Map<K, ?> row = labeledTable.get(expectedLabel);
				
				html.append("<tr><td>").append(expectedName).append("</td>");
				
				if (row != null) {
					for (final K actualLabel : namedLabels.values()) {
						final Object actualCount = row.get(actualLabel);
						
						html.append("<td>").append(actualCount != null ? actualCount : "").append("</td>");
					}
				}
				
				html.append("</tr>");
			}
			
			html.append("</table></body></html>");
			
			final JLabel tableView = new JLabel(html.toString());
			
			SwingTools.show(tableView, title, false);
		}
		
		/**
		 * @author codistmonk (creation 2015-02-21)
		 */
		public static interface Transform extends Serializable {
			
			public abstract void updateBounds(Rectangle bounds, int x, int y, int endX, int endY);
			
			public static Transform get(final Rectangle bounds, final int x, final int y) {
				if (bounds == null) {
					return null;
				}
				
				final int x0 = bounds.x;
				final int y0 = bounds.y;
				final int w = bounds.width;
				final int halfW = w / 2;
				final int h = bounds.height;
				final int halfH = h / 2;
				
				if (Resize.closeEnough(x0, y0, x, y)) {
					return Resize.NORTH_WEST;
				}
				
				if (Resize.closeEnough(x0 + halfW, y0, x, y)) {
					return Resize.NORTH;
				}
				
				if (Resize.closeEnough(x0 + w, y0, x, y)) {
					return Resize.NORTH_EAST;
				}
				
				if (Resize.closeEnough(x0, y0 + halfH, x, y)) {
					return Resize.WEST;
				}
				
				if (Resize.closeEnough(x0 + w, y0 + halfH, x, y)) {
					return Resize.EAST;
				}
				
				if (Resize.closeEnough(x0, y0 + h, x, y)) {
					return Resize.SOUTH_WEST;
				}
				
				if (Resize.closeEnough(x0 + halfW, y0 + h, x, y)) {
					return Resize.SOUTH;
				}
				
				if (Resize.closeEnough(x0 + w, y0 + h, x, y)) {
					return Resize.SOUTH_EAST;
				}
				
				if (bounds.contains(x, y)) {
					return new Translate(x - bounds.x, y - bounds.y);
				}
				
				return null;
			}
			
			/**
			 * @author codistmonk (creation 2015-02-21)
			 */
			public static final class Translate implements Transform {
				
				private final int offsetX;
				
				private final int offsetY;
				
				public Translate(final int offsetX, final int offsetY) {
					this.offsetX = offsetX;
					this.offsetY = offsetY;
				}
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					bounds.x = max(0, min(x - this.offsetX, endX - bounds.width));
					bounds.y = max(0, min(y - this.offsetY, endY - bounds.height));
				}
				
				private static final long serialVersionUID = 6750762176816903863L;
				
			}
			
		}
		
		/**
		 * @author codistmonk (creation 2015-02-21)
		 */
		public static enum Resize implements Transform {
			
			NORTH {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					final int bottom = bounds.y + bounds.height - 1;
					
					bounds.y = max(0, min(y, bottom));
					bounds.height = bottom - bounds.y + 1;
				}
				
			}, EAST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					bounds.width = max(1, min(x, endX - 1) - bounds.x + 1);
				}
				
			}, SOUTH {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					bounds.height = max(1, min(y, endY - 1) - bounds.y + 1);
				}
				
			}, WEST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					final int right = bounds.x + bounds.width - 1;
					
					bounds.x = max(0, min(x, right));
					bounds.width = right - bounds.x + 1;
				}
				
			}, NORTH_WEST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					NORTH.updateBounds(bounds, x, y, endX, endY);
					WEST.updateBounds(bounds, x, y, endX, endY);
				}
				
			}, NORTH_EAST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					NORTH.updateBounds(bounds, x, y, endX, endY);
					EAST.updateBounds(bounds, x, y, endX, endY);
				}
				
			}, SOUTH_EAST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					SOUTH.updateBounds(bounds, x, y, endX, endY);
					EAST.updateBounds(bounds, x, y, endX, endY);
				}
				
			}, SOUTH_WEST {
				
				@Override
				public final void updateBounds(final Rectangle bounds, final int x, final int y, final int endX, final int endY) {
					SOUTH.updateBounds(bounds, x, y, endX, endY);
					WEST.updateBounds(bounds, x, y, endX, endY);
				}
				
			};
			
			public static final boolean closeEnough(final int x0, final int y0, final int x1, final int y1) {
				return square(x1 - x0) + square(y1 - y0) <= square(12.0);
			}
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class Context implements Serializable {
		
		private MainPanel mainPanel;
		
		private final Canvas groundTruth = new Canvas();
		
		private final Canvas classification = new Canvas();
		
		public final MainPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final void setMainPanel(final MainPanel mainPanel) {
			this.mainPanel = mainPanel;
		}
		
		public final File getPipelineFile() {
			return this.getMainPanel().getPipelineSelector().getSelectedFile();
		}
		
		public final String getGroundTruthName() {
			return this.getMainPanel().getGroundTruthSelector().getText();
		}
		
		public final Image2D getImage() {
			final MainPanel mainPanel = this.getMainPanel();
			final Image2DComponent imageComponent = mainPanel == null ? null : mainPanel.getImageComponent();
			
			return imageComponent == null ? null : imageComponent.getImage();
		}

		public final Canvas getGroundTruth() {
			return this.groundTruth;
		}
		
		public final Context formatGroundTruth() {
			return format(this.getGroundTruth());
		}
		
		public final Context saveGroundTruth(final boolean rasterize) {
			final Image2D image = this.getImage();
			
			if (image != null) {
				this.saveGroundTruth(this.getGroundTruthName());
				
				if (rasterize) {
					final String groundTruthPath = this.getGroundTruthPath();
					final Image2D groundTruth = IMJTools.read(groundTruthPath, 0);
					final int optimalTileWidth = groundTruth.getOptimalTileWidth();
					final int optimalTileHeight = groundTruth.getOptimalTileHeight();
					final ExecutorService executor = MultiThreadTools.getExecutor();
					int w = groundTruth.getWidth();
					int h = groundTruth.getHeight();
					final Collection<Future<?>> tasks = new ArrayList<>();
					final Map<Integer, Area> regions = this.getMainPanel().groundTruthRegions;
					final Map<Integer, Image2D> pyramid = Collections.synchronizedMap(new HashMap<>());
					int level = 0;
					
					pyramid.put(level, groundTruth);
					
					while (0 < w && 0 < h) {
						final int lod = level;
						final Image2D lodImage = pyramid.computeIfAbsent(lod, l -> IMJTools.read(getGroundTruthPath(), l));
						
						for (int i = 0; i < h; i += optimalTileHeight) {
							final int tileY = i;
							final int tileHeight = lodImage.getTileHeight(tileY);
							
							for (int j = 0; j < w; j += optimalTileWidth) {
								final int tileX = j;
								final int tileWidth = lodImage.getTileWidth(tileX);
								
								tasks.add(executor.submit(new Runnable() {
									
									@Override
									public final void run() {
										final double scale = lodImage.getScale();
										final BufferedImage tile = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_3BYTE_BGR);
										final Graphics2D graphics = tile.createGraphics();
										
										graphics.setComposite(AlphaComposite.Src);
										graphics.translate(-tileX, -tileY);
										graphics.scale(scale, scale);
										
										for (final Map.Entry<Integer, Area> region : regions.entrySet()) {
											graphics.setColor(new Color(region.getKey(), true));
											graphics.fill(region.getValue());
										}
										
										graphics.dispose();
										
										final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
										
										try {
											ImageIO.write(tile, "png", buffer);
										} catch (final IOException exception) {
											throw new UncheckedIOException(exception);
										}
										
										synchronized (pyramid) {
											lodImage.setTile(tileX, tileY, new AwtImage2D(lodImage.getTileKey(tileX, tileY), tile));
										}
										Tools.debugPrint(tileX, tileY);
									}
									
								}));
							}
						}
						
						w /= 2;
						h /= 2;
						++level;
					}
					
					MultiThreadTools.wait(tasks);
				}
			}
			
			return this;
		}
		
		public final Context saveClassification() {
			if (this.getImage() != null && !this.getGroundTruthName().isEmpty()) {
				final File outputFile = new File(this.getClassificationPath());
				
				try {
					Tools.debugPrint("Writing", outputFile);
					ImageIO.write(this.getClassification().getImage(), "png", outputFile);
				} catch (final IOException exception) {
					throw new UncheckedIOException(exception);
				}
			}
			
			return this;
		}
		
		public final Context saveGroundTruth(final String name) {
			final Image2D image = this.getImage();
			
			if (name != null && image != null) {
				final String groundTruthPath = this.getGroundTruthPath(name);
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				
				Tools.debugPrint("Writing", groundTruthPath);
				
				final MultifileImage2D groundTtruthImage = new MultifileImage2D(new MultifileSource(groundTruthPath), SVS2Multifile.newMetadata(
						imageWidth, imageHeight, 512, "png", image.getMetadata().getOrDefault("micronsPerPixel", "0.25").toString()));
				
				try (final OutputStream output = groundTtruthImage.getSource().getOutputStream("annotations.xml")) {
					XMLTools.write(XMLSerializable.objectToXML(this.getMainPanel().groundTruthRegions), output, 1);
				} catch (final IOException exception) {
					throw new UncheckedIOException(exception);
				}
				
				this.setGroundTruth(name);
			}
			
			return this;
		}
		
		public final Canvas getClassification() {
			return this.classification;
		}
		
		public final String getPipelineName() {
			final File pipelineFile = this.getPipelineFile();
			
			return pipelineFile == null ? null : baseName(pipelineFile.getName());
		}
		
		public final File getImageFile() {
			return this.getMainPanel().getImageSelector().getSelectedFile();
		}
		
		public final String getGroundTruthPath() {
			return this.getGroundTruthPath(this.getGroundTruthName());
		}
		
		public final String getGroundTruthPath(final String name) {
			return Pipeline.getGroundTruthPathFromImagePath(this.getImageFile().getPath(), name);
		}
		
		public final String getGroundTruthPathFromImagePath(final String imagePath) {
			return Pipeline.getGroundTruthPathFromImagePath(imagePath, this.getGroundTruthName());
		}
		
		public final String getClassificationPath() {
			return Pipeline.getClassificationPathFromImagePath(this.getImageFile().getPath(), this.getGroundTruthName(), this.getPipelineName());
		}
		
		public final void refreshGroundTruthAndClassification() {
			final Image2D image = this.getImage();
			
			if (image == null) {
				return;
			}
			
			final String groundTruthPath = this.getGroundTruthPath();
			
			Tools.debugPrint(groundTruthPath);
			
			this.getMainPanel().groundTruthRegions.clear();
			
			try (final InputStream input = new MultifileSource(groundTruthPath).getInputStream("annotations.xml")) {
				final Document xml = XMLTools.parse(input);
				
				this.getMainPanel().groundTruthRegions.putAll(XMLSerializable.objectFromXML(xml.getDocumentElement(), new HashMap<>()));
			} catch (final UncheckedIOException exception) {
				if (!(exception.getCause() instanceof FsEntryNotFoundException)) {
					throw exception;
				}
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
			
			// TODO load classification
			
			this.getMainPanel().getImageComponent().repaint();
		}
		
		public final Context setImage(final File imageFile) {
			System.out.println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, imageFile));
			
			final File oldImageFile = this.getImageFile();
			
			if (imageFile.isFile() && !imageFile.equals(oldImageFile)) {
				this.getMainPanel().setImage(imageFile.getPath());
				this.getMainPanel().getImageSelector().setFile(imageFile);
				
				final Pattern pattern = Pattern.compile(baseName(imageFile.getName()) + "_groundtruth_(.*)\\.[^.]+");
				
				for (final String name : imageFile.getParentFile().list()) {
					final Matcher matcher = pattern.matcher(name);
					
					if (matcher.matches()) {
						this.setGroundTruth(matcher.group(1));
					}
				}
				
				this.refreshGroundTruthAndClassification();
				
				preferences.put(IMAGE_PATH, imageFile.getPath());
			}
			
			return this;
		}
		
		public final Context setGroundTruth(final String name) {
			if (new File(this.getGroundTruthPath(name)).isFile()) {
				this.getMainPanel().getGroundTruthSelector().setFile(new File(name));
				
				preferences.put(GROUND_TRUTH, name);
			}
			
			return this;
		}
		
		public final Context setPipeline(final File pipelineFile) {
			if (pipelineFile != null && pipelineFile.isFile()) {
				final String path = pipelineFile.getPath();
				
				if (path.endsWith(".jo")) {
					this.getMainPanel().setPipeline((Pipeline) Tools.readObject(path));
				} else if (path.endsWith(".xml")) {
					this.getMainPanel().setPipeline(XMLSerializable.objectFromXML(pipelineFile));
				} else {
					Tools.debugError("Invalid file name:", pipelineFile.getName());
				}
				
				this.getMainPanel().getPipelineSelector().setFile(pipelineFile);
				
				preferences.put(PIPELINE, pipelineFile.getPath());
			}
			
			return this;
		}
		
		public final Context savePipeline(final File pipelineFile) {
			if (pipelineFile != null) {
				Pipeline pipeline = this.getMainPanel().getPipeline();
				
				if (pipeline == null) {
					pipeline = new Pipeline();
				}
				
				final String path = pipelineFile.getPath();
				
				if (path.endsWith(".jo")) {
					Tools.writeObject(pipeline, path);
				} else if (path.endsWith(".xml")) {
					XMLTools.write(pipeline.toXML(), pipelineFile, 0);
				} else {
					Tools.debugError("Invalid file name:", pipelineFile.getName());
				}
				
				this.setPipeline(pipelineFile);
			}
			
			return this;
		}
		
		public final Context savePipeline() {
			final File pipelineFile = this.getPipelineFile();
			
			if (pipelineFile != null && this.getMainPanel().getPipeline() != null) {
				final String path = pipelineFile.getPath();
				
				if (path.endsWith(".jo")) {
					Tools.writeObject(this.getMainPanel().getPipeline(), path);
				} else if (path.endsWith(".xml")) {
					XMLTools.write(this.getMainPanel().getPipeline().toXML(), pipelineFile, 0);
				} else {
					Tools.debugError("Invalid file name:", pipelineFile.getName());
				}
			}
			
			return this;
		}
		
		private final Context format(final Canvas canvas) {
			final Image2D image = this.getImage();
			
			if (image != null) {
				canvas.setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			} else {
				canvas.setFormat(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
			
			canvas.clear(CLEAR);
			
			return this;
		}
		
		private static final long serialVersionUID = -2487965125442868238L;
		
		public static final Color CLEAR = new Color(0, true);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-04-04)
	 */
	public static final class StatusPanel extends JPanel {
		
		private final Box permanentMessagesBox;
		
		private final LinkedHashMap<String, JLabel> permanentMessages;
		
		public StatusPanel() {
			super(new BorderLayout());
			this.permanentMessagesBox = Box.createHorizontalBox();
			this.permanentMessages = new LinkedHashMap<>();
			this.add(this.permanentMessagesBox, BorderLayout.CENTER);
		}
		
		public final void addSeparator() {
			this.permanentMessagesBox.add(Box.createHorizontalStrut(4));
		}
		
		public final void setMessage(final String key, final String text) {
			if (text == null) {
				final JLabel label = this.permanentMessages.get(key);
				
				if (label != null) {
					final Container parent = label.getParent();
					final int index = indexOf(label, parent.getComponents());
					
					parent.remove(index);
					parent.remove(index - 1);
				}
			} else {
				this.permanentMessages.computeIfAbsent(key, k -> {
					StatusPanel.this.getPermanentMessagesBox().add(new JLabel(key));
					
					final JLabel result = new JLabel();
					
					StatusPanel.this.getPermanentMessagesBox().add(result);
					StatusPanel.this.getPermanentMessagesBox().add(Box.createHorizontalStrut(4));
					
					return result;
				}).setText(text);
			}
		}
		
		public final void setMessage(final String text) {
			this.add(new JLabel(text), BorderLayout.EAST);
		}
		
		final Box getPermanentMessagesBox() {
			return this.permanentMessagesBox;
		}
		
		private static final long serialVersionUID = 7159742213422027495L;
		
		public static final int indexOf(final Object needle, final Object[] haystack) {
			final int n = haystack.length;
			
			for (int i = 0; i < n; ++i) {
				if (needle == haystack[i]) {
					return i;
				}
			}
			
			return -1;
		}
		
	}
	
}
