package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-03-02)
 */
public abstract interface Datum extends Serializable {
	
	public default int getIndex() {
		return -1;
	}
	
	public default Datum setIndex(final int index) {
		ignore(index);
		
		throw new UnsupportedOperationException();
	}
	
	public default double getWeight() {
		return 1.0;
	}
	
	public default Datum setWeight(final double weight) {
		ignore(weight);
		
		throw new UnsupportedOperationException();
	}
	
	public default Datum updateWeight(final double delta) {
		return this.setWeight(this.getWeight() + delta);
	}
	
	public abstract double[] getValue();
	
	public default Datum setValue(final double[] value) {
		ignore(value);
		
		throw new UnsupportedOperationException();
	}
	
	public default Datum getPrototype() {
		return this;
	}
	
	public default Datum setPrototype(final Datum prototype) {
		ignore(prototype);
		
		throw new UnsupportedOperationException();
	}
	
	public default double getScore() {
		return 0.0;
	}
	
	public default Datum setScore(final double score) {
		ignore(score);
		
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @author codistmonk (creation 2015-03-02)
	 */
	public static final class Default implements Datum {
		
		private int index;
		
		private double weight = 1.0;
		
		private double[] value;
		
		private Datum prototype = this;
		
		private double score;
		
		@Override
		public final int getIndex() {
			return this.index;
		}
		
		@Override
		public final Default setIndex(final int index) {
			this.index = index;
			
			return this;
		}
		
		@Override
		public final double getWeight() {
			return this.weight;
		}
		
		@Override
		public final Default setWeight(final double weight) {
			this.weight = weight;
			
			return this;
		}
		
		@Override
		public final double[] getValue() {
			return this.value;
		}
		
		@Override
		public final Default setValue(final double[] value) {
			this.value = value;
			
			return this;
		}
		
		@Override
		public final Datum getPrototype() {
			return this.prototype;
		}
		
		@Override
		public final Datum setPrototype(final Datum prototype) {
			this.prototype = prototype;
			
			return this;
		}
		
		@Override
		public final double getScore() {
			return this.score;
		}
		
		@Override
		public final Default setScore(final double score) {
			this.score = score;
			
			return this;
		}
		
		private static final long serialVersionUID = 2527401229608310695L;
		
		public static final Default datum(final double... input) {
			return new Default().setValue(input);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static abstract interface Measure<D extends Datum> extends Serializable {
		
		public double compute(D d1, D d2);
		
		/**
		 * @author codistmonk (creation 2015-02-04)
		 */
		public static final class Default<D extends Datum> implements Measure<D> {
			
			private final imj3.draft.machinelearning.Measure inputMeasure;
			
			public Default(final imj3.draft.machinelearning.Measure inputMeasure) {
				this.inputMeasure = inputMeasure;
			}
			
			@Override
			public final double compute(final D d1, final D d2) {
				return this.inputMeasure.compute(d1.getValue(), d2.getValue(), Double.POSITIVE_INFINITY);
			}
			
			private static final long serialVersionUID = -1398649605392286153L;
			
		}
		
	}
	
}
