package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import multij.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class IterativeBitRoundingViewFilter extends ViewFilter {
	
	private int bitCount;
	
	public IterativeBitRoundingViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("bitCount", "0");
	}
	
	public final int transform(final int channelValue) {
		int result = channelValue;
		
		for (int i = 1; i <= this.bitCount; ++i) {
			final int offset = 1 << (i - 1);
			final int mask = (~((1 << i) - 1)) & 0x7FFFFFFF;
			result = min(255, (result + offset) & mask);
		}
		
		return result;
	}
	
	@Override
	protected final void doInitialize() {
		this.bitCount = parseInt(this.getParameters().get("bitCount"));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return IterativeBitRoundingViewFilter.this.transform(channel.getValue(oldValue));
			}
			
		};
	}
	
}
