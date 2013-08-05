package imj.apps;

import static imj.apps.ExtractConfusionTable.format;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static net.sourceforge.aprog.tools.Tools.readObject;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.apps.GenerateClassificationData.ExtendedConfusionTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Factory;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-06)
 */
public final class ExtractPrecisionAndRecall {
	
	private ExtractPrecisionAndRecall() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "confusions.jo");
		final String key = arguments.get("key", "");
		final Map<String, Map<String, ExtendedConfusionTable[]>> confusions = readObject(filePath);
		final boolean printKeys = arguments.get("printKeys", 0)[0] != 0;
		final Collection<String> fieldNames = toLowerCase(new ArrayList<String>(Arrays.asList(
				arguments.get("fields", "sensitivity,specificity").split(","))));
		
		if (printKeys) {
			System.out.println("keys: " + confusions.keySet());
		}
		
		final DefaultFactory<Statistics> statisticsFactory = Factory.DefaultFactory.forClass(Statistics.class);
		final Map<String, ExtendedConfusionTable[]> record = confusions.get(key);
		final Collection<String> classNames = record.keySet();
		
		System.out.println("classes: " + classNames);
		
		for (final String className : classNames) {
			final ExtendedConfusionTable[] tables = record.get(className);
			final Map<String, Statistics> precisions = new LinkedHashMap<String, Statistics>();
			final Map<String, Statistics> recalls = new LinkedHashMap<String, Statistics>();
			final Map<String, Statistics> specificities = new LinkedHashMap<String, Statistics>();
			
			for (final ExtendedConfusionTable table : tables) {
				for (final String predictedClassName : classNames) {
					getOrCreate(precisions, predictedClassName, statisticsFactory).addValue(ratio(table.getTruePositive(), table.getPositive(), 1.0));
					getOrCreate(recalls, predictedClassName, statisticsFactory).addValue(ratio(table.getTruePositive(), table.getActualPositive(), 1.0));
					getOrCreate(specificities, predictedClassName, statisticsFactory).addValue(ratio(table.getTrueNegative(), table.getActualNegative(), 1.0));
				}
			}
			
			final Statistics precision = precisions.get(className);
			final Statistics recall = recalls.get(className);
			final Statistics specificity = specificities.get(className);
			
			for (final String fieldName : fieldNames) {
				if ("precision".equals(fieldName)) {
					System.out.print(format(precision.getMean(), precision.getVariance()));
				} else if ("recall".equals(fieldName) || "sensitivity".equals(fieldName)) {
					System.out.print(format(recall.getMean(), recall.getVariance()));
				} else if ("specificity".equals(fieldName)) {
					System.out.print(format(specificity.getMean(), specificity.getVariance()));
				}
			}
			
			System.out.println("\\\\");
		}
	}
	
	public static final Collection<String> toLowerCase(final Collection<String> strings) {
		try {
			final Collection<String> result = strings.getClass().newInstance();
			
			for (final String string : strings) {
				result.add(string.toLowerCase(Locale.ENGLISH));
			}
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final double ratio(final double numerator, final double denominator, final double defaultValue) {
		return denominator != 0.0 ? numerator / denominator : defaultValue;
	}
	
}
