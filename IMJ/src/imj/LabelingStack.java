package imj;

import static imj.IMJTools.getRegionStatistics;
import static imj.IMJTools.newImage;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.IMJTools.StatisticsSelector;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public abstract class LabelingStack {
	
	private final List<Image> images;
	
	private final List<Labeling> watersheds;
	
	private final List<Statistics[]> statistics;
	
	private final StatisticsSelector reconstructionFeature;
	
	protected LabelingStack(final Image image, final int n, final StatisticsSelector reconstructionFeature) {
		this.images = new ArrayList<Image>();
		this.watersheds = new ArrayList<Labeling>();
		this.statistics = new ArrayList<Statistics[]>();
		this.reconstructionFeature = reconstructionFeature;
		
		this.images.add(image);
		this.addWatershedAndStatistics(image);
		
		for (int i = 2; i <= n; ++i) {
			this.addImageAndWatershedAndStatistics();
		}
	}
	
	public final int getImageCount() {
		return this.images.size();
	}
	
	public final Image getImage(final int index) {
		return this.images.get(index);
	}
	
	public final Image getImageLabels(final int index) {
		return this.watersheds.get(index).getResult();
	}
	
	public final Statistics[] getImageRegionStatistics(final int index) {
		return this.statistics.get(index);
	}
	
	public final Image[] getAllImages() {
		return this.images.toArray(new Image[this.getImageCount()]);
	}
	
	public final Image[] getAllLabels() {
		final int n = this.getImageCount();
		final Image[] result = new Image[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = this.getImageLabels(i);
		}
		
		return result;
	}
	
	/**
	 * Called during initialization.
	 * 
	 * @param image
	 * <br>Not null
	 * @return
	 * <br>Must not be null
	 */
	protected abstract Labeling newLabeling(Image image);
	
	private final void addImageAndWatershedAndStatistics() {
		final int i = this.getImageCount() - 1;
		final Image image = newImage(this.getImageLabels(i), this.getImageRegionStatistics(i), this.reconstructionFeature);
		this.images.add(image);
		this.addWatershedAndStatistics(image);
	}
	
	private final void addWatershedAndStatistics(final Image image) {
		final Labeling watershed = this.newLabeling(image);
		this.watersheds.add(watershed);
		this.statistics.add(getRegionStatistics(image, watershed.getResult()));
	}
	
	@Deprecated
	public static final LabelingStack newInstanceFor(final Image image, final int n,
			final StatisticsSelector reconstructionFeature, final Class<? extends Labeling> labelingClass) {
		return new LabelingStack(image, n, reconstructionFeature) {
			
			@Override
			protected final Labeling newLabeling(final Image image) {
				try {
					return labelingClass.getConstructor(Image.class).newInstance(image);
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			}
		};
	}
	
}
