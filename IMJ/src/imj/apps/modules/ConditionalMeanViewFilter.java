package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.brightness;
import static imj.IMJTools.green;
import static imj.IMJTools.hue;
import static imj.IMJTools.red;
import static imj.IMJTools.saturation;
import static java.awt.Color.HSBtoRGB;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Arrays.fill;

import java.awt.Color;

import imj.IMJTools;
import imj.apps.modules.FilteredImage.StructuringElementFilter;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-03-25)
 */
public final class ConditionalMeanViewFilter extends ViewFilter.FromFilter {
	
	public ConditionalMeanViewFilter(final Context context) {
		super(context);
		
		this.getParameters().remove("channels");
		this.getParameters().put("maximumAmplitude", "2");
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
		
		private final int[] pixelRGB;
		
		private final int[] neighborRGB;
		
		private final int[] sums;
		
		private int count;
		
		public ConditionalMeanFilter(final int[] deltas, final int maximumAmplitude) {
			super(deltas);
			this.maximumAmplitude = maximumAmplitude;
			this.pixelRGB = new int[3];
			this.neighborRGB = new int[3];
			this.sums = new int[3];
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			fill(this.sums, 0);
			this.pixelRGB[0] = red(oldValue);
			this.pixelRGB[1] = green(oldValue);
			this.pixelRGB[2] = blue(oldValue);
			this.count = 0;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			this.neighborRGB[0] = red(neighborValue);
			this.neighborRGB[1] = green(neighborValue);
			this.neighborRGB[2] = blue(neighborValue);
			
			if (chebyshevDistance(this.pixelRGB, this.neighborRGB) <= this.maximumAmplitude) {
				this.sums[0] += hue(neighborValue);
				this.sums[1] = max(this.sums[1], saturation(neighborValue));
				this.sums[2] += brightness(neighborValue);
				
				++this.count;
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			this.count *= 255;
			
			return 0 < this.count ?
					HSBtoRGB((float) this.sums[0] / this.count, this.sums[1] / 255F, (float) this.sums[2] / this.count) : oldValue;
		}
		
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
