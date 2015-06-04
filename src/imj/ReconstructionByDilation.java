package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author codistmonk (creation 2013-01-28)
 */
public final class ReconstructionByDilation extends Reconstruction {
	
	public ReconstructionByDilation(final Image mask, final Image marker, final int[] connectivity) {
		super(mask, marker, connectivity);
	}
	
	@Override
	protected final int getExtremalMarkerValue(final int current, final int candidate) {
		return max(current, candidate);
	}
	
	@Override
	protected final int mergeMaskAndMarkerValues(final int maskValue, final int markerValue) {
		return min(maskValue, markerValue);
	}
	
	@Override
	protected final boolean acceptNeighborMarkerValue(final int neighborMarkerValue, final int threshold) {
		return neighborMarkerValue < threshold;
	}
	
}
	