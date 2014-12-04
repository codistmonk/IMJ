package imj3.draft;

import static net.sourceforge.aprog.swing.SwingTools.scrollable;

import imj3.tools.AwtImage2D;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;

import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
		SwingUtilities.invokeLater(() -> {
			final JFrame mainFrame = new JFrame();
			final Component[] view = { null };
			final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode()));
			
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
	
}
