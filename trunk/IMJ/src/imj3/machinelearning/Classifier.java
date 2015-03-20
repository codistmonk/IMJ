package imj3.machinelearning;

import imj3.tools.XMLSerializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface Classifier extends XMLSerializable {
	
	public abstract int getClassCount();
	
	public abstract Datum.Measure<Datum> getClassMeasure();
	
	public default Datum classify(final Datum inOut) {
		return this.classify(inOut, inOut);
	}
	
	public abstract Datum classify(Datum in, Datum out);
	
	public abstract int getClassDimension(int inputDimension);
	
}
