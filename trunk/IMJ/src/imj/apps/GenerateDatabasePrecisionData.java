package imj.apps;

import static imj.IMJTools.getOrCreate;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.readObject;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj.database.IMJDatabaseTools.DBInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-10)
 */
public final class GenerateDatabasePrecisionData {
	
	private GenerateDatabasePrecisionData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	private static final String DBINFO = ".dbinfo.";
	
	/**
	 * {@value}.
	 */
	private static final String JO = ".jo";
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String directoryPath = arguments.get("directory", "");
		final String filePath = arguments.get("file", "");
		final int verbosity = arguments.get("verbosity", 0)[0];
		final boolean split = arguments.get("split", 1)[0] != 0;
		
		if (!"".equals(filePath)) {
			final DBInfo dbInfo = readObject(filePath);
			
			System.out.println("entryCount: " + dbInfo.getDatabaseEntryCount());
			System.out.println("sampleCount: " + dbInfo.getDatabaseSampleCount());
			System.out.println("classes: " + dbInfo.getClassCounts());
			System.out.println("groups: " + dbInfo.getGroups());
		} else if (split) {
			final Map<String, Map<String, Statistics>> data = new HashMap<String, Map<String, Statistics>>();
			final Map<String, Statistics> mergedPrecisions = new HashMap<String, Statistics>();
			
			for (final File file : new File(directoryPath).listFiles()) {
				final String fileName = file.getName();
				final int imageNameLength = fileName.indexOf(DBINFO);
				
				if (imageNameLength < 0 && fileName.endsWith(JO)) {
					continue;
				}
				
				final String parameters = fileName.substring(imageNameLength + DBINFO.length(), fileName.length() - JO.length());
				final DBInfo dbInfo = readObject(file.getPath());
				long mergedGroupSize = 0L;
				long mergedClassSize = 0L;
				
				for (final Map.Entry<String, AtomicInteger> entry : dbInfo.getClassCounts().entrySet()) {
					final Collection<String> group = Collections.singleton(entry.getKey());
					final AtomicInteger groupSizeHolder = dbInfo.getGroups().get(group);
					@SuppressWarnings("unchecked")
					final Map<String, Statistics> precisions = getOrCreate(data, entry.getKey(), (Class) TreeMap.class);
					
					getOrCreate(precisions, parameters, Statistics.class).addValue(
							groupSizeHolder == null ? 0.0 : (double) groupSizeHolder.get() / entry.getValue().get());
					
					if (groupSizeHolder != null) {
						mergedGroupSize += groupSizeHolder.get();
					}
					mergedClassSize += entry.getValue().get();
				}
				
				getOrCreate(mergedPrecisions, parameters, Statistics.class).addValue(
						mergedGroupSize == 0L ? 0.0 : (double) mergedGroupSize / mergedClassSize);
			}
			
			print(mergedPrecisions, "all.dbinfo.csv");
			
			for (final Map.Entry<String, Map<String, Statistics>> classEntry : data.entrySet()) {
				print(classEntry.getValue(), classEntry.getKey() + ".dbinfo.csv");
			}
		} else {
			final int[] lods = arguments.get("lod", 6);
			final int[] qs = arguments.get("q", 6);
			final String[] images = arguments.get("image", "").split(",");
			final String[] moreKeys = arguments.get("moreKeys", "").split(",");
			final Map<String, Statistics> precisions = new HashMap<String, Statistics>();
			
			for (final File file : new File(directoryPath).listFiles()) {
				final String fileName = file.getName();
				
				if (!(fileName.contains(DBINFO) && fileName.endsWith(".jo") && startsWithKey(fileName, images) &&
						containsAllKeys(fileName, moreKeys))) {
					continue;
				}
				
				for (final int lod : lods) {
					if (!fileName.contains(".lod" + lod + ".")) {
						continue;
					}
					
					for (final int q : qs) {
						if (!fileName.contains(".q" + q + ".")) {
							continue;
						}
						
						if (0 < verbosity) {
							System.out.println("Using " + fileName);
						}
						
						final DBInfo dbInfo = readObject(file.getPath());
						
						for (final Map.Entry<String, AtomicInteger> entry : dbInfo.getClassCounts().entrySet()) {
							final Collection<String> group = Collections.singleton(entry.getKey());
							final AtomicInteger classCount = dbInfo.getGroups().get(group);
							
							getOrCreate(precisions, entry.getKey(), Statistics.class).addValue(
									classCount == null ? 0.0 : (double) classCount.get() / entry.getValue().get());
						}
					}
				}
			}
			
			for (final Map.Entry<String, Statistics> entry : precisions.entrySet()) {
				final Statistics precision = entry.getValue();
				
				System.out.println(entry.getKey().replaceAll("\\s+", "_") + "	" + precision.getMean() + "	" + sqrt(precision.getVariance()));
			}
		}
	}
	
	public static final void print(final Map<String, Statistics> mergedPrecisions, final String fileName) {
		try {
			final PrintStream out = new PrintStream(fileName);
			
			try {
				print(mergedPrecisions, out);
			} finally {
				out.close();
			}
		} catch (final FileNotFoundException exception) {
			throw unchecked(exception);
		}
	}
	
	public static final void print(final Map<String, Statistics> precisions, final PrintStream out) {
		for (final Map.Entry<String, Statistics> precisionEntry : precisions.entrySet()) {
			out.print(precisionEntry.getKey());
			out.print(" ");
			out.print(precisionEntry.getValue().getMean());
			out.print(" ");
			out.print(sqrt(precisionEntry.getValue().getVariance()));
			out.println();
		}
	}
	
	public static boolean startsWithKey(final String string, final String[] protokeys) {
		for (final String image : protokeys) {
			final String key = image.trim() + ".";
			
			if (".".equals(key)) {
				return true;
			} else {
				if (string.startsWith(key)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean containsAllKeys(final String string, final String[] protokeys) {
		boolean result = true;
		
		for (final String image : protokeys) {
			final String key = image.trim() + ".";
			
			if (".".equals(key)) {
				break;
			} else {
				if (!string.contains(key)) {
					result = false;
				}
			}
		}
		
		return result;
	}
	
}
