package imj2.tools;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.xml.XMLTools.getNode;
import static net.sourceforge.aprog.xml.XMLTools.getNodes;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;
import net.sourceforge.aprog.xml.XMLTools;

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
	public static final void main(final String[] commandLineArguments) throws Exception {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File root = new File(arguments.get("root", ""));
		final File categoriesFile = new File(arguments.get("categories", "categories.xml"));
		final String categoryName = arguments.get("category", "");
		
		if (categoryName.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		final Document categories = parseOrCreateCategories(categoriesFile);
		final Element category = getOrCreateCategory(categories, categoryName);
		final Collection<Path> compressedAnnotations = deepCollect(root.toString(), RegexFilter.newExtensionFilter("xml.gz"));
		final Map<String, Integer> labelIds = new LinkedHashMap<>();
		
		if (category != null) {
			for (final Node node : getNodes(category, "label")) {
				final Element label = (Element) node;
				final int labelId = Integer.parseInt(label.getAttribute("labelId"));
				
				labelIds.put(computeLabelKey(label), labelId);
			}
			
			debugPrint(labelIds);
		}
		
		for (final Path compressedAnnotation : compressedAnnotations) {
			try (final GZIPInputStream input = new GZIPInputStream(new FileInputStream(compressedAnnotation.toString()))) {
				final Document annotations = parse(input);
				
				for (final Node node : getNodes(annotations, "annotations/labels/label")) {
					final Element label = (Element) node;
					final String key = computeLabelKey(label);
					Integer labelId = labelIds.get(key);
					
					if (labelId == null) {
						labelId = labelIds.size();
						
						labelIds.put(key, labelId);
						category.appendChild(categories.adoptNode(label.cloneNode(true)));
					}
				}
			}
		}
		
		debugPrint(labelIds);
		
		XMLTools.write(categories, categoriesFile, 0);
	}
	
	public static final String computeLabelKey(final Element label) {
		return label.getAttribute("mnemonic") + " " + label.getAttribute("description");
	}
	
	public static final Element getOrCreateCategory(final Document categories, final String categoryName) {
		Element result = (Element) getNode(categories, "categories/category[@name='" + categoryName + "']");
		
		if (result == null) {
			result = (Element) categories.getDocumentElement().appendChild(categories.createElement("category"));
			
			result.setAttribute("name", categoryName);
		}
		
		return result;
	}
	
	public static final Document parseOrCreateCategories(final File inputFile) {
		try {
			final Document result = parse(new FileInputStream(inputFile));
			
			if (!"categories".equals(result.getDocumentElement().getTagName())) {
				throw new IllegalArgumentException();
			}
			
			return result;
		} catch (final Exception exception) {
			return parse("<categories/>");
		}
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
