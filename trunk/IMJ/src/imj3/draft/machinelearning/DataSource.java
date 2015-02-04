package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface DataSource<C extends ClassifierClass> extends Serializable, Iterable<Classification<C>> {
	
	public abstract int getDimension();
	
	public default int size() {
		int result = 0;
		
		for (final Object object : this) {
			ignore(object);
			
			++result;
		}
		
		return result;
	}
	
}
