package imj.apps.modules;

import static java.lang.Integer.parseInt;

import multij.context.Context;

/**
 * @author codistmonk (creation 2013-12-10)
 */
public final class BinaryTruncationViewFilter extends ViewFilter {
	
	private int bits;
	
	private int mask;
	
	public BinaryTruncationViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("bits", "4");
	}
	
	public final int transform(final int channelValue) {
		return channelValue & this.mask;
	}
	
	@Override
	protected final void doInitialize() {
		this.bits = parseInt(this.getParameters().get("bits").trim());
		this.mask = (~0) << this.bits;
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				return BinaryTruncationViewFilter.this.transform(channel.getValue(oldValue));
			}
			
		};
	}
	
}
