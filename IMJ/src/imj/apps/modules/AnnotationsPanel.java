package imj.apps.modules;

import static imj.apps.modules.Plugin.fireUpdate;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static net.sourceforge.aprog.af.AFTools.item;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-26)
 */
public final class AnnotationsPanel extends JPanel {
	
	private final Context context;
	
	public AnnotationsPanel(final Context context) {
		super(new BorderLayout());
		this.context = context;
		
		final TreeNode treeNode = context.get("annotations");
		final JTree tree = new JTree(treeNode, false);
		
		for (int i = 0; i < tree.getRowCount(); ++i) {
			tree.expandRow(i);
		}
		
		this.add(tree, BorderLayout.CENTER);
		
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public final void valueChanged(final TreeSelectionEvent event) {
				context.set("selectedAnnotations", tree.getSelectionPaths());
				
				fireUpdate(context, "sieve");
			}
			
		});
		
		tree.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu popup;
			
			{
				this.popup = new JPopupMenu();
				
				this.popup.add(item("Use as ROI", context, ""));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (isRightMouseButton(event)) {
					final int row = tree.getClosestRowForLocation(event.getX(), event.getY());
					tree.setSelectionRow(row);
					this.popup.show(event.getComponent(), event.getX(), event.getY());
				}
			}
			
		});
	}
	
}
