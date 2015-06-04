package imj.apps.modules;

import static imj.apps.modules.ShowActions.ACTIONS_DELETE_ANNOTATION;
import static imj.apps.modules.ShowActions.ACTIONS_PICK_ANNOTATION_COLOR;
import static imj.apps.modules.ShowActions.ACTIONS_TOGGLE_ANNOTATION_VISIBILITY;
import static imj.apps.modules.ShowActions.ACTIONS_USE_ANNOTATION_AS_ROI;
import static javax.swing.SwingUtilities.isRightMouseButton;
import static multij.af.AFTools.fireUpdate;
import static multij.af.AFTools.item;
import static multij.tools.Tools.cast;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import multij.context.Context;
import multij.events.Variable.Listener;
import multij.events.Variable.ValueChangedEvent;

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
				final TreePath[] selectionPaths = tree.getSelectionPaths();
				final Annotations annotations = context.get("annotations");
				
				context.set("selectedAnnotations", selectionPaths);
				
				for (final Annotation annotation : annotations.getAnnotations()) {
					annotation.setSelected(false);
				}
				
				if (selectionPaths != null) {
					for (final TreePath path : selectionPaths) {
						final Annotation annotation = cast(Annotation.class, path.getLastPathComponent());
						
						if (annotation != null) {
							annotation.setSelected(true);
						}
					}
				}
				
				fireUpdate(context, "sieve");
			}
			
		});
		
		tree.addMouseListener(new MouseAdapter() {
			
			private final JPopupMenu annotationPopup;
			
			private final JPopupMenu regionPopup;
			
			{
				this.annotationPopup = new JPopupMenu();
				this.regionPopup = new JPopupMenu();
				
				this.annotationPopup.add(item("Use as ROI", context, ACTIONS_USE_ANNOTATION_AS_ROI));
				this.annotationPopup.addSeparator();
				this.annotationPopup.add(item("Pick color...", context, ACTIONS_PICK_ANNOTATION_COLOR));
				this.annotationPopup.add(item("Toggle visibility", context, ACTIONS_TOGGLE_ANNOTATION_VISIBILITY));
				this.annotationPopup.addSeparator();
				this.annotationPopup.add(item("Delete", context, ACTIONS_DELETE_ANNOTATION));
				
				this.regionPopup.add(item("Use as ROI", context, ACTIONS_USE_ANNOTATION_AS_ROI));
				this.regionPopup.addSeparator();
				this.regionPopup.add(item("Delete", context, ACTIONS_DELETE_ANNOTATION));
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (isRightMouseButton(event)) {
					final int row = tree.getRowForLocation(event.getX(), event.getY());
					
					if (row < 0) {
						return;
					}
					
					final Object element = tree.getPathForRow(row).getLastPathComponent();
					
					if (element instanceof Annotation) {
//						tree.setSelectionRow(row);
						this.annotationPopup.show(event.getComponent(), event.getX(), event.getY());
					} else if (element instanceof Region) {
						this.regionPopup.show(event.getComponent(), event.getX(), event.getY());
					}
				}
			}
			
		});
		
		context.getVariable("annotations").addListener(new Listener<Object>() {
			
			@Override
			public final void valueChanged(final ValueChangedEvent<Object, ?> event) {
				((DefaultTreeModel) tree.getModel()).reload();
			}
			
		});
	}
	
}
