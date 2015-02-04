package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Classifier<C extends ClassifierClass> extends Serializable {
	
	public abstract ClassifierClass.Measure<C> getClassMeasure();
	
	public abstract Classification<C> classify(double... input);
	
}
