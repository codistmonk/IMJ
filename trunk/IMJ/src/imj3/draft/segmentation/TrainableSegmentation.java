package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.a8r8g8b8;
import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.join;
import imj2.draft.PaletteBasedHistograms;
import imj2.pixel3d.MouseHandler;
import imj2.tools.Canvas;
import imj3.draft.KMeans;
import imj3.draft.segmentation.ImageComponent.Painter;
import imj3.draft.segmentation.QuantizerNode;
import imj3.tools.AwtImage2D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jgencode.primitivelists.IntList;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class TrainableSegmentation {
	
	private TrainableSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	public static final String PALETTE_XML = "palette.xml";
	
	static final Preferences preferences = Preferences.userNodeForPackage(TrainableSegmentation.class);
	
	static final Map<Object, Map<String, Object>> sharedProperties = new WeakHashMap<>();
	
	public static final void setSharedProperty(final Object object, final String key, final Object value) {
		sharedProperties.computeIfAbsent(object, o -> new HashMap<>()).put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getSharedProperty(final Object object, final String key) {
		return (T) sharedProperties.getOrDefault(object, Collections.emptyMap()).get(key);
	}
	
	public static final JMenuItem newItem(final String text, final ActionListener action) {
		final JMenuItem result = new JMenuItem(text);
		
		result.addActionListener(action);
		
		return result;
	}
	
	public static final void showEditDialog(final String title, final Runnable ifOkClicked, final Property... properties) {
		final JDialog dialog = new JDialog((Window) null, title);
		final Box box = Box.createVerticalBox();
		final Map<Property, Supplier<String>> newValues = new IdentityHashMap<>();
		
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		for (final Property property : properties) {
			final JTextField field = new JTextField("" + property.getGetter().get());
			
			box.add(horizontalBox(new JLabel(property.getName()), field));
			
			newValues.put(property, field::getText);
		}
		
		box.add(horizontalBox(
				Box.createHorizontalGlue(),
				new JButton(new AbstractAction("Ok") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						for (final Property property : properties) {
							property.getParser().apply(newValues.get(property).get());
						}
						
						dialog.dispose();
						
						ifOkClicked.run();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
				
				}),
				new JButton(new AbstractAction("Cancel") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						dialog.dispose();
					}
					
					private static final long serialVersionUID = -1250254465599248142L;
					
				})
		));
		
		dialog.add(box);
		
		SwingTools.packAndCenter(dialog).setVisible(true);
	}
	
	public static final Property property(final String name, final Supplier<?> getter,
			final Function<String, ?> parser) {
		return new Property(name, getter, parser);
	}
	
	public static final Iterable<int[]> cartesian(final int... minMaxes) {
		return new Iterable<int[]>() {
			
			@Override
			public final Iterator<int[]> iterator() {
				final int n = minMaxes.length / 2;
				
				return new Iterator<int[]>() {
					
					private int[] result;
					
					@Override
					public final boolean hasNext() {
						if (this.result == null) {
							this.result = new int[n];
							
							for (int i = 0; i < n; ++i) {
								this.result[i] = minMaxes[2 * i + 0];
							}
							
							--this.result[n - 1];
						}
						
						for (int i = 0; i < n; ++i) {
							if (this.result[i] < minMaxes[2 * i + 1]) {
								return true;
							}
						}
						
						return false;
					}
					
					@Override
					public final int[] next() {
						for (int i = n - 1; minMaxes[2 * i + 1] < ++this.result[i] && 0 < i; --i) {
							this.result[i] = minMaxes[2 * i + 0];
						}
						
						return this.result;
					}
					
				};
			}
			
		};
	}
	
	public static final Iterable<double[]> valuesAndWeights(final BufferedImage image, final IntList pixels, final int patchSize) {
		return new Iterable<double[]>() {
			
			@Override
			public final Iterator<double[]> iterator() {
				final int n = patchSize * patchSize;
				
				return new Iterator<double[]>() {
					
					private final int[] buffer = new int[n];
					
					private final double[] result = new double[n * 3 + 1];
					
					private int i = 0;
					
					{
						this.result[n * 3] = 1.0;
					}
					
					@Override
					public final boolean hasNext() {
						return this.i < pixels.size();
					}
					
					@Override
					public final double[] next() {
						final int pixel = pixels.get(this.i++);
						
						Quantizer.extractValues(image, pixel % image.getWidth(), pixel / image.getWidth(), patchSize, this.buffer);
						
						for (int i = 0; i < n; ++i) {
							final int rgb = this.buffer[i];
							this.result[3 * i + 0] = red8(rgb);
							this.result[3 * i + 1] = green8(rgb);
							this.result[3 * i + 2] = blue8(rgb);
						}
						
						return this.result;
					}
					
				};
			}
			
		};
	}
	
	public static final double f1(final int[][] confusionMatrix, final int i) {
		final int n = confusionMatrix.length;
		final int tp = confusionMatrix[i][i];
		int fp = 0;
		int fn = 0;
		
		for (int j = 0; j < n; ++j) {
			if (i != j) {
				fp += confusionMatrix[j][i];
				fn += confusionMatrix[i][j];
			}
		}
		
		return 2.0 * tp / (2.0 * tp + fp + fn);
	}
	
	@SuppressWarnings("unchecked")
	public static final <A> A deepCopy(final A array) {
		if (!array.getClass().isArray()) {
			return array;
		}
		
		final int n = Array.getLength(array);
		final Object result = Array.newInstance(array.getClass().getComponentType(), n);
		
		for (int i = 0; i < n; ++i) {
			Array.set(result, i, deepCopy(Array.get(array, i)));
		}
		
		return (A) result;
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
				final Component[] view = { null };
				final JComboBox<String> actionSelector = new JComboBox<>(array("Train and classify", "Classify"));
				final JLabel scoreView = new JLabel("------");
				final int[][][] confusionMatrix = { null };
				final JToggleButton showGroundtruthButton = new JToggleButton(new AbstractAction("Show ground truth") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final AtomicBoolean groundtruthUpdateNeeded = getSharedProperty(mainFrame, "groundtruthUpdateNeeded");
						groundtruthUpdateNeeded.set(true);
						view[0].repaint();
					}
					
				});
				final JToggleButton showSegmentsButton = new JToggleButton(new AbstractAction("Show segments") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final AtomicBoolean segmentsUpdateNeeded = getSharedProperty(mainFrame, "segmentsUpdateNeeded");
						segmentsUpdateNeeded.set(true);
						view[0].repaint();
					}
					
				});
				
				setSharedProperty(mainFrame, "showGroundtruthButton", showGroundtruthButton);
				setSharedProperty(mainFrame, "showSegmentsButton", showSegmentsButton);
				
				toolBar.add(showGroundtruthButton);
				toolBar.add(showSegmentsButton);
				toolBar.addSeparator();
				toolBar.add(actionSelector);
				toolBar.add(new JButton(new AbstractAction("Run") {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						final double[] bestScore = { 0.0 };
						final Quantizer quantizer = (Quantizer) tree.getModel().getRoot();
						final BufferedImage image = ((ImageComponent) getSharedProperty(mainFrame, "view")).getImage();
						final Canvas groundTruth = getSharedProperty(mainFrame, "groundtruth");
						final Canvas labels = getSharedProperty(mainFrame, "labels");
						final TicToc timer = new TicToc();
						
						if ("Train and classify".equals(actionSelector.getSelectedItem())) {
							Tools.debugPrint("Training...", new Date(timer.tic()));
							
							final int clusterCount = quantizer.getChildCount();
							final int[] minMaxes = new int[clusterCount * 2];
							final IntList[] classPixels = instances(clusterCount, IntList.FACTORY);
							final int[][] clusterings = new int[clusterCount][];
							
							for (int i = 0; i < clusterCount; ++i) {
								minMaxes[2 * i + 0] = 1;
								minMaxes[2 * i + 1] = ((QuantizerCluster) quantizer.getChildAt(i)).getMaximumPrototypeCount();
							}
							
							final BufferedImage groundtruthImage = groundTruth.getImage();
							final int imageWidth = image.getWidth();
							
							PaletteBasedHistograms.forEachPixelIn(groundtruthImage, (x, y) -> {
								final int label = groundtruthImage.getRGB(x, y);
								final QuantizerCluster cluster = quantizer.findCluster(label);
								
								if (cluster != null) {
									classPixels[quantizer.getIndex(cluster)].add(y * imageWidth + x);
								}
								
								return true;
							});
							
							Tools.debugPrint("Ground truth pixels collected in", timer.toc(), "ms");
							Tools.debugPrint("counts:", Arrays.stream(classPixels).map(IntList::size).toArray());
							
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
										final IntList pixels = classPixels[clusterIndex];
										final int n = pixels.size();
										final int prototypeCount = prototypeCounts[clusterIndex];
										clusterings[clusterIndex] = new int[n];
										
										for (int j = 0; j < n; ++j) {
											clusterings[clusterIndex][j] = j % prototypeCount;
										}
										
										final Iterable<double[]> points = valuesAndWeights(image, classPixels[clusterIndex], scale);
										double[][] means = null;
										
										for (int j = 0; j < 8; ++j) {
											means = new double[prototypeCount][scale * scale * 3];
											final double[] sizes = new double[prototypeCount];
											final int[] counts = new int[prototypeCount];
											
											KMeans.computeMeans(points, clusterings[clusterIndex], means, sizes, counts);
											KMeans.recluster(points, clusterings[clusterIndex], means);
										}
										
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
									
									Tools.debugPrint("Clustering done in", timer.toc(), "ms");
									Tools.debugPrint("Evaluation...", new Date(timer.tic()));
									
									final int[][] confusionMatrix = new int[clusterCount][clusterCount];
									
									for (int clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
										final int truthIndex = clusterIndex;
										
										classPixels[clusterIndex].forEach(pixel -> {
											final int x = pixel % imageWidth;
											final int y = pixel / imageWidth;
											final QuantizerCluster prediction = quantizer.quantize(image, x, y);
											final int predictionIndex = quantizer.getIndex(prediction);
											
											++confusionMatrix[truthIndex][predictionIndex];
											
											return true;
										});
									}
									
									final double score = score(confusionMatrix);
									
									if (bestScore[0] < score) {
										bestScore[0] = score;
										bestQuantizer = quantizer.copy();
									}
									
									Tools.debugPrint("Evaluation done in", timer.toc(), "ms");
								}
							}
							
							Tools.debugPrint("Training done in", timer.getTotalTime(), "ms");
							
							quantizer.set(bestQuantizer);
							
							((DefaultTreeModel) tree.getModel()).nodeStructureChanged(quantizer);
							mainFrame.validate();
						}
						
						Tools.debugPrint("Classifying...", new Date(timer.tic()));
						
						final int clusterCount = quantizer.getChildCount();
						confusionMatrix[0] = new int[clusterCount][clusterCount];
						scoreView.setText("---");
						final int[] referenceCount = new int[1];
						
						PaletteBasedHistograms.forEachPixelIn(image, (x, y) -> {
							final QuantizerCluster prediction = quantizer.quantize(image, x, y);
							
							labels.getImage().setRGB(x, y, prediction.getLabel());
							
							final QuantizerCluster truth = quantizer.findCluster(groundTruth.getImage().getRGB(x, y));
							
							if (truth != null) {
								++confusionMatrix[0][quantizer.getIndex(truth)][quantizer.getIndex(prediction)];
								++referenceCount[0];
							}
							
							return true;
						});
						
						Tools.debugPrint("Classifying done in", timer.toc(), "ms");
						
						if (0 < referenceCount[0]) {
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
				
				repeat(mainFrame, 30_000, e -> {
					final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					
					try (final OutputStream output = new FileOutputStream(PALETTE_XML)) {
						synchronized (model) {
							writePaletteXML((Quantizer) model.getRoot(), output);
							final File imageFile = getSharedProperty(mainFrame, "file");
							final File groundtruthFile = groundtruthFile(imageFile);
							Tools.debugPrint("Writing", groundtruthFile);
							ImageIO.write(((Canvas) getSharedProperty(mainFrame, "groundtruth")).getImage(),
									"png", groundtruthFile);
						}
					} catch (final IOException exception) {
						exception.printStackTrace();
					}
				});
			}
			
		});
	}
	
	public static final void repeat(final Window window, final int delay, final ActionListener action) {
		final Timer timer = new Timer(delay, action) {
			
			@Override
			public final void stop() {
				try {
					action.actionPerformed(null);
				} finally {
					super.stop();
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4028342893634389147L;
			
		};
		
		timer.setRepeats(true);
		timer.start();
		
		window.addWindowListener(new WindowAdapter() {
			
			@Override
			public final void windowClosing(final WindowEvent event) {
				timer.stop();
			}
			
		});
	}
	
	public static final File groundtruthFile(final File imageFile) {
		return new File(baseName(imageFile.getPath()) + "_groundtruth.png");
	}
	
	public static final void writePaletteXML(final Quantizer quantizer, final OutputStream output) {
		XMLTools.write(quantizer.accept(new ToXML()), output, 0);
	}
	
	/**
	 * @author codistmonk (creation 2015-01-19)
	 */
	public static final class InvertComposite implements Composite {
		
		@Override
		public final CompositeContext createContext(final ColorModel srcColorModel,
				final ColorModel dstColorModel, final RenderingHints hints) {
			return new CompositeContext() {
				
				@Override
				public final void dispose() {
					// NOP
				}
				
				@Override
				public final void compose(final Raster src, final Raster dstIn, final WritableRaster dstOut) {
					final Rectangle inBounds = dstIn.getBounds();
					final Rectangle outBounds = dstOut.getBounds();
					final int[] buffer = dstIn.getPixels(inBounds.x, inBounds.y, inBounds.width, inBounds.height, (int[]) null);
					int n = buffer.length;
					
					for (int i = 0; i < n; ++i) {
						if (((i + 1) % 4) != 0) {
							buffer[i] ^= ~0;
						}
					}
					
					dstOut.setPixels(outBounds.x, outBounds.y, outBounds.width, outBounds.height, buffer);
				}
				
			};
		}
		
	}

	/**
	 * @author codistmonk (creation 2015-01-18)
	 */
	public static final class ToXML implements QuantizerNode.Visitor<Node> {
		
		private final Document xml = XMLTools.newDocument();
		
		@Override
		public Element visit(final Quantizer quantizer) {
			final Element result = (Element) this.xml.appendChild(this.xml.createElement("palette"));
			final int n = quantizer.getChildCount();
			
			result.setAttribute("scale", quantizer.getScaleAsString());
			result.setAttribute("maximumScale", quantizer.getMaximumScaleAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerCluster) quantizer.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerCluster cluster) {
			final Element result = this.xml.createElement("cluster");
			final int n = cluster.getChildCount();
			
			result.setAttribute("name", cluster.getName());
			result.setAttribute("label", cluster.getLabelAsString());
			result.setAttribute("minimumSegmentSize", cluster.getMinimumSegmentSizeAsString());
			result.setAttribute("maximumSegmentSize", cluster.getMaximumSegmentSizeAsString());
			result.setAttribute("maximumPrototypeCount", cluster.getMaximumPrototypeCountAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerPrototype) cluster.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerPrototype prototype) {
			final Element result = this.xml.createElement("prototype");
			
			result.setTextContent(prototype.getDataAsString());
			
			return result;
		}
		
		private static final long serialVersionUID = -8012834350224027358L;
		
	}
	
	public static final BufferedImage newMaskFor(final BufferedImage image) {
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		
		{
			final Graphics2D g = result.createGraphics();
			
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		}
		
		return result;
	}
	
	public static final QuantizerCluster getSelectedCluster(final JTree tree) {
		final TreePath selectionPath = tree.getSelectionPath();
		
		return selectionPath == null ? null : cast(QuantizerCluster.class, selectionPath.getLastPathComponent());
	}
	
	public static final void outlineSegments(final BufferedImage segments, final BufferedImage labels,
			final BufferedImage mask, final BufferedImage image) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		PaletteBasedHistograms.forEachPixelIn(segments, (x, y) -> {
			if (mask == null || (mask.getRGB(x, y) & 1) != 0) {
				final int segmentId = segments.getRGB(x, y);
				
				if (0 != segmentId) {
					final int eastId = x + 1 < imageWidth ? segments.getRGB(x + 1, y) : segmentId;
					final int westId = 1 < x ? segments.getRGB(x - 1, y) : segmentId;
					final int southId = y + 1 < imageHeight ? segments.getRGB(x, y + 1) : segmentId;
					final int northId = 1 < y ? segments.getRGB(x, y - 1) : segmentId;
					
					if (edge(segmentId, eastId) || edge(segmentId, southId) || edge(segmentId, westId) || edge(segmentId, northId)) {
						image.setRGB(x, y, labels.getRGB(x, y));
					}
				}
			}
			
			return true;
		});
	}
	
	public static final boolean edge(final int segmentId1, final int segmentId2) {
		return segmentId1 != segmentId2;
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
		final BufferedImage image = AwtImage2D.awtRead(file.getPath());
		final Canvas filtered = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas groundtruth = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas labels = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		{
			final File groundtruthFile = groundtruthFile(file);
			
			try {
				groundtruth.getGraphics().drawImage(ImageIO.read(groundtruthFile), 0, 0, null);
			} catch (final IOException exception) {
				Tools.debugError(exception);
			}
		}
		
		setSharedProperty(mainFrame, "groundtruth", groundtruth);
		setSharedProperty(mainFrame, "labels", labels);
		setSharedProperty(mainFrame, "file", file);
		final BufferedImage mask = newMaskFor(image);
		final ImageComponent newView = new ImageComponent(filtered.getImage());
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
					canvas.getGraphics().drawImage(groundtruth.getImage(), 0, 0, null);
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
					outlineSegments(labels.getImage(), labels.getImage(), mask, canvas.getImage());
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
				final Composite savedComposite = groundtruth.getGraphics().getComposite();
				final Stroke savedStroke = groundtruth.getGraphics().getStroke();
				
				if (cluster == null) {
					groundtruth.getGraphics().setComposite(AlphaComposite.Clear);
				} else {
					groundtruth.getGraphics().setColor(new Color(cluster.getLabel()));
				}
				
				groundtruth.getGraphics().setStroke(new java.awt.BasicStroke(s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				groundtruth.getGraphics().drawLine(oldX, oldY, x, y);
				groundtruth.getGraphics().setStroke(savedStroke);
				
				if (cluster == null) {
					groundtruth.getGraphics().setComposite(savedComposite);
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
							Quantizer.extractValues(image, event.getX(), event.getY(),
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
		
		final Semaphore modelChanged = new Semaphore(1);
		final Thread updater = new Thread() {
			
			@Override
			public final void run() {
				while (!this.isInterrupted()) {
					try {
						modelChanged.acquire();
						modelChanged.drainPermits();
					} catch (final InterruptedException exception) {
						break;
					}
					
					filtered.getGraphics().drawImage(image, 0, 0, null);
					
					final TicToc timer = new TicToc();
//					final Map<Integer, List<Pair<Point, Integer>>> labelCells = extractCells(
//							file, image, mask, labels, segments.getImage(), (PaletteRoot) treeModel.getRoot());
//					
//					outlineSegments(segments.getImage(), labels.getImage(), null, filtered.getImage());
//					
//					Tools.debugPrint(labelCells.size());
					// TODO
					Tools.debugPrint(timer.toc());
					
					newView.repaint();
				}
			}
			
		};
		
		updater.start();
		
		tree.getModel().addTreeModelListener(new TreeModelListener() {
			
			@Override
			public final void treeStructureChanged(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesRemoved(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesInserted(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			@Override
			public final void treeNodesChanged(final TreeModelEvent event) {
				this.modelChanged(event);
			}
			
			private final void modelChanged(final TreeModelEvent event) {
				ignore(event);
				modelChanged.release();
				mainFrame.validate();
			}
			
		});
		
		newView.addHierarchyListener(new HierarchyListener() {
			
			@Override
			public final void hierarchyChanged(final HierarchyEvent event) {
				if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !newView.isShowing()) {
					updater.interrupt();
				}
			}
			
		});
		
		setSharedProperty(mainFrame, "view", newView);
		
		setView(mainFrame, view, scrollable(center(newView)), file.getName());
	}
	
	public static final JPanel center(final Component component) {
		final JPanel result = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.CENTER;
		
		result.add(component, c);
		
		return result;
	}
	
	public static final JTree newQuantizerTree() {
		final DefaultTreeModel treeModel = loadModel();
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
				
				this.rootPopup.add(newItem("Edit quantizer properties...", e -> {
					final Quantizer currentNode = (Quantizer) this.currentPath[0].getLastPathComponent();
					
					showEditDialog("Edit quantizer properties",
							() -> {
								treeModel.valueForPathChanged(this.currentPath[0], currentNode.renewUserObject());
								result.getRootPane().validate();
							},
							property("scale:", currentNode::getScale, currentNode::setScale),
							property("maximumScale:", currentNode::getMaximumScale, currentNode::setMaximumScale)
					);
				}));
				this.rootPopup.add(newItem("Add cluster", e -> {
					final Quantizer currentNode = (Quantizer) this.currentPath[0].getLastPathComponent();
					final QuantizerCluster newNode = new QuantizerCluster().setName("cluster").setLabel(1).setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(newItem("Edit cluster properties...", e -> {
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
				this.clusterPopup.add(newItem("Add prototype", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					final QuantizerNode newNode = new QuantizerPrototype().setUserObject();
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
					result.getRootPane().validate();
				}));
				this.clusterPopup.add(newItem("Remove cluster", e -> {
					final QuantizerCluster currentNode = (QuantizerCluster) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
					result.getRootPane().validate();
				}));
				this.prototypePopup.add(newItem("Remove prototype", e -> {
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
	
	public static final DefaultTreeModel loadModel() {
		try {
			return new DefaultTreeModel(fromXML(Tools.getResourceAsStream(PALETTE_XML)));
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		
		return new DefaultTreeModel(new Quantizer().setUserObject());
	}
	
	public static final Quantizer fromXML(final InputStream input) {
		return load(XMLTools.parse(input), new Quantizer().setUserObject());
	}
	
	public static final String select(final String string, final Object defaultValue) {
		return string.isEmpty() ? defaultValue.toString() : string;
	}
	
	public static final Quantizer load(final Document xml, final Quantizer result) {
		final Element paletteElement = (Element) XMLTools.getNode(xml, "palette");
		
		result.setScale(select(paletteElement.getAttribute("scale"), Quantizer.DEFAULT_SCALE));
		result.setMaximumScale(select(paletteElement.getAttribute("maximumScale"), Quantizer.DEFAULT_MAXIMUM_SCALE));
		result.removeAllChildren();
		
		for (final Node clusterNode : XMLTools.getNodes(xml, "palette/cluster")) {
			final Element clusterElement = (Element) clusterNode;
			final QuantizerCluster cluster = new QuantizerCluster()
				.setName(select(clusterElement.getAttribute("name"), QuantizerCluster.DEFAULT_NAME))
				.setLabel(select(clusterElement.getAttribute("label"), QuantizerCluster.DEFAULT_LABEL))
				.setMinimumSegmentSize(select(clusterElement.getAttribute("minimumSegmentSize"), QuantizerCluster.DEFAULT_MINIMUM_SEGMENT_SIZE))
				.setMaximumSegmentSize(select(clusterElement.getAttribute("maximumSegmentSize"), QuantizerCluster.DEFAULT_MAXIMUM_SEGMENT_SIZE))
				.setMaximumPrototypeCount(select(clusterElement.getAttribute("maximumPrototypeCount"), QuantizerCluster.DEFAULT_MAXIMUM_PROTOTYPE_COUNT))
				.setUserObject();
			
			result.add(cluster);
			
			for (final Node prototypeNode : XMLTools.getNodes(clusterNode, "prototype")) {
				final QuantizerPrototype prototype = new QuantizerPrototype();
				
				cluster.add(prototype);
				
				prototype.setData(prototypeNode.getTextContent()).setUserObject();
			}
		}
		
		return result;
	}
	
	public static final double score(final int[][] confusionMatrix) {
		final int n = confusionMatrix.length;
		double result = 1.0;
		
		for (int i = 0; i < n; ++i) {
			result *= f1(confusionMatrix, i);
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-01-14)
	 */
	public static final class Property implements Serializable {
		
		private final String name;
		
		private final Supplier<?> getter;
		
		private final Function<String, ?> parser;
		
		public Property(final String name, final Supplier<?> getter,
				final Function<String, ?> parser) {
			this.name = name;
			this.getter = getter;
			this.parser = parser;
		}
		
		public final String getName() {
			return this.name;
		}
		
		public final Supplier<?> getGetter() {
			return this.getter;
		}
		
		public final Function<String, ?> getParser() {
			return this.parser;
		}
		
		private static final long serialVersionUID = -6068768247605642711L;
		
	}
	
}
