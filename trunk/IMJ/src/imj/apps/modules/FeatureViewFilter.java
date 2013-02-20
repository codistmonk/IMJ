package imj.apps.modules;

import static imj.IMJTools.argb;

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
	public final int getNewValue(final int index, final int oldValue, final Channel channel) {
		final int featureValue = this.feature.getNewValue(index, oldValue);
		
		switch (this.feature) {
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
	
	@Override
	protected final boolean splitInputChannels() {
		return false;
	}
	
}
