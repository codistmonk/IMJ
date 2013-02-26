package imj.apps.modules;

import java.util.AbstractList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * @author codistmonk (creation 2013-02-26)
 *
 * @param <T>
 */
public abstract class GenericTreeNode<T extends MutableTreeNode> extends DefaultMutableTreeNode {
	
	private final List<T> items;
	
	protected GenericTreeNode() {
		this.items = new AbstractList<T>() {
			
			@Override
			public final T get(final int index) {
				return (T) GenericTreeNode.this.getChildAt(index);
			}
			
			@Override
			public final int size() {
				return GenericTreeNode.this.getChildCount();
			}
			
			@Override
			public final void add(final int index, final T element) {
				GenericTreeNode.this.insert(element, index);
			}
			
		};
		
		this.setUserObject(this.getClass().getSimpleName());
	}
	
	public final List<T> getItems() {
		return this.items;
	}
	
}
