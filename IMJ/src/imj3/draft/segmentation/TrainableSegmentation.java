package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.a8r8g8b8;
import static imj3.draft.segmentation.CommonSwingTools.*;
import static imj3.draft.segmentation.CommonTools.*;
import static imj3.draft.segmentation.SegmentationTools.*;
import static imj3.draft.segmentation.TrainableSegmentation.Context.context;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.join;

import imj2.draft.PaletteBasedHistograms;
import imj2.pixel3d.MouseHandler;
import imj2.tools.Canvas;
import imj2.tools.MultiThreadTools;

import imj3.draft.KMeans;
import imj3.draft.segmentation.ImageComponent.Painter;
import imj3.draft.segmentation.QuantizerNode;
import imj3.draft.segmentation.QuantizerNode.ToXML;
import imj3.tools.AwtImage2D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jgencode.primitivelists.IntList;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class TrainableSegmentation {
	
	private TrainableSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	public static final String DEFAULT_QUANTIZER_PATH = "quantizer.xml";
	
	static final Preferences preferences = Preferences.userNodeForPackage(TrainableSegmentation.class);
	
	public static final void write(final BufferedImage image, final String path) {
		Tools.getDebugOutput().println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, "Writing", path));
		
		try (final OutputStream output = new FileOutputStream(path)) {
			ImageIO.write(image, "png", output);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		
		SwingTools.useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final JFrame mainFrame = new JFrame();
				final JToolBar toolBar = new JToolBar();
				final JTree tree = newQuantizerTree();
				
				context(mainFrame).setQuantizer((Quantizer) tree.getModel().getRoot());
				
				final Component[] view = { null };
				final JComboBox<String> actionSelector = new JComboBox<>(array("Train and classify", "Classify"));
				final JLabel scoreView = new JLabel("--");
				final JLabel trainingTimeView = new JLabel("--");
				final JLabel classificationTimeView = new JLabel("--");
				final int[][][] confusionMatrix = { null };
				final JButton saveButton = new JButton(new AbstractAction("Save") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final Context context = context(mainFrame);
						
						writeXML(context.getQuantizer());
						write(context.getGroundTruth().getImage(), context.getGroundTruthPath());
						write(context.getClassification().getImage(), context.getClassificationPath());
					}
					
					private static final long serialVersionUID = -6680573992750711448L;
					
				});
				final JToggleButton showGroundtruthButton = new JToggleButton(new AbstractAction("Show ground truth") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final AtomicBoolean groundtruthUpdateNeeded = getSharedProperty(mainFrame, "groundtruthUpdateNeeded");
						groundtruthUpdateNeeded.set(true);
						view[0].repaint();
					}
					
					private static final long serialVersionUID = 8956800165218713075L;
					
				});
				final JToggleButton showSegmentsButton = new JToggleButton(new AbstractAction("Show segments") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final AtomicBoolean segmentsUpdateNeeded = getSharedProperty(mainFrame, "segmentsUpdateNeeded");
						segmentsUpdateNeeded.set(true);
						view[0].repaint();
					}
					
					private static final long serialVersionUID = -6729300010871284168L;
					
				});
				
				setSharedProperty(mainFrame, "showGroundtruthButton", showGroundtruthButton);
				setSharedProperty(mainFrame, "showSegmentsButton", showSegmentsButton);
				
				toolBar.add(saveButton);
				toolBar.addSeparator();
				toolBar.add(showGroundtruthButton);
				toolBar.add(showSegmentsButton);
				toolBar.addSeparator();
				toolBar.add(actionSelector);
				toolBar.add(new JButton(new AbstractAction("Run") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final Context context = context(mainFrame);
						final Quantizer quantizer = context.getQuantizer();
						final BufferedImage image = ((ImageComponent) getSharedProperty(mainFrame, "view")).getImage();
						final Canvas groundtruth = context.getGroundTruth();
						final Canvas classification = context.getClassification();
						final TicToc timer = new TicToc();
						
						if ("Train and classify".equals(actionSelector.getSelectedItem())) {
							Tools.debugPrint("Training...", new Date(timer.tic()));
							
							train(quantizer, image, groundtruth, timer);
							
							{
								final long trainingMilliseconds = timer.getTotalTime();
								
								Tools.debugPrint("Training done in", trainingMilliseconds, "ms");
								
								trainingTimeView.setText("" + trainingMilliseconds / 1000.0);
							}
							
							((DefaultTreeModel) tree.getModel()).nodeStructureChanged(quantizer);
							mainFrame.validate();
						}
						
						Tools.debugPrint("Classification...", new Date(timer.tic()));
						
						scoreView.setText("---");
						
						final int referenceCount = classify(image,
								quantizer, classification, groundtruth, confusionMatrix);
						
						{
							final long classificationMilliseconds = timer.toc();
							
							Tools.debugPrint("Classification done in", classificationMilliseconds, "ms");
							
							classificationTimeView.setText("" + classificationMilliseconds / 1000.0);
						}
						
						if (0 < referenceCount) {
							scoreView.setText((int) (100.0 * score(confusionMatrix[0])) + "%");
						}
						
						((AtomicBoolean) getSharedProperty(mainFrame, "segmentsUpdateNeeded")).set(true);
						view[0].repaint();
					}
					
					private static final long serialVersionUID = 7976765722450613823L;
					
				}));
				toolBar.addSeparator();
				toolBar.add(new JButton(new AbstractAction("Confusion matrix...") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final List<List<String>> table = new ArrayList<>();
						{
							final Quantizer quantizer = (Quantizer) tree.getModel().getRoot();
							final int n = quantizer.getChildCount();
							final List<String> header = new ArrayList<>(n + 1);
							
							header.add("<td></td>");
							
							for (int i = 0; i < n; ++i) {
								header.add("<td>" + ((QuantizerCluster) quantizer.getChildAt(i)).getName() + "</td>");
							}
							
							table.add(header);
							
							for (int i = 0; i < n; ++i) {
								final List<String> row = new ArrayList<>(n + 1);
								
								row.add(header.get(i + 1));
								
								for (int j = 0; j < n; ++j) {
									row.add("<td>" + confusionMatrix[0][i][j] + "</td>");
								}
								
								table.add(row);
							}
						}
						final JLabel confusionMatrixView = new JLabel("<html><body><table>"
								+ join("", table.stream().map(row -> "<tr>" + join("", row.stream().map(e -> "<td>" + e + "</td>").toArray()) + "</tr>\n" ).toArray())
								+ "</table></body></html>");
						SwingTools.show(confusionMatrixView, "Confusion matrix", false);
					}
					
					private static final long serialVersionUID = -148814823214636457L;
					
				}));
				toolBar.addSeparator();
				toolBar.add(new JLabel(" Training (s): "));
				toolBar.add(trainingTimeView);
				toolBar.addSeparator();
				toolBar.add(new JLabel(" Classification (s): "));
				toolBar.add(classificationTimeView);
				toolBar.addSeparator();
				toolBar.add(new JLabel(" F1: "));
				toolBar.add(scoreView);
				
				tree.addTreeExpansionListener(new TreeExpansionListener() {
					
					@Override
					public final void treeExpanded(final TreeExpansionEvent event) {
						mainFrame.validate();
					}
					
					@Override
					public final void treeCollapsed(final TreeExpansionEvent event) {
						mainFrame.validate();
					}
					
				});
				
				mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				
				mainFrame.setPreferredSize(new Dimension(512, 512));
				mainFrame.add(toolBar, BorderLayout.NORTH);
				mainFrame.add(scrollable(tree), BorderLayout.WEST);
				
				tree.addComponentListener(new ComponentAdapter() {
					
					@Override
					public final void componentResized(final ComponentEvent event) {
						if (256 < tree.getWidth()) {
							tree.getParent().setPreferredSize(new Dimension(256, tree.getParent().getPreferredSize().height));
							mainFrame.validate();
						} else {
							tree.getParent().setPreferredSize(null);
							mainFrame.validate();
						}
					}
					
				});
				
				setSharedProperty(mainFrame, "tree", tree);
				
				mainFrame.setDropTarget(new DropTarget() {
					
					@Override
					public final synchronized void drop(final DropTargetDropEvent event) {
						final File file = SwingTools.getFiles(event).get(0);
						
						setView(mainFrame, view, file);
						
						preferences.put("filePath", file.getPath());
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 5442000733451223725L;
					
				});
				
				SwingTools.packAndCenter(mainFrame).setVisible(true);
				
				{
					final String filePath = preferences.get("filePath", "");
					
					if (!filePath.isEmpty()) {
						setView(mainFrame, view, new File(filePath));
					}
				}
				
				repeat(mainFrame, 60_000, e -> {
					final Context context = context(mainFrame);
					
					try (final OutputStream output = new FileOutputStream(DEFAULT_QUANTIZER_PATH)) {
						synchronized (context) {
							writeXML(context.getQuantizer(), output);
							
							final File imageFile = context.getImageFile();
							
							if (context.isGroundTruthUnsaved()) {
								final File groundtruthFile = groundtruthFile(imageFile);
								
								Tools.debugPrint("Writing", groundtruthFile);
								ImageIO.write(((Canvas) getSharedProperty(mainFrame, "groundtruth")).getImage(),
										"png", groundtruthFile);
							}
							
							if (context.isClassificationUnsaved()) {
								final Canvas classification = (Canvas) getSharedProperty(mainFrame, "labels");
								
								if (classification != null) {
									final File classificationFile = classificationFile(imageFile);
									Tools.debugPrint("Writing", classificationFile);
									ImageIO.write(classification.getImage(), "png", classificationFile);
								}
							}
						}
					} catch (final IOException exception) {
						exception.printStackTrace();
					}
				});
			}
			
		});
	}
	
	public static final int classify(final BufferedImage image, final Quantizer quantizer,
			final Canvas classification, final Canvas groundtruth, final int[][][] confusionMatrix) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int clusterCount = quantizer.getChildCount();
		confusionMatrix[0] = new int[clusterCount][clusterCount];
		final int[] referenceCount = new int[1];
		final Collection<Future<?>> tasks = new ArrayList<>(imageHeight);
		
		for (int y0 = 0; y0 < imageHeight; ++y0) {
			final int y = y0;
			tasks.add(MultiThreadTools.getExecutor().submit(() -> {
				for (int x = 0; x < imageWidth; ++x) {
					classification.getImage().setRGB(x, y, quantizer.quantize(image, x, y).getLabel());
				}
			}));
		}
		
		MultiThreadTools.wait(tasks);
		
		final Map<Integer, Integer> labelIndices = new HashMap<>();
		
		for (int i = 0; i < clusterCount; ++i) {
			labelIndices.put(((QuantizerCluster) quantizer.getChildAt(i)).getLabel(), i);
		}
		
		PaletteBasedHistograms.forEachPixelIn(image, (x, y) -> {
			final int expected = groundtruth.getImage().getRGB(x, y);
			
			if (expected != 0) {
				final int predicted = classification.getImage().getRGB(x, y);
				
				++confusionMatrix[0][labelIndices.get(expected)][labelIndices.get(predicted)];
				++referenceCount[0];
			}
			
			return true;
		});
		
		return referenceCount[0];
	}
	
	public static final File groundtruthFile(final File imageFile) {
		return new File(baseName(imageFile.getPath()) + "_groundtruth.png");
	}
	
	public static final File classificationFile(final File imageFile) {
		return new File(baseName(imageFile.getPath()) + "_labels.png");
	}
	
	public static final void writeXML(final Quantizer quantizer) {
		Tools.getDebugOutput().println(Tools.debug(Tools.DEBUG_STACK_OFFSET + 1, "Writing", quantizer.getFilePath()));
		
		try (final OutputStream output = new FileOutputStream(quantizer.getFilePath())) {
			XMLTools.write(quantizer.accept(new ToXML()), output, 0);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final void writeXML(final Quantizer quantizer, final OutputStream output) {
		XMLTools.write(quantizer.accept(new ToXML()), output, 0);
	}
	
	public static final QuantizerCluster getSelectedCluster(final JTree tree) {
		final TreePath selectionPath = tree.getSelectionPath();
		
		return selectionPath == null ? null : cast(QuantizerCluster.class, selectionPath.getLastPathComponent());
	}
	
	public static final void setView(final JFrame frame, final Component[] view, final Component newView, final String title) {
		if (view[0] != null) {
			frame.remove(view[0]);
		}
		
		frame.add(view[0] = newView, BorderLayout.CENTER);
		frame.setTitle(title);
		frame.revalidate();
	}
	
	public static final void setView(final JFrame mainFrame, final Component[] view, final File file) {
		final Context context = context(mainFrame);
		final Canvas input = context.getInput();
		final Canvas groundTruth = context.getGroundTruth();
		final Canvas classification = context.getClassification();
		
		context.setImageFile(file);
		Tools.debugPrint();
		
		final BufferedImage mask = newMaskFor(input.getImage());
		final ImageComponent newView = new ImageComponent(input.getImage());
		final JTree tree = getSharedProperty(mainFrame, "tree");
		final int[] xys = { -1, 0, 16 };
		final AtomicBoolean groundtruthUpdateNeeded = new AtomicBoolean(true);
		final AtomicBoolean segmentsUpdateNeeded = new AtomicBoolean(true);
		final AtomicBoolean overlayUpdateNeeded = new AtomicBoolean(true);
		
		setSharedProperty(mainFrame, "groundtruthUpdateNeeded", groundtruthUpdateNeeded);
		setSharedProperty(mainFrame, "segmentsUpdateNeeded", segmentsUpdateNeeded);
		
		newView.addLayer().getPainters().add(new Painter() {
			
			@Override
			public final AtomicBoolean getUpdateNeeded() {
				return groundtruthUpdateNeeded;
			}
			
			@Override
			public final void paint(final Canvas canvas) {
				final JToggleButton showGroundtruthButton = getSharedProperty(mainFrame, "showGroundtruthButton");
				
				if (showGroundtruthButton.isSelected()) {
					final Composite saved = canvas.getGraphics().getComposite();
					canvas.getGraphics().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3F));
					canvas.getGraphics().drawImage(groundTruth.getImage(), 0, 0, null);
					canvas.getGraphics().setComposite(saved);
				}
			}
			
			private static final long serialVersionUID = -5052994161290677219L;
			
		});
		
		newView.addLayer().getPainters().add(new Painter() {
			
			@Override
			public final AtomicBoolean getUpdateNeeded() {
				return segmentsUpdateNeeded;
			}
			
			@Override
			public final void paint(final Canvas canvas) {
				final JToggleButton showSegmentsButton = getSharedProperty(mainFrame, "showSegmentsButton");
				
				if (showSegmentsButton.isSelected()) {
					outlineSegments(classification.getImage(), classification.getImage(), mask, canvas.getImage());
				}
			}
			
			private static final long serialVersionUID = -995036829480742335L;
			
		});
		
		newView.addLayer().getPainters().add(new Painter() {
			
			@Override
			public final AtomicBoolean getUpdateNeeded() {
				return overlayUpdateNeeded;
			}
			
			@Override
			public final void paint(final Canvas canvas) {
				final Graphics2D graphics = canvas.getGraphics();
				final QuantizerCluster cluster = getSelectedCluster(tree);
				
				if (0 <= xys[0]) {
					final int x = xys[0];
					final int y = xys[1];
					final int s = xys[2];
					final Composite savedComposite = graphics.getComposite();
					
					if (cluster == null) {
						graphics.setComposite(new InvertComposite());
					} else {
						graphics.setColor(new Color(cluster.getLabel()));
					}
					
					graphics.drawOval(x - s / 2, y - s / 2, s, s);
					
					if (cluster == null) {
						graphics.setComposite(savedComposite);
					}
				}
			}
			
			private static final long serialVersionUID = -891880936915736755L;
			
		});
		
		new MouseHandler(null) {
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouseDragged(event);
			}
			
			@Override
			public final void mouseDragged(final MouseEvent event) {
				final int oldX = xys[0];
				final int oldY = xys[1];
				final int x = xys[0] = event.getX();
				final int y = xys[1] = event.getY();
				final int s = xys[2];
				final QuantizerCluster cluster = getSelectedCluster(tree);
				final Composite savedComposite = groundTruth.getGraphics().getComposite();
				final Stroke savedStroke = groundTruth.getGraphics().getStroke();
				
				if (cluster == null) {
					groundTruth.getGraphics().setComposite(AlphaComposite.Clear);
				} else {
					groundTruth.getGraphics().setColor(new Color(cluster.getLabel()));
				}
				
				groundTruth.getGraphics().setStroke(new java.awt.BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				groundTruth.getGraphics().drawLine(oldX, oldY, x, y);
				groundTruth.getGraphics().setStroke(savedStroke);
				
				if (cluster == null) {
					groundTruth.getGraphics().setComposite(savedComposite);
				}
				
				groundtruthUpdateNeeded.set(true);
				newView.repaint();
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				xys[0] = event.getX();
				xys[1] = event.getY();
				overlayUpdateNeeded.set(true);
				newView.repaint();
			}
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (event.getWheelRotation() < 0) {
					xys[2] = Math.max(1, xys[2] - 1);
				} else {
					++xys[2];
				}
				overlayUpdateNeeded.set(true);
				newView.repaint();
			}
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				xys[0] = -1;
				overlayUpdateNeeded.set(true);
				newView.repaint();
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.getClickCount() == 2) {
					final TreePath selectionPath = tree.getSelectionPath();
					
					if (selectionPath != null) {
						final QuantizerPrototype node = cast(QuantizerPrototype.class,
								selectionPath.getLastPathComponent());
						
						if (node != null) {
							Quantizer.extractValues(input.getImage(), event.getX(), event.getY(),
									node.getQuantizer().getScale(), node.getData());
							tree.getModel().valueForPathChanged(selectionPath, node.renewUserObject());
							mainFrame.validate();
						}
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 6325908040300578842L;
			
		}.addTo(newView);
		
		setSharedProperty(mainFrame, "view", newView);
		
		setView(mainFrame, view, scrollable(center(newView)), file.getName());
	}
	
	public static final JTree newQuantizerTree() {
		final DefaultTreeModel treeModel = loadQuantizer();
		final JTree result = new JTree(treeModel);
		
		result.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu rootPopup;
			
			private final JPopupMenu clusterPopup;
			
			private final JPopupMenu prototypePopup;
			
			private final TreePath[] currentPath;
			
			{
				this.rootPopup = new JPopupMenu();
				this.clusterPopup = new JPopupMenu();
				this.prototypePopup = new JPopupMenu();
				this.currentPath = new TreePath[1];
				
				this.rootPopup.add(item("Edit quantizer properties...", e -> {
					final Quantizer currentNode = (Quantizer) this.currentPath[0].getLastPathComponent();
					
					showEditDialog("Edit quantizer properties",
							() -> {
								treeModel.valueForPathChanged(this.currentPath[0], currentNode.renewUserObject());
								result.getRootPane().validate();
							},
							property("file:", currentNode::getFilePath, currentNode::setFilePath),
							property("scale:", currentNode::getScale, currentNode::setScale),
							property("maximumScale:", currentNode::getMaximumScale, currentNode::setMaximumScale)
					);
				}));
				this.rootPopup.add(item("Add cluster", e -> {
					final Quantizer currentNode = (Quantizer) this.currentPath[0].getLastPathComponent();
					final QuantizerCluster newNode = new QuantizerCluster().setName("cluster").setLabel(1).setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(item("Edit cluster properties...", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					
					showEditDialog("Edit cluster properties",
							() -> {
								treeModel.valueForPathChanged(this.currentPath[0], currentNode.renewUserObject());
								result.getRootPane().validate();
							},
							property("name:", currentNode::getName, currentNode::setName),
							property("label:", currentNode::getLabelAsString, currentNode::setLabel),
							property("minimumSegmentSize:", currentNode::getMinimumSegmentSize, currentNode::setMinimumSegmentSize),
							property("maximumSegmentSize:", currentNode::getMaximumSegmentSize, currentNode::setMaximumSegmentSize),
							property("maximumPrototypeCount:", currentNode::getMaximumPrototypeCount, currentNode::setMaximumPrototypeCount)
					);
				}));
				this.clusterPopup.add(item("Add prototype", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					final QuantizerNode newNode = new QuantizerPrototype().setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(item("Remove cluster", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
					result.getRootPane().validate();
				}));
				this.prototypePopup.add(item("Remove prototype", e -> {
					final QuantizerPrototype currentNode = (QuantizerPrototype) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
					result.getRootPane().validate();
				}));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			@Override
			public final void mouseReleased(final MouseEvent event) {
				this.mouseUsed(event);
			}
			
			private final void mouseUsed(final MouseEvent event) {
				if (event.isPopupTrigger()) {
					final TreePath path = result.getPathForLocation(event.getX(), event.getY());
					this.currentPath[0] = path;
					
					if (path != null) {
						final Object node = path.getLastPathComponent();
						JPopupMenu popup = null;
						
						if (node instanceof Quantizer) {
							popup = this.rootPopup;
						} else if (node instanceof QuantizerCluster) {
							popup = this.clusterPopup;
						} else if (node instanceof QuantizerPrototype) {
							popup = this.prototypePopup;
						}
						
						if (popup != null) {
							popup.show(event.getComponent(), event.getX(), event.getY());
						}
					}
				}
			}
			
		});
		
		return result;
	}
	
	public static final DefaultTreeModel loadQuantizer() {
		final String quantizerPath = preferences.get("quantizerFile", DEFAULT_QUANTIZER_PATH);
		
		try {
			final Quantizer quantizer = QuantizerNode.fromXML(getResourceAsStream(quantizerPath))
					.setFilePath(quantizerPath);
			
			return new DefaultTreeModel(quantizer);
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		
		return new DefaultTreeModel(new Quantizer().setFilePath(quantizerPath).setUserObject());
	}
	
	public static final IntList[] collectGroundtruthPixels(final BufferedImage groundtruthImage, final Quantizer quantizer) {
		final int imageWidth = groundtruthImage.getWidth();
		final int clusterCount = quantizer.getChildCount();
		final IntList[] result = instances(clusterCount, IntList.FACTORY);
		
		PaletteBasedHistograms.forEachPixelIn(groundtruthImage, (x, y) -> {
			final int label = groundtruthImage.getRGB(x, y);
			final QuantizerCluster cluster = quantizer.findCluster(label);
			
			if (cluster != null) {
				result[quantizer.getIndex(cluster)].add(y * imageWidth + x);
			}
			
			return true;
		});
		
		return result;
	}
	
	public static final void setPrototypes(final QuantizerCluster cluster, final double[][] means) {
		final int prototypeCount = means.length;
		
		cluster.removeAllChildren();
		
		for (int j = 0; j < prototypeCount; ++j) {
			final QuantizerPrototype prototype = new QuantizerPrototype();
			
			cluster.add(prototype);
			
			final double[] prototypeElements = means[j];
			final int[] data = prototype.getData();
			
			for (int k = 0; k < data.length; ++k) {
				data[k] = a8r8g8b8(0xFF,
						(int) prototypeElements[3 * k + 0],
						(int) prototypeElements[3 * k + 1],
						(int) prototypeElements[3 * k + 2]);
			}
		}
	}
	
	public static final void train(final Quantizer quantizer, final BufferedImage image, final Canvas groundtruth, final TicToc timer) {
		final int imageWidth = image.getWidth();
		final int clusterCount = quantizer.getChildCount();
		final int[] minMaxes = new int[clusterCount * 2];
		final int[][] clusterings = new int[clusterCount][];
		final double[] bestScore = { 0.0 };
		
		for (int i = 0; i < clusterCount; ++i) {
			minMaxes[2 * i + 0] = 1;
			minMaxes[2 * i + 1] = ((QuantizerCluster) quantizer.getChildAt(i)).getMaximumPrototypeCount();
		}
		
		final BufferedImage groundtruthImage = groundtruth.getImage();
		final IntList[] groundtruthPixels = collectGroundtruthPixels(groundtruthImage, quantizer);
		
		Tools.debugPrint("Ground truth pixels collected in", timer.toc(), "ms");
		Tools.debugPrint("counts:", Arrays.toString(Arrays.stream(groundtruthPixels).map(IntList::size).toArray()));
		
		Quantizer bestQuantizer = quantizer.copy();
		bestScore[0] = 0.0;
		
		for (int scale = 1; scale <= quantizer.getMaximumScale(); ++scale) {
			Tools.debugPrint("scale:", scale);
			
			quantizer.setScale(scale);
			
			for (final int[] prototypeCounts : cartesian(minMaxes)) {
				Tools.debugPrint("prototypes:", Arrays.toString(prototypeCounts));
				Tools.debugPrint("Clustering...", new Date(timer.tic()));
				
				for (int clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
					final QuantizerCluster cluster = (QuantizerCluster) quantizer.getChildAt(clusterIndex);
					final IntList pixels = groundtruthPixels[clusterIndex];
					final int n = pixels.size();
					final int prototypeCount = prototypeCounts[clusterIndex];
					clusterings[clusterIndex] = new int[n];
					
					for (int j = 0; j < n; ++j) {
						clusterings[clusterIndex][j] = j % prototypeCount;
					}
					
					final Iterable<double[]> points = valuesAndWeights(image, groundtruthPixels[clusterIndex], scale);
					double[][] means = null;
					
					for (int j = 0; j < 8; ++j) {
						means = new double[prototypeCount][scale * scale * 3];
						final double[] sizes = new double[prototypeCount];
						final int[] counts = new int[prototypeCount];
						
						KMeans.computeMeans(points, clusterings[clusterIndex], means, sizes, counts);
						
						if (prototypeCount == 1) {
							break;
						}
						
						KMeans.recluster(points, clusterings[clusterIndex], means);
					}
					
					setPrototypes(cluster, means);
				}
				
				Tools.debugPrint("Clustering done in", timer.toc(), "ms");
				Tools.debugPrint("Evaluation...", new Date(timer.tic()));
				
				final int[][] confusionMatrix = new int[clusterCount][clusterCount];
				
				for (int clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
					final int truthIndex = clusterIndex;
					
					groundtruthPixels[clusterIndex].forEach(pixel -> {
						final int x = pixel % imageWidth;
						final int y = pixel / imageWidth;
						final QuantizerCluster prediction = quantizer.quantize(image, x, y);
						final int predictionIndex = quantizer.getIndex(prediction);
						
						++confusionMatrix[truthIndex][predictionIndex];
						
						return true;
					});
				}
				
				final double score = score(confusionMatrix);
				
				if (bestScore[0] * 1.01 <= score) {
					bestScore[0] = score;
					bestQuantizer = quantizer.copy();
					
					Tools.debugPrint("bestScore:", score);
				}
				
				Tools.debugPrint("Evaluation done in", timer.toc(), "ms");
			}
		}
		
		quantizer.set(bestQuantizer);
	}

	/**
	 * @author codistmonk (creation 2015-02-02)
	 */
	public static final class Context implements Serializable {
		
		private Quantizer quantizer;
		
		private File imageFile;
		
		private final Canvas input;
		
		private final Canvas groundTruth;
		
		private boolean groundTruthUnsaved;
		
		private final Canvas classification;
		
		private boolean classificationUnsaved;
		
		public Context(final Object object) {
			this.input = new Canvas();
			this.groundTruth = new Canvas();
			this.classification = new Canvas();
			
			setSharedProperty(object, KEY, this);
		}
		
		public final Context resetData() {
			if (this.getImageFile() != null) {
				this.reset(this.getImageFile().getPath(), this.getInput());
				
				{
					final int width = this.getInput().getWidth();
					final int height = this.getInput().getHeight();
					final Color clearColor = new Color(0, true);
					
					this.getGroundTruth().setFormat(width, height, BufferedImage.TYPE_INT_ARGB).clear(clearColor);
					this.getClassification().setFormat(width, height, BufferedImage.TYPE_INT_ARGB).clear(clearColor);
				}
				
				if (this.getQuantizer() != null) {
					this.reset(this.getGroundTruthPath(), this.getGroundTruth());
					this.reset(this.getClassificationPath(), this.getClassification());
				}
			} else {
				this.getInput().setFormat(1, 1, BufferedImage.TYPE_INT_ARGB);
				this.getGroundTruth().setFormat(1, 1, BufferedImage.TYPE_INT_ARGB);
				this.getClassification().setFormat(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
			
			return this;
		}
		
		public final String getImageBasePath() {
			return baseName(this.getImageFile().getPath());
		}
		
		public final String getGroundTruthPath() {
			return this.getImageBasePath() + "_" + this.getQuantizer().getName() + "_groundtruth.png";
		}
		
		public final String getClassificationPath() {
			return this.getImageBasePath() + "_" + this.getQuantizer().getName() + "_classification.png";
		}
		
		public final Quantizer getQuantizer() {
			return this.quantizer;
		}
		
		public final Context setQuantizer(final Quantizer quantizer) {
			this.quantizer = quantizer;
			
			return this.resetData();
		}
		
		public final File getImageFile() {
			return this.imageFile;
		}
		
		public final Context setImageFile(final File imageFile) {
			this.imageFile = imageFile;
			
			return this.resetData();
		}
		
		public final Canvas getInput() {
			return this.input;
		}
		
		public final Canvas getGroundTruth() {
			return this.groundTruth;
		}
		
		public final boolean isGroundTruthUnsaved() {
			return this.groundTruthUnsaved;
		}
		
		public final Context setGroundTruthUnsaved(final boolean groundTruthUnsaved) {
			this.groundTruthUnsaved = groundTruthUnsaved;
			
			return this;
		}
		
		public final Canvas getClassification() {
			return this.classification;
		}
		
		public final boolean isClassificationUnsaved() {
			return this.classificationUnsaved;
		}
		
		public final Context setClassificationUnsaved(final boolean classificationUnsaved) {
			this.classificationUnsaved = classificationUnsaved;
			
			return this;
		}
		
		private final Context reset(final String imagePath, final Canvas canvas) {
			if (new File(imagePath).exists()) {
				final BufferedImage image = AwtImage2D.awtRead(imagePath);
				
				canvas.setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB)
					.getGraphics().drawImage(image, 0, 0, null);
				
				if (canvas == this.getGroundTruth()) {
					this.setGroundTruthUnsaved(false);
				} else if (canvas == this.getClassification()) {
					this.setClassificationUnsaved(false);
				}
			}
			
			return this;
		}
		
		private static final long serialVersionUID = -252741989094974406L;
		
		public static final String KEY = "context";
		
		public static final Context context(final Object object) {
			return getSharedProperty(object, KEY, k -> new Context(object));
		}
		
	}
	
}
