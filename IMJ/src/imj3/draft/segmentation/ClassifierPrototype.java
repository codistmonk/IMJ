package imj3.draft.segmentation;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-03)
 */
public abstract class ClassifierPrototype extends ClassifierNode {
	
	private static final long serialVersionUID = 7111244764386006986L;
	
	public abstract ClassifierPrototype setData(double[] elements);
	
	public abstract ClassifierPrototype setData(String dataAsString);
	
	public abstract String getDataAsString();
	
	public abstract double distanceTo(int[] data, double limit);
	
	/**
	 * @author codistmonk (creation 2015-02-03)
	 */
	public static abstract interface Factory extends Serializable {
		
		public abstract ClassifierPrototype newPrototype();
		
		public abstract int[] allocateDataBuffer(int scale, int[] old);
		
		public abstract void extractData(BufferedImage image, int x, int y, int scale, int[] result);
		
	}
	
}
