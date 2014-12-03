package imj2.draft;

import static net.sourceforge.aprog.tools.Tools.baseName;
import imj2.tools.ColorSeparationTest.RGBTransformer;

import java.io.File;

import javax.swing.ComboBoxModel;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-12-03)
 */
public final class PaletteBasedHistograms {
	
	private PaletteBasedHistograms() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("in", ""));
		final File paletteFile = new File(arguments.get("palette", "transformerSelectorModel.jo"));
		final ComboBoxModel<? extends RGBTransformer> palette = Tools.readObject(paletteFile.getPath());
		
		Tools.debugPrint(palette);
		
		for (final File file : root.listFiles()) {
			final String fileName = file.getName();
			if (fileName.endsWith(".png")) {
				final String baseName = baseName(fileName);
				final String maskName = baseName + "_mask.png";
				final String labelsName = baseName + "_labels.png";
				final String segmentedName = baseName + "_segmented.png";
				final File maskFile = new File(file.getParentFile(), maskName);
				final File labelsFile = new File(file.getParentFile(), labelsName);
				final File segmentedFile = new File(file.getParentFile(), segmentedName);
				
				if (maskFile.exists() && !(labelsFile.exists() && segmentedFile.exists())) {
					Tools.debugPrint(file);
				}
			}
		}
	}

}
