package imj.apps;

import static java.lang.Math.sqrt;
import static multij.tools.Tools.getOrCreate;
import static multij.tools.Tools.readObject;
import imj.apps.GenerateClassificationData.ExtendedConfusionTable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.Factory;
import multij.tools.Factory.DefaultFactory;
import multij.tools.IllegalInstantiationException;
import multij.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-06)
 */
public final class ExtractConfusionTable {
	
	private ExtractConfusionTable() {
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
		
		if (printKeys) {
			System.out.println("keys: " + confusions.keySet());
		}
		
		final DefaultFactory<Statistics> statisticsFactory = Factory.DefaultFactory.forClass(Statistics.class);
		final Map<String, ExtendedConfusionTable[]> record = confusions.get(key);
		final Collection<String> classNames = record.keySet();
		
		System.out.println("classes: " + classNames);
		
		for (final String actualClassName : classNames) {
			final ExtendedConfusionTable[] tables = record.get(actualClassName);
			final Map<String, Statistics> statistics = new LinkedHashMap<String, Statistics>();
			
			for (final ExtendedConfusionTable table : tables) {
				final double actualTotal = table.getTruePositive() + table.getFalseNegative();
				
				for (final String predictedClassName : classNames) {
					final double proportion = 0.0 != actualTotal ? table.getCount(predictedClassName) / actualTotal :
						actualClassName.equals(predictedClassName) ? 1.0 : 0.0;
					
					getOrCreate(statistics, predictedClassName, statisticsFactory).addValue(proportion);
				}
			}
			
			for (final String predictedClassName : classNames) {
				final Statistics predictedProportion = statistics.get(predictedClassName);
				
				System.out.print(format(predictedProportion.getMean(), predictedProportion.getVariance()));
			}
			
			System.out.println("\\\\");
		}
	}
	
	public static final String format(final double mean, final double variance) {
		return " & $" + mean + "\\%\\pm" + sqrt(variance) + "$";
	}
	
}
