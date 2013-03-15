package imj.apps.modules;

import static imj.IMJTools.alpha;
import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.brightness;
import static imj.IMJTools.hue;
import static imj.IMJTools.red;
import static imj.IMJTools.saturation;
import static java.lang.Integer.parseInt;
import imj.Labeling.NeighborhoodShape.Distance;
import imj.MorphologicalOperations.StructuringElement;
import imj.RankFilter;
import imj.apps.modules.FilteredImage.Filter;
import imj.apps.modules.SimpleSieve.Feature;

import java.awt.Color;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class SaturationSieve201303080950 extends Sieve {
	
	private final Feature feature;
	
	private int minimum;
	
	private int maximum;
	
	public SaturationSieve201303080950(final Context context) {
		super(context);
		this.feature = Feature.SATURATION;
		
		this.getParameters().put("minimum", "10");
		this.getParameters().put("maximum", "20");
	}
	
	@Override
	public final boolean accept(final int index, final int value) {
		if (this.maximum < this.minimum) {
			return false;
		}
		
		final int featureValue = this.feature.getNewValue(index, value);
		
		return this.minimum <= featureValue && featureValue <= this.maximum;
	}
	
	@Override
	public final void initialize() {
		this.minimum = parseInt(this.getParameters().get("minimum"));
		this.maximum = parseInt(this.getParameters().get("maximum"));
	}
	
	@Override
	protected final void finish(final RegionOfInterest roi) {
		final RegionOfInterest tmp = RegionOfInterest.newInstance(roi.getRowCount(), roi.getColumnCount());
		final int[] structuringElement1 = StructuringElement.newDisk(1.0, Distance.CHESSBOARD);
		final int[] structuringElement2 = StructuringElement.newDisk(2.0, Distance.CHESSBOARD);
		final int[] structuringElement3 = StructuringElement.newDisk(2.0, Distance.EUCLIDEAN);
		
		new RankFilter(roi, tmp, -1, structuringElement1);
		new RankFilter(tmp, roi, 0, structuringElement1);
		
		new RankFilter(roi, tmp, 0, structuringElement2);
		new RankFilter(tmp, roi, -1, structuringElement2);
		
		new RankFilter(roi, tmp, -1, structuringElement3);
		new RankFilter(tmp, roi, 0, structuringElement3);
	}
	
}
