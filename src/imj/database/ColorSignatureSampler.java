package imj.database;

import static java.util.Arrays.sort;

import imj.Image;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Comparator;

/**
 * @author codistmonk (creation 2013-04-22)
 */
public final class ColorSignatureSampler extends HistogramSampler {
	
	public ColorSignatureSampler(final Image image, final Quantizer quantizer, final Channel[] channels,
			final SampleProcessor processor) {
		super(image, quantizer, channels, processor);
	}
	
	@Override
	protected final void sortIndices(final Integer[] indices, final int[] counts) {
		sort(indices, new Comparator<Integer>() {
			
			@Override
			public final int compare(final Integer i1, final Integer i2) {
				int result = counts[i2] - counts[i1];
				
				return result != 0 ? result : i2 - i1;
			}
			
		});
	}
	
}
