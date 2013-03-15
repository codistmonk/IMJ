package imj.apps.modules;

import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.Image;
import imj.apps.modules.SimpleSieve.Feature;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class ContourSieve extends Sieve {
	
	private Feature feature;
	
	private Image image;
	
	public ContourSieve(final Context context) {
		super(context);
		
		this.getParameters().put("feature", "brightness");
	}
	
	private static final int[][] NEIGHBORHOOD = {
		{ -1, -1 },
		{ -1, +0 },
		{ -1, +1 },
		{ +0, -1 },
		{ +0, +1 },
		{ +1, -1 },
		{ +1, +0 },
		{ +1, +1 },
	};
	
	@Override
	public final boolean accept(final int index, final int value) {
		if (this.feature == null) {
			return true;
		}
		
		final int featureValue = this.feature.getNewValue(index, value);
		final int rowCount = this.image.getRowCount();
		final int columnCount = this.image.getColumnCount();
		final int rowIndex = index / columnCount;
		final int columnIndex = index % columnCount;
		boolean result = false;
		
		for (final int[] drdc : NEIGHBORHOOD) {
			final int r = rowIndex + drdc[0];
			final int c = columnIndex + drdc[1];
			
			if (0 <= r && r < rowCount && 0 <= c && c < columnCount) {
				final int j = r * columnCount + c;
				final int neighborFeatureValue = this.feature.getNewValue(j, this.image.getValue(j));
				
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
	
	private final boolean neighborIsStrictlyLarger(final int rowIndex, final int columnIndex, final int value) {
		final int rowCount = this.image.getRowCount();
		final int columnCount = this.image.getColumnCount();
		
		if (rowIndex < 0 || rowCount <= rowIndex || columnIndex < 0 || columnCount <= columnIndex) {
			return false;
		}
		
		final int index = rowIndex * columnCount + columnIndex;
		
		return value < this.feature.getNewValue(index, this.image.getValue(index));
	}
	
	@Override
	public final void initialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(ENGLISH));
		this.image = ViewFilter.getCurrentImage(this.getContext());
	}
	
}
