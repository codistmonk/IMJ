package imj;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import net.sourceforge.aprog.tools.Tools;

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
	
	public static final Image[] watershedTopDown26(final Image[] image, final Image[] result) {
		final int[][] neighborhood = {
				{ -1, -1, -1 },
				{ -1, -1, +0 },
				{ -1, -1, +1 },
				{ -1, +0, -1 },
				{ -1, +0, +0 },
				{ -1, +0, +1 },
				{ -1, +1, -1 },
				{ -1, +1, +0 },
				{ -1, +1, +1 },
				
				{ +0, -1, -1 },
				{ +0, -1, +0 },
				{ +0, -1, +1 },
				{ +0, +0, -1 },
				{ +0, +0, +1 },
				{ +0, +1, -1 },
				{ +0, +1, +0 },
				{ +0, +1, +1 },
				
				{ +1, -1, -1 },
				{ +1, -1, +0 },
				{ +1, -1, +1 },
				{ +1, +0, -1 },
				{ +1, +0, +0 },
				{ +1, +0, +1 },
				{ +1, +1, -1 },
				{ +1, +1, +0 },
				{ +1, +1, +1 },
		};
		
		final int layerCount = image.length;
		final int rowCount = image[0].getRowCount();
		final int columnCount = image[0].getColumnCount();
		final int layerPixelCount = rowCount * columnCount;
		final IntList todo = new IntList();
		int processedPixels = 0;
		
		for (int layerIndex = 0; layerIndex < layerCount; ++layerIndex) {
			for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
					final int label = result[layerIndex].getValue(rowIndex, columnIndex);
					
					if (0 < label) {
						todo.add(layerIndex * layerPixelCount + rowIndex * columnCount + columnIndex);
						++processedPixels;
					}
				}
			}
		}
		
		while (!todo.isEmpty()) {
			final int pixel = todo.remove(0);
			final int layerIndex = pixel / layerPixelCount;
			final int rowIndex = (pixel % layerPixelCount) / columnCount;
			final int columnIndex = (pixel % layerPixelCount) % columnCount;
			final int pixelValue = image[layerIndex].getValue(rowIndex, columnIndex);
			final int pixelLabel = result[layerIndex].getValue(rowIndex, columnIndex);
			
			for (final int[] dzrc : neighborhood) {
				final int z = layerIndex + dzrc[0];
				final int r = rowIndex + dzrc[1];
				final int c = columnIndex + dzrc[2];
				
				if (0 <= z && z < layerCount && 0 <= r && r < rowCount && 0 <= c && c < columnCount) {
					final int neighbor = z * layerPixelCount + r * columnCount + c;
					final int neighborValue = image[z].getValue(r, c);
					final int neighborLabel = result[z].getValue(r, c);
					
					if (neighborValue <= pixelValue && neighborLabel <= 0) {
//					if (neighborLabel <= 0) {
						result[z].setValue(r, c, pixelLabel);
						todo.add(neighbor);
						++processedPixels;
					}
				}
			}
		}
		
		debugPrint(processedPixels, layerCount * layerPixelCount);
		
		for (int layerIndex = 0; layerIndex < layerCount; ++layerIndex) {
			for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
				for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
					final int label = result[layerIndex].getValue(rowIndex, columnIndex);
					
					if (label == 0) {
						debugPrint(layerIndex, rowIndex, columnIndex, result[layerIndex].getValue(rowIndex, columnIndex), image[layerIndex].getValue(rowIndex, columnIndex));
						
						for (final int[] dzrc : neighborhood) {
							final int z = layerIndex + dzrc[0];
							final int r = rowIndex + dzrc[1];
							final int c = columnIndex + dzrc[2];
							
							if (0 <= z && z < layerCount && 0 <= r && r < rowCount && 0 <= c && c < columnCount) {
								debugPrint(z, r, c, result[z].getValue(r, c), image[z].getValue(r, c));
							}
						}
						
						throw new IllegalStateException();
					}
				}
			}
		}
		
		return result;
	}
	
}
