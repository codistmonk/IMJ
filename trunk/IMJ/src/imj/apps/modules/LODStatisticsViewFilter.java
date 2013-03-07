package imj.apps.modules;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import imj.IMJTools.StatisticsSelector;
import imj.Image;
import imj.ImageWrangler;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class LODStatisticsViewFilter extends ViewFilter {
	
	private int sourceLOD;
	
	private Image sourceImage;
	
	private int currentLOD;
	
	private Image currentImage;
	
	private StatisticsSelector feature;
	
	private final Statistics statistics;
	
	public LODStatisticsViewFilter(final Context context) {
		super(context);
		this.statistics = new Statistics();
		
		this.getParameters().put("statistic", "mean");
		this.getParameters().put("lod", "-1");
	}
	
	@Override
	protected final void doInitialize() {
		final String imageId = this.getContext().get("imageId");
		this.currentLOD = this.getContext().get("lod");
		this.currentImage = this.getContext().get("image");
		this.sourceLOD = parseInt(this.getParameters().get("lod"));
		
		if (this.sourceLOD < 0) {
			this.sourceLOD = max(0, this.currentLOD + this.sourceLOD);
		}
		
		this.sourceImage = ImageWrangler.INSTANCE.load(imageId, this.sourceLOD);
		this.feature = StatisticsSelector.valueOf(this.getParameters().get("statistic").toUpperCase(Locale.ENGLISH));
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return this.new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				final int rowIndex = index / currentImage.getColumnCount();
				final int columnIndex = index % currentImage.getColumnCount();
				final int scale = (int) pow(2.0, currentLOD - sourceLOD);
				final int rStart = rowIndex * scale;
				final int cStart = columnIndex * scale;
				final int rEnd = min(sourceImage.getRowCount(), rStart + scale);
				final int cEnd = min(sourceImage.getColumnCount(), cStart + scale);
				
				statistics.reset();
				
				for (int r = rStart; r < rEnd; ++r) {
					for (int c = cStart; c < cEnd; ++c) {
						statistics.addValue(channel.getValue(sourceImage.getValue(r, c)));
					}
				}
				
				return (int) feature.getValue(statistics);
			}
			
			@Override
			protected final boolean isOutputMonochannel() {
				return true;
			}
			
		};
	}
	
}
