package imj3.draft.segmentation;

import java.io.InputStream;
import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
	
	public static final Quantizer fromXML(final InputStream input) {
		return load(XMLTools.parse(input), new Quantizer().setUserObject());
	}
	
	public static final String select(final String string, final Object defaultValue) {
		return string.isEmpty() ? defaultValue.toString() : string;
	}
	
	public static final Quantizer load(final Document xml, final Quantizer result) {
		final Element paletteElement = (Element) XMLTools.getNode(xml, "palette");
		
		result.setScale(select(paletteElement.getAttribute("scale"), Quantizer.DEFAULT_SCALE));
		result.setMaximumScale(select(paletteElement.getAttribute("maximumScale"), Quantizer.DEFAULT_MAXIMUM_SCALE));
		result.removeAllChildren();
		
		for (final Node clusterNode : XMLTools.getNodes(xml, "palette/cluster")) {
			final Element clusterElement = (Element) clusterNode;
			final QuantizerCluster cluster = new QuantizerCluster()
				.setName(select(clusterElement.getAttribute("name"), QuantizerCluster.DEFAULT_NAME))
				.setLabel(select(clusterElement.getAttribute("label"), QuantizerCluster.DEFAULT_LABEL))
				.setMinimumSegmentSize(select(clusterElement.getAttribute("minimumSegmentSize"), QuantizerCluster.DEFAULT_MINIMUM_SEGMENT_SIZE))
				.setMaximumSegmentSize(select(clusterElement.getAttribute("maximumSegmentSize"), QuantizerCluster.DEFAULT_MAXIMUM_SEGMENT_SIZE))
				.setMaximumPrototypeCount(select(clusterElement.getAttribute("maximumPrototypeCount"), QuantizerCluster.DEFAULT_MAXIMUM_PROTOTYPE_COUNT))
				.setUserObject();
			
			result.add(cluster);
			
			for (final Node prototypeNode : XMLTools.getNodes(clusterNode, "prototype")) {
				final QuantizerPrototype prototype = new QuantizerPrototype();
				
				cluster.add(prototype);
				
				prototype.setData(prototypeNode.getTextContent()).setUserObject();
			}
		}
		
		return result;
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
	
	/**
	 * @author codistmonk (creation 2015-01-18)
	 */
	public static final class ToXML implements QuantizerNode.Visitor<Node> {
		
		private final Document xml = XMLTools.newDocument();
		
		@Override
		public Element visit(final Quantizer quantizer) {
			final Element result = (Element) this.xml.appendChild(this.xml.createElement("palette"));
			final int n = quantizer.getChildCount();
			
			result.setAttribute("scale", quantizer.getScaleAsString());
			result.setAttribute("maximumScale", quantizer.getMaximumScaleAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerCluster) quantizer.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerCluster cluster) {
			final Element result = this.xml.createElement("cluster");
			final int n = cluster.getChildCount();
			
			result.setAttribute("name", cluster.getName());
			result.setAttribute("label", cluster.getLabelAsString());
			result.setAttribute("minimumSegmentSize", cluster.getMinimumSegmentSizeAsString());
			result.setAttribute("maximumSegmentSize", cluster.getMaximumSegmentSizeAsString());
			result.setAttribute("maximumPrototypeCount", cluster.getMaximumPrototypeCountAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((QuantizerPrototype) cluster.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final QuantizerPrototype prototype) {
			final Element result = this.xml.createElement("prototype");
			
			result.setTextContent(prototype.getDataAsString());
			
			return result;
		}
		
		private static final long serialVersionUID = -8012834350224027358L;
		
	}
	
}