package imj.apps;

import static imj.IMJTools.readObject;
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
import net.sourceforge.aprog.tools.Tools;

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
		final Map<String, Map<String, ConfusionTable[]>> confusions = readObject(filePath);
		final Map<String, List<DataPointXY>> data = new HashMap<String, List<DataPointXY>>();
		final String[] moreFields = arguments.get("moreFields", "").split(",");
		
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
						out.println(dataPoint.toString(moreFields));
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
		
		public final String toString(final String[] fieldsFromLabel) {
			final StringBuilder resultBuilder = new StringBuilder();
			
			resultBuilder
			.append(this.getLabel()).append(SEPARATOR);
			
			if (fieldsFromLabel != null) {
				final Map<String, String> values = new HashMap<String, String>();
				
				for (final String nameValue : this.getLabel().split("\\b+")) {
					final String[] value = nameValue.split("\\D+");
					
					if (2 == value.length) {
						values.put(nameValue.split("\\d+")[0], value[1]);
					}
				}
				
				for (final String field : fieldsFromLabel) {
					resultBuilder.append(values.get(field)).append(SEPARATOR);
				}
			}
			
			resultBuilder
			.append(this.getX()).append(SEPARATOR)
			.append(this.getY()).append(SEPARATOR)
			.append(this.getErrorX()).append(SEPARATOR)
			.append(this.getErrorY());
			
			return resultBuilder.toString();
		}
		
		@Override
		public final String toString() {
			return this.toString(null);
		}
		
		/**
		 * {@value}.
		 */
		public static final String SEPARATOR = "\t";
		
		public static final <T> int indexOf(final T element, final T[] array) {
			int result = array.length - 1;
			
			while (0 <= result && !Tools.equals(element, array[result])) {
				--result;
			}
			
			return result;
		}
		
	}
	
}
