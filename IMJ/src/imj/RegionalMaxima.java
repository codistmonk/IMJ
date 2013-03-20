package imj;

import static imj.Labeling.NeighborhoodShape.CONNECTIVITY_8;
import static imj.MorphologicalOperations.StructuringElement.SIMPLE_CONNECTIVITY_8;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public final class RegionalMaxima extends RegionalExtrema {
	
	public RegionalMaxima(final Image image, final Image result, final int[] connectivity) {
		super(image, result, connectivity);
	}
	
	public RegionalMaxima(final Image image, final int[] connectivity) {
		super(image, connectivity);
	}
	
	@Override
	protected final int compare(final int value1, final int value2) {
		return value2 - value1;
	}
	
	public static final Image[] regionalMaxima26(final Image[] image, final Image[] result) {
		final int layerCount = image.length;
		final Image[] actualResult = result != null ? result : new Image[layerCount];
		
		for (int i = 0; i < layerCount; ++i) {
			actualResult[i] = new RegionalMaxima(image[0], CONNECTIVITY_8).getResult();
		}
		
		final Set<Integer>[] nonmaxima = new Set[layerCount];
		final int rowCount = image[0].getRowCount();
		final int columnCount = image[0].getColumnCount();
		
		for (int z = 0; z < layerCount; ++z) {
			nonmaxima[z] = new HashSet<Integer>();
			
			if (0 < z) {
				for (int rowIndex = 0; rowIndex < rowCount; ++rowIndex) {
					for (int columnIndex = 0; columnIndex < columnCount; ++columnIndex) {
						final int label = actualResult[z].getValue(rowIndex, columnIndex);
						
						if (label <= 0) {
							continue;
						}
						
						final int value = image[z].getValue(rowIndex, columnIndex);
						
						for (int i = 0; i < SIMPLE_CONNECTIVITY_8.length; i += 2) {
							final int r = rowIndex + SIMPLE_CONNECTIVITY_8[i];
							final int c = columnIndex + SIMPLE_CONNECTIVITY_8[i + 1];
							
							if (0 <= r && r < rowCount && 0 <= c && c < columnCount) {
								final int downstairsNeighborValue = image[z - 1].getValue(r, c);
								final int downstairsNeighborLabel = actualResult[z - 1].getValue(r, c);
								
								if (downstairsNeighborValue < value) {
									nonmaxima[z - 1].add(downstairsNeighborLabel);
								} else if (value < downstairsNeighborValue) {
									nonmaxima[z].add(label);
								}
							}
						}
					}
				}
			}
		}
		
		final int layerPixelCount = rowCount * columnCount;
		
		for (int z = 0, labelCount = 0; z < layerCount; ++z) {
			final Map<Integer, Integer> newLabels = new HashMap<Integer, Integer>();
			
			newLabels.put(0, 0);
			
			for (final Integer nonmaximum : nonmaxima[z]) {
				newLabels.put(nonmaximum, 0);
			}
			
			for (int pixel = 0; pixel < layerPixelCount; ++pixel) {
				final int label = actualResult[z].getValue(pixel);
				Integer newLabel = newLabels.get(label);
				
				if (newLabel == null) {
					newLabel = ++labelCount;
					newLabels.put(label, newLabel);
				}
				
				actualResult[z].setValue(pixel, newLabel);
			}
		}
		
		return actualResult;
	}
	
}
