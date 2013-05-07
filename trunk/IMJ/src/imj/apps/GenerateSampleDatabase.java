package imj.apps;

import static imj.IMJTools.maybeCacheImage;
import static imj.apps.modules.ShowActions.baseName;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageWrangler;
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
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final Map<Object, Object> database = new LinkedHashMap<Object, Object>();
		final TicToc timer = new TicToc();
		final String imageId = arguments.get("file", "");
		final int tileRowCount = arguments.get("tileHeight", 3)[0];
		final int tileColumnCount = arguments.get("tileWidth", tileRowCount)[0];
		final int verticalTileStride = arguments.get("yStep", tileRowCount)[0];
		final int horizontalTileStride = arguments.get("xStep", verticalTileStride)[0];
		final int lod = arguments.get("lod", 4)[0];
		final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
		final Quantizer quantizer = new BinningQuantizer();
		final int quantizationLevel = arguments.get("q", 0)[0];
		final String segmenterName = arguments.get("segmenter", "tiles").toLowerCase(ENGLISH);
		String outPath = arguments.get("out", baseName(imageId) + ".lod" + lod + ".q" + quantizationLevel + "." + segmenterName);
		final Segmenter segmenter;
		
		if ("tiles".equals(segmenterName)) {
			segmenter = new TileSegmenter(tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride);
			outPath += "_h" + tileRowCount + "w" + tileColumnCount + "dy" + verticalTileStride + "dx" + horizontalTileStride;
		} else if ("seams".equals(segmenterName)) {
			segmenter = new SeamGridSegmenter(tileRowCount);
			outPath += tileRowCount;
		} else {
			throw new IllegalArgumentException("Invalid segmenter: " + segmenterName);
		}
		
		final String descriptorName = arguments.get("descriptor", "linear").toLowerCase(ENGLISH);
		final Class<? extends Sampler> samplerClass;
		
		if ("linear".equals(descriptorName)) {
			samplerClass = LinearSampler.class;
		} else if ("histogram".equals(descriptorName)) {
			samplerClass = SparseHistogramSampler.class;
		} else {
			throw new IllegalArgumentException("Invalid descriptor: " + descriptorName);
		}
		
		outPath += "." + descriptorName + ".jo";
		
		System.out.println("Collecting data... " + new Date(timer.tic()));
		
		quantizer.initialize(maybeCacheImage(ImageWrangler.INSTANCE.load(imageId, lod)), null, RGB, quantizationLevel);
		
		updateDatabase(imageId, lod, segmenter, LinearSampler.class, RGB, quantizer,
				new HashMap<String, RegionOfInterest>(), sampleDatabase);
		
		database.put("samples", sampleDatabase);
		
		System.out.println("Collecting data done" + " time: " + timer.toc() + " memory: " + usedMemory());
		
		System.out.println("Writing data... " + new Date(timer.tic()));
		
		final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outPath));
		
		try {
			out.writeObject(database);
		} finally {
			out.close();
		}
		
		System.out.println("Writing data done" + " time: " + timer.toc() + " memory: " + usedMemory());
	}
	
}
