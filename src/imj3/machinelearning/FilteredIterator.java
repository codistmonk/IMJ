package imj3.machinelearning;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author codistmonk (creation 2015)
 *
 * @param <T>
 */
public final class FilteredIterator<T> implements Iterator<T> {
	
	private final Iterator<T> source;
	
	private final Function<T, Boolean> filter;
	
	private T next;
	
	public FilteredIterator(final Iterator<T> source, final Function<T, Boolean> filter) {
		this.source = source;
		this.filter = filter;
	}
	
	@Override
	public boolean hasNext() {
		while (this.source.hasNext() && this.next == null) {
			this.next = this.source.next();
			
			if (!this.filter.apply(this.next)) {
				this.next = null;
			}
		}
		
		return this.next != null;
	}
	
	@Override
	public final T next() {
		final T result = this.next;
		this.next = null;
		
		return result;
	}
	
}