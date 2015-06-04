package imj.apps;

import static multij.tools.Tools.readObject;
import static multij.tools.Tools.writeObject;
import imj.apps.GenerateClassificationData.ExtendedConfusionTable;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-06-06)
 */
public final class CollectConfusionTables {
	
	private CollectConfusionTables() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String directoryPath = arguments.get("from", ".");
		final String outPath = arguments.get("to", "confusions.jo");
		final File directory = new File(directoryPath);
		final Map<String, Map<String, ExtendedConfusionTable[]>> confusions = new TreeMap<String, Map<String, ExtendedConfusionTable[]>>();
		final String prefix = "confusion.";
		final String suffix = ".jo";
		
		for (final File file : directory.listFiles()) {
			final String fileName = file.getName();
			
			if (file.isFile() && file.canRead() && fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
				try {
					final Map<String, ExtendedConfusionTable[]> confusionMatrices = readObject(file.getPath());
					
					confusions.put(fileName.substring(prefix.length(), fileName.length() - suffix.length()), new TreeMap<String, ExtendedConfusionTable[]>(confusionMatrices));
				} catch (final Exception exception) {
					System.err.println(fileName);
				}
			}
		}
		
		writeObject((Serializable) confusions, outPath);
	}
	
}
