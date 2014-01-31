package imj2.tools;

import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.ignore;
import imj.apps.modules.Annotations;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2014-01-31)
 */
public final class ConvertAnnotationsFromV1ToV2 {
	
	/**
	 * @param commandLineArguments
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("path", "."));
		final String author = arguments.get("author", "IMJ");
		
		deepForEachFile(root, new FileProcessor() {
			
			@Override
			public final void process(final File file) {
				if (file.getName().toLowerCase(ENGLISH).endsWith(".xml")) {
					try {
						final Annotations annotations = Annotations.fromXML(file.getPath());
						Tools.debugPrint(file, annotations.getAnnotations().size());
					} catch (final Exception exception) {
						ignore(exception);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4863822089880335019L;
			
		});
	}
	
	public static final void deepForEachFile(final File root, final FileProcessor processor) {
		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				deepForEachFile(file, processor);
			} else {
				processor.process(file);
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2014-01-31)
	 */
	public static abstract interface FileProcessor extends Serializable {
		
		public abstract void process(File file);
		
	}
	
}
