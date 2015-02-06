package imj3.draft.machinelearning;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class LinearTransform implements Classifier<LinearTransform.Transformed> {
	
	private final double[][] matrix;
	
	private final ClassifierClass.Measure<LinearTransform.Transformed> transformedMeasure;
	
	public LinearTransform(final Measure measure, double[][] matrix) {
		this.matrix = matrix;
		this.transformedMeasure = new ClassifierClass.Measure.Default<>(measure);
	}
	
	public final double[][] getMatrix() {
		return this.matrix;
	}
	
	public final int getMatrixRowCount() {
		return this.getMatrix().length;
	}
	
	public final int getMatrixColumnCount() {
		return this.getMatrix()[0].length;
	}
	
	@Override
	public final ClassifierClass.Measure<LinearTransform.Transformed> getClassMeasure() {
		return this.transformedMeasure;
	}
	
	@Override
	public final Classification<Transformed> classify(final Classification<Transformed> result, final double... input) {
		final int n = this.getMatrixRowCount();
		final double[] datum = new double[n];
		
		for (int i = 0; i < n; ++i) {
			datum[i] = dot(this.getMatrix()[i], input);
		}
		
		return result.setInput(input).setClassifierClass(new Transformed(datum)).setScore(0.0);
	}
	
	@Override
	public final int getClassDimension(final int inputDimension) {
		if (inputDimension == this.getMatrixColumnCount()) {
			return this.getMatrixRowCount();
		}
		
		Tools.debugPrint(inputDimension, this.getMatrixRowCount(), this.getMatrixColumnCount());
		
		throw new IllegalArgumentException();
	}
	
	private static final long serialVersionUID = -1314577713442253655L;
	
	public static final double dot(final double[] v1, final double[] v2) {
		final int n = v1.length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += v1[i] * v2[i];
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class Transformed implements ClassifierClass {
		
		private final double[] datum;
		
		public Transformed(final double[] datum) {
			this.datum = datum;
		}
		
		@Override
		public final double[] toArray() {
			return this.datum;
		}
		
		private static final long serialVersionUID = 5806137995866347277L;
		
	}
	
}