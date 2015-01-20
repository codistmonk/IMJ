package imj3.draft.segmentation;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public abstract class QuantizerNode extends DefaultMutableTreeNode {
	
	public abstract QuantizerNode copy();
	
	public abstract <V> V accept(QuantizerNode.Visitor<V> visitor);
	
	public abstract QuantizerNode setUserObject();
	
	public final UserObject renewUserObject() {
		return (UserObject) this.setUserObject().getUserObject();
	}
	
	protected final <N extends QuantizerNode> N copyChildrenTo(final N node) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final QuantizerNode child = ((QuantizerNode) this.getChildAt(i)).copy();
			
			node.add(child);
		}
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public final <N extends QuantizerNode> N visitChildren(final QuantizerNode.Visitor<?> visitor) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			((QuantizerNode) this.getChildAt(i)).accept(visitor);
		}
		
		return (N) this;
	}
	
	private static final long serialVersionUID = 7636724853656189383L;
	
	public static final int parseARGB(final String string) {
		return string.startsWith("#") ? (int) Long.parseLong(string.substring(1), 16) : Integer.parseInt(string);
	}
	
	/**
	 * @author codistmonk (creation 2015-01-18)
	 */
	public abstract class UserObject implements Serializable {
		
		private static final long serialVersionUID = 1543313797613503533L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static abstract interface Visitor<V> extends Serializable {
		
		public abstract V visit(Quantizer quantizer);
		
		public abstract V visit(QuantizerCluster cluster);
		
		public abstract V visit(QuantizerPrototype prototype);
		
	}
	
}