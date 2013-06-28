package imj.apps;

import static java.lang.Double.parseDouble;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj.apps.modules.ShowActions;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-14)
 */
public final class GenerateGroupStatistics {
	
	private GenerateGroupStatistics() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "");
		final int keyIndex = arguments.get("key", 0)[0];
		final Scanner scanner = new Scanner(new File(filePath));
		final Map<String, Statistics[]> data = new LinkedHashMap<String, Statistics[]>();
		final String outPath = ShowActions.baseName(filePath) + ".statistics.csv";
		
		while (scanner.hasNext()) {
			final String[] fields = scanner.nextLine().split("\\s+");
			
			for (int i = 0; i < fields.length; ++i) {
				if (i != keyIndex) {
					getOrCreate(data, fields[keyIndex], Statistics.class, fields.length)[i].addValue(parseDouble(fields[i]));
				}
			}
		}
		
		final PrintStream out = new PrintStream(outPath);
		final String separator = "	";
		
		try {
			for (final Map.Entry<String, Statistics[]> entry : data.entrySet()) {
				final StringBuilder row = new StringBuilder();
				final String key = entry.getKey();
				final Statistics[] statistics = entry.getValue();
				
				for (int i = 0; i < statistics.length; ++i) {
					if (0 < i) {
						row.append(separator);
					}
					
					if (i == keyIndex) {
						row.append(key);
					} else {
						row.append(statistics[i].getMean());
					}
				}
				
				for (int i = 0; i < statistics.length; ++i) {
					if (i != keyIndex) {
						row.append(separator).append(sqrt(statistics[i].getVariance()));
					}
				}
				
				out.println(row);
			}
		} finally {
			out.close();
		}
	}
	
	public static final <K, V> V[] getOrCreate(final Map<K, V[]> map, final K key, final Class<V> factory, final int n) {
		V[] result = map.get(key);
		
		if (result == null) {
			result = instances(factory, n);
			
			map.put(key, result);
		}
		
		return result;
	}
	
	public static final <T> T[] instances(final Class<T> elementFactory, final int elementCount) {
		try {
			final T[] result = (T[]) Array.newInstance(elementFactory, elementCount);
			
			for (int i = 0; i < elementCount; ++i) {
				result[i] = elementFactory.newInstance();
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
}
