package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.TreeSet;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;

/**
 * @author codistmonk (creation 2014-07-17)
 */
public final class ExtractAnnotationsCategories {
	
	private ExtractAnnotationsCategories() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("root", ""));
		
		debugPrint(deepCollect(root.toString(), RegexFilter.newExtensionFilter("xml.gz")));
		
		// TODO Auto-generated method stub
	}
	
	public static final Collection<Path> deepCollect(final String rootDirectory, final FilenameFilter filter) {
		final Collection<Path> result = new TreeSet<>();
		
		try {
			Files.walkFileTree(FileSystems.getDefault().getPath(rootDirectory), new SimpleFileVisitor<Path>() {
				
				@Override
				public final FileVisitResult visitFile(final Path file,
						final BasicFileAttributes attrs) throws IOException {
					final Path parent = file.getParent();
					final Path name = file.getFileName();
					
					if (filter.accept(parent != null ? parent.toFile() : null, name.toString())) {
						result.add(file);
					}
					
					return super.visitFile(file, attrs);
				}
				
			});
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
		
		return result;
	}
	
}
