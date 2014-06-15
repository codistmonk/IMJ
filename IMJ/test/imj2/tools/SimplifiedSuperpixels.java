package imj2.tools;

import static imj.IMJTools.loadAndTryToCache;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newSampler;
import static java.lang.Integer.parseInt;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.xml.XMLTools.getNode;
import static net.sourceforge.aprog.xml.XMLTools.getNodes;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jgencode.primitivelists.IntList;
import jgencode.primitivelists.IntList.Processor;

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
		final Configuration configuration = new Configuration(commandLineArguments);
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File file = new File(arguments.get("file", ""));
		
		if (file.isDirectory()) {
			final List<Future<?>> tasks = new ArrayList<>();
			
			try {
				for (final File subfile : file.listFiles()) {
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							try {
								process(configuration, arguments, subfile.getPath());
							} catch (final IOException exception) {
								throw unchecked(exception);
							}
						}
						
					});
				}
				
				MultiThreadTools.wait(tasks);
			} finally {
				MultiThreadTools.shutdownExecutor();
			}
		} else {
			process(configuration, arguments, file.getPath());
		}
	}
	
	public static final void process(final Configuration configuration,
			final CommandLineArgumentsParser arguments, final String imagePath)
			throws FileNotFoundException, IOException {
		final TicToc timer = new TicToc();
		final Matcher tmMatcher = PragueTextureSegmentation.TM_FILE_PATH.matcher(imagePath);
		
		if (!tmMatcher.matches()) {
			debugPrint("Ignoring", imagePath);
			
			return;
		}
		
		final int setIndex = parseInt(tmMatcher.group(1));
		final int maskIndex = parseInt(tmMatcher.group(2));
		final int mosaicIndex = parseInt(tmMatcher.group(3));
		final String root = new File(imagePath).getParent();
		final String metadataPath = arguments.get("metadata", new File(root, "data.xml").getPath());
		final Map<String, String> textureMap = PragueTextureSegmentation.getTextureMap(
				metadataPath, setIndex, maskIndex, mosaicIndex);
		
		debugPrint(textureMap);
		
		final Image image = loadAndTryToCache(imagePath, configuration.getLod());
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
				
				final Image texture = loadAndTryToCache(texturePath, configuration.getLod());
				
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
		final BufferedImage result = SimpleGray8ColorModel.newByteGrayAWTImage(image.getColumnCount(), image.getRowCount());
		final byte[] prediction = new byte[1];
		
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
				
				prediction[0] = (byte) parseInt(sample.getClasses().iterator().next());
				
				this.pixels.forEach(new Processor() {
					
					@Override
					public final boolean process(final int pixel) {
						final int x = pixel % image.getColumnCount();
						final int y = pixel / image.getColumnCount();
						
						result.setRGB(x, y, prediction[0]);
						
						return true;
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = -4667208741018178698L;
					
				});
				
				this.pixels.clear();
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4448450281104902695L;
			
		});
		
		gc();
		
		final String outputPath = new File(imagePath).getName().replace("tm" + setIndex, "seg" + setIndex);
		
		debugPrint(outputPath);
		
		try (final OutputStream output = new FileOutputStream(outputPath)) {
			ImageIO.write(result, "png", output);
		}
		
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
		
		public static final Map<String, String> getTextureMap(final String metadataPath
				, final int setIndex, final int maskIndex, final int mosaicIndex)
				throws FileNotFoundException {
			final Document metadata = parse(new FileInputStream(metadataPath));
			final Node texturesNode = getNode(metadata, "//section[@name='Set_" + setIndex + "']/section[@name='TMMap_" + maskIndex + "_" + mosaicIndex + "']");
			final Element textures = (Element) getNode(texturesNode, "var[contains(@name, 'Textures')]");
			final String[] textureIds = textures.getTextContent().replaceAll("[^0-9]", " ").trim().split(" +");
			final Map<String, String> result = new LinkedHashMap<>();
			
			{
				final List<Node> texturePaths = getNodes(texturesNode, "var[contains(@name, 'Train')]");
				
				for (int i = 0; i < textureIds.length; ++i) {
					result.put(textureIds[i], texturePaths.get(i).getTextContent().replaceAll("\"", ""));
				}
			}
			
			return result;
		}
		
	}
	
}