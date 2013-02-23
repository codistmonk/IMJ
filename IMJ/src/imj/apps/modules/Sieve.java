package imj.apps.modules;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;

import java.util.Date;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class Sieve extends Plugin {
	
	private RegionOfInterest backupROI;
	
	private Sieve backupSieve;
	
	protected Sieve(final Context context) {
		super(context);
	}
	
	public final RegionOfInterest getROI() {
		return Sieve.getROI(this.getContext());
	}
	
	@Override
	public final void apply() {
		final TicToc timer = new TicToc();
		
		debugPrint("Applying", this, "(", new Date(timer.tic()), ")");
		
		this.cancel();
		
		final Context context = this.getContext();
		final RegionOfInterest roi = this.getROI();
		
		if (roi != null) {
			final int rowCount = roi.getRowCount();
			final int columnCount = roi.getColumnCount();
			final Image image = context.get("image");
			
			if (image != null && image.getRowCount() == rowCount && image.getColumnCount() == columnCount) {
				final int pixelCount = rowCount * columnCount;
				
				for (int pixel = 0; pixel < pixelCount; ++pixel) {
					if (roi.get(pixel) && !this.accept(pixel, image.getValue(pixel))) {
						roi.set(pixel, false);
					}
				}
				
				context.set("sieve", null);
				context.set("sieve", this);
			}
		}
		
		debugPrint("Done:", "time:", timer.toc(), "memory:", usedMemory());
	}
	
	@Override
	public final void backup() {
		final RegionOfInterest roi = this.getROI();
		this.backupROI = new RegionOfInterest(roi.getRowCount(), roi.getColumnCount());
		this.backupSieve = this.getContext().get("sieve");
		
		roi.copyTo(this.backupROI);
	}
	
	@Override
	public final void cancel() {
		this.backupROI.copyTo(this.getROI());
		this.getContext().set("sieve", this.backupSieve);
	}
	
	@Override
	public final void clearBackup() {
		this.backupROI = null;
		this.backupSieve = null;
	}
	
	public abstract boolean accept(int index, int value);
	
	public static final RegionOfInterest getROI(final Context context) {
		final RegionOfInterest[] rois = context.get("rois");
		final int lod = context.get("lod");
		
		return lod < rois.length ? rois[lod] : null;
	}
	
}
