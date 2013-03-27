package imj.apps.modules;

import static imj.IMJTools.brightness;
import static imj.IMJTools.hue;
import static imj.IMJTools.saturation;
import static java.awt.Color.HSBtoRGB;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import imj.apps.modules.FilteredImage.StructuringElementFilter;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-03-25)
 */
public final class ConditionalMeanViewFilter extends ViewFilter.FromFilter {
	
	public ConditionalMeanViewFilter(final Context context) {
		super(context);
		
		this.getParameters().remove("channels");
		this.getParameters().put("maximumAmplitude", "8");
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
		
		this.setFilter(new ConditionalMeanFilter(this.parseStructuringElement(), this.getIntParameter("maximumAmplitude")));
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class ConditionalMeanFilter extends StructuringElementFilter {
		
		private final int maximumAmplitude;
		
		private int pixelHue;
		
		private final int[] sums;
		
		private int count;
		
		public ConditionalMeanFilter(final int[] deltas, final int maximumAmplitude) {
			super(deltas);
			this.maximumAmplitude = maximumAmplitude;
			this.sums = new int[3];
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			fill(this.sums, 0);
			this.pixelHue = hue(oldValue);
			this.count = 0;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			final int neighborHue = hue(neighborValue);
			final int neighborSaturation = saturation(neighborValue);
			final int neighborBrightness = brightness(neighborValue);
			
			if (cyclicDistance(this.pixelHue, neighborHue, 256) <= this.maximumAmplitude) {
				this.sums[0] = this.count == 0 ? neighborHue : cyclicSumForMean(this.sums[0] / this.count, this.count, neighborHue, 1, 256);
				this.sums[1] = max(this.sums[1], neighborSaturation);
				this.sums[2] += neighborBrightness;
				
				++this.count;
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			return 0 < this.count ?
					HSBtoRGB((float) ((this.sums[0] / this.count) % 256) / 255F, this.sums[1] / 255F, (float) this.sums[2] / (255 * this.count)) : oldValue;
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
