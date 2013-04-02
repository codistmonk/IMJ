package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.apps.modules.SimpleSieve.Feature.valueOf;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import imj.apps.modules.FilteredImage.StructuringElementFilter;
import imj.apps.modules.SimpleSieve.Feature;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-03-25)
 */
public final class RegionGrowingViewFilter extends ViewFilter.FromFilter {
	
	public RegionGrowingViewFilter(final Context context) {
		super(context);
		
		this.getParameters().remove("channels");
		this.getParameters().put("maximumDistance", "8");
		this.getParameters().put("selector", "saturation");
	}
	
	@Override
	protected final boolean splitInputChannels() {
		return false;
	}
	
	@Override
	protected final boolean isOutputMonochannel() {
		return true;
	}
	
	@Override
	protected final void doInitialize() {
		super.doInitialize();
		
		this.setFilter(new ConditionalMeanFilter(this.parseStructuringElement(),
				this.getIntParameter("maximumDistance"),
				valueOf(this.getParameters().get("selector").trim().toUpperCase(Locale.ENGLISH))));
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class ConditionalMeanFilter extends StructuringElementFilter {
		
		private final int maximumDistance;
		
		private final Feature selector;
		
//		private int pixelHue;
//		
//		private final int[] sums;
		
		private final int[] pixelRGB;
		
		private final int[] neighborRGB;
		
		private final int[] resultRGB;
		
		private int maximum;
		
//		private int count;
		
		public ConditionalMeanFilter(final int[] deltas, final int maximumDistance, final Feature selector) {
			super(deltas);
			this.maximumDistance = maximumDistance;
			this.selector = selector;
//			this.sums = new int[3];
			this.pixelRGB = new int[3];
			this.neighborRGB = new int[3];
			this.resultRGB = new int[3];
			this.maximum = 0;
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
//			fill(this.sums, 0);
//			this.pixelHue = hue(oldValue);
//			this.count = 0;
			
			final int red = red(oldValue);
			final int green = green(oldValue);
			final int blue = blue(oldValue);
			this.resultRGB[0] = this.pixelRGB[0] = red;
			this.resultRGB[1] = this.pixelRGB[1] = green;
			this.resultRGB[2] = this.pixelRGB[2] = blue;
			this.maximum = this.selector.getNewValue(index, oldValue);
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
//			final int neighborHue = hue(neighborValue);
//			final int neighborSaturation = saturation(neighborValue);
//			final int neighborBrightness = brightness(neighborValue);
//			
//			if (cyclicDistance(this.pixelHue, neighborHue, 256) <= this.maximumAmplitude) {
//				this.sums[0] = this.count == 0 ? neighborHue : cyclicSumForMean(this.sums[0] / this.count, this.count, neighborHue, 1, 256);
//				this.sums[1] = max(this.sums[1], neighborSaturation);
//				this.sums[2] += neighborBrightness;
//				
//				++this.count;
//			}
			
			this.neighborRGB[0] = red(neighborValue);
			this.neighborRGB[1] = green(neighborValue);
			this.neighborRGB[2] = blue(neighborValue);
			final int distance = chebyshevDistance(this.pixelRGB, this.neighborRGB);
			final int neighborScore = this.selector.getNewValue(neighborIndex, neighborValue);
			
			if (distance <= this.maximumDistance && this.maximum < neighborScore) {
				System.arraycopy(this.neighborRGB, 0, this.resultRGB, 0, 3);
				this.maximum = neighborScore;
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			return argb(255, this.resultRGB[0], this.resultRGB[1], this.resultRGB[2]);
//			return 0 < this.count ?
//					HSBtoRGB((float) ((this.sums[0] / this.count) % 256) / 255F, this.sums[1] / 255F, (float) this.sums[2] / (255 * this.count)) : oldValue;
		}
		
	}
	
	public static final int cyclicSumForMean(final int currentMean, final int currentCount, final int newValue, final int newCount, final int n) {
		if (newValue < currentMean) {
			return cyclicSumForMean(newValue, newCount, currentMean, currentCount, n);
		}
		
		return newValue - currentMean <= n + currentMean - newValue ? currentMean * currentCount + newValue * newCount :
			(n + currentMean) * currentCount + newValue * newCount;
	}
	
	public static final int cyclicDistance(final int a, final int b, final int n) {
		if (b < a) {
			return cyclicDistance(b, a, n);
		}
		
		return min(b - a, n + a - b);
	}
	
	public static final int chebyshevDistance(final int[] u, final int[] v) {
		final int n = u.length;
		int result = 0;
		
		for (int i = 0; i < n; ++i) {
			final int d = abs(u[i] - v[i]);
			
			if (result < d) {
				result = d;
			}
		}
		
		return result;
	}
	
}
