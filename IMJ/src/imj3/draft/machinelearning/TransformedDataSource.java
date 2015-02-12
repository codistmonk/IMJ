package imj3.draft.machinelearning;

/**
 * @author codistmonk (creation 2015-02-09)
 *
 * @param <M>
 * @param <In>
 * @param <Out>
 */
public abstract class TransformedDataSource<M extends DataSource.Metadata, In extends ClassifierClass, Out extends ClassifierClass> implements DataSource<M, Out> {
	
	private final DataSource<? extends M, ? extends In> source;
	
	protected TransformedDataSource(final DataSource<? extends M, ? extends In> source) {
		this.source = source;
	}
	
	@SuppressWarnings("unchecked")
	public final DataSource<M, In> getSource() {
		return (DataSource<M, In>) this.source;
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
