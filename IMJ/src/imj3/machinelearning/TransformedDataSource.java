package imj3.machinelearning;

/**
 * @author codistmonk (creation 2015-02-09)
 */
public abstract class TransformedDataSource extends DataSource.Abstract<DataSource> {
	
	protected TransformedDataSource(final DataSource source) {
		super(source);
	}
	
	@Override
	public final int size() {
		return this.getSource().size();
	}
	
	private static final long serialVersionUID = -1318008499856095525L;
	
}
