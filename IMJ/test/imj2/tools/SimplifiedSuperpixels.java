package imj2.tools;

import static imj.IMJTools.loadAndTryToCache;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newSampler;
import static java.lang.Integer.parseInt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.xml.XMLTools.getNode;
import static net.sourceforge.aprog.xml.XMLTools.getNodes;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgencode.primitivelists.IntList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.apps.GenerateSampleDatabase.Configuration;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BinningQuantizer;
import imj.database.PatchDatabase;
import imj.database.Quantizer;
import imj.database.Sample;
import imj.database.Sampler;
import imj.database.BKSearch.BKDatabase;
import imj.database.Segmenter;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2014-06-15)
 */
public final class SimplifiedSuperpixels {
	
	private SimplifiedSuperpixels() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final TicToc timer = new TicToc();
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("file", "");
		final Matcher tmMatcher = PragueTextureSegmentation.TM_FILE_PATH.matcher(imagePath);
		
		if (!tmMatcher.matches()) {
			return;
		}
		
		final int setIndex = parseInt(tmMatcher.group(1));
		final int maskIndex = parseInt(tmMatcher.group(2));
		final int mosaicIndex = parseInt(tmMatcher.group(3));
		final String root = new File(imagePath).getParent();
		final String metadataPath = arguments.get("metadata", new File(root, "data.xml").getPath());
		final Map<String, String> textureMap = PragueTextureSegmentation.getTextureMap(metadataPath, maskIndex, mosaicIndex);
		
		debugPrint(textureMap);
		
		final Image image = loadAndTryToCache(imagePath, 0);
		final Configuration configuration = new Configuration(commandLineArguments);
		final Channel[] channels = RGB;
		final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
		final Class<? extends Sampler> samplerFactory = configuration.getSamplerClass();
		final Quantizer quantizer = new BinningQuantizer();
		
		quantizer.initialize(image, null, channels, configuration.getQuantizationLevel());
		
		final Segmenter segmenter = configuration.newSegmenter();
		
		{
			for (final Map.Entry<String, String> entry : textureMap.entrySet()) {
				final String texturePath = new File(root, entry.getValue()).getPath();
				
				debugPrint(texturePath);
				
				final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
				
				final Image texture = loadAndTryToCache(texturePath, 0);
				
				classes.put(entry.getKey(), new RegionOfInterest.UsingBitSet(
						texture.getRowCount(), texture.getColumnCount(), true));
				
				final Sampler collectorSampler = newSampler(samplerFactory, channels, quantizer, texture
						, new Sample.ClassSetter(classes, sampleDatabase));
				
				segmenter.process(texture, collectorSampler);
				
				debugPrint(sampleDatabase.getEntryCount());
			}
		}
		
		
		final Sample.Collector collector = new Sample.Collector();
		final Sampler sampler = newSampler(configuration.getSamplerClass(), channels, quantizer, image, collector);
		final BKDatabase<Sample> bkDatabase = newBKDatabase(sampleDatabase, getPreferredMetric(sampler));
		
		timer.tic();
		
		segmenter.process(image, new PixelProcessor() {
			
			private final IntList pixels = new IntList();
			
			@Override
			public final void process(final int pixel) {
				sampler.process(pixel);
				
				this.pixels.add(pixel);
			}
			
			@Override
			public void finishPatch() {
				sampler.finishPatch();
				
				final Sample sample = bkDatabase.findClosest(collector.getSample());
				
				if (1 != sample.getClasses().size()) {
					throw new IllegalArgumentException("Invalid group: " + sample.getClasses());
				}
				
				final String predicted = sample.getClasses().iterator().next();
				
				// TODO create result image
				
				this.pixels.clear();
			}
			
		});
		
		gc();
		
		debugPrint("time:", timer.toc());
	}
	
	/**
	 * @author codistmonk (creation 2014-06-15)
	 */
	public static final class PragueTextureSegmentation {
		
		private PragueTextureSegmentation() {
			throw new IllegalInstantiationException();
		}
		
		public static final Pattern TM_FILE_PATH = Pattern.compile(".*tm([0-9]+)_([0-9]+)_([0-9]+)\\.png");
		
		public static final Map<String, String> getTextureMap(final String metadataPath,
				final int maskIndex, final int mosaicIndex)
				throws FileNotFoundException {
			final Document metadata = parse(new FileInputStream(metadataPath));
			final Node textureMaps = getNode(metadata, "//section[@name='TMMap_" + maskIndex + "_" + mosaicIndex + "']");
			final Element textures = (Element) getNode(textureMaps, "var[contains(@name, 'Textures')]");
			final String[] textureIds = textures.getTextContent().replaceAll("[^0-9]", " ").trim().split(" +");
			final Map<String, String> result = new LinkedHashMap<>();
			
			{
				final List<Node> texturePaths = getNodes(textureMaps, "var[contains(@name, 'Train')]");
				
				for (int i = 0; i < textureIds.length; ++i) {
					result.put(textureIds[i], texturePaths.get(i).getTextContent().replaceAll("\"", ""));
				}
			}
			
			return result;
		}
		
	}
	
}
