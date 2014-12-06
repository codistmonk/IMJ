package imj3.draft;

import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj2.draft.PaletteBasedHistograms;
import imj2.pixel3d.MouseHandler;
import imj2.tools.Canvas;
import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Pair;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class VisualSegmentation {
	
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
				final Serializable model = (Serializable) tree.getModel();
				
				synchronized (model) {
					Tools.writeObject(model, "palette.jo");
				}
			});
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
		
		window.addWindowListener(new WindowAdapter() {
			
			@Override
			public final void windowClosing(final WindowEvent event) {
				timer.stop();
			}
			
		});
	}
	
	public static final DefaultTreeModel loadModel() {
		try {
			return Tools.readObject("palette.jo");
		} catch (final Exception exception) {
			Tools.debugError(exception);
		}
		
		return new DefaultTreeModel(new PaletteRoot());
	}
	
	public static final JTree newPaletteTree() {
		final DefaultTreeModel treeModel = loadModel();
		final JTree result = new JTree(treeModel);
		
		result.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu rootPopup;
			
			private final JPopupMenu clusterPopup;
			
			private final JPopupMenu prototypePopup;
			
			private final PaletteNode[] currentNode;
			
			{
				this.rootPopup = new JPopupMenu();
				this.clusterPopup = new JPopupMenu();
				this.prototypePopup = new JPopupMenu();
				this.currentNode = new PaletteNode[1];
				
				this.rootPopup.add(newItem("Add cluster", e -> {
					final PaletteNode newNode = new PaletteCluster();
					
					newNode.setUserObject(new Pair<>("cluster", 1));
					
					treeModel.insertNodeInto(newNode, this.currentNode[0], this.currentNode[0].getChildCount());
				}));
				this.clusterPopup.add(newItem("Set cluster name", e -> {
					final Pair<String, Integer> pair = (Pair<String, Integer>) this.currentNode[0].getUserObject();
					final String newValue = JOptionPane.showInputDialog(result, "Cluster name:", pair.getFirst());
					
					if (newValue != null) {
						this.currentNode[0].setUserObject(new Pair<>(newValue, pair.getSecond()));
					}
				}));
				this.clusterPopup.add(newItem("Set patch size", e -> {
					final Pair<String, Integer> pair = (Pair<String, Integer>) this.currentNode[0].getUserObject();
					final String newValue = JOptionPane.showInputDialog(result, "Patch size:", pair.getSecond());
					
					if (newValue != null) {
						this.currentNode[0].setUserObject(new Pair<>(pair.getFirst(), Integer.parseInt(newValue)));
					}
				}));
				this.clusterPopup.add(newItem("Add prototype", e -> {
					final PaletteNode newNode = new PalettePrototype();
					
					newNode.setUserObject(new Color(0));
					
					treeModel.insertNodeInto(newNode, this.currentNode[0], this.currentNode[0].getChildCount());
				}));
				this.clusterPopup.add(newItem("Remove cluster", e -> {
					treeModel.removeNodeFromParent(this.currentNode[0]);
				}));
				this.prototypePopup.add(newItem("Remove prototype", e -> {
					treeModel.removeNodeFromParent(this.currentNode[0]);
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
					
					if (path != null) {
						final Object node = path.getLastPathComponent();
						this.currentNode[0] = cast(PaletteNode.class, node);
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
		final Canvas filtered = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final JLabel newView = new JLabel(new ImageIcon(filtered.getImage()));
		final JTree tree = getSharedProperty(mainFrame, "tree");
		final DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
		
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
						Tools.debugPrint();
						modelChanged.acquire();
						modelChanged.drainPermits();
						Tools.debugPrint();
					} catch (final InterruptedException exception) {
						break;
					}
					
					filtered.getGraphics().drawImage(image, 0, 0, null);
					
					final List<Color> prototypes = new ArrayList<>();
					final List<PaletteCluster> clusters = new ArrayList<>();
					
					{
						final PaletteCluster current[] = { null };
						
						forEachNodeIn((DefaultMutableTreeNode) treeModel.getRoot(), node -> {
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
						
						Tools.debugPrint(prototypes);
						Tools.debugPrint(clusters);
					}
					
					PaletteBasedHistograms.forEachPixelIn(image, (x, y) -> {
						// TODO
						return true;
					});
					
					newView.repaint();
					
					Tools.debugPrint(this.isInterrupted());
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
	
	public static final <N extends DefaultMutableTreeNode> boolean forEachNodeIn(final N root, final NodeProcessor<N> process) {
		final int n = root.getChildCount();
		
		if (n == 0) {
			return process.node(root);
		}
		
		if (!process.node(root)) {
			return false;
		}
		
		for (int i = 0; i < n; ++i) {
			if (!forEachNodeIn((N) root.getChildAt(i), process)) {
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
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static abstract class PaletteNode extends DefaultMutableTreeNode {
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5014476872249171076L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PaletteRoot extends PaletteNode {
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8341977047619165159L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PaletteCluster extends PaletteNode {
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -7107858305341502676L;
		
	}
	
	/**
	 * @author codistmonk (creation 2014-12-05)
	 */
	public static final class PalettePrototype extends PaletteNode {
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -856497652362830861L;
		
	}
	
}
