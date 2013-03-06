package imj.apps.modules;

import imj.IMJTools.StatisticsSelector;
import imj.apps.modules.FilteredImage.StatisticsFilter;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class StatisticsViewFilter extends ViewFilter.FromFilter {
	
	public StatisticsViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("statistic", "mean");
	}
	
	@Override
	protected final boolean isOutputMonochannel() {
		return true;
	}
	
	@Override
	protected final void doInitialize() {
		final StatisticsSelector feature = StatisticsSelector.valueOf(
				this.getParameters().get("statistic").toUpperCase(Locale.ENGLISH));
		
		this.setFilter(new StatisticsFilter(this.parseStructuringElement(), feature));
	}
	
}
