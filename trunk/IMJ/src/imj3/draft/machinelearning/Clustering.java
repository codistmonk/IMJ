package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Clustering extends Serializable {
	
	public abstract Classifier cluster(DataSource inputs);
	
}
