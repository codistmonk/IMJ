package imj3.draft.segmentation;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;

import imj2.draft.PaletteBasedHistograms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import jgencode.primitivelists.IntList;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-01-20)
 */
public final class SegmentationTools {
	
	private SegmentationTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final double f1(final int[][] confusionMatrix, final int i) {
		final int n = confusionMatrix.length;
		final int tp = confusionMatrix[i][i];
		int fp = 0;
		int fn = 0;
		
		for (int j = 0; j < n; ++j) {
			if (i != j) {
				fp += confusionMatrix[j][i];
				fn += confusionMatrix[i][j];
			}
		}
		
		return 2.0 * tp / (2.0 * tp + fp + fn);
	}
	
	public static final double score(final int[][] confusionMatrix) {
		final int n = confusionMatrix.length;
		double result = 1.0;
		
		for (int i = 0; i < n; ++i) {
			result *= f1(confusionMatrix, i);
		}
		
		return result;
	}
	
	public static final Iterable<double[]> valuesAndWeights(final BufferedImage image, final IntList pixels, final int patchSize) {
		return new Iterable<double[]>() {
			
			@Override
			public final Iterator<double[]> iterator() {
				final int n = patchSize * patchSize;
				
				return new Iterator<double[]>() {
					
					private final int[] buffer = new int[n];
					
					private final double[] result = new double[n * 3 + 1];
					
					private int i = 0;
					
					{
						this.result[n * 3] = 1.0;
					}
					
					@Override
					public final boolean hasNext() {
						return this.i < pixels.size();
					}
					
					@Override
					public final double[] next() {
						final int pixel = pixels.get(this.i++);
						
						// TODO use actual factory from classifier
						ClassifierRawPrototype.FACTORY.extractData(image, pixel % image.getWidth(), pixel / image.getWidth(), patchSize, this.buffer);
						
						for (int i = 0; i < n; ++i) {
							final int rgb = this.buffer[i];
							this.result[3 * i + 0] = red8(rgb);
							this.result[3 * i + 1] = green8(rgb);
							this.result[3 * i + 2] = blue8(rgb);
						}
						
						return this.result;
					}
					
				};
			}
			
		};
	}
	
	public static final BufferedImage newMaskFor(final BufferedImage image) {
		final BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		
		{
			final Graphics2D g = result.createGraphics();
			
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		}
		
		return result;
	}
	
	public static final void outlineSegments(final BufferedImage segments, final BufferedImage labels,
			final BufferedImage mask, final BufferedImage image) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		
		PaletteBasedHistograms.forEachPixelIn(segments, (x, y) -> {
			if (mask == null || (mask.getRGB(x, y) & 1) != 0) {
				final int segmentId = segments.getRGB(x, y);
				
				if (0 != segmentId) {
					final int eastId = x + 1 < imageWidth ? segments.getRGB(x + 1, y) : segmentId;
					final int westId = 1 < x ? segments.getRGB(x - 1, y) : segmentId;
					final int southId = y + 1 < imageHeight ? segments.getRGB(x, y + 1) : segmentId;
					final int northId = 1 < y ? segments.getRGB(x, y - 1) : segmentId;
					
					if (edge(segmentId, eastId) || edge(segmentId, southId) || edge(segmentId, westId) || edge(segmentId, northId)) {
						image.setRGB(x, y, labels.getRGB(x, y));
					}
				}
			}
			
			return true;
		});
	}
	
	private static final boolean edge(final int segmentId1, final int segmentId2) {
		return segmentId1 != segmentId2;
	}
	
}
