package imj.database;

import static imj.IMJTools.argb;
import static imj.IMJTools.unsigned;
import static imj.apps.modules.ShowActions.baseName;
import static imj.database.HistogramSampler.COUNT_QUANTIZATION_MASK;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.loadRegions;
import static java.util.Collections.unmodifiableMap;
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

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
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
		
		for (final int lod : new int[] { 4, 2 }) {
			for (final int q : new int[] { 4, 2 }) {
				for (final int s : new int[] { 32, 16 }) {
					for (int i = 0; i < imageIds.length; ++i) {
						exportARFF(imageIds[i], lod, q, s);
					}
				}
			}
		}
	}
	
	public static final void exportARFF(final String imageId, final int lod,
			final int quantizationLevel, final int tileRowCount) {
		final Channel[] channels = RGB;
		final Segmenter trainingSegmenter = new SeamGridSegmenter(tileRowCount);
		
		debugPrint("imageId:", imageId);
		
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		gc();
		
		debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
		
		final Quantizer quantizer = new BinningQuantizer();
		quantizer.initialize(image, null, channels, quantizationLevel);
		
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
		
		loadRegions(imageId, lod, imageRowCount, imageColumnCount, annotations, classes);
		
		final int significantBits = 8 - quantizationLevel;
		final int classKey = 1 << (3 * significantBits);
		final Sampler sampler;
		final PrintStream[] out = { null };
		// TODO(codistmonk) write to file
		// TODO(codistmonk) write ARFF header
		try {
			out[0] = new PrintStream(new File(baseName(imageId) +
					"_lod" + lod + "_q" + quantizationLevel + "_s" + tileRowCount + ".arff"));
			
			out[0].println("@RELATION \"" + baseName(imageId) + "\"");
			out[0].println();
			
			for (int i = 0; i < classKey; ++i) {
				out[0].println("@ATTRIBUTE color" + i + " numeric");
			}
			
			out[0].println("@ATTRIBUTE class string");
			
			out[0].println();
			out[0].println("@DATA");
			
			sampler = new SparseHistogramSampler(image, quantizer, channels, new SampleProcessor() {
						
						private final Collection<String> group = new TreeSet<String>();
						
						@Override
						public final void processSample(final ByteList sample) {
							final int n = sample.size();
							
							out[0].print("{");
							
							for (int i = 0; i < n; i += 4) {
								final double frequency = unsigned(sample.get(i + 3)) / (double) COUNT_QUANTIZATION_MASK;
								
								if (0.0 != frequency) {
									final int rKey = unsigned(sample.get(i + 2)) >> quantizationLevel;
									final int gKey = unsigned(sample.get(i + 1)) >> quantizationLevel;
									final int bKey = unsigned(sample.get(i + 0)) >> quantizationLevel;
									final int color = (rKey << (2 * significantBits)) | (gKey << significantBits) | bKey;
									
									out[0].print(color);
									out[0].print(" ");
									out[0].print(frequency);
									out[0].print(", ");
								}
							}
							
							out[0].print(classKey);
							out[0].print(" ");
							out[0].print(formatGroup(this.group));
							
							out[0].println("}");
							out[0].flush();
							
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
			
			trainingSegmenter.process(image, sampler);
		} catch (final Exception exception) {
			throw unchecked(exception);
		} finally {
			if (out[0] != null) {
				out[0].close();
			}
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
