package imj3.draft.machinelearning;

/**
 * @author codistmonk (creation 2015-02-09)
 *
 * @param <M>
 * @param <In>
 * @param <Out>
 */
public abstract class TransformedDataSource<M extends DataSource.Metadata> implements DataSource<M> {
	
	private final DataSource<? extends M> source;
	
	protected TransformedDataSource(final DataSource<? extends M> source) {
		this.source = source;
	}
	
	@SuppressWarnings("unchecked")
	public final DataSource<M> getSource() {
		return (DataSource<M>) this.source;
	}
	
	@Override
	public final M getMetadata() {
		return this.getSource().getMetadata();
	}
	
	@Override
	public final int size() {
		return this.getSource().size();
	}
	
	private static final long serialVersionUID = -1318008499856095525L;
	
}
