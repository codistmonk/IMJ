package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Clustering<C extends ClassifierClass> extends Serializable {
	
	public abstract Classifier<C> cluster(DataSource<C> inputs);
	
}
