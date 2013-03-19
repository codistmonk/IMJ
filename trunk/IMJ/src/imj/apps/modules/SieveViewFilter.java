package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.util.Locale.ENGLISH;
import imj.apps.modules.SimpleSieve.Feature;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class SieveViewFilter extends ViewFilter {
	
	private Feature feature;
	
	private int minimum;
	
	private int maximum;
	
	public SieveViewFilter(final Context context) {
		super(context);
		
		this.getParameters().clear();
		
		this.getParameters().put("feature", "brightness");
		this.getParameters().put("minimum", "0");
		this.getParameters().put("maximum", "255");
	}
	
	public final int transform(final int index, final int oldValue) {
		final int featureValue = this.feature.getNewValue(index, oldValue);
		
		return this.minimum <= featureValue && featureValue <= this.maximum ? oldValue : 0;
	}
	
	@Override
	protected final void doInitialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(ENGLISH));
		this.minimum = parseInt(this.getParameters().get("minimum"));
		this.maximum = parseInt(this.getParameters().get("maximum"));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return SieveViewFilter.this.transform(index, oldValue);
			}
			
		};
	}
	
}
