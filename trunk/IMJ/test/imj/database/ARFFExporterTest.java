package imj.database;

import static imj.IMJTools.argb;
import static imj.IMJTools.unsigned;
import static imj.apps.GenerateClassificationData.resetExcludedRegions;
import static imj.apps.modules.ShowActions.baseName;
import static imj.database.HistogramSampler.COUNT_QUANTIZATION_MASK;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.loadRegions;
import static java.util.Collections.unmodifiableMap;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.ByteList;
import imj.IMJTools;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.GenerateClassificationData;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.Sampler.SampleProcessor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

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
			// TODO(codistmonk) write to file
			// TODO(codistmonk) write ARFF header
			final PrintStream out = System.out;
			
			try {
 				sampler = samplerFactory.getConstructor(Image.class, Quantizer.class, Channel[].class, SampleProcessor.class)
						.newInstance(image, quantizers[i], channels, new SampleProcessor() {
							
							private final Collection<String> group = new TreeSet<String>();
							
							@Override
							public final void processSample(final ByteList sample) {
								final int n = sample.size();
								
								out.print("{");
								
								for (int i = 0; i < n; i += 4) {
									final double frequency = unsigned(sample.get(i + 3)) / (double) COUNT_QUANTIZATION_MASK;
									
									if (0.0 != frequency) {
										final int color = argb(0, unsigned(sample.get(i + 2)),
												unsigned(sample.get(i + 1)), unsigned(sample.get(i + 0)));
										
										out.print(color);
										out.print(" ");
										out.print(frequency);
										out.print(", ");
									}
								}
								
								out.print(1 << 24);
								out.print(" ");
								out.print(formatGroup(this.group));
								
								out.println("}");
								
								this.group.clear();
							}
							
							@Override
							public final void processPixel(final int pixel, final int pixelValue) {
								for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
									if (entry.getValue().get(pixel)) {
										this.group.add(CLASS_ALIASES.get(entry.getKey()));
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
	
	@SuppressWarnings("unchecked")
	static final Map<String, String> CLASS_ALIASES = unmodifiableMap((Map<String, String>) map(
			"Edges & Artifacts to be excluded", "EXCLUDED",
			"invasive tumor (diffusely infiltrating pre-existing tissues)", "DIFFUSE",
			"Invasive tumor (solid formations)", "SOLID",
			"Intersecting stromal bands", "STROMA",
			"Non-neoplastic glands and ducts", "NONNEOPLASTIC",
			"DCIS and invasive inside ductal structures", "DCIS",
			null, "UNKNOWN"
	));
	
	public static final LinkedHashMap<?, ?> map(final Object... keyAndValues) {
		final int n = keyAndValues.length;
		final LinkedHashMap<Object, Object> result = new LinkedHashMap<Object, Object>(n / 2);
		
		for (int i = 0; i < n; i += 2) {
			result.put(keyAndValues[i], keyAndValues[i + 1]);
		}
		
		return result;
	}
	
	public static final String formatGroup(final Iterable<String> group) {
		final StringBuilder resultBuilder = new StringBuilder();
		final Iterator<String> i = group.iterator();
		
		resultBuilder.append('"');
		
		if (i.hasNext()) {
			resultBuilder.append(i.next());
			
			while (i.hasNext()) {
				resultBuilder.append(" ").append(i.next());
			}
		}
		
		resultBuilder.append('"');
		
		return resultBuilder.toString();
	}
	
}
