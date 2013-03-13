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
		if (this.feature == null) {
			return true;
		}
		
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
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static enum Feature implements Filter {
		
		RED {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return red(oldValue);
			}
			
		}, GREEN {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return red(oldValue);
			}
			
		}, BLUE {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return blue(oldValue);
			}
			
		}, ALPHA {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return alpha(oldValue);
			}
			
		}, BRIGHTNESS {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return brightness(oldValue);
			}
			
		}, SATURATION {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return saturation(oldValue);
			}
			
		}, HUE {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return hue(oldValue);
			}
			
		};
		
		static {
			final float[] hsb = new float[3];
			
			for (int r = 0; r < 256; r += 2) {
				for (int g = 0; g < 256; g += 2) {
					for (int b = 0; b < 256; b += 2) {
						Color.RGBtoHSB(r, g, b, hsb);
						
						if (HUE.getNewValue(0, argb(255, r, g, b)) != (int) (hsb[0] * 255F)) {
							throw new RuntimeException();
						}
					}
				}
			}
		}
		
	}
	
}
