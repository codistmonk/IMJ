package imj3.draft.segmentation2;

import imj3.draft.machinelearning.Classification;
import imj3.draft.machinelearning.DataSource;

/**
 * @author codistmonk (creation 2015-02-06)
 */
public enum DataSourceArrayProperty {
	
	INPUT {
		
		@Override
		public final int getDimension(final DataSource<?> source) {
			return source.getInputDimension();
		}
		
		@Override
		public final double[] getArray(final Classification<?> classification) {
			return classification.getInput();
		}
		
	}, CLASS {
		
		@Override
		public final int getDimension(final DataSource<?> source) {
			return source.getClassDimension();
		}
		
		@Override
		public final double[] getArray(final Classification<?> classification) {
			return classification.getClassifierClass().toArray();
		}
		
	};
	
	public abstract int getDimension(DataSource<?> source);
	
	public abstract double[] getArray(Classification<?> classification);
	
}