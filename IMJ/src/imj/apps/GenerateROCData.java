package imj.apps;

import static imj.apps.ExtractRegions.loadLods;
import static imj.apps.modules.ShowActions.baseName;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;

import imj.Image;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.Sieve;
import imj.apps.modules.SimpleSieve;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

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
		final String annotationsId = arguments.get("annotations", baseName(imageId) + ".xml");
		final Annotations annotations = Annotations.fromXML(arguments.get("annotations", annotationsId));
		final int[] forceLods = arguments.get("lods");
		final Class<? extends Sieve> sieveClass =
				(Class<? extends Sieve>) Class.forName(arguments.get("sieve", SimpleSieve.class.getName()));
		final Sieve sieve = sieveClass.getConstructor(Context.class).newInstance(new Context());
		
		debugPrint(sieve);
		
		for (final Map.Entry<String, String> entry : sieve.getParameters().entrySet()) {
			entry.setValue(arguments.get(entry.getKey(), entry.getValue()));
			
			debugPrint(entry);
		}
		
		final List<Image> lods = loadLods(imageId);
		
		for (int lod = 0; lod < lods.size(); ++lod) {
			if (Arrays.binarySearch(forceLods, lod) < 0) {
				continue;
			}
			
			final TicToc timer = new TicToc();
			
			System.out.println("Processing lod " + lod + "... (" + new Date(timer.tic()) + ")");
			
			final Image image = lods.get(lod);
			final RegionOfInterest roi = RegionOfInterest.newInstance(image.getRowCount(), image.getColumnCount());
			
			sieve.setROI(roi, image);
			
			System.out.println("Processing lod " + lod + " done (time:" + timer.getTotalTime() + " memory:" + usedMemory() + ")");
		}
	}
	
}
