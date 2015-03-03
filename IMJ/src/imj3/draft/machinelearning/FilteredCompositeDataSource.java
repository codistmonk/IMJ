package imj3.draft.machinelearning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * @author codistmonk (creation 2015-02-24)
 */
public final class FilteredCompositeDataSource extends DataSource.Abstract<DataSource.Metadata> {
	
	private final List<DataSource<?>> sources;
	
	private final Function<Datum, Boolean> filter;
	
	public FilteredCompositeDataSource(final Function<Datum, Boolean> filter) {
		super(new Metadata.Default());
		this.sources = new ArrayList<>();
		this.filter = filter;
	}
	
	public final FilteredCompositeDataSource add(final DataSource<?> source) {
		this.sources.add(source);
		
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public final Iterator<Datum> iterator() {
		final Iterator<DataSource<?>> i = this.sources.iterator();
		
		return new FilteredIterator(new Iterator<Datum>() {
			
			private Iterator<Datum> j;
			
			{
				this.update();
			}
			
			@Override
			public final boolean hasNext() {
				return this.j != null && this.j.hasNext();
			}
			
			@Override
			public final Datum next() {
				final Datum result = this.j.next();
				
				this.update();
				
				return result;
			}
			
			private final void update() {
				while ((this.j == null || !this.j.hasNext()) && i.hasNext()) {
					this.j = (Iterator) i.next().iterator();
				}
			}
			
			
		}, this.filter);
	}
	
	@Override
	public final int getInputDimension() {
		if (!this.sources.isEmpty()) {
			return this.sources.get(0).getInputDimension();
		}
		
		return 0;
	}
	
	@Override
	public final int getClassDimension() {
		if (!this.sources.isEmpty()) {
			return this.sources.get(0).getClassDimension();
		}
		
		return 0;
	}
	
	private static final long serialVersionUID = 6526966621533776530L;
	
}