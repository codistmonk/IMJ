package imj3.draft;

import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Pair;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class VisualSegmentation {
	
	private VisualSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(VisualSegmentation.class);
	
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
		});
	}
	
	public static final JTree newPaletteTree() {
		final DefaultTreeModel treeModel = new DefaultTreeModel(new PaletteRoot());
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
		setView(mainFrame, view, scrollable(new JLabel(new ImageIcon(AwtImage2D.awtRead(file.getPath())))), file.getName());
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
