package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class RoundingViewFilter extends ViewFilter {
	
	private int offset;
	
	private int mask;
	
	public RoundingViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("bitCount", "0");
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
		return this.transform(channel.getValue(oldValue));
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
		return this.new ComplexFilter() {
			// NOP
		};
	}
	
}
	