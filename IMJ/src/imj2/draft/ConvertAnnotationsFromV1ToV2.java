package imj2.draft;

import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getResourceAsStream;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Scanner;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;

/**
 * @author codistmonk (creation 2014-01-31)
 */
public final class ConvertAnnotationsFromV1ToV2 {
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("path", "."));
		final String imjVersion = scan1("imj/version");
		final String author = arguments.get("author", "IMJ.r" + imjVersion);
		
		System.out.println("Force author to: " + author);
		
		deepForEachFile(root, new FileProcessor() {
			
			@Override
			public final void process(final File file) {
				if (file.getName().toLowerCase(ENGLISH).endsWith(".xml")) {
					try {
						final Annotations annotations = Annotations.fromXML(file.getPath());
						
						System.out.println("Processing: " + file);
						
						for (final Annotation annotation : annotations.getAnnotations()) {
							annotation.setAuthor(author);
						}
						
						Annotations.toXML(annotations, new PrintStream(file));
					} catch (final Exception exception) {
						debugPrint(file, exception);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 4863822089880335019L;
			
		});
	}
	
	public static final String scan1(final String resourcePath) {
		return scan1(getResourceAsStream(resourcePath));
	}
	
	public static final String scan1(final InputStream input) {
		final Scanner scanner = new Scanner(input);
		
		try {
			return scanner.next();
		} finally {
			scanner.close();
		}
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
