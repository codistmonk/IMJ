package imj.apps.modules;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.IMJTools.rgba;
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
	public final void initialize() {
		final int bitCount = parseInt(this.getParameters().get("bitCount"));
		
		this.offset = 1 << (bitCount - 1);
		this.mask = (~((1 << bitCount) - 1)) & 0x7FFFFFFF;
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue) {
		return rgba(255,
				this.transform(red(oldValue)),
				this.transform(green(oldValue)),
				this.transform(blue(oldValue)));
	}
	
	public final int transform(final int channelValue) {
		return min(255, (channelValue + this.offset) & this.mask);
	}
	
}
	