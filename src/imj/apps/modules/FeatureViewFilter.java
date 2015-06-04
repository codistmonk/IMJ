package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.apps.modules.ViewFilter.ComplexFilter.DEFAULT_OUTPUT_MONOCHANNEL;
import imj.apps.modules.SimpleSieve.Feature;

import java.util.Locale;

import multij.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class FeatureViewFilter extends ViewFilter {
	
	private Feature feature;
	
	public FeatureViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("feature", "brightness");
	}
	
	@Override
	protected final void doInitialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(Locale.ENGLISH));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, DEFAULT_OUTPUT_MONOCHANNEL) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				final int featureValue = feature.getNewValue(index, oldValue);
				
				switch (feature) {
				case RED:
					return argb(255, featureValue, 0, 0);
				case GREEN:
					return argb(255, 0, featureValue, 0);
				case BLUE:
					return argb(255, 0, 0, featureValue);
				default:
					return argb(255, featureValue, featureValue, featureValue);
				}
			}
			
		};
	}
	
}
