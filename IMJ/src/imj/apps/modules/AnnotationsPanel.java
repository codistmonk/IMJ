package imj.apps.modules;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-26)
 */
public final class AnnotationsPanel extends JPanel {
	
	private final Context context;
	
	public AnnotationsPanel(final Context context) {
		super(new BorderLayout());
		this.context = context;
		
		final TreeNode treeNode = context.get("annotations");
		final JTree tree = new JTree(treeNode, true);
		
		for (int i = 0; i < tree.getRowCount(); ++i) {
			tree.expandRow(i);
		}
		
		this.add(tree, BorderLayout.CENTER);
	}
	
}
