package imj.apps.modules;

import static imj.IMJTools.rgba;

import imj.apps.modules.SimpleSieve.Feature;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;

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
	public final void initialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(Locale.ENGLISH));
	}
	
	@Override
	public final int getNewValue(final int index, final int oldValue) {
		final int featureValue = this.feature.getNewValue(index, oldValue);
		
		switch (this.feature) {
		case RED:
			return rgba(255, featureValue, 0, 0);
		case GREEN:
			return rgba(255, 0, featureValue, 0);
		case BLUE:
			return rgba(255, 0, 0, featureValue);
		default:
			return rgba(255, featureValue, featureValue, featureValue);
		}
	}
	
}
