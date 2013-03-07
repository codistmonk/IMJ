package imj.apps.modules;

import static java.lang.Integer.parseInt;
import imj.apps.modules.FilteredImage.RankFilter;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class RankViewFilter extends ViewFilter.FromFilter {
	
	public RankViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("rank", "0");
	}
	
	@Override
	protected final void doInitialize() {
		super.doInitialize();
		
		final int rank = parseInt(this.getParameters().get("rank"));
		
		this.setFilter(new RankFilter(this.parseStructuringElement(), rank));
	}
	
}
