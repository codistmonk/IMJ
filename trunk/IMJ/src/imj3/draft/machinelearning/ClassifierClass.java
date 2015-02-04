package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface ClassifierClass extends Serializable {
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static abstract interface Measure<C extends ClassifierClass> extends Serializable {
		
		public double compute(C c1, C c2);
		
	}
	
}
