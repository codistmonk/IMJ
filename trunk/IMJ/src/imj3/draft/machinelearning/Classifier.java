package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Classifier<C extends ClassifierClass> extends Serializable {
	
	public abstract ClassifierClass.Measure<C> getClassMeasure();
	
	public default Classification<C> classify(final double... input) {
		return this.classify(new Classification<>(), input);
	}
	
	public abstract Classification<C> classify(Classification<C> result, double... input);
	
	public abstract int getClassDimension(int inputDimension);
	
}
