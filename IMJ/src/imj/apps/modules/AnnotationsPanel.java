package imj.apps.modules;

import static imj.apps.modules.Plugin.fireUpdate;
import static imj.apps.modules.ShowActions.ACTIONS_USE_ANNOTATION_AS_ROI;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static net.sourceforge.aprog.af.AFTools.item;

import imj.apps.modules.Annotations.Annotation;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
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
				
				this.popup.add(item("Use as ROI", context, ACTIONS_USE_ANNOTATION_AS_ROI));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (isRightMouseButton(event)) {
					final int row = tree.getRowForLocation(event.getX(), event.getY());
					
					if (0 <= row && tree.getPathForRow(row).getLastPathComponent() instanceof Annotation) {
						tree.setSelectionRow(row);
						this.popup.show(event.getComponent(), event.getX(), event.getY());
					}
				}
			}
			
		});
	}
	
}
