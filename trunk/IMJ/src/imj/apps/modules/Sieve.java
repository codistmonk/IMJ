package imj.apps.modules;

import imj.Image;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class Sieve extends Plugin {
	
	protected Sieve(final Context context) {
		super(context);
	}
	
	@Override
	public final void apply() {
		final Context context = this.getContext();
		
		final RegionOfInterest[] rois = context.get("rois");
		final int lod = context.get("lod");
		final RegionOfInterest roi = lod < rois.length ? rois[lod] : null;
		
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
	}
	
	public abstract boolean accept(int index, int value);
	
}
