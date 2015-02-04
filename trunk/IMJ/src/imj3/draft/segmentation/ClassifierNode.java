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
public abstract class ClassifierNode extends DefaultMutableTreeNode {
	
	public abstract ClassifierNode copy();
	
	public abstract <V> V accept(ClassifierNode.Visitor<V> visitor);
	
	public abstract ClassifierNode setUserObject();
	
	public final UserObject renewUserObject() {
		return (UserObject) this.setUserObject().getUserObject();
	}
	
	protected final <N extends ClassifierNode> N copyChildrenTo(final N node) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			final ClassifierNode child = ((ClassifierNode) this.getChildAt(i)).copy();
			
			node.add(child);
		}
		
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public final <N extends ClassifierNode> N visitChildren(final ClassifierNode.Visitor<?> visitor) {
		final int n = this.getChildCount();
		
		for (int i = 0; i < n; ++i) {
			((ClassifierNode) this.getChildAt(i)).accept(visitor);
		}
		
		return (N) this;
	}
	
	private static final long serialVersionUID = 7636724853656189383L;
	
	public static final int parseARGB(final String string) {
		return string.startsWith("#") ? (int) Long.parseLong(string.substring(1), 16) : Integer.parseInt(string);
	}
	
	public static final Classifier fromXML(final InputStream input) {
		return load(XMLTools.parse(input), new Classifier().setUserObject());
	}
	
	public static final String select(final String string, final Object defaultValue) {
		return string.isEmpty() ? defaultValue.toString() : string;
	}
	
	public static final Classifier load(final Document xml, final Classifier result) {
		final Element classifierElement = (Element) XMLTools.getNode(xml, "classifier");
		
		result.setPrototypeFactory(select(classifierElement.getAttribute("prototypeFactory"), ClassifierRawPrototype.Factory.class.getName()));
		result.setScale(select(classifierElement.getAttribute("scale"), Classifier.DEFAULT_SCALE));
		result.setMaximumScale(select(classifierElement.getAttribute("maximumScale"), Classifier.DEFAULT_MAXIMUM_SCALE));
		result.removeAllChildren();
		
		for (final Node clusterNode : XMLTools.getNodes(xml, "classifier/cluster")) {
			final Element clusterElement = (Element) clusterNode;
			final ClassifierCluster cluster = new ClassifierCluster()
				.setLabel(select(clusterElement.getAttribute("label"), ClassifierCluster.DEFAULT_LABEL))
				.setMinimumSegmentSize(select(clusterElement.getAttribute("minimumSegmentSize"), ClassifierCluster.DEFAULT_MINIMUM_SEGMENT_SIZE))
				.setMaximumSegmentSize(select(clusterElement.getAttribute("maximumSegmentSize"), ClassifierCluster.DEFAULT_MAXIMUM_SEGMENT_SIZE))
				.setMaximumPrototypeCount(select(clusterElement.getAttribute("maximumPrototypeCount"), ClassifierCluster.DEFAULT_MAXIMUM_PROTOTYPE_COUNT))
				.setUserObject();
			
			result.add(cluster);
			
			for (final Node prototypeNode : XMLTools.getNodes(clusterNode, "prototype")) {
				final ClassifierPrototype prototype = result.getPrototypeFactory().newPrototype();
				
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
		
		public abstract V visit(Classifier classifier);
		
		public abstract V visit(ClassifierCluster cluster);
		
		public abstract V visit(ClassifierPrototype prototype);
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-18)
	 */
	public static final class ToXML implements ClassifierNode.Visitor<Node> {
		
		private final Document xml = XMLTools.newDocument();
		
		@Override
		public Element visit(final Classifier classifier) {
			final Element result = (Element) this.xml.appendChild(this.xml.createElement("classifier"));
			final int n = classifier.getChildCount();
			
			result.setAttribute("scale", classifier.getScaleAsString());
			result.setAttribute("maximumScale", classifier.getMaximumScaleAsString());
			result.setAttribute("prototypeFactory", classifier.getPrototypeFactoryAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((ClassifierCluster) classifier.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final ClassifierCluster cluster) {
			final Element result = this.xml.createElement("cluster");
			final int n = cluster.getChildCount();
			
			result.setAttribute("label", cluster.getLabelAsString());
			result.setAttribute("minimumSegmentSize", cluster.getMinimumSegmentSizeAsString());
			result.setAttribute("maximumSegmentSize", cluster.getMaximumSegmentSizeAsString());
			result.setAttribute("maximumPrototypeCount", cluster.getMaximumPrototypeCountAsString());
			
			for (int i = 0; i < n; ++i) {
				result.appendChild(((ClassifierPrototype) cluster.getChildAt(i)).accept(this));
			}
			
			return result;
		}
		
		@Override
		public final Element visit(final ClassifierPrototype prototype) {
			final Element result = this.xml.createElement("prototype");
			
			result.setTextContent(prototype.getDataAsString());
			
			return result;
		}
		
		private static final long serialVersionUID = -8012834350224027358L;
		
	}
	
}