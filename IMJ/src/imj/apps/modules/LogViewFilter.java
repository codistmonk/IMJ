package imj.apps.modules;

import static java.lang.Math.log;
import static java.lang.Math.round;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class LogViewFilter extends ViewFilter {
	
	public LogViewFilter(final Context context) {
		super(context);
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return transform(channel.getValue(oldValue));
			}
			
		};
	}
	
	public static final double LOG_256 = log(256);
	
	public static final int transform(final int channelValue) {
		return (int) round(255 * log(1 + channelValue) / LOG_256);
	}
	
}
