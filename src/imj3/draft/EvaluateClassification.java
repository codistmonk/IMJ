package imj3.draft;

import static java.lang.Integer.toHexString;

import imj3.draft.ConfusionMatrix.AtomicDouble;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-22)
 */
public final class EvaluateClassification {
	
	private EvaluateClassification() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File groundtruthFile = new File(arguments.get("groundtruth", ""));
		final File classificationFile = new File(arguments.get("classification", ""));
		
		System.out.println("groundtruth: " + groundtruthFile);
		System.out.println("classification: " + classificationFile);
		
		final BufferedImage groundtruth = ImageIO.read(groundtruthFile);
		final BufferedImage classification = ImageIO.read(classificationFile);
		final boolean binarize = arguments.get("binarize", 0)[0] != 0;
		final ConfusionMatrix<?> confusionMatrix = evaluate(classification, groundtruth, binarize);
		
		System.out.println("counts: " + confusionMatrix.getCounts());
		System.out.println("F1s: " +  confusionMatrix.computeF1s());
		System.out.println("macroF1: " + confusionMatrix.computeMacroF1());
		
		System.out.println("BEGIN COUNTS TABLE");
		writeCSV(confusionMatrix, "\t", System.out);
		System.out.println("END COUNTS TABLE");
	}
	
	public static final <K extends Comparable<K>> void writeCSV(final ConfusionMatrix<K> confusionMatrix, final String separator, final PrintStream output) {
		final Collection<K> keys = collectKeys(confusionMatrix.getCounts());
		
		for (final K key : keys) {
			final Map<K, AtomicDouble> row = confusionMatrix.getCounts().getOrDefault(key, Collections.emptyMap());
			boolean printSeparator = false;
			
			for (final K subkey : keys) {
				if (printSeparator) {
					output.print(separator);
				} else {
					printSeparator = true;
				}
				
				output.print(row.getOrDefault(subkey, ConfusionMatrix.ZERO));
			}
			
			output.println();
		}
	}
	
	public static final <K extends Comparable<K>, V> Collection<K> collectKeys(final Map<K, Map<K, V>> map) {
		final Collection<K> result = new TreeSet<>(map.keySet());
		
		map.values().stream().forEach(m -> result.addAll(m.keySet()));
		
		return result;
	}
	
	public static final ConfusionMatrix<String> evaluate(final BufferedImage classification, final BufferedImage groundtruth, final boolean binarize) {
		final int groundtruthWidth = groundtruth.getWidth();
		final int groundtruthHeight = groundtruth.getHeight();
		final int classificationWidth = classification.getWidth();
		final int classificationHeight = classification.getHeight();
		final ConfusionMatrix<String> result = new ConfusionMatrix<>();
		
		for (int y = 0; y < groundtruthHeight; ++y) {
			final int classificationY = y * classificationHeight / groundtruthHeight;
			
			for (int x = 0; x < groundtruthWidth; ++x) {
				final int classificationX = x * classificationWidth / groundtruthWidth;
				final int predicted = maybeBinarize(classification.getRGB(classificationX, classificationY), binarize);
				final int actual = maybeBinarize(groundtruth.getRGB(x, y), binarize);
				
				result.count(toHexString(predicted), toHexString(actual));
			}
		}
		
		return result;
	}
	
	public static final int maybeBinarize(final int label, final boolean binarize) {
		if (!binarize) {
			return label;
		}
		
		if ((label & 0x00FFFFFF) != 0) {
			return ~0;
		}
		
		return 0xFF000000;
	}
	
}
