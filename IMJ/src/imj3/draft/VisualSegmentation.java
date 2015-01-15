package imj3.draft;

import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.sourceforge.aprog.swing.SwingTools.horizontalBox;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.ignore;
import imj2.core.Image2D;
import imj2.draft.KMeans;
import imj2.draft.PaletteBasedHistograms;
import imj2.draft.PaletteBasedHistograms.Patch2DProcessor;
import imj2.pixel3d.MouseHandler;
import imj2.tools.AwtBackedImage;
import imj2.tools.Canvas;
import imj2.tools.IMJTools;
import imj3.core.Channels;
import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Pair;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class VisualSegmentation {
	
	public static final String PALETTE_XML = "palette.xml";
	
	private VisualSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(VisualSegmentation.class);
	
	static final Map<Object, Map<String, Object>> sharedProperties = new WeakHashMap<>();
	
	public static final void setSharedProperty(final Object object, final String key, final Object value) {
		sharedProperties.computeIfAbsent(object, o -> new HashMap<>()).put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public static final <T> T getSharedProperty(final Object object, final String key) {
		return (T) sharedProperties.getOrDefault(object, Collections.emptyMap()).get(key);
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		SwingTools.useSystemLookAndFeel();
		
		SwingUtilities.invokeLater(() -> {
			final JFrame mainFrame = new JFrame();
			final Component[] view = { null };
			final JTree tree = newPaletteTree();
			
			mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			mainFrame.setPreferredSize(new Dimension(512, 512));
			mainFrame.add(scrollable(tree), BorderLayout.WEST);
			
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
						writePaletteXML(model, output);
					}
				} catch (final IOException exception) {
					exception.printStackTrace();
				}
			});
		});
	}
	
	public static final void writePaletteXML(final DefaultTreeModel model, final OutputStream output) {
		XMLTools.write(((PaletteNode) model.getRoot()).accept(new ToXml()).getOwnerDocument(), output, 0);
	}
	
	/**
	 * @author codistmonk (creation 2015-01-14)
	 */
	public static final class ToXml implements PaletteNode.Visitor<Node> {
		
		private final Document xml = XMLTools.newDocument();
		
		@Override
		public final Element visit(final PaletteRoot root) {
			final Element result = (Element) this.xml.appendChild(this.xml.createElement("palette"));
			
			final int n = root.getChildCount();
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((PaletteNode) root.getChildAt(i)).accept(this)); 
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final PaletteCluster cluster) {
			final Element result = this.xml.createElement("cluster");
			
			result.setAttribute("name", cluster.getName());
			result.setAttribute("label", Integer.toString(cluster.getLabel()));
			result.setAttribute("minimumSegmentSize", Integer.toString(cluster.getMinimumSegmentSize()));
			result.setAttribute("maximumSegmentSize", Integer.toString(cluster.getMaximumSegmentSize()));
			
			final int n = cluster.getChildCount();
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((PaletteNode) cluster.getChildAt(i)).accept(this)); 
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final PalettePrototype prototype) {
			final Element result = this.xml.createElement("prototype");
			final Color color = (Color) prototype.getUserObject();
			
			result.appendChild(this.xml.createTextNode(Integer.toString(color.getRGB())));
			
			return result;
		}
		
		private static final long serialVersionUID = 1863579901467828030L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-11-14)
	 */
	public static abstract interface TreeProcessor extends Serializable {
		
		public default void beginTree(final TreeModel model) {
			ignore(model);
		}
		
		public default void beginNode(final Object node) {
			ignore(node);
		}
		
		public default void endNode(final Object node) {
			ignore(node);
		}
		
		public default void endTree(final TreeModel model) {
			ignore(model);
		}
		
		public static void process(final TreeModel model, final TreeProcessor processor) {
			processor.beginTree(model);
			process(model, model.getRoot(), processor);
			processor.endTree(model);
		}
		
		public static void process(final TreeModel model, final Object node, final TreeProcessor processor) {
			processor.beginNode(node);
			
			final int n = model.getChildCount(node);
			
			for (int i = 0; i < n; ++i) {
				final Object child = model.getChild(node, i);
				
				process(model, child, processor);
			}
			
			processor.endNode(node);
		}
		
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
	
	public static final DefaultTreeModel loadModel() {
		try {
			return new DefaultTreeModel(fromXML(Tools.getResourceAsStream(PALETTE_XML)));
		} catch (final Exception exception) {
			Tools.debugError(exception);
		}
		
		return new DefaultTreeModel(new PaletteRoot());
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
	
	public static final PaletteRoot fromXML(final InputStream input) {
		final Document xml = XMLTools.parse(input);
		final PaletteRoot result = new PaletteRoot();
		
		for (final Node clusterNode : XMLTools.getNodes(xml, "palette/cluster")) {
			final PaletteCluster cluster = new PaletteCluster()
				.setName(clusterNode.getAttributes().getNamedItem("name").getTextContent())
				.setLabel(parseInt(clusterNode.getAttributes().getNamedItem("label").getTextContent()))
				.setMinimumSegmentSize(parseInt(clusterNode.getAttributes().getNamedItem("minimumSegmentSize").getTextContent()))
				.setMaximumSegmentSize(parseInt(clusterNode.getAttributes().getNamedItem("maximumSegmentSize").getTextContent()))
				;
			
			result.add(cluster);
			
			for (final Node prototypeNode : XMLTools.getNodes(clusterNode, "prototype")) {
				final PalettePrototype prototype = new PalettePrototype();
				
				cluster.add(prototype);
				
				prototype.setUserObject(new Color(parseInt(prototypeNode.getTextContent())));
			}
		}
		
		return result;
	}
	
	public static final JTree newPaletteTree() {
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
				
				this.rootPopup.add(newItem("Add cluster", e -> {
					final PaletteNode currentNode = (PaletteNode) this.currentPath[0].getLastPathComponent();
					final PaletteNode newNode = new PaletteCluster().setName("cluster").setLabel(1);
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
				}));
				this.clusterPopup.add(newItem("Edit cluster properties...", e -> {
					final PaletteCluster currentNode = (PaletteCluster) this.currentPath[0].getLastPathComponent();
					showEditDialog("Edit cluster properties",
							() -> { treeModel.valueForPathChanged(this.currentPath[0], new Object()); },
							property("name:", currentNode::getName, currentNode::parseName),
							property("label:", currentNode::getLabelString, currentNode::parseLabel),
							property("minimumSegmentSize:", currentNode::getMinimumSegmentSize, currentNode::parseMinimumSegmentSize),
							property("maximumSegmentSize:", currentNode::getMaximumSegmentSize, currentNode::parseMaximumSegmentSize)
					);
				}));
				this.clusterPopup.add(newItem("Add prototype", e -> {
					final PaletteCluster currentNode = (PaletteCluster) this.currentPath[0].getLastPathComponent();
					final PaletteNode newNode = new PalettePrototype();
					
					newNode.setUserObject(new Color(0));
					
					treeModel.insertNodeInto(newNode, currentNode, currentNode.getChildCount());
				}));
				this.clusterPopup.add(newItem("Remove cluster", e -> {
					final PaletteCluster currentNode = (PaletteCluster) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
				}));
				this.prototypePopup.add(newItem("Remove prototype", e -> {
					final PalettePrototype currentNode = (PalettePrototype) this.currentPath[0].getLastPathComponent();
					treeModel.removeNodeFromParent(currentNode);
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
						
						if (node instanceof PaletteRoot) {
							popup = this.rootPopup;
						} else if (node instanceof PaletteCluster) {
							popup = this.clusterPopup;
						} else if (node instanceof PalettePrototype) {
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
	
	public static final JMenuItem newItem(final String text, final ActionListener action) {
		final JMenuItem result = new JMenuItem(text);
		
		result.addActionListener(action);
		
		return result;
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
		final BufferedImage mask = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		{
			final Graphics2D g = mask.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		}
		final Canvas labels = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas cells = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Canvas filtered = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final JLabel newView = new JLabel(new ImageIcon(filtered.getImage()));
		final JTree tree = getSharedProperty(mainFrame, "tree");
		final DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
		
		// XXX temporary fix for Java 8 defect on Mac OS X Lion with low graphic capabilities
		newView.setMaximumSize(new Dimension(1024, 1024));
		
		new MouseHandler(null) {
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (event.getClickCount() == 2) {
					final TreePath selectionPath = tree.getSelectionPath();
					
					if (selectionPath != null) {
						final PalettePrototype node = cast(PalettePrototype.class,
								selectionPath.getLastPathComponent());
						
						if (node != null) {
							tree.getModel().valueForPathChanged(selectionPath,
									new Color(image.getRGB(event.getX(), event.getY())));
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
					final Map<Integer, List<Pair<Point, Integer>>> labelCells = extractCells(
							file, image, mask, labels, cells, (PaletteRoot) treeModel.getRoot());
					
					outlineSegments(cells.getImage(), labels.getImage(), null, filtered.getImage());
					
					Tools.debugPrint(labelCells.size());
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
				tree.repaint();
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
		
		setView(mainFrame, view, scrollable(center(newView)), file.getName());
	}
	
	public static final Map<Integer, List<Pair<Point, Integer>>> extractCellsFromLabels(final BufferedImage labels,
			final String labelsName, final BufferedImage image, final BufferedImage mask,
			final PaletteRoot palette, final BufferedImage cells) {
		final Map<Integer, List<Pair<Point, Integer>>> result = new HashMap<>();
		final Map<Integer, PaletteCluster> labelClusters = new HashMap<>();
		
		for (final PaletteCluster cluster : Tools.<PaletteCluster>iterable(palette.children())) {
			labelClusters.put(cluster.getLabel(), cluster);
		}
		
		IMJTools.forEachPixelInEachComponent4(new AwtBackedImage(labelsName, labels), new Image2D.Process() {
			
			private Integer label = null;
			
			private final List<Point> pixels = new ArrayList<>();
			
			private final List<Integer> weights = new ArrayList<>();
			
			private int cellId;
			
			@Override
			public final void pixel(final int x, final int y) {
				if ((mask.getRGB(x, y) & 1) != 0) {
					this.label = labels.getRGB(x, y);
					this.pixels.add(new Point(x, y));
					this.weights.add(256 - Channels.Predefined.lightness(image.getRGB(x, y)));
				}
			}
			
			@Override
			public final void endOfPatch() {
				final int n = this.pixels.size();
				final PaletteCluster cluster = labelClusters.get(this.label);
				
				if (cluster == null) {
					return;
				}
				
				if (n < cluster.getMinimumSegmentSize()) {
					this.pixels.forEach(p -> cells.setRGB(p.x, p.y, 0));
				} else {
					final int cellSize = cluster.getMaximumSegmentSize();
					final int k = max(1, this.pixels.size() / cellSize);
					final int[] clustering = new int[n];
					final Point[] kMeans = Tools.instances(k, DefaultFactory.forClass(Point.class));
					final int[] kWeights = new int[k];
					final int[] kCounts = new int[k];
					
					for (int i = 0; i < n; ++i) {
						clustering[i] = k * i / n;
					}
					
					for (int i = 0; i < 16; ++i) {
						KMeans.computeMeans(this.pixels, this.weights, clustering, kMeans, kWeights, kCounts);
						KMeans.recluster(this.pixels, clustering, kMeans);
					}
					
					for (int i = 0; i < n; ++i) {
						final Point p = this.pixels.get(i);
						cells.setRGB(p.x, p.y, this.cellId + 1 + clustering[i]);
					}
					
					this.cellId += k;
					
					final List<Pair<Point, Integer>> cellProperties = result.computeIfAbsent(this.label, l -> new ArrayList<>());
					
					for (int i = 0; i < k; ++i) {
						cellProperties.add(new Pair<>(kMeans[i], kCounts[i]));
					}
				}
				
				this.label = null;
				this.pixels.clear();
				this.weights.clear();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -4862431243082713712L;
			
		});
		
		return result;
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
	
	public static final BufferedImage smootheLabels(final BufferedImage labels,
			final BufferedImage mask, final int patchSize) {
		final int imageWidth = labels.getWidth(); 
		final int imageHeight = labels.getHeight();
		final BufferedImage tmp = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		
		labels.copyData(tmp.getRaster());
		
		PaletteBasedHistograms.forEachConvolutionPixelIn(tmp, patchSize, new Patch2DProcessor() {
			
			private final Map<Integer, int[]> histogram = new HashMap<>();
			
			private int x, y;
			
			private final int[] best = new int[2];
			
			@Override
			public final boolean beginPatch(final int x, final int y) {
				if ((mask.getRGB(x, y) & 1) != 0) {
					this.x = x;
					this.y = y;
					this.best[0] = tmp.getRGB(x, y);
					this.best[1] = 0;
					this.histogram.clear();
					
					return true;
				}
				
				return false;
			}
			
			@Override
			public final boolean pixel(final int x, final int y) {
				final int rgb = tmp.getRGB(x, y);
				
				if (rgb != 0) {
					++this.histogram.computeIfAbsent(rgb, v -> new int[1])[0];
				}
				
				return true;
			}
			
			@Override
			public final boolean endPatch() {
				this.histogram.forEach((label, count) -> {
					if (this.best[COUNT] < count[0]) {
						this.best[LABEL] = label;
						this.best[COUNT] = count[0];
					}
				});
				
				final int rgb = tmp.getRGB(this.x, this.y);
				
				if (rgb != 0) {
					if (this.histogram.get(rgb)[0] < this.best[COUNT]) {
						labels.setRGB(this.x, this.y, this.best[LABEL]);
					}
				}
				
				return true;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -5117333476493324315L;
			
			public static final int LABEL = 0;
			
			public static final int COUNT = 1;
			
		});
		
		return tmp;
	}
	
	public static final void quantize(final BufferedImage image, final PaletteRoot root, final Canvas labels) {
		final List<Color> prototypes = new ArrayList<>();
		final List<PaletteCluster> clusters = new ArrayList<>();
		
		{
			final PaletteCluster current[] = { null };
			
			forEachNodeIn(root, node -> {
				final PaletteCluster cluster = cast(PaletteCluster.class, node);
				
				if (cluster != null) {
					current[0] = cluster;
				} else {
					final PalettePrototype prototype = cast(PalettePrototype.class, node);
					
					if (prototype != null) {
						prototypes.add((Color) prototype.getUserObject());
						clusters.add(current[0]);
					}
				}
				
				return true;
			});
		}
		
		final int k = clusters.size();
		
		PaletteBasedHistograms.forEachPixelIn(image, (x, y) -> {
			int bestDistance = Integer.MAX_VALUE;
			PaletteCluster cluster = null;
			
			for (int i = 0; i < k; ++i) {
				final Color c = prototypes.get(i);
				final int rgb = image.getRGB(x, y);
				int distance = abs(red8(rgb) - c.getRed())
						+ abs(green8(rgb) - c.getGreen()) + abs(blue8(rgb) - c.getBlue());
				
				if (distance < bestDistance) {
					bestDistance = distance;
					cluster = clusters.get(i);
				}
			}
			
			if (cluster != null) {
				labels.getImage().setRGB(x, y, cluster.getLabel());
			}
			
			return true;
		});
	}
	
	public static final boolean forEachNodeIn(final DefaultMutableTreeNode root, final NodeProcessor<DefaultMutableTreeNode> process) {
		final int n = root.getChildCount();
		
		if (n == 0) {
			return process.node(root);
		}
		
		if (!process.node(root)) {
			return false;
		}
		
		for (int i = 0; i < n; ++i) {
			if (!forEachNodeIn((DefaultMutableTreeNode) root.getChildAt(i), process)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * @author codistmonk (creation 2014-12-06)
	 *
	 * @param <N>
	 */
	public static abstract interface NodeProcessor<N extends DefaultMutableTreeNode> {
		
		public abstract boolean node(N node);
		
	}
	
	public static final JPanel center(final Component component) {
		final JPanel result = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.CENTER;
		
		result.add(component, c);
		
		return result;
	}
	
	public static Map<Integer, List<Pair<Point, Integer>>> extractCells(
			final File file, final BufferedImage image,
			final BufferedImage mask, final Canvas labels, final Canvas cells,
			final PaletteRoot palette) {
		labels.getGraphics().setColor(new Color(0, true));
		labels.getGraphics().fillRect(0, 0, labels.getWidth(), labels.getHeight());
		
		quantize(image, (PaletteRoot) palette.getRoot(), labels);
		smootheLabels(labels.getImage(), mask, 3);
		final Map<Integer, List<Pair<Point, Integer>>> labelCells = extractCellsFromLabels(
				labels.getImage(), file.getPath() + "_labels", image, mask, palette, cells.getImage());
		
		return labelCells;
	}

	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static abstract class PaletteNode extends DefaultMutableTreeNode {
		
		public abstract <V> V accept(Visitor<V> visitor);
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5014476872249171076L;
		
		/**
		 * @author codistmonk (creation 2015-01-14)
		 */
		public static abstract interface Visitor<V> extends Serializable {
			
			public abstract V visit(PaletteRoot root);
			
			public abstract V visit(PaletteCluster cluster);
			
			public abstract V visit(PalettePrototype prototype);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PaletteRoot extends PaletteNode {
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8341977047619165159L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PaletteCluster extends PaletteNode {
		
		private String name;
		
		private int label;
		
		private int minimumSegmentSize = 0;
		
		private int maximumSegmentSize = Integer.MAX_VALUE;
		
		public final String getName() {
			return this.name;
		}
		
		public final PaletteCluster setName(final String name) {
			this.name = name;
			
			return this;
		}
		
		public final PaletteCluster parseName(final String name) {
			return this.setName(name);
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final String getLabelString() {
			return "#" + Integer.toHexString(this.getLabel()).toUpperCase(Locale.ENGLISH);
		}
		
		public final PaletteCluster setLabel(final int label) {
			this.label = label;
			
			return this;
		}
		
		public final PaletteCluster parseLabel(final String label) {
			return this.setLabel(label.startsWith("#") ? (int) Long.parseLong(label.substring(1), 16) : parseInt(label));
		}
		
		public final int getMinimumSegmentSize() {
			return this.minimumSegmentSize;
		}
		
		public final PaletteCluster setMinimumSegmentSize(final int minimumSegmentSize) {
			this.minimumSegmentSize = minimumSegmentSize;
			
			return this;
		}
		
		public final PaletteCluster parseMinimumSegmentSize(final String minimumSegementSize) {
			return this.setMinimumSegmentSize(parseInt(minimumSegementSize));
		}
		
		public final int getMaximumSegmentSize() {
			return this.maximumSegmentSize;
		}
		
		public final PaletteCluster setMaximumSegmentSize(final int maximumSegmentSize) {
			this.maximumSegmentSize = maximumSegmentSize;
			
			return this;
		}
		
		public final PaletteCluster parseMaximumSegmentSize(final String maximumSegementSize) {
			return this.setMaximumSegmentSize(parseInt(maximumSegementSize));
		}
		
		@Override
		public final String toString() {
			return this.getName() + " (label: " + this.getLabelString() + " size: "
					+ this.getMinimumSegmentSize() + ".." + (this.getMaximumSegmentSize() == Integer.MAX_VALUE ? "" : this.getMaximumSegmentSize()) + ")";
		}
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -7107858305341502676L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PalettePrototype extends PaletteNode {
		
		@Override
		public final <V> V accept(final Visitor<V> visitor) {
			return visitor.visit(this);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -856497652362830861L;
		
	}
	
}
