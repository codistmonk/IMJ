package imj3.draft.machinelearning;

import java.util.ArrayList;
import java.util.List;

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
	
}
