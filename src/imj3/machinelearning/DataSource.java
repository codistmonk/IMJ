package imj3.machinelearning;

import static multij.tools.Tools.ignore;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public abstract interface DataSource extends Source, Iterable<Datum> {
	
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
	
	@Override
	public abstract Iterator iterator();
	
	/**
	 * @author codistmonk (creation 2015-03-03)
	 */
	public static abstract interface Iterator extends Source, java.util.Iterator<Datum> {
		
		public static Iterator wrap(final java.util.Iterator<Datum> iterator) {
			return new Abstract<Iterator>() {
				
				@Override
				public final boolean hasNext() {
					return iterator.hasNext();
				}
				
				@Override
				public final Datum next() {
					return iterator.next();
				}
				
				private static final long serialVersionUID = -4234192592608156487L;
				
			};
		}
		
		/**
		 * @author codistmonk (creation 2015-03-03)
		 */
		public static abstract class Abstract<I extends Iterator> extends Source.Abstract<I> implements Iterator {
			
			protected Abstract() {
				// NOP
			}
			
			protected Abstract(final I source) {
				super(source);
			}
			
			private static final long serialVersionUID = 3124020024633605773L;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static abstract class Abstract<D extends DataSource> extends Source.Abstract<D> implements DataSource {
		
		protected Abstract() {
			// NOP
		}
		
		protected Abstract(final D source) {
			super(source);
		}
		
		private static final long serialVersionUID = 8357748600830831869L;
		
	}
	
}
