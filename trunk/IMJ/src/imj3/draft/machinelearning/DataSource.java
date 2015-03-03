package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.Tools.ignore;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface DataSource extends Serializable, Iterable<Datum> {
	
	public abstract DataSource getSource();
	
	@SuppressWarnings("unchecked")
	public default <D extends DataSource> D findSource(Class<D> cls) {
		DataSource result = this;
		
		while (result != null && ! cls.isInstance(result)) {
			result = result.getSource();
		}
		
		return (D) result;
	}
	
	public abstract int getInputDimension();
	
	public abstract int getClassDimension();
	
	// TODO use long instead of int
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
	 */
	public static abstract class Abstract<D extends DataSource> implements DataSource {
		
		private final D source;
		
		protected Abstract() {
			this(null);
		}
		
		protected Abstract(final D source) {
			this.source = source;
		}
		
		@Override
		public final D getSource() {
			return this.source;
		}
		
		private static final long serialVersionUID = 8357748600830831869L;
		
	}
	
}
