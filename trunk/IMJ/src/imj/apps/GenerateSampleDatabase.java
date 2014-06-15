package imj.apps;

import static imj.IMJTools.loadAndTryToCache;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import imj.apps.modules.RegionOfInterest;
import imj.database.BinningQuantizer;
import imj.database.LinearSampler;
import imj.database.PatchDatabase;
import imj.database.Quantizer;
import imj.database.Sample;
import imj.database.Sampler;
import imj.database.SeamGridSegmenter;
import imj.database.Segmenter;
import imj.database.SparseHistogramSampler;
import imj.database.TileSegmenter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-04-11)
 */
public final class GenerateSampleDatabase {
	
	private GenerateSampleDatabase() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static final void main(final String[] commandLineArguments) throws FileNotFoundException, IOException {
		final Configuration configuration = new Configuration(commandLineArguments);
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final Map<Object, Object> database = new LinkedHashMap<Object, Object>();
		final TicToc timer = new TicToc();
		final String imageId = arguments.get("file", "");
		final int lod = configuration.getLod();
		final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
		final String outPath = arguments.get("out", baseName(imageId) + configuration.getDatabaseSuffix());
		final Quantizer quantizer = new BinningQuantizer();
		
		System.out.println("Collecting data... " + new Date(timer.tic()));
		
		quantizer.initialize(loadAndTryToCache(imageId, lod), null, RGB, configuration.getQuantizationLevel());
		
		updateDatabase(imageId, lod, configuration.newSegmenter(), configuration.getSamplerClass(), RGB, quantizer,
				new HashMap<String, RegionOfInterest>(), sampleDatabase);
		
		database.put("samples", sampleDatabase);
		
		System.out.println("Collecting data done" + " time: " + timer.toc() + " memory: " + usedMemory());
		
		checkDatabase(sampleDatabase);
		
		System.out.println("Writing data... " + new Date(timer.tic()));
		
		final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outPath));
		
		try {
			out.writeObject(database);
		} finally {
			out.close();
		}
		
		System.out.println("Writing data done" + " time: " + timer.toc() + " memory: " + usedMemory());
	}
	
	/**
	 * @author codistmonk (creation 2013-06-04)
	 */
	public static final class Configuration {
		
		private final int tileRowCount;
		
		private final int tileColumnCount;
		
		private final int verticalTileStride;
		
		private final int horizontalTileStride;
		
		private final int lod;
		
		private final int quantizationLevel;
		
		private final String segmenterName;
		
		private final String descriptorName;
		
		private final String protosuffix;
		
		private final String databaseSuffix;
		
		private final String confusionSuffix;
		
		private final Class<? extends Sampler> samplerClass;
		
		private final int maximumGroupSize;
		
		public Configuration(final String... commandLineArguments) {
			final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
			this.tileRowCount = arguments.get("tileHeight", 3)[0];
			this.tileColumnCount = arguments.get("tileWidth", this.tileRowCount)[0];
			this.verticalTileStride = arguments.get("yStep", this.tileRowCount)[0];
			this.horizontalTileStride = arguments.get("xStep", this.verticalTileStride)[0];
			this.lod = arguments.get("lod", 4)[0];
			this.quantizationLevel = arguments.get("q", 0)[0];
			this.segmenterName = arguments.get("segmenter", "tiles").toLowerCase(ENGLISH);
			this.descriptorName = arguments.get("descriptor", "linear").toLowerCase(ENGLISH);
			this.maximumGroupSize = arguments.get("maximumGroupSize", 0)[0];
			
			String protosuffix =  ".lod" + this.lod + ".q" + this.quantizationLevel + "." + this.segmenterName;
			
			if ("tiles".equals(this.segmenterName)) {
				protosuffix += "_h" + this.tileRowCount + "w" + this.tileColumnCount + "dy" + this.verticalTileStride + "dx" + this.horizontalTileStride;
			} else if ("seams".equals(this.segmenterName)) {
				protosuffix += this.tileRowCount;
			} else {
				throw new IllegalArgumentException("Invalid segmenter: " + this.segmenterName);
			}
			
			protosuffix += "." + this.descriptorName;
			this.protosuffix = protosuffix;
			this.databaseSuffix = protosuffix + ".jo";
			this.confusionSuffix = protosuffix + ".mgs" + this.maximumGroupSize + ".jo";
			
			if ("linear".equals(this.descriptorName)) {
				this.samplerClass = LinearSampler.class;
			} else if ("histogram".equals(this.descriptorName)) {
				this.samplerClass = SparseHistogramSampler.class;
			} else {
				throw new IllegalArgumentException("Invalid descriptor: " + this.descriptorName);
			}
		}
		
		public final int getTileRowCount() {
			return this.tileRowCount;
		}
		
		public final int getTileColumnCount() {
			return this.tileColumnCount;
		}
		
		public final int getVerticalTileStride() {
			return this.verticalTileStride;
		}
		
		public final int getHorizontalTileStride() {
			return this.horizontalTileStride;
		}
		
		public final int getLod() {
			return this.lod;
		}
		
		public final int getQuantizationLevel() {
			return this.quantizationLevel;
		}
		
		public final String getSegmenterName() {
			return this.segmenterName;
		}
		
		public final String getDescriptorName() {
			return this.descriptorName;
		}
		
		public final int getMaximumGroupSize() {
			return this.maximumGroupSize;
		}
		
		public final String getProtosuffix() {
			return this.protosuffix;
		}
		
		public final String getDatabaseSuffix() {
			return this.databaseSuffix;
		}
		
		public final String getConfusionSuffix() {
			return this.confusionSuffix;
		}
		
		public final Segmenter newSegmenter() {
			if ("tiles".equals(this.segmenterName)) {
				return new TileSegmenter(this.tileRowCount, this.tileColumnCount, this.verticalTileStride, this.horizontalTileStride);
			} else if ("seams".equals(this.segmenterName)) {
				return new SeamGridSegmenter(this.tileRowCount);
			} else {
				throw new IllegalArgumentException("Invalid segmenter: " + this.segmenterName);
			}
		}
		
		public final Class<? extends Sampler> getSamplerClass() {
			return this.samplerClass;
		}
		
	}
	
}
