package imj.apps.modules;

import static java.lang.Thread.currentThread;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.iterable;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author codistmonk (creation 2013-04-08)
 */
public enum ClassPathProtocol {
	
	FILE {
		
		@Override
		public final Collection<Class<?>> getClasses(final String packageNameAsPath, final String resourceFile, final Collection<Class<?>> result) {
			final File file = new File(resourceFile);
			final File[] files = file.listFiles();
			
			for (final File f : files) {
				if (f.getName().toLowerCase(ENGLISH).endsWith(".class")) {
					final int i = f.getPath().indexOf(packageNameAsPath);
					
					result.add(getClass(f.getPath().substring(i)));
				}
			}
			
			return result;
		}
		
	}, JAR {
		
		@Override
		public final Collection<Class<?>> getClasses(final String packageNameAsPath, final String resourceFile, final Collection<Class<?>> result) {
			try {
				final String[] resourcePath = resourceFile.split("!/");
				final JarFile jar = new JarFile(new URL(resourcePath[0]).getFile());
				
				for (final JarEntry entry : iterable(jar.entries())) {
					if (entry.getName().startsWith(resourcePath[1]) && entry.getName().toLowerCase(ENGLISH).endsWith(".class")) {
						result.add(getClass(entry.getName()));
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace();
			}
			
			return result;
		}
		
	}, UNKNOWN {
		
		@Override
		public final Collection<Class<?>> getClasses(final String packageNameAsPath, final String resourceFile, final Collection<Class<?>> result) {
			return result;
		}
		
	};
	
	public abstract Collection<Class<?>> getClasses(String packageNameAsPath, String resourceFile, Collection<Class<?>> result);
	
	public static final ClassPathProtocol parseProtocol(final String protocol) {
		try {
			return valueOf(protocol.trim().toUpperCase(Locale.ENGLISH));
		} catch (final Exception exception) {
			return UNKNOWN;
		}
	}
	
	public static final Class<?> getClass(final String classFilePath) {
		try {
			return Class.forName(classFilePath.replaceFirst(".class$", "").replaceAll("/", "."));
		} catch (final ClassNotFoundException exception) {
			return null;
		}
	}
	
	public static final Collection<Class<?>> getClassesInPackage(final String packageName) {
		try {
			final Collection<Class<?>> result = new ArrayList<Class<?>>();
			final String packageNameAsPath = packageName.replaceAll("\\.", "/");
			final Enumeration<URL> resources = currentThread().getContextClassLoader().getResources(packageNameAsPath);
			
			for (final URL resource : iterable(resources)) {
				debugPrint("Scanning", resource);
				parseProtocol(resource.getProtocol()).getClasses(packageNameAsPath, resource.getFile(), result);
			}
			
			return result;
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
	}
	
}
