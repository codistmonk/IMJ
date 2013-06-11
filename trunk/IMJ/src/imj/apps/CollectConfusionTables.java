package imj.apps;

import static imj.IMJTools.readObject;
import static imj.IMJTools.writeObject;
import imj.apps.GenerateClassificationData.ConfusionTable;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

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
		final Map<String, Map<String, ConfusionTable[]>> confusions = new HashMap<String, Map<String, ConfusionTable[]>>();
		final String prefix = "confusion.";
		final String suffix = ".jo";
		
		for (final File file : directory.listFiles()) {
			final String fileName = file.getName();
			
			if (file.isFile() && file.canRead() && fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
				final Map<String, ConfusionTable[]> confusionTables = readObject(file.getPath());
				
				confusions.put(fileName.substring(prefix.length(), fileName.length() - suffix.length()), confusionTables);
			}
		}
		
		writeObject((Serializable) confusions, outPath);
	}
	
}
