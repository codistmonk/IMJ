package imj.apps.modules;

import imj.apps.modules.FilteredImage.StatisticsFilter;
import imj.apps.modules.FilteredImage.StatisticsFilter.ChannelStatistics;

import java.util.Locale;

import multij.context.Context;

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
		super.doInitialize();
		
		final ChannelStatistics.Selector feature = ChannelStatistics.Selector.valueOf(
				this.getParameters().get("statistic").toUpperCase(Locale.ENGLISH));
		
		this.setFilter(new StatisticsFilter(this.parseStructuringElement(), feature));
	}
	
}
