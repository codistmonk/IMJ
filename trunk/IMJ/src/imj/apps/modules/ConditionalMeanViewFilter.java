package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.IMJTools;
import imj.IMJTools.StatisticsSelector;
import imj.apps.modules.FilteredImage.StatisticsFilter;
import imj.apps.modules.FilteredImage.StructuringElementFilter;

import java.util.Arrays;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-03-25)
 */
public final class ConditionalMeanViewFilter extends ViewFilter.FromFilter {
	
	public ConditionalMeanViewFilter(final Context context) {
		super(context);
		
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
		
		private final int[] rgb;
		
		private final int[] minima;
		
		private final int[] maxima;
		
		private final int[] sums;
		
		private int amplitude;
		
		public ConditionalMeanFilter(final int[] deltas, final int maximumAmplitude) {
			super(deltas);
			this.maximumAmplitude = maximumAmplitude;
			this.rgb = new int[3];
			this.minima = new int[3];
			this.maxima = new int[3];
			this.sums = new int[3];
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			fill(this.minima, Integer.MAX_VALUE);
			fill(this.maxima, Integer.MIN_VALUE);
			fill(this.sums, 0);
			this.amplitude = 0;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			this.rgb[0] = red(neighborValue);
			this.rgb[1] = green(neighborValue);
			this.rgb[2] = blue(neighborValue);
			
			for (int i = 0; i < 3; ++i) {
				final int c = this.rgb[i];
				
				if (c < this.minima[i]) {
					this.minima[i] = c;
				}
				
				if (this.maxima[i] < c) {
					this.maxima[i] = c;
				}
				
				this.sums[i] += c;
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			int amplitude = 0;
			
			for (int i = 0; i < 3; ++i) {
				final int a = this.maxima[i] - this.minima[i];
				
				if (amplitude < a) {
					amplitude = a;
				}
			}
			
			return amplitude <= this.maximumAmplitude ? argb(255,
					this.sums[0] / this.getSize(), this.sums[1] / this.getSize(), this.sums[2] / this.getSize()) : oldValue;
		}
		
	}
	
}
