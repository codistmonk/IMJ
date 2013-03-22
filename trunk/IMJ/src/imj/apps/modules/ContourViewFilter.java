package imj.apps.modules;

import static imj.MorphologicalOperations.StructuringElement.newRing;
import static java.util.Locale.ENGLISH;
import imj.Image;
import imj.Labeling.NeighborhoodShape.Distance;
import imj.apps.modules.SimpleSieve.Feature;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class ContourViewFilter extends ViewFilter {
	
	private Feature feature;
	
	public ContourViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("feature", "brightness");
	}
	
	public final boolean accept(final int index, final int value) {
		if (this.feature == null) {
			return true;
		}
		
		final Image source = this.getImage().getSource();
		
		if (source == null) {
			return true;
		}
		
		final int neighborhoodArrayLength = NEIGHBORHOOD.length;
		final int featureValue = this.feature.getNewValue(index, value);
		final int rowCount = source.getRowCount();
		final int columnCount = source.getColumnCount();
		final int rowIndex = index / columnCount;
		final int columnIndex = index % columnCount;
		boolean result = false;
		
		for (int i = 0; i < neighborhoodArrayLength; i += 2) {
			final int r = rowIndex + NEIGHBORHOOD[i];
			final int c = columnIndex + NEIGHBORHOOD[i + 1];
			
			if (0 <= r && r < rowCount && 0 <= c && c < columnCount) {
				final int j = r * columnCount + c;
				final int neighborFeatureValue = this.feature.getNewValue(j, source.getValue(j));
				
				if (featureValue < neighborFeatureValue) {
					return false;
				}
				
				if (neighborFeatureValue < featureValue) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	@Override
	public final void doInitialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(ENGLISH));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return ContourViewFilter.this.accept(index, oldValue) ? oldValue : 0;
			}
			
		};
	}
	
	private static final int[] NEIGHBORHOOD = newRing(1.0, 1.0, Distance.CHESSBOARD);
	
}
