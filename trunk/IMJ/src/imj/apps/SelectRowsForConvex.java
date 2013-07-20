package imj.apps;

import static imj.apps.modules.ShowActions.baseName;
import static java.lang.Double.parseDouble;
import static java.lang.Math.max;
import static java.util.Arrays.fill;
import static java.util.Collections.sort;
import imj.apps.GenerateROCPlots.DataPointXY;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-06-14)
 */
public final class SelectRowsForConvex {
	
	private SelectRowsForConvex() {
		throw new IllegalInstantiationException();
	}
	
	public static final <T> void swap(final T[] array, final int i, final int j) {
		final T tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}
	
	public static final <T> String join(final T[] array, final String separator) {
		final StringBuilder resultBuilder = new StringBuilder();
		final int n = array.length;
		
		if (0 < n) {
			resultBuilder.append(array[0]);
			
			for (int i = 1; i < n; ++i) {
				resultBuilder.append(separator).append(array[i]);
			}
		}
		
		return resultBuilder.toString();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String filePath = arguments.get("file", "");
		final int xField = arguments.get("x", 1)[0];
		final int yField = arguments.get("y", xField + 1)[0];
		final String outPath = arguments.get("out", baseName(filePath) + ".convex.csv");
		final boolean fixBadPoints = arguments.get("fixBadPoints", 0)[0] != 0;
		final Scanner scanner = new Scanner(new File(filePath));
		final List<DataPointXY> points = new ArrayList<DataPointXY>();
		int fieldCount = 0;
		
		scanner.useLocale(Locale.ENGLISH);
		
		while (scanner.hasNext()) {
			final String line = scanner.nextLine();
			final String[] fields = line.split("\\s+");
			final double x = parseDouble(fields[xField]);
			final double y = parseDouble(fields[yField]);
			fieldCount = max(fieldCount, fields.length);
			
			if (fixBadPoints) {
				if (x <= y) {
					points.add(new DataPointXY(line, x, y, 0.0, 0.0));
				} else {
					fields[xField] = "" + (1.0 - x);
					fields[yField] = "" + (1.0 - y);
					
					points.add(new DataPointXY(join(fields, "	"), 1.0 - x, 1.0 - y, 0.0, 0.0));
				}
			} else {
				points.add(new DataPointXY(line, x, y, 0.0, 0.0));
			}
		}
		
		{
			final String[] dummyFields = new String[fieldCount];
			
			fill(dummyFields, "0");
			points.add(new DataPointXY(join(dummyFields, "	"), 0.0, 0.0, 0.0, 0.0));
			dummyFields[xField] = dummyFields[yField] = "1";
			points.add(new DataPointXY(join(dummyFields, "	"), 1.0, 1.0, 0.0, 0.0));
		}
		
		sort(points, new Comparator<DataPointXY>() {
			
			@Override
			public final int compare(final DataPointXY p1, final DataPointXY p2) {
				int result = Double.compare(p1.getX(), p2.getX());
				
				if (result == 0) {
					result = Double.compare(p1.getY(), p2.getY());
				}
				
				return result;
			}
			
		});
		
		final int n = points.size();
		
		if (n == 0) {
			return;
		}
		
		final PrintStream out = new PrintStream(outPath);
		int edgeStart = findValidPoint(points);
		int edgeEnd = nextEdgeEnd(points, edgeStart);
		
		printLabelIfvalid(points.get(edgeStart), out);
		
		while (edgeEnd != n - 1) {
			printLabelIfvalid(points.get(edgeEnd), out);
			edgeStart = edgeEnd;
			edgeEnd = nextEdgeEnd(points, edgeStart);
		}
		
		printLabelIfvalid(points.get(edgeEnd), out);
	}
	
	public static final void printLabelIfvalid(final DataPointXY point, final PrintStream out) {
		if (point.isValid()) {
			out.println(point.getLabel());
		}
	}
	
	public static final int findValidPoint(final List<DataPointXY> points) {
		final int n = points.size();
		
		for (int i = 0; i < n; ++i) {
			if (points.get(i).isValid()) {
				return i;
			}
		}
		
		return n - 1;
	}
	
	public static final int nextEdgeEnd(final List<DataPointXY> points, final int edgeStart) {
		final int n = points.size();
		int result = edgeStart;
		double slope = Double.NEGATIVE_INFINITY;
		
		for (int i = edgeStart + 1; i < n; ++i) {
			final double s = slope(points.get(edgeStart), points.get(i));
			
			if (slope <= s) {
				slope = s;
				result = i;
			}
		}
		
		return result;
	}
	
	public static final double slope(final DataPointXY p0, final DataPointXY p1) {
		final double x0 = p0.getX();
		final double y0 = p0.getY();
		final double x1 = p1.getX();
		final double y1 = p1.getY();
		
		return !p0.isValid() || !p1.isValid() || x0 == x1 ? Double.NEGATIVE_INFINITY : (y1 - y0) / (x1 - x0);
	}
	
}
