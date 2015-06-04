package imj;

import jgencode.primitivelists.IntList;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class Watershed extends Labeling {
	
	public Watershed(final Image image, final Image initialLabels, final int[] connectivity) {
		super(image);
		
		final int levelCount = 256;
		final int pixelCount = this.getPixelCount();
		final Neighborhood neighborhood = this.new Neighborhood(connectivity);
		final IntList[] levelPixels = new IntList[levelCount];
		
		for (int i = 0; i < levelCount; ++i) {
			levelPixels[i] = new IntList();
		}
		
		assert this.levelsAreConsistent(levelPixels);
		
		// copy_labels_and_extract_contours:
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int pixelLabel = initialLabels.getValue(pixel);
			
			this.getResult().setValue(pixel, pixelLabel);
			
			if (0 != pixelLabel) {
				neighborhood.reset(pixel);
				
				while (neighborhood.hasNext()) {
					final int neighbor = neighborhood.getNext();
					
					if (0 == initialLabels.getValue(neighbor)) {
						levelPixels[image.getValue(pixel)].add(pixel);
						break;
					}
				}
			}
		}
		
		// grow_contours:
		for (int level = 0; level < levelCount; ++level) {
			final IntList contourPixels = levelPixels[level];
			
			while (!contourPixels.isEmpty()) {
				final int pixel = contourPixels.remove(0);
				final int pixelLabel = this.getResult().getValue(pixel);
				
				assert 0 < pixelLabel;
				
				neighborhood.reset(pixel);
				
				while (neighborhood.hasNext()) {
					final int neighbor = neighborhood.getNext();
					final int neighborLabel = this.getResult().getValue(neighbor);
					
					if (0 == neighborLabel) {
						levelPixels[image.getValue(neighbor)].add(neighbor);
						this.getResult().setValue(neighbor, pixelLabel);
					}
				}
			}
		}
	}
	
	private final boolean levelsAreConsistent(final IntList[] levels) {
		final int n = levels.length;
		
		for (int i = 0; i < n; ++i) {
			final IntList levelI = levels[i];
			final int m = levelI.size();
			
			for (int j = 0; j < m; ++j) {
				if (this.getImage().getValue(levelI.get(j)) != i) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * {@value}.
	 */
	private static final boolean DONT_GROW_UNLESS_PIXEL_IS_CLOSEST_TO_NEIGHBOR = false;
	
}
