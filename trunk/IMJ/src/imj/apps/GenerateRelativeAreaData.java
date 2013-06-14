package imj.apps;

import static java.lang.Math.sqrt;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-06-13)
 */
public final class GenerateRelativeAreaData {
	
	private GenerateRelativeAreaData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws FileNotFoundException 
	 */
	public static final void main(final String[] commandLineArguments) throws FileNotFoundException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String[] baseNames = arguments.get("from", "").split(",");
		final int n = baseNames.length;
		final Map<String, double[]> areas = new HashMap<String, double[]>();
		final double[] totalAreas = new double[n];
		final String outFilePath = arguments.get("out", "areas.csv");
		
		for (int i = 0; i < n; ++i) {
			final Annotations annotations = Annotations.fromXML(baseNames[i] + ".xml");
			
			for (final Annotation annotation : annotations.getAnnotations()) {
				final String className = annotation.getUserObject().toString();
				double[] annotationAreas = areas.get(className);
				
				if (annotationAreas == null) {
					annotationAreas = new double[n];
					areas.put(className, annotationAreas);
				}
				
				for (final Region region : annotation.getRegions()) {
					annotationAreas[i] += region.getArea();
					totalAreas[i] += region.getArea();
				}
			}
		}
		
		final PrintStream out = new PrintStream(outFilePath);
		
		try {
			for (final Map.Entry<String, double[]> entry : areas.entrySet()) {
				final Statistics areaStatistics = new Statistics();
				
				System.out.print(entry.getKey() + " ");
				
				for (int i = 0; i < n; ++i) {
					final double area = computeLeaveOneOutRelativeArea(entry.getValue(), totalAreas, i);
					
					areaStatistics.addValue(area);
					
					System.out.print(area + " ");
				}
				
				System.out.println();
				
				out.println(entry.getKey().replaceAll("\\s+", "_") + "	" +
						areaStatistics.getMean() + "	" + sqrt(areaStatistics.getVariance()));
			}
		} finally {
			out.close();
		}
	}
	
	public static final double computeLeaveOneOutRelativeArea(
			final double[] areas, final double[] totalAreas, final int excludedIndex) {
		final int n = areas.length;
		double sum = 0.0;
		double total = 0.0;
		
		for (int i = 0; i < n; ++i) {
			if (excludedIndex != i) {
				sum += areas[i];
				total += totalAreas[i];
			}
		}
		
		return total == 0.0 ? 0.0 : sum / total;
	}
	
}
