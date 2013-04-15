package imj;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public final class RegionalMaxima extends RegionalExtrema {
	
	public RegionalMaxima(final Image image, final Image result, final int[] connectivity) {
		super(image, result, connectivity);
	}
	
	public RegionalMaxima(final Image image, final int[] connectivity) {
		super(image, connectivity);
	}
	
	@Override
	protected final int compare(final int value1, final int value2) {
		return value2 - value1;
	}
	
}
