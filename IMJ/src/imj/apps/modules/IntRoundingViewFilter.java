package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-03-08)
 */
public final class IntRoundingViewFilter extends ViewFilter {
	
	private int step;
	
	private boolean roundUp;
	
	private boolean roundDown;
	
	public IntRoundingViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("step", "4");
		this.getParameters().put("roundUp", "1");
		this.getParameters().put("roundDown", "1");
	}
	
	public final int transform(final int channelValue) {
		if (this.step <= 1) {
			return channelValue;
		}
		
		final int mod = channelValue % this.step;
		final boolean isUp = (this.step - mod) <= mod;
		
		return min(255, isUp ? (this.roundUp ? channelValue + this.step - mod : channelValue) :
			(this.roundDown ? channelValue - mod : channelValue));
	}
	
	@Override
	protected final void doInitialize() {
		this.step = parseInt(this.getParameters().get("step").trim());
		this.roundUp = Annotations.parseBoolean(this.getParameters().get("roundUp").trim());
		this.roundDown = Annotations.parseBoolean(this.getParameters().get("roundDown").trim());
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return IntRoundingViewFilter.this.transform(channel.getValue(oldValue));
			}
			
		};
	}
	
}
