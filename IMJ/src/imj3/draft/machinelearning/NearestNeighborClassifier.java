package imj3.draft.machinelearning;

import java.util.ArrayList;
import java.util.List;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class NearestNeighborClassifier implements Classifier<NearestNeighborClassifier.Prototype> {
	
	private final List<NearestNeighborClassifier.Prototype> prototypes;
	
	private final Measure measure;
	
	private final ClassifierClass.Measure<Prototype> prototypeMeasure;
	
	public NearestNeighborClassifier(final Measure measure) {
		this.prototypes = new ArrayList<>();
		this.measure = measure;
		this.prototypeMeasure = new ClassifierClass.Measure.Default<>(measure);
	}
	
	public final NearestNeighborClassifier updatePrototypeIndices() {
		final int n = this.getPrototypes().size();
		
		for (int i = 0; i < n; ++i) {
			this.getPrototypes().get(i).setIndex(i);
		}
		
		return this;
	}
	
	public final List<NearestNeighborClassifier.Prototype> getPrototypes() {
		return this.prototypes;
	}
	
	public final Measure getMeasure() {
		return this.measure;
	}
	
	@Override
	public final ClassifierClass.Measure<Prototype> getClassMeasure() {
		return this.prototypeMeasure;
	}
	
	@Override
	public final Classification<Prototype> classify(final Classification<Prototype> result, final double... input) {
		NearestNeighborClassifier.Prototype bestPrototype = null;
		double bestDistance = Double.POSITIVE_INFINITY;
		
		for (final NearestNeighborClassifier.Prototype prototype : this.getPrototypes()) {
			final double d = this.getMeasure().compute(prototype.toArray(), input, bestDistance);
			
			if (d < bestDistance) {
				bestPrototype = prototype;
				bestDistance = d;
			}
		}
		
		return result.setInput(input).setClassifierClass(bestPrototype).setScore(bestDistance);
	}
	
	@Override
	public final int getClassDimension(final int inputDimension) {
		return inputDimension;
	}
	
	private static final long serialVersionUID = 8724283262153100459L;
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class Prototype implements ClassifierClass {
		
		private int index;
		
		private final double[] datum;
		
		private double weight;
		
		public Prototype(final double[] datum) {
			this(datum, 1.0);
		}
		
		public Prototype(final double[] datum, final double weight) {
			this.datum = datum;
			this.weight = weight;
		}
		
		public final int getIndex() {
			return this.index;
		}
		
		public final Prototype setIndex(final int index) {
			this.index = index;
			
			return this;
		}
		
		@Override
		public final double[] toArray() {
			return this.datum;
		}
		
		public final double getWeight() {
			return this.weight;
		}
		
		public final Prototype setWeight(final double weight) {
			this.weight = weight;
			
			return this;
		}
		
		public final Prototype updateWeight(final double delta) {
			this.weight += delta;
			
			return this;
		}
		
		private static final long serialVersionUID = 2041173451916012723L;
		
	}
	
}
