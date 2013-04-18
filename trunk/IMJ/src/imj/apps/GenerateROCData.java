package imj.apps;

import static imj.apps.ExtractRegions.loadLods;
import static imj.apps.modules.ShowActions.baseName;
import static java.util.Arrays.binarySearch;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions.UseAnnotationAsROI;
import imj.apps.modules.Sieve;
import imj.apps.modules.SimpleSieve;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-03-13)
 */
public final class GenerateROCData {
	
	private GenerateROCData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception If an error occurs
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final int[] forceLods = arguments.get("lods");
		final Class<? extends Sieve> sieveClass =
				(Class<? extends Sieve>) Class.forName(arguments.get("sieve", SimpleSieve.class.getName()));
		final Sieve sieve = sieveClass.getConstructor(Context.class).newInstance(new Context());
		final String classIdFilePath = arguments.get("classIds", "");
		
		System.out.println("sieve: " + sieve);
		
		for (final Map.Entry<String, String> entry : sieve.getParameters().entrySet()) {
			entry.setValue(arguments.get(entry.getKey(), entry.getValue()));
			
			System.out.println("parameter: " + entry);
		}
		
		final String annotationsId = arguments.get("annotations", baseName(imageId) + ".xml");
		
		generateROCData(imageId, annotationsId, forceLods, sieve, readClassIds(classIdFilePath), new ROCRowGenerator.Default());
	}
	
	public static final List<String> readClassIds(final String classIdFilePath) {
		final List<String> result = new ArrayList<String>();
		
		try {
			final Scanner scanner = new Scanner(new File(classIdFilePath));
			
			while (scanner.hasNext()) {
				result.add(scanner.nextLine());
			}
		} catch (final FileNotFoundException exception) {
			throw unchecked(exception);
		}
		
		return result;
	}
	
	public static final void generateROCData(final String imageId,
			final String annotationsId, final int[] forceLods, final Sieve sieve,
			final List<String> classIds, final ROCRowGenerator generator) {
		final List<Image> lods = loadLods(imageId);
		final Annotations annotations = Annotations.fromXML(annotationsId);
//		final Iterable<Region> regions = collectRegions(annotations, ".*excluded.*");
		final String fileName = new File(imageId).getName();
		
		for (int lod = 0; lod < lods.size(); ++lod) {
			if (forceLods.length != 0 && binarySearch(forceLods, lod) < 0) {
				continue;
			}
			
			final TicToc timer = new TicToc();
			
			System.out.println("Processing lod " + lod + "... (" + new Date(timer.tic()) + ")");
			
			final Image image = lods.get(lod);
			final int rowCount = image.getRowCount();
			final int columnCount = image.getColumnCount();
//			final RegionOfInterest reference = RegionOfInterest.newInstance(rowCount, columnCount);
			
			System.out.println("Initializing references... (" + new Date(timer.tic()) + ")");
			
//			UseAnnotationAsROI.set(reference, lod, regions);
			final List<RegionOfInterest> references = generateReferences(annotations, classIds, lod, rowCount, columnCount);
			
			System.out.println("Initializing reference done (time:" + timer.toc() + " memory:" + usedMemory() + ")");
			System.out.println("Generating data... (" + new Date(timer.tic()) + ")");
			
			sieve.initialize();
			
			generator.generateROCRow(fileName, image, lod, references, sieve);
			
			System.out.println("Generating data done (time:" + timer.toc() + " memory:" + usedMemory() + ")");
			System.out.println("Processing lod " + lod + " done (time:" + timer.getTotalTime() + " memory:" + usedMemory() + ")");
		}
	}
	
	public static final List<RegionOfInterest> generateReferences(final Annotations annotations, final List<String> classIds,
			final int lod, final int rowCount, final int columnCount) {
		final List<RegionOfInterest> result = new ArrayList<RegionOfInterest>(classIds.size());
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			final int classIndex = classIds.indexOf(annotation.getUserObject());
			
			if (classIndex < 0) {
				continue;
			}
			
			while (result.size() <= classIndex) {
				result.add(null);
			}
			
			if (result.get(classIndex) == null) {
				result.set(classIndex, RegionOfInterest.newInstance(rowCount, columnCount));
			}
			
			UseAnnotationAsROI.set(result.get(classIndex), lod, annotation.getRegions());
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-03-13)
	 */
	public static abstract interface ROCRowGenerator {
		
		public abstract void generateROCRow(String fileName, Image image, int lod,
				List<RegionOfInterest> references, Sieve sieve);
		
		/**
		 * @author codistmonk (creation 2013-03-13)
		 */
		public static final class Default implements ROCRowGenerator {
			
			@Override
			public final void generateROCRow(final String fileName, final Image image, final int lod,
					final List<RegionOfInterest> references, final Sieve sieve) {
				final TicToc timer = new TicToc();
				
				System.out.println("Appling sieve... (" + new Date(timer.tic()) + ")");
				
				final int rowCount = image.getRowCount();
				final int columnCount = image.getColumnCount();
				final RegionOfInterest computed = RegionOfInterest.newInstance(rowCount, columnCount);
				
				sieve.setROI(computed, image);
				
				System.out.println("Applying sieve done (time:" + timer.toc() + " memory:" + usedMemory() + ")");
				System.out.println("Collecting data... (" + new Date(timer.tic()) + ")");
				
				for (int classIndex = 0; classIndex < references.size(); ++classIndex) {
					final RegionOfInterest reference = references.get(classIndex);
					
					if (reference == null) {
						continue;
					}
					
					int truePositives = 0;
					int falsePositives = 0;
					int trueNegatives = 0;
					int falseNegatives = 0;
					final int pixelCount = rowCount * columnCount;
					
					for (int pixel = 0; pixel < pixelCount; ++pixel) {
						if (computed.get(pixel)) {
							if (reference.get(pixel)) {
								++truePositives;
							} else {
								++falsePositives;
							}
						} else {
							if (reference.get(pixel)) {
								++falseNegatives;
							} else {
								++trueNegatives;
							}
						}
					}
					
					final StringBuilder row = new StringBuilder();
					
					row.append("file_lod_class_");
					
					for (final String parameterName : sieve.getParameters().keySet()) {
						row.append(parameterName).append('_');
					}
					
					final char separator = '	';
					
					row.append("tp_fp_tn_fn: ")
					.append(fileName).append(separator)
					.append(lod).append(separator)
					.append(classIndex).append(separator);
					
					for (final String parameterValue : sieve.getParameters().values()) {
						row.append(parameterValue).append(separator);
					}
					
					row
					.append(truePositives).append(separator)
					.append(falsePositives).append(separator)
					.append(trueNegatives).append(separator)
					.append(falseNegatives);
					
					System.out.println(row);
				}
				
				System.out.println("Collecting data done (" + new Date(timer.tic()) + ")");
			}
			
		}
		
	}
	
}
