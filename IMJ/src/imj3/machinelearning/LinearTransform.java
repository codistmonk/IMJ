package imj3.machinelearning;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public final class LinearTransform implements Classifier {
	
	private final double[][] matrix;
	
	private final Datum.Measure<Datum> transformedMeasure;
	
	public LinearTransform(final Measure measure, double[][] matrix) {
		this.matrix = matrix;
		this.transformedMeasure = new Datum.Measure.Default<>(measure);
	}
	
	@Override
	public final Element toXML(final Document document, final Map<Object, Integer> ids) {
		final Element result = Classifier.super.toXML(document, ids);
		// TODO Auto-generated method stub
		return result;
	}
	
	@Override
	public final LinearTransform fromXML(final Element xml, final Map<Integer, Object> objects) {
		Classifier.super.fromXML(xml, objects);
		// TODO Auto-generated method stub
		return this;
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
	public final int getClassCount() {
		return 0;
	}
	
	@Override
	public final Datum.Measure<Datum> getClassMeasure() {
		return this.transformedMeasure;
	}
	
	@Override
	public final Datum classify(final Datum in, final Datum out) {
		final int n = this.getMatrixRowCount();
		final double[] datum = new double[n];
		
		for (int i = 0; i < n; ++i) {
			datum[i] = dot(this.getMatrix()[i], in.getValue());
		}
		
		return out.setValue(in.getValue()).setPrototype(new Datum.Default().setValue(datum)).setScore(0.0);
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
	
}