package imj;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public final class RegionalMinima extends RegionalExtrema {
	
	public RegionalMinima(final Image image, final Image result, final int[] connectivity) {
		super(image, result, connectivity);
	}
	
	public RegionalMinima(final Image image, final int[] connectivity) {
		super(image, connectivity);
	}
	
	@Override
	protected final int compare(final int value1, final int value2) {
		return value1 - value2;
	}
	
}
