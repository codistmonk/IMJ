package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import multij.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class BitRoundingViewFilter extends ViewFilter {
	
	private int offset;
	
	private int mask;
	
	public BitRoundingViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("bitCount", "0");
	}
	
	public final int transform(final int channelValue) {
		return min(255, (channelValue + this.offset) & this.mask);
	}
	
	@Override
	protected final void doInitialize() {
		final int bitCount = parseInt(this.getParameters().get("bitCount"));
		
		this.offset = 1 << (bitCount - 1);
		this.mask = (~((1 << bitCount) - 1)) & 0x7FFFFFFF;
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return BitRoundingViewFilter.this.transform(channel.getValue(oldValue));
			}
			
		};
	}
	
}
