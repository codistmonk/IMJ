package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface ClassifierClass extends Serializable {
	
	public abstract int getClassIndex();
	
	public abstract double[] toArray();
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Default implements ClassifierClass {
		
		private final int classIndex;
		
		private final double[] datum;
		
		public Default(final double[] datum) {
			this(-1, datum);
		}
		
		public Default(final int classIndex, final double[] datum) {
			this.classIndex = classIndex;
			this.datum = datum;
		}
		
		@Override
		public final int getClassIndex() {
			return this.classIndex;
		}
		
		@Override
		public final double[] toArray() {
			return this.datum;
		}
		
		private static final long serialVersionUID = 5806137995866347277L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static abstract interface Measure<C extends ClassifierClass> extends Serializable {
		
		public double compute(C c1, C c2);
		
		/**
		 * @author codistmonk (creation 2015-02-04)
		 */
		public static final class Default<C extends ClassifierClass> implements Measure<C> {
			
			private final imj3.draft.machinelearning.Measure inputMeasure;
			
			public Default(final imj3.draft.machinelearning.Measure inputMeasure) {
				this.inputMeasure = inputMeasure;
			}
			
			@Override
			public final double compute(final C c1, final C c2) {
				return this.inputMeasure.compute(c1.toArray(), c2.toArray(), Double.POSITIVE_INFINITY);
			}
			
			private static final long serialVersionUID = -1398649605392286153L;
			
		}
		
	}
	
}
