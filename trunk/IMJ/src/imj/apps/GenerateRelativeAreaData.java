package imj.apps;

import static imj.apps.Constants.EXCLUDED;
import static imj.apps.modules.ShowActions.baseName;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;
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
		final String[] imagePaths = arguments.get("from", "").split(",");
		final int n = imagePaths.length;
		final Map<String, double[]> areas = new HashMap<String, double[]>();
		final double[] totalAreas = new double[n];
		final String outFilePath = arguments.get("out", "areas.csv");
		
		for (int i = 0; i < n; ++i) {
			final Annotations annotations = Annotations.fromXML(baseName(imagePaths[i]) + ".xml");
			double excludedArea = getPixelCount(imagePaths[i]);
			
			for (final Annotation annotation : annotations.getAnnotations()) {
				final String className = annotation.getUserObject().toString();
				
				if (!EXCLUDED.equals(className)) {
					final double[] annotationAreas = getOrCreateAnnotationAreas(n, areas, className);
					
					for (final Region region : annotation.getRegions()) {
						final double regionArea = region.getArea();
						
						annotationAreas[i] += regionArea;
						totalAreas[i] += regionArea;
						excludedArea -= regionArea;
					}
				}
			}
			
			getOrCreateAnnotationAreas(n, areas, EXCLUDED)[i] = excludedArea;
			totalAreas[i] += excludedArea;
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

	private static double[] getOrCreateAnnotationAreas(final int n,
			final Map<String, double[]> areas, final String className) {
		double[] annotationAreas = areas.get(className);
		
		if (annotationAreas == null) {
			annotationAreas = new double[n];
			areas.put(className, annotationAreas);
		}
		return annotationAreas;
	}
	
	public static final long getPixelCount(final String imageFilePath) {
		final IFormatReader reader = new ImageReader();
		try {
			reader.setId(imageFilePath);
			
			return (long) reader.getSizeX() * reader.getSizeY();
		} catch (final Exception exception) {
			throw unchecked(exception);
		} finally {
			try {
				reader.close();
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
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
