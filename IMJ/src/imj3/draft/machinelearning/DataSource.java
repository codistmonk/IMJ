package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface DataSource<M extends DataSource.Metadata, C extends ClassifierClass> extends Serializable, Iterable<Classification<C>> {
	
	public abstract M getMetadata();
	
	public abstract int getInputDimension();
	
	public abstract int getClassDimension();
	
	public default int size() {
		int result = 0;
		
		for (final Object object : this) {
			ignore(object);
			
			++result;
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 *
	 * @param <M>
	 * @param <C>
	 */
	public static abstract class Abstract<M extends DataSource.Metadata, C extends ClassifierClass>  implements DataSource<M, C> {
		
		private final M metadata;
		
		protected Abstract(final M metadata) {
			this.metadata = metadata;
		}
		
		@Override
		public final M getMetadata() {
			return this.metadata;
		}
		
		private static final long serialVersionUID = 8357748600830831869L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static abstract interface Metadata extends Serializable {
		// Deliberately left empty
	}
	
}
