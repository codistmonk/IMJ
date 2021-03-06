package imj.apps;

import static imj.apps.GenerateROCData.generateROCData;
import static imj.apps.GenerateROCData.readClassIds;
import static java.lang.Math.min;
import static multij.tools.Tools.baseName;

import java.util.List;

import imj.Image;
import imj.apps.GenerateROCData.ROCRowGenerator;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.SaturationSieve201303080950;
import imj.apps.modules.Sieve;

import multij.context.Context;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-03-13)
 */
public final class GenerateROCDataForSaturationSieve201303080950 {
	
	private GenerateROCDataForSaturationSieve201303080950() {
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
		final String annotationsId = arguments.get("annotations", baseName(imageId) + ".xml");
		final int[] forceLods = arguments.get("lods");
		final Sieve sieve = new SaturationSieve201303080950(new Context());
		final ROCRowGenerator defaultGenerator = new ROCRowGenerator.Default();
		final int step = arguments.get("step", 8)[0];
		final String classIdFilePath = arguments.get("classIds", "classIds.txt");
		
		generateROCData(imageId, annotationsId, forceLods, sieve, readClassIds(classIdFilePath), new ROCRowGenerator() {
			
			@Override
			public final void generateROCRow(final String fileName, final Image image, final int lod,
					final List<RegionOfInterest> references, final Sieve sieve) {
				for (int minimum = 0; minimum <= 256; minimum = minimum += step) {
					sieve.getParameters().put("minimum", "" + min(255, minimum));
					
					for (int maximum = minimum; maximum <= 256; maximum += step) {
						sieve.getParameters().put("maximum", "" + min(255, maximum));
						
						sieve.initialize();
						
						defaultGenerator.generateROCRow(fileName, image, lod, references, sieve);
					}
				}
			}
			
		});
		
	}
	
}
