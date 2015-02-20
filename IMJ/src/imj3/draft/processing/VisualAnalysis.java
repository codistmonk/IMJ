package imj3.draft.processing;

import static imj3.tools.AwtImage2D.awtRead;
import static imj3.tools.CommonSwingTools.limitHeight;
import static imj3.tools.CommonSwingTools.setModel;
import static java.lang.Math.max;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.append;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.join;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import imj2.tools.Canvas;
import imj3.draft.processing.VisualAnalysis.Context.Refresh;
import imj3.draft.segmentation.ImageComponent;
import imj3.draft.segmentation.ImageComponent.Layer;
import imj3.draft.segmentation.ImageComponent.Painter;
import imj3.tools.CommonSwingTools.NestedList;
import imj3.tools.CommonSwingTools.PropertyGetter;
import imj3.tools.CommonSwingTools.PropertySetter;
import imj3.tools.CommonSwingTools.StringGetter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-13)
 */
public final class VisualAnalysis {
	
	private VisualAnalysis() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(VisualAnalysis.class);
	
	static final XStream xstream = new XStream(new StaxDriver());
	
	public static final String IMAGE_PATH = "image.path";
	
	public static final String GROUND_TRUTH = "groundtruth";
	
	public static final String EXPERIMENT = "experiment";
	
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
				
