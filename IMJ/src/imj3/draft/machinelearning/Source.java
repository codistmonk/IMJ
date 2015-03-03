package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-03-03)
 */
public abstract interface Source extends Serializable {

	public abstract Source getSource();
	
	@SuppressWarnings("unchecked")
	public default <D extends Source> D findSource(final Class<D> cls) {
		Source result = this;
		
		while (result != null && ! cls.isInstance(result)) {
			result = result.getSource();
		}
		
		return (D) result;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-03)
	 *
	 * @param <S>
	 */
	public static abstract class Abstract<S extends Source> implements Source {
		
		private final S source;
		
		protected Abstract() {
			this(null);
		}
		
		protected Abstract(final S source) {
			this.source = source;
		}
		
		@Override
		public final S getSource() {
			return this.source;
		}
		
		private static final long serialVersionUID = -2945024375130955121L;
		
	}
	
}
