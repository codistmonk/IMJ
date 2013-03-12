package imj.apps;

import static imj.apps.modules.ShowActions.baseName;

import java.util.ArrayList;
import java.util.List;

import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-03-12)
 */
public final class ExtractRegions {
	
	private ExtractRegions() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imageId = arguments.get("file", "");
		final String annotationsId = arguments.get("annotations", baseName(imageId) + ".xml");
		final Annotations annotations = Annotations.fromXML(arguments.get("annotations", annotationsId));
		int regionCount = 0;
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			regionCount += annotation.getRegions().size();
		}
		
		if (regionCount == 0) {
			System.out.println("No region found");
			
			return;
		}
		
		final List<Image> lods = new ArrayList<Image>();
		int lod = 0;
		Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		
		while (1 <= image.getRowCount() && 1 <= image.getColumnCount()) {
			lods.add(image);
			image = ImageWrangler.INSTANCE.load(imageId, ++lod);
		}
		
		// TODO
		Tools.debugPrint("TODO");
	}
	
}