				context.setImageFile(new File(preferences.get(IMAGE_PATH, "")));
				context.setGroundTruth(preferences.get(GROUND_TRUTH, "gt"));
				context.setExperiment(new File(preferences.get(EXPERIMENT, "experiment.xml")));
			}
			
		});
	}
	
	public static final Component label(final String text, final Component... components) {
		return limitHeight(horizontalBox(append(array((Component) new JLabel(text)), components)));
	}
	
	public static final <C extends JComponent> C centerX(final C component) {
		component.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		return component;
	}
	
	public static final JTextField textView(final String text) {
		final JTextField result = new JTextField(text);
		
		result.setEditable(false);
		
		return result;
	}
	
	public static final JButton button(final String type) {
		final JButton result = new JButton(new ImageIcon(Tools.getResourceURL("lib/tango/" + type + ".png")));
		final int size = max(result.getIcon().getIconWidth(), result.getIcon().getIconHeight());
		
		result.setPreferredSize(new Dimension(size + 2, size + 2));
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-19)
	 */
	public static final class FileSelector extends JButton {
		
		private final List<File> files = new ArrayList<>();
		
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
		
		public ActionListener getFileListener() {
			return fileListener;
		}
		
		public void setFileListener(final ActionListener fileListener) {
			this.fileListener = fileListener;
		}
		
		public final FileSelector setFile(final File file) {
			this.files.remove(file);
			this.files.add(0, file);
			
			Tools.debugPrint(file);
			
			final boolean changed = file.equals(new File(this.getText()));
			
			this.setText(file.getName());
			
			if (changed && this.fileListener != null) {
				this.fileListener.actionPerformed(new ActionEvent(this, -1, null));
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
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class MainPanel extends JPanel {
		
		private final Context context;
		
		private final FileSelector imageSelector;
		
		private final JCheckBox imageVisibilitySelector;
		
		private final FileSelector groundTruthSelector;
		
		private final JCheckBox groundTruthVisibilitySelector;
		
		private final PathSelector experimentSelector;
		
		private final JTextField trainingTimeView;
		
		private final JTextField classificationTimeView;
		
		private final JCheckBox classificationVisibilitySelector;
		
		private final JTextField scoreView;
		
		private final JTree tree;
		
		private final JSplitPane mainSplitPane;
		
		private ImageComponent imageComponent;
		
		private Experiment experiment;
		
		public MainPanel(final Context context) {
			super(new BorderLayout());
			
			this.context = context;
			this.imageSelector = new FileSelector();
			this.imageVisibilitySelector = new JCheckBox("", true);
			this.groundTruthSelector = new FileSelector();
			this.groundTruthVisibilitySelector = new JCheckBox();
			this.experimentSelector = new PathSelector();
			this.trainingTimeView = textView("-");
			this.classificationTimeView = textView("-");
			this.classificationVisibilitySelector = new JCheckBox();
			this.scoreView = textView("-");
			this.tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("No experiment")));
			
			final int padding = this.imageVisibilitySelector.getPreferredSize().width;
			final JButton openImageButton = button("open");
			final JButton newGroundTruthButton = button("new");
			final JButton saveGroundTruthButton = button("save");
			final JButton refreshGroundTruthButton = button("refresh");
			
			this.mainSplitPane = horizontalSplit(verticalBox(
					label(" Image: ", this.imageSelector, openImageButton, this.imageVisibilitySelector),
					label(" Ground truth: ", this.groundTruthSelector, newGroundTruthButton, saveGroundTruthButton, refreshGroundTruthButton, this.groundTruthVisibilitySelector),
					label(" Experiment: ", this.experimentSelector, button("new"), button("open"), button("save"), button("refresh"), Box.createHorizontalStrut(padding)),
					label(" Training (s): ", this.trainingTimeView, button("process"), Box.createHorizontalStrut(padding)),
					label(" Classification (s): ", this.classificationTimeView, button("process"), button("save"), button("refresh"), this.classificationVisibilitySelector),
					label(" F1: ", this.scoreView, Box.createHorizontalStrut(padding)),
					centerX(new JButton("Confusion matrix...")),
					scrollable(this.tree)), scrollable(new JLabel("Drop file here")));
			
			this.mainSplitPane.getLeftComponent().setMaximumSize(new Dimension(128, Integer.MAX_VALUE));
			this.add(this.mainSplitPane, BorderLayout.CENTER);
			
			openImageButton.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final JFileChooser fileChooser = new JFileChooser(new File(preferences.get(IMAGE_PATH, "")).getParent());
					
					if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(MainPanel.this)) {
						context.setImageFile(fileChooser.getSelectedFile());
					}
				}
				
			});
			this.imageSelector.setFileListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					context.setImageFile(new File(MainPanel.this.getImageSelector().getText()));
				}
				
			});
			
			this.groundTruthSelector.setFileListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					context.refreshGroundTruthAndClassification(Refresh.FROM_FILE);
				}
				
			});
			newGroundTruthButton.addActionListener(e -> {
				final String name = JOptionPane.showInputDialog("Ground truth name:");
				
				if (name != null && context.getImage() != null) {
					try {
						ImageIO.write(context.formatGroundTruth().getGroundTruth().getImage(), "png", new File(context.getGroundTruthPath(name)));
					} catch (final IOException exception) {
						throw new UncheckedIOException(exception);
					}
					
					context.setGroundTruth(name);
				}
			});
			saveGroundTruthButton.addActionListener(e -> Tools.debugPrint("TODO"));
			
			this.experimentSelector.setPathListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					try {
						MainPanel.this.setExperiment((Experiment) xstream.fromXML(
								new File(MainPanel.this.getExperimentSelector().getSelectedItem().toString())));
					} catch (final Exception exception) {
						Tools.debugError(exception);
					}
				}
				
			});
			this.experimentSelector.setOptionListener(PathSelector.Option.NEW, e -> {
				final JFileChooser fileChooser = new JFileChooser(new File(preferences.get(EXPERIMENT, "")).getParentFile());
				
				if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(this)) {
					final File selectedFile = fileChooser.getSelectedFile();
					
					try (final OutputStream output = new FileOutputStream(selectedFile)) {
						xstream.toXML(new Experiment(), output);
					} catch (final IOException exception) {
						throw new UncheckedIOException(exception);
					}
					
					context.setExperiment(selectedFile);
				}
			});
			this.experimentSelector.setOptionListener(PathSelector.Option.OPEN, e -> Tools.debugPrint("TODO"));
			this.experimentSelector.setOptionListener(PathSelector.Option.SAVE, e -> Tools.debugPrint("TODO"));
			
			this.imageVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Layer imageLayer = MainPanel.this.getImageComponent().getLayers().get(0);
					final Painter imagePainter = imageLayer.getPainters().get(0);
					final boolean imageVisible = MainPanel.this.getImageVisibilitySelector().isSelected();
					
					if (!imageVisible) {
						imageLayer.getCanvas().clear(Color.GRAY);
					}
					
					imagePainter.getActive().set(imageVisible);
					imagePainter.getUpdateNeeded().set(true);
					
					MainPanel.this.getImageComponent().repaint();
				}
				
			});
			
			this.groundTruthVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Layer groundTruthLayer = MainPanel.this.getImageComponent().getLayers().get(1);
					final Painter groundTruthPainter = groundTruthLayer.getPainters().get(0);
					final boolean groundTruthVisible = MainPanel.this.getGroundTruthVisibilitySelector().isSelected();
					
					groundTruthPainter.getActive().set(groundTruthVisible);
					groundTruthPainter.getUpdateNeeded().set(true);
					
					MainPanel.this.getImageComponent().repaint();
				}
				
			});
			
			this.classificationVisibilitySelector.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					final Layer classificationLayer = MainPanel.this.getImageComponent().getLayers().get(2);
					final Painter classificationPainter = classificationLayer.getPainters().get(0);
					final boolean classificationVisible = MainPanel.this.getClassificationVisibilitySelector().isSelected();
					
					classificationPainter.getActive().set(classificationVisible);
					classificationPainter.getUpdateNeeded().set(true);
					
					MainPanel.this.getImageComponent().repaint();
				}
				
			});
			
			this.setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					final File file = SwingTools.getFiles(event).get(0);
					
					if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
						
					} else {
						context.setImageFile(file);
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 5442000733451223725L;
				
			});
			
			this.setPreferredSize(new Dimension(800, 600));
			
			context.setMainPanel(this);
		}
		
		public final Context getContext() {
			return this.context;
		}
		
		public final Experiment getExperiment() {
			return this.experiment;
		}
		
		public final void setExperiment(final Experiment experiment) {
			this.experiment = experiment;
			
			setModel(this.tree, experiment, "Experiment");
		}
		
		public final ImageComponent getImageComponent() {
			return this.imageComponent;
		}
		
		final void setImage(final String path) {
			this.imageComponent = new ImageComponent(awtRead(path));
			
			this.getImageComponent().addLayer().getPainters().add(new Painter.Abstract() {
				
				@Override
				public final void paint(final Canvas canvas) {
					canvas.getGraphics().drawImage(MainPanel.this.getContext().getGroundTruth().getImage(), 0, 0, null);
				}
				
				private static final long serialVersionUID = 4700895082820237288L;
				
			});
			
			this.getImageComponent().addLayer().getPainters().add(new Painter.Abstract() {
				
				@Override
				public final void paint(final Canvas canvas) {
					canvas.getGraphics().drawImage(MainPanel.this.getContext().getClassification().getImage(), 0, 0, null);
				}
				
				private static final long serialVersionUID = 7941391067177261093L;
				
			});
			
			this.setContents(this.getImageComponent());
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
		
		public final PathSelector getExperimentSelector() {
			return this.experimentSelector;
		}
		
		public final JTextField getTrainingTimeView() {
			return this.trainingTimeView;
		}
		
		public final JTextField getClassificationTimeView() {
			return this.classificationTimeView;
		}
		
		public final JCheckBox getClassificationVisibilitySelector() {
			return this.classificationVisibilitySelector;
		}
		
		public final JTextField getScoreView() {
			return this.scoreView;
		}
		
		public final void setContents(final Component component) {
			this.mainSplitPane.setRightComponent(scrollable(component));
		}
		
		private static final long serialVersionUID = 2173077945563031333L;
		
		public static final int IMAGE_SELECTOR_RESERVED_SLOTS = 2;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class Context implements Serializable {
		
		private MainPanel mainPanel;
		
		private Experiment experiment;
		
		private final Canvas groundTruth = new Canvas();
		
		private final Canvas classification = new Canvas();
		
		public final MainPanel getMainPanel() {
			return this.mainPanel;
		}
		
		public final void setMainPanel(final MainPanel mainPanel) {
			this.mainPanel = mainPanel;
		}
		
		public final File getExperimentFile() {
			return new File(this.getMainPanel().getExperimentSelector().getSelectedItem().toString());
		}
		
		public final String getGroundTruthName() {
			return this.getMainPanel().getGroundTruthSelector().getText();
		}
		
		public final Context setGroundTruthName(final String groundTruthName) {
			this.getMainPanel().getGroundTruthSelector().setFile(new File(groundTruthName));
			
			return this;
		}
		
		public final Experiment getExperiment() {
			return this.experiment;
		}
		
		public final Context setExperiment(final Experiment experiment) {
			this.experiment = experiment;
			
			return this;
		}
		
		public final BufferedImage getImage() {
			final MainPanel mainPanel = this.getMainPanel();
			final ImageComponent imageComponent = mainPanel == null ? null : mainPanel.getImageComponent();
			
			return imageComponent == null ? null : imageComponent.getImage();
		}

		public final Canvas getGroundTruth() {
			return this.groundTruth;
		}
		
		public final Context formatGroundTruth() {
			return format(this.getGroundTruth());
		}
		
		public final Canvas getClassification() {
			return this.classification;
		}
		
		public final String getExperimentName() {
			return baseName(this.getExperimentFile().getName());
		}
		
		public final File getImageFile() {
			return new File(this.getMainPanel().getImageSelector().getText());
		}
		
		public final String getGroundTruthPath() {
			return this.getGroundTruthPath(this.getGroundTruthName());
		}
		
		public final String getGroundTruthPath(final String name) {
			return baseName(this.getImageFile().getPath()) + "_groundtruth_" + name + ".png";
		}
		
		public final String getClassificationPath() {
			return baseName(this.getImageFile().getPath()) + "_classification_" + this.getGroundTruthName() + "_" + this.getExperimentName() + ".png";
		}
		
		public final void refreshGroundTruthAndClassification(final Refresh refresh) {
			final BufferedImage image = this.getImage();
			
			if (image == null) {
				return;
			}
			
			System.out.println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1));
			Tools.debugPrint(this.getGroundTruthName());
			
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			
			this.getGroundTruth().setFormat(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			this.getClassification().setFormat(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			
			switch (refresh) {
			case CLEAR:
				this.getGroundTruth().clear(CLEAR);
				this.getClassification().clear(CLEAR);
				break;
			case FROM_FILE:
			{
				{
					final String groundTruthPath = this.getGroundTruthPath();
					
					if (new File(groundTruthPath).isFile()) {
						this.getGroundTruth().getGraphics().drawImage(awtRead(groundTruthPath), 0, 0, null);
					} else {
						this.getGroundTruth().clear(CLEAR);
					}
				}
				{
					final String classificationPath = this.getClassificationPath();
					
					if (new File(classificationPath).isFile()) {
						this.getClassification().getGraphics().drawImage(awtRead(classificationPath), 0, 0, null);
					} else {
						this.getClassification().clear(CLEAR);
					}
				}
				break;
			}
			case NOP:
				break;
			}
		}
		
		public final Context setImageFile(final File imageFile) {
			System.out.println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, imageFile));
			
			final File oldImageFile = this.getImageFile();
			
			if (imageFile.isFile() && !imageFile.equals(oldImageFile)) {
				this.getMainPanel().setImage(imageFile.getPath());
				this.getMainPanel().getImageSelector().setFile(imageFile);
				
				this.refreshGroundTruthAndClassification(Refresh.FROM_FILE);
				
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
		
		public final Context setExperiment(final File experimentFile) {
			if (experimentFile.isFile()) {
				this.getMainPanel().getExperimentSelector().setPath(experimentFile.getPath());
				
				preferences.put(EXPERIMENT, experimentFile.getPath());
			}
			
			return this;
		}
		
		private final Context format(final Canvas canvas) {
			final BufferedImage image = this.getImage();
			
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
		
		/**
		 * @author codistmonk (creation 2015-02-17)
		 */
		public static enum Refresh {
			
			NOP, CLEAR, FROM_FILE;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	public static final class Experiment implements Serializable {
		
		private final List<ClassDescription> classDescriptions = new ArrayList<>();
		
		private final List<TrainingField> trainingFields = new ArrayList<>();
		
		@Override
		public final String toString() {
			return "Experiment";
		}
		
		@NestedList(name="classes", element="class", elementClass=ClassDescription.class)
		public final List<ClassDescription> getClassDescriptions() {
			return this.classDescriptions;
		}
		
		@NestedList(name="training", element="training field", elementClass=TrainingField.class)
		public final List<TrainingField> getTrainingFields() {
			return this.trainingFields;
		}
		
		private static final long serialVersionUID = -4539259556658072410L;
		
		/**
		 * @author codistmonk (creation 2015-02-16)
		 */
		public static final class ClassDescription implements Serializable {
			
			private String name = "class";
			
			private int label = 0xFF000000;
			
			@StringGetter
			@PropertyGetter("name")
			public final String getName() {
				return this.name;
			}
			
			@PropertySetter("name")
			public final ClassDescription setName(final String name) {
				this.name = name;
				
				return this;
			}
			
			public final int getLabel() {
				return this.label;
			}
			
			public final ClassDescription setLabel(final int label) {
				this.label = label;
				
				return this;
			}
			
			@PropertyGetter("label")
			public final String getLabelAsString() {
				return "#" + Integer.toHexString(this.getLabel()).toUpperCase(Locale.ENGLISH);
			}
			
			@PropertySetter("label")
			public final ClassDescription setLabel(final String labelAsString) {
				return this.setLabel((int) Long.parseLong(labelAsString.substring(1), 16));
			}
			
			private static final long serialVersionUID = 4974707407567297906L;
			
		}
		
		/**
		 * @author codistmonk (creation 2015-02-17)
		 */
		public static final class TrainingField implements Serializable {
			
			private String imagePath = "";
			
			private final Rectangle bounds = new Rectangle();
			
			@PropertyGetter("image")
			public final String getImagePath() {
				return this.imagePath;
			}
			
			@PropertySetter("image")
			public final TrainingField setImagePath(final String imagePath) {
				this.imagePath = imagePath;
				
				return this;
			}
			
			public final Rectangle getBounds() {
				return this.bounds;
			}
			
			@PropertyGetter("bounds")
			public final String getBoundsAsString() {
				return join(",", this.getBounds().x, this.getBounds().y, this.getBounds().width, this.getBounds().height);
			}
			
			@PropertySetter("bounds")
			public final TrainingField setBounds(final String boundsAsString) {
				final int[] bounds = Arrays.stream(boundsAsString.split(",")).mapToInt(Integer::parseInt).toArray();
				
				this.getBounds().setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
				
				return this;
			}
			
			@Override
			public final String toString() {
				return new File(this.getImagePath()).getName() + "[" + this.getBoundsAsString() + "]";
			}
			
			private static final long serialVersionUID = 847822079141878928L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-18)
	 */
	public static final class PathSelector extends JComboBox<Object> {
		
		private final Map<Option, ActionListener> optionListeners;
		
		private ActionListener pathListener;
		
		public PathSelector() {
			this.optionListeners = new LinkedHashMap<>();
			
			this.setRenderer(new DefaultListCellRenderer() {
				
				@Override
				public final Component getListCellRendererComponent(final JList<?> list,
						final Object value, final int index, final boolean isSelected,
						final boolean cellHasFocus) {
					final Component result = super.getListCellRendererComponent(
							list, value, index, isSelected, cellHasFocus);
					
					if (value instanceof Option) {
						this.setText(((Option) value).getTranslationKey());
					} else if (index < 0 || isSelected) {
						this.setText(new File(this.getText()).getName());
					}
					
					return result;
				}
				
				private static final long serialVersionUID = -3014056515590258107L;
				
			});
			
			this.addActionListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					PathSelector.this.action(event);
				}
				
			});
		}
		
		public final PathSelector setPath(final String path) {
			final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) this.getModel();
			
			model.insertElementAt(path, 0);
			
			for (int i = model.getSize() - this.optionListeners.size() - 1; 0 < i; --i) {
				if (model.getElementAt(i).equals(path)) {
					model.removeElementAt(i);
				}
			}
			
			SwingUtilities.invokeLater(() -> this.setSelectedIndex(0));
			
			return this;
		}
		
		public final PathSelector setPathListener(final ActionListener listener) {
			this.pathListener = listener;
			
			return this;
		}
		
		public final PathSelector setOptionListener(final Option option, final ActionListener listener) {
			final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) this.getModel();
			
			if (this.optionListeners.isEmpty()) {
				model.addElement("-");
			} else {
				this.optionListeners.remove(option);
				model.removeElement(option);
			}
			
			if (listener != null) {
				this.optionListeners.put(option, listener);
				model.addElement(option);
			}
			
			return this;
		}
		
		final void action(final ActionEvent event) {
			final Object selectedItem = this.getSelectedItem();
			
			if (selectedItem instanceof Option) {
				this.optionListeners.get(selectedItem).actionPerformed(event);
			} else if (selectedItem != null && this.pathListener != null) {
				this.pathListener.actionPerformed(event);
			}
			
			if (!this.optionListeners.isEmpty()
					&& this.getItemCount() - this.optionListeners.size() - 1 <= this.getSelectedIndex()) {
				this.setSelectedIndex(0);
			}
		}
		
		private static final long serialVersionUID = 2024380772192514052L;
		
		/**
		 * @author codistmonk (creation 2015-02-18)
		 */
		public static enum Option {
			
			NEW {
				
				@Override
				public final String getTranslationKey() {
					return "New...";
				}
				
			}, OPEN {
				
				@Override
				public final String getTranslationKey() {
					return "Open...";
				}
				
			}, SAVE {
				
				@Override
				public final String getTranslationKey() {
					return "Save";
				}
				
			}, DELETE {
				
				@Override
				public final String getTranslationKey() {
					return "Delete";
				}
				
			};
			
			public abstract String getTranslationKey();
			
		}
		
	}
	
}
