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
	
	private RankFilter filter2;
	
	private RegionOfInterest backup;
	
	private RegionOfInterest tmp;
	
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
		final String operationType = operationParameters[0].toLowerCase(Locale.ENGLISH);
		final int rank;
		int rank2 = Integer.MIN_VALUE;
		
		if ("rank".equals(operationType)) {
			rank = Integer.parseInt(operationParameters[1]);
		} else if ("dilate".equals(operationType)) {
			rank = -1;
		} else if ("erode".equals(operationType)) {
			rank = 0;
		} else if ("open".equals(operationType)) {
			rank = 0;
			rank2 = -1;
		} else if ("close".equals(operationType)) {
			rank = -1;
			rank2 = 0;
		} else {
			throw new IllegalArgumentException("Invalid operation: " + operationParameters[0]);
		}
		
		final int[] structuringElement = ViewFilter.parseStructuringElement(this.getParameters().get("structuringElement"));
		
		this.filter = new RankFilter(structuringElement, rank);
		
		if (rank2 != Integer.MIN_VALUE) {
			this.filter2 = new RankFilter(structuringElement, rank2);
			final RegionOfInterest roi = this.getROI();
			this.tmp = new RegionOfInterest(roi.getRowCount(), roi.getColumnCount());
		} else {
			this.filter2 = null;
		}
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
		
		if (this.filter2 != null) {
			this.filter.setImage(this.backup);
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				this.tmp.setValue(pixel, this.filter.getNewValue(pixel, this.backup.getValue(pixel), Channel.Primitive.INT));
			}
			
			this.filter2.setImage(this.tmp);
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				roi.setValue(pixel, this.filter2.getNewValue(pixel, this.tmp.getValue(pixel), Channel.Primitive.INT));
			}
		} else {
			this.filter.setImage(this.backup);
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				roi.setValue(pixel, this.filter.getNewValue(pixel, this.backup.getValue(pixel), Channel.Primitive.INT));
			}
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
		this.filter = null;
		this.filter2 = null;
	}
	
}
