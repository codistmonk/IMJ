package imj.apps;

import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.ImageWrangler;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.AdaptiveQuantizationViewFilter.AdaptiveQuantizer;
import imj.database.LinearSampler;
import imj.database.Sample;
import imj.database.PatchDatabase;

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
		final String outPath = arguments.get("out", "sdb.jo");
		final Map<Object, Object> database = new LinkedHashMap<Object, Object>();
		final TicToc timer = new TicToc();
		final String imageId = arguments.get("file", "");
		final int tileRowCount = arguments.get("tileHeight", 3)[0];
		final int tileColumnCount = arguments.get("tileWidth", tileRowCount)[0];
		final int verticalTileStride = arguments.get("yStep", tileRowCount)[0];
		final int horizontalTileStride = arguments.get("xStep", verticalTileStride)[0];
		final int lod = arguments.get("lod", 4)[0];
		final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
		final AdaptiveQuantizer quantizer = new AdaptiveQuantizer();
		final int quantizationLevel = arguments.get("q", 0)[0];
		
		System.out.println("Collecting data... " + new Date(timer.tic()));
		
		quantizer.initialize(ImageWrangler.INSTANCE.load(imageId, lod), null, RGB, quantizationLevel);
		
		updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				LinearSampler.class, RGB, quantizer, new HashMap<String, RegionOfInterest>(), sampleDatabase);
		
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
