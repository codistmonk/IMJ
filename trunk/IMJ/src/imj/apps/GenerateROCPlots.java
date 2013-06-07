package imj.apps;

import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.apps.GenerateClassificationData.ConfusionTable;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-07)
 */
public final class GenerateROCPlots {
	
	private GenerateROCPlots() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "confusions.jo");
		final Map<String, Map<String, ConfusionTable[]>> confusions = CollectConfusionTables.readObject(filePath);
		final Map<String, List<DataPointXY>> data = new HashMap<String, List<DataPointXY>>();
		
		for (final Map.Entry<String, Map<String, ConfusionTable[]>> configurationEntry : confusions.entrySet()) {
			for (final Map.Entry<String, ConfusionTable[]> annotationEntry : configurationEntry.getValue().entrySet()) {
				getOrCreate(data, annotationEntry.getKey(), (Class<List<DataPointXY>>) (Object) ArrayList.class).add(
						newDataPoint(configurationEntry.getKey(), annotationEntry.getValue()));
			}
		}
		
		for (final Map.Entry<String, List<DataPointXY>> entry : data.entrySet()) {
			try {
				final PrintStream out = new PrintStream(entry.getKey() + ".csv");
				
				try {
					for (final DataPointXY dataPoint : entry.getValue()) {
						out.println(dataPoint);
					}
				} finally {
					out.close();
				}
			} catch (final FileNotFoundException exception) {
				throw unchecked(exception);
			}
		}
	}
	
	public static final <K, V> V getOrCreate(final Map<K, V> map, final K key, final Class<? extends V> valueFactory) {
		V result = map.get(key);
		
		if (result == null) {
			try {
				result = valueFactory.newInstance();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
			
			map.put(key, result);
		}
		
		return result;
	}
	
	public static final DataPointXY newDataPoint(final String label, final ConfusionTable[] tables) {
		final Statistics fpr = new Statistics();
		final Statistics tpr = new Statistics();
		
		for (final ConfusionTable table : tables) {
			if (0L < table.getActualNegative() && 0L < table.getActualPositive()) {
				fpr.addValue(table.getFalsePositiveRate());
				tpr.addValue(table.getTruePositiveRate());
			}
		}
		
		return new DataPointXY(label, fpr.getMean(), tpr.getMean(), sqrt(fpr.getVariance()), sqrt(tpr.getVariance()));
	}
	
	/**
	 * @author codistmonk (creation 2013-0-07)
	 */
	public static final class DataPointXY {
		
		private final String label;
		
		private final double x;
		
		private final double y;
		
		private final double errorX;
		
		private final double errorY;
		
		public DataPointXY(final String label, final double x, final double y, final double errorX, final double errorY) {
			this.label = label;
			this.x = x;
			this.y = y;
			this.errorX = errorX;
			this.errorY = errorY;
		}
		
		public final String getLabel() {
			return this.label;
		}
		
		public final double getX() {
			return this.x;
		}
		
		public final double getY() {
			return this.y;
		}
		
		public final double getErrorX() {
			return this.errorX;
		}
		
		public final double getErrorY() {
			return this.errorY;
		}
		
		@Override
		public final String toString() {
			return this.getLabel() + "	" + this.getX() + "	" + this.getY() + "	" + this.getErrorX() + "	" + this.getErrorY();
		}
		
	}
	
}
