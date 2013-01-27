package imj;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class Watershed extends Labeling {
	
	public Watershed(final Image image, final Image initialLabels, final int[] connectivity) {
		super(image);
		
		final int levelCount = 256;
		final int pixelCount = this.getPixelCount();
		final Neighborhood neighborhood = this.new Neighborhood(connectivity);
		final Neighborhood neighborhood2 = this.new Neighborhood(connectivity);
		final IntList[] levelContours = new IntList[levelCount];
		
		for (int i = 0; i < levelCount; ++i) {
			levelContours[i] = new IntList();
		}
		
		// copy_labels_and_extract_contours:
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			final int pixelLabel = initialLabels.getValue(pixel);
			
			this.getResult().setValue(pixel, pixelLabel);
			
			if (0 != pixelLabel) {
				neighborhood.reset(pixel);
				
				while (neighborhood.hasNext()) {
					final int neighbor = neighborhood.getNext();
					
					if (0 == initialLabels.getValue(neighbor)) {
						levelContours[image.getValue(pixel)].add(pixel);
						break;
					}
				}
			}
		}
		
		// grow_contours:
		for (int level = 0; level < levelCount; ++level) {
			final IntList contourPixels = levelContours[level];
			
			while (!contourPixels.isEmpty()) {
				final int pixel = contourPixels.remove(0);
				final int pixelLabel = this.getResult().getValue(pixel);
				
				assert 0 < pixelLabel;
				
				neighborhood.reset(pixel);
				
				while (neighborhood.hasNext()) {
					final int neighbor = neighborhood.getNext();
					final int neighborLabel = this.getResult().getValue(neighbor);
					
					if (0 == neighborLabel) {
						final int neighborValue = image.getValue(neighbor);
						final int distance = neighborValue - level;
						
						if (0 == distance) {
							this.getResult().setValue(neighbor, pixelLabel);
							contourPixels.add(neighbor);
						} else if (0 < distance) {
							boolean closerPixelFound = false;
							
							if (DONT_GROW_UNLESS_PIXEL_IS_CLOSEST_TO_NEIGHBOR) {
								neighborhood2.reset(neighbor);
								
								while (neighborhood2.hasNext() && !closerPixelFound) {
									final int neighborNeighbor = neighborhood2.getNext();
									final int distance2 = neighborValue - image.getValue(neighborNeighbor);
									
									closerPixelFound |= 0 < distance2 && distance2 < distance;
								}
							}
							
							if (!closerPixelFound) {
								this.getResult().setValue(neighbor, pixelLabel);
								levelContours[neighborValue].add(neighbor);
							}
						} else {
							throw new IllegalStateException();
						}
					}
				}
			}
		}
	}
	
	/**
	 * {@value}.
	 */
	private static final boolean DONT_GROW_UNLESS_PIXEL_IS_CLOSEST_TO_NEIGHBOR = false;
	
}
