package imj.database;

import static imj.apps.modules.ShowActions.baseName;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.loadRegions;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj.ByteList;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.Sampler.SampleProcessor;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-09-25)
 */
public final class ARFFExporterTest {
	
	@Test
	public final void test() {
		final String[] imageIds = {
				"../Libraries/images/svs/45656.svs",
				"../Libraries/images/svs/45657.svs",
				"../Libraries/images/svs/45659.svs",
				"../Libraries/images/svs/45660.svs",
				"../Libraries/images/svs/45662.svs",
				"../Libraries/images/svs/45668.svs",
				"../Libraries/images/svs/45683.svs"
		};
		
		final Quantizer[] quantizers = new Quantizer[imageIds.length];
		final int quantizationLevel = 4;
		final int lod = 4;
		final int tileRowCount = 32;
		
		final Class<? extends Sampler> samplerFactory = SparseHistogramSampler.class;
		final Channel[] channels = RGB;
		final Segmenter trainingSegmenter = new SeamGridSegmenter(tileRowCount);
		
		for (int i = 0; i < imageIds.length; ++i) {
			final String imageId = imageIds[i];
			
			debugPrint("imageId:", imageId);
			
			final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			gc();
			
			debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
			
			quantizers[i] = new BinningQuantizer();
			quantizers[i].initialize(image, null, channels, quantizationLevel);
			
			final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
			final int imageRowCount = image.getRowCount();
			final int imageColumnCount = image.getColumnCount();
			final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
			
			loadRegions(imageId, lod, imageRowCount, imageColumnCount, annotations, classes);
			
			final Sampler sampler;
			
			try {
 				sampler = samplerFactory.getConstructor(Image.class, Quantizer.class, Channel[].class, SampleProcessor.class)
						.newInstance(image, quantizers[i], channels, new SampleProcessor() {
							
							private final Collection<String> group = new HashSet<String>();
							
							@Override
							public final void processSample(final ByteList sample) {
								// TODO(codistmonk) normalize group
								// TODO(codistmonk) write ARFF line
								this.group.clear();
							}
							
							@Override
							public final void processPixel(final int pixel, final int pixelValue) {
								for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
									if (entry.getValue().get(pixel)) {
										this.group.add(entry.getKey());
									}
								}
							}
							
						});
			} catch (final Exception exception) {
				throw unchecked(exception);
			}

			trainingSegmenter.process(image, sampler);
		}
	}
	
}
