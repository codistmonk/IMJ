package imj2.draft;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.xml.XMLTools.getNode;
import static net.sourceforge.aprog.xml.XMLTools.getNodes;
import static net.sourceforge.aprog.xml.XMLTools.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.RegexFilter;
import net.sourceforge.aprog.tools.Tools;
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
		final String suffix = arguments.get("suffix", "\\.xml\\.gz");
		final File categoriesCompressedFile = new File(arguments.get("categories", "categories.xml.gz"));
		final String categoryName = arguments.get("category", "");
		
		if (categoryName.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		final Document categories = parseOrCreateCategories(categoriesCompressedFile);
		final Element category = getOrCreateCategory(categories, categoryName);
		final Collection<Path> compressedAnnotations = deepCollect(root.toString(), RegexFilter.newSuffixFilter(suffix));
		final Map<String, Integer> labelIds = new LinkedHashMap<>();
		final String categoryId = category.getAttribute("categoryId");
		
		getLabelIds(category, labelIds);
		
		debugPrint(categoryId, labelIds);
		
		for (final Path compressedAnnotation : compressedAnnotations) {
			debugPrint("Analyzing", compressedAnnotation);
			
			try (final GZIPInputStream input = new GZIPInputStream(new FileInputStream(compressedAnnotation.toString()))) {
				final Document annotations = parse(input);
				
				collectLabels(annotations, categories, category, labelIds);
			}
		}
		
		fixDamagedLabelIds(labelIds);
		
		debugPrint(labelIds);
		
		debugPrint("Updating", categoriesCompressedFile);
		
		for (final Node node : getNodes(category, "label")) {
			final Element label = (Element) node;
			label.setAttribute("labelId", labelIds.get(computeLabelKey(label)).toString());
		}
		
		try (final OutputStream output = new GZIPOutputStream(new FileOutputStream(categoriesCompressedFile))) {
			XMLTools.write(categories, output, 0);
		}
		
		for (final Path compressedAnnotation : compressedAnnotations) {
			debugPrint("Updating", compressedAnnotation);
			
			Document annotations = null;
			
			try (final InputStream input = new GZIPInputStream(new FileInputStream(compressedAnnotation.toString()))) {
				annotations = parse(input);
				
				for (final Node node : getNodes(annotations, "//region/labels/label")) {
					final Element regionLabel = (Element) node;
					final Element documentLabel = (Element) getNode(annotations, "/annotations/labels/label[@labelId=\"" + regionLabel.getAttribute("labelId") + "\"]");
					
					regionLabel.setAttribute("categoryId", categoryId);
					regionLabel.setAttribute("labelId", labelIds.get(computeLabelKey(documentLabel)).toString());
				}
			}
			
			try (final OutputStream output = new GZIPOutputStream(new FileOutputStream(compressedAnnotation.toFile()))) {
				XMLTools.write(annotations, output, 0);
			}
		}
	}
	
	public static final void fixDamagedLabelIds(final Map<String, Integer> labelIds) {
		final Map<String, Integer> tmp = new HashMap<>();
		
		for (final String key : labelIds.keySet()) {
			tmp.put(key, tmp.size());
		}
		
		labelIds.clear();
		labelIds.putAll(tmp);
	}
	
	public static final void getLabelIds(final Element category, final Map<String, Integer> labelIds) {
		for (final Node node : getNodes(category, "label")) {
			final Element label = (Element) node;
			final int labelId = Integer.parseInt(label.getAttribute("labelId"));
			
			labelIds.put(computeLabelKey(label), labelId);
		}
	}
	
	public static final void collectLabels(final Document annotations,
			final Document categories, final Element category,
			final Map<String, Integer> labelIds) {
		for (final Node node : getNodes(annotations, "annotations/labels/label")) {
			final Element label = (Element) node;
			final String key = computeLabelKey(label);
			Integer labelId = labelIds.get(key);
			
			if (labelId == null) {
				labelId = labelIds.size();
				
				Tools.debugPrint(key, labelId);
				
				labelIds.put(key, labelId);
				category.appendChild(categories.adoptNode(label.cloneNode(true)));
			}
		}
	}
	
	public static final String computeLabelKey(final Element label) {
		return label.getAttribute("mnemonic") + " " + label.getAttribute("description");
	}
	
	public static final Element getOrCreateCategory(final Document categories, final String categoryName) {
		Element result = (Element) getNode(categories, "categories/category[@name='" + categoryName + "']");
		
		if (result == null) {
			final int newCategoryId = computeNewCategoryId(categories);
			result = (Element) categories.getDocumentElement().appendChild(categories.createElement("category"));
			
			result.setAttribute("name", categoryName);
			result.setAttribute("categoryId", Integer.toString(newCategoryId));
		}
		
		return result;
	}
	
	public static final int computeNewCategoryId(final Document categories) {
		int lastCategoryId = -1;
		
		for (final Node node : getNodes(categories, "categories/category")) {
			lastCategoryId = max(lastCategoryId, getIntegerAttribute(node, "categoryId"));
		}
		
		return lastCategoryId + 1;
	}
	
	public static final int getIntegerAttribute(final Node node, final String attributeName) {
		final String attribute = ((Element) node).getAttribute(attributeName).trim();
		
		return attribute.isEmpty() ? 0 : parseInt(attribute);
	}
	
	public static final Document parseOrCreateCategories(final File compressedInputFile) {
		try (final InputStream input = new GZIPInputStream(new FileInputStream(compressedInputFile))) {
			final Document result = parse(input);
			
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
