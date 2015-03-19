package imj3.draft.machinelearning;

import imj3.tools.XMLSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class NearestNeighborClassifier implements Classifier {
	
	private final List<Datum> prototypes;
	
	private final Measure measure;
	
	private final Datum.Measure<Datum> prototypeMeasure;
	
	public NearestNeighborClassifier(final Measure measure) {
		this.prototypes = new ArrayList<>();
		this.measure = measure;
		this.prototypeMeasure = new Datum.Measure.Default<>(measure);
	}
	
	@Override
	public final Element toXML(final Document document, final Map<Object, Integer> ids) {
		final Element result = Classifier.super.toXML(document, ids);
		
		final Node prototypesNode = result.appendChild(document.createElement("prototypes"));
		
		for (final Datum prototype : this.getPrototypes()) {
			prototypesNode.appendChild(XMLSerializable.objectToXML(prototype, document, ids));
		}
		
		result.appendChild(XMLSerializable.newElement("measure", this.getMeasure(), document, ids));
		
		return result;
	}
	
	@Override
	public final NearestNeighborClassifier fromXML(final Element xml, final Map<Integer, Object> objects) {
		Classifier.super.fromXML(xml, objects);
		
		for (final Node node : XMLTools.getNodes(xml, "prototypes/*")) {
			this.getPrototypes().add(XMLSerializable.objectFromXML((Element) node, objects));
		}
		
		return this;
	}

	public final NearestNeighborClassifier updatePrototypeIndices() {
		final int n = this.getPrototypes().size();
		
		for (int i = 0; i < n; ++i) {
			this.getPrototypes().get(i).setIndex(i);
		}
		
		return this;
	}
	
	public final List<Datum> getPrototypes() {
		return this.prototypes;
	}
	
	public final Measure getMeasure() {
		return this.measure;
	}
	
	@Override
	public final int getClassCount() {
		return this.getPrototypes().size();
	}
	
	@Override
	public final Datum.Measure<Datum> getClassMeasure() {
		return this.prototypeMeasure;
	}
	
	@Override
	public final Datum classify(final Datum in, final Datum out) {
		Datum bestDatum = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for (final Datum prototype : this.getPrototypes()) {
			final double d = this.getMeasure().compute(prototype.getValue(), in.getValue(), bestDistance);
			
			if (d < bestDistance) {
				bestDatum = prototype;
				bestDistance = d;
			}
		}
		
		return out.setValue(in.getValue()).setPrototype(bestDatum).setScore(bestDistance);
	}
	
	@Override
	public final int getClassDimension(final int inputDimension) {
		return inputDimension;
	}
	
	private static final long serialVersionUID = 8724283262153100459L;
	
	public static final NearestNeighborClassifier objectFromXML(final Element element, final Map<Integer, Object> objects) {
		final Measure measure = XMLSerializable.objectFromXML((Element) XMLTools.getNode(element, "measure").getFirstChild(), objects);
		
		return new NearestNeighborClassifier(measure).fromXML(element, objects);
	}
	
}
