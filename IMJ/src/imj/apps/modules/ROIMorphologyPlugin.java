package imj.apps.modules;

import imj.apps.modules.FilteredImage.RankFilter;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-23)
 */
public final class ROIMorphologyPlugin extends Plugin {
	
	private RankFilter filter;
	
	private RegionOfInterest backup;
	
	public ROIMorphologyPlugin(final Context context) {
		super(context);
		
		this.getParameters().put("operation", "rank -1");
		this.getParameters().put("structuringElement", "disk 1 chessboard");
	}
	
	public final RegionOfInterest getROI() {
		return Sieve.getROI(this.getContext());
	}
	
	@Override
	public final void initialize() {
		final String[] operationParameters = this.getParameters().get("operation").trim().split("\\s+");
		final int rank;
		
		if ("rank".equals(operationParameters[0].toLowerCase(Locale.ENGLISH))) {
			rank = Integer.parseInt(operationParameters[1]);
		} else {
			throw new IllegalArgumentException("Invalid operation: " + operationParameters[0]);
		}
		
		final int[] structuringElement = ViewFilter.parseStructuringElement(this.getParameters().get("structuringElement"));
		
		this.filter = new RankFilter(structuringElement, rank);
	}
	
	@Override
	public final void backup() {
		final RegionOfInterest roi = this.getROI();
		this.backup = new RegionOfInterest(roi.getRowCount(), roi.getColumnCount());
		roi.copyTo(this.backup);
	}
	
	@Override
	public final void apply() {
		final RegionOfInterest roi = this.getROI();
		final int pixelCount = roi.getPixelCount();
		
		this.filter.setImage(this.backup);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			roi.setValue(pixel, this.filter.getNewValue(pixel, this.backup.getValue(pixel), Channel.Primitive.INT));
		}
		
		fireUpdate(this.getContext(), "sieve");
	}
	
	@Override
	public final void cancel() {
		this.backup.copyTo(this.getROI());
		Plugin.fireUpdate(this.getContext(), "rois");
	}
	
	@Override
	public final void clearBackup() {
		this.backup = null;
	}
	
}
