package imj.apps;

import static imj.IMJTools.getOrCreate;
import static java.lang.Double.isNaN;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.readObject;
import imj.apps.GenerateClassificationData.ExtendedConfusionTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "confusions.jo");
		final Map<String, Map<String, ExtendedConfusionTable[]>> confusions = readObject(filePath);
		final Map<String, List<DataPointXY>> data = new TreeMap<String, List<DataPointXY>>();
		final String[] moreFields = arguments.get("moreFields", "").split(",");
		final Map<String, Statistics> configurationFPRs = new TreeMap<String, Statistics>();
		final Map<String, Statistics> configurationTPRs = new TreeMap<String, Statistics>();
		
		for (final Map.Entry<String, Map<String, ExtendedConfusionTable[]>> configurationEntry : confusions.entrySet()) {
			ExtendedConfusionTable[] configurationConfusionTables = null;
			
			for (final Map.Entry<String, ExtendedConfusionTable[]> annotationEntry : configurationEntry.getValue().entrySet()) {
				getOrCreate(data, annotationEntry.getKey(), (Class<List<DataPointXY>>) (Object) ArrayList.class).add(
						newDataPoint(configurationEntry.getKey(), annotationEntry.getValue()));
				
				final int n = annotationEntry.getValue().length;
				
				if (configurationConfusionTables == null) {
					configurationConfusionTables = new ExtendedConfusionTable[n];
				}
				
				for (int i = 0; i < n; ++i) {
					final ExtendedConfusionTable configurationConfusionTable = getOrCreate(
							configurationConfusionTables, i, ExtendedConfusionTable.class);
					final ExtendedConfusionTable annotationConfusionTable = annotationEntry.getValue()[i];
					
					configurationConfusionTable.incrementTruePositive("", annotationConfusionTable.getTruePositive());
					configurationConfusionTable.incrementFalsePositive("", annotationConfusionTable.getFalsePositive());
					configurationConfusionTable.incrementTrueNegative("", annotationConfusionTable.getTrueNegative());
					configurationConfusionTable.incrementFalseNegative("", annotationConfusionTable.getFalseNegative());
				}
			}
			
			final Statistics configurationFPR = getOrCreate(configurationFPRs, configurationEntry.getKey(), Statistics.class);
			final Statistics configurationTPR = getOrCreate(configurationTPRs, configurationEntry.getKey(), Statistics.class);
			
			if (configurationConfusionTables != null) {
				for (final ExtendedConfusionTable confusionTable : configurationConfusionTables) {
					configurationFPR.addValue(confusionTable.getFalsePositiveRate());
					configurationTPR.addValue(confusionTable.getTruePositiveRate());
				}
			}
		}
		
		{
			final PrintStream out = new PrintStream("all.csv");
			
			try {
				for (final String configurationKey : configurationFPRs.keySet()) {
					final Statistics fpr = configurationFPRs.get(configurationKey);
					final Statistics tpr = configurationTPRs.get(configurationKey);
					
					out.println(new DataPointXY(configurationKey, fpr.getMean(), tpr.getMean(), sqrt(fpr.getVariance()), sqrt(tpr.getVariance())));
				}
			} finally {
				out.close();
			}
		}
		
		for (final Map.Entry<String, List<DataPointXY>> entry : data.entrySet()) {
			final PrintStream out = new PrintStream(entry.getKey() + ".csv");
			
			try {
				for (final DataPointXY dataPoint : entry.getValue()) {
					out.println(dataPoint.toString(moreFields));
				}
			} finally {
				out.close();
			}
		}
	}
	
	public static final DataPointXY newDataPoint(final String label, final ExtendedConfusionTable[] tables) {
		final Statistics fpr = new Statistics();
		final Statistics tpr = new Statistics();
		
		for (final ExtendedConfusionTable table : tables) {
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
		
		public final boolean isValid() {
			return !isNaN(this.getX()) && !isNaN(this.getY());
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
					if (!"".equals(field)) {
						resultBuilder.append(values.get(field)).append(SEPARATOR);
					}
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
