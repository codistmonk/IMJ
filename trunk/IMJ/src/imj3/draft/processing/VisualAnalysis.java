package imj3.draft.processing;

import static imj3.tools.AwtImage2D.awtRead;
import static imj3.tools.CommonSwingTools.limitHeight;
import static imj3.tools.CommonSwingTools.setModel;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.horizontalSplit;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.swing.SwingTools.verticalBox;
import static net.sourceforge.aprog.tools.Tools.append;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.join;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;import net.sourceforge.aprog.tools.Tools;

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
			}
			
		});
	}
	
	public static final Component label(final String text, final Component... components) {
		return limitHeight(horizontalBox(append(array(new JLabel(text), Box.createGlue()), components)));
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
	
	/**
	 * @author codistmonk (creation 2015-02-13)
	 */
	public static final class MainPanel extends JPanel {
		
		private final Context context;
		
		private final PathSelector imageSelector;
		
		private final JCheckBox imageVisibilitySelector;
		
		private final PathSelector groundTruthSelector;
		
		private final JCheckBox groundTruthVisibilitySelector;
		
		private final PathSelector experimentSelector;
		
		private final JTextField trainingTimeView;
		
		private final JTextField classificationTimeView;
		
		private final JCheckBox classificationVisibilitySelector;
		
		private final JTextField scoreView;
		
		private final JTree tree;
		
		private final JSplitPane mainSplitPane;
		
		private ImageComponent imageComponent;
		
		public MainPanel(final Context context) {
			super(new BorderLayout());
			
			this.context = context;
			this.imageSelector = new PathSelector();
			this.imageVisibilitySelector = new JCheckBox("", true);
			this.groundTruthSelector = new PathSelector();
			this.groundTruthVisibilitySelector = new JCheckBox();
			this.experimentSelector = new PathSelector();
			this.trainingTimeView = textView("-");
			this.classificationTimeView = textView("-");
			this.classificationVisibilitySelector = new JCheckBox();
			this.scoreView = textView("-");
			this.tree = new JTree();
			
			final int padding = this.imageVisibilitySelector.getPreferredSize().width;
			
			this.mainSplitPane = horizontalSplit(verticalBox(
					label(" Image: ", this.imageSelector, this.imageVisibilitySelector),
					label(" Ground truth: ", this.groundTruthSelector, this.groundTruthVisibilitySelector),
					label(" Experiment: ", this.experimentSelector, Box.createHorizontalStrut(padding)),
					label(" Training (s): ", this.trainingTimeView, Box.createHorizontalStrut(padding)),
					label(" Classification (s): ", this.classificationTimeView, this.classificationVisibilitySelector),
					label(" F1: ", this.scoreView, Box.createHorizontalStrut(padding)),
					centerX(new JButton("Confusion matrix...")),
					scrollable(this.tree)), scrollable(new JLabel("Drop file here")));
			
			setModel(this.tree, context.setExperiment(new Experiment()).getExperiment(), "Session");
			
			this.add(this.mainSplitPane, BorderLayout.CENTER);
			
			this.imageSelector.setOptionListener(PathSelector.Option.OPEN, e -> Tools.debugPrint("TODO"));
			this.imageSelector.setPathListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					MainPanel.this.setImage(MainPanel.this.getImageSelector().getSelectedItem().toString());
				}
				
			});
			
			this.groundTruthSelector.setPathListener(new ActionListener() {
				
				@Override
				public final void actionPerformed(final ActionEvent event) {
					context.refreshGroundTruthAndClassification(Refresh.FROM_FILE);
				}
				
			});
			this.groundTruthSelector.setOptionListener(PathSelector.Option.NEW, e -> {
				final String name = JOptionPane.showInputDialog("Ground truth name:");
				
				if (name != null) {
					context.setGroundTruth(name);
				}
			});
			this.groundTruthSelector.setOptionListener(PathSelector.Option.SAVE, e -> Tools.debugPrint("TODO"));
			
			this.experimentSelector.setOptionListener(PathSelector.Option.NEW, e -> Tools.debugPrint("TODO"));
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
		
		public final PathSelector getImageSelector() {
			return this.imageSelector;
		}
		
		public final JTree getTree() {
			return this.tree;
		}
		
		public final JCheckBox getImageVisibilitySelector() {
			return this.imageVisibilitySelector;
		}
		
		public final PathSelector getGroundTruthSelector() {
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
			return this.getMainPanel().getGroundTruthSelector().getSelectedItem().toString();
		}
		
		public final Context setGroundTruthName(final String groundTruthName) {
			this.getMainPanel().getGroundTruthSelector().setPath(groundTruthName);
			
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
		
		public final Canvas getClassification() {
			return this.classification;
		}
		
		public final String getExperimentName() {
			return baseName(this.getExperimentFile().getName());
		}
		
		public final File getImageFile() {
			return new File(this.getMainPanel().getImageSelector().getSelectedItem().toString());
		}
		
		public final String getGroundTruthPath() {
			return baseName(this.getImageFile().getPath()) + "_groundtruth_" + this.getGroundTruthName() + ".png";
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
				this.getMainPanel().getImageSelector().setPath(imageFile.getPath());
				
				this.refreshGroundTruthAndClassification(Refresh.FROM_FILE);
				
				preferences.put(IMAGE_PATH, imageFile.getPath());
			}
			
			return this;
		}
		
		public final Context setGroundTruth(final String name) {
			this.getMainPanel().getGroundTruthSelector().setPath(name);
			
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
			
			this.setSelectedIndex(0);
			
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
