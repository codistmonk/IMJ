package imj3.draft;

import static java.lang.Integer.toHexString;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

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
	
	private static final AtomicLong ZERO = new AtomicLong();
	
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
		final ConfusionMatrix confusionMatrix = evaluate(classification, groundtruth, binarize);
		
		System.out.println("counts: " + confusionMatrix.getCounts());
		System.out.println("F1s: " +  confusionMatrix.computeF1s());
		System.out.println("macroF1: " + confusionMatrix.computeMacroF1());
		
		System.out.println("BEGIN COUNTS TABLE");
		writeCSV(confusionMatrix, "\t", System.out);
		System.out.println("END COUNTS TABLE");
	}
	
	public static void writeCSV(final ConfusionMatrix confusionMatrix, final String separator, final PrintStream output) {
		final Collection<Comparable<?>> keys = collectKeys(confusionMatrix.getCounts());
		
		for (final Comparable<?> key : keys) {
			final Map<Comparable<?>, AtomicLong> row = confusionMatrix.getCounts().getOrDefault(key, Collections.emptyMap());
			boolean printSeparator = false;
			
			for (final Comparable<?> subkey : keys) {
				if (printSeparator) {
					output.print(separator);
				} else {
					printSeparator = true;
				}
				
				output.print(row.getOrDefault(subkey, ZERO));
			}
			
			output.println();
		}
	}
	
	public static final <V> Collection<Comparable<?>> collectKeys(final Map<Comparable<?>, Map<Comparable<?>, V>> map) {
		final Collection<Comparable<?>> result = new TreeSet<>(map.keySet());
		
		map.values().stream().forEach(m -> result.addAll(m.keySet()));
		
		return result;
	}
	
	public static final ConfusionMatrix evaluate(final BufferedImage classification, final BufferedImage groundtruth, final boolean binarize) {
		final int groundtruthWidth = groundtruth.getWidth();
		final int groundtruthHeight = groundtruth.getHeight();
		final int classificationWidth = classification.getWidth();
		final int classificationHeight = classification.getHeight();
		final ConfusionMatrix result = new ConfusionMatrix();
		
		for (int y = 0; y < groundtruthHeight; ++y) {
			final int classificationY = y * classificationHeight / groundtruthHeight;
			
			for (int x = 0; x < groundtruthWidth; ++x) {
				final int classificationX = x * classificationWidth / groundtruthWidth;
				final int predicted = maybeBinarize(classification.getRGB(classificationX, classificationY), binarize);
				final int expected = maybeBinarize(groundtruth.getRGB(x, y), binarize);
				
				result.count(toHexString(predicted), toHexString(expected));
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
	
	/**
	 * @author codistmonk (creation 2015-04-22)
	 */
	public static final class ConfusionMatrix implements Serializable {
		
		private final Map<Comparable<?>, Map<Comparable<?>, AtomicLong>> counts = new TreeMap<>();
		
		public final Map<Comparable<?>, Map<Comparable<?>, AtomicLong>> getCounts() {
			return this.counts;
		}
		
		public final <C extends Comparable<C>> void count(final C predicted, final C expected) {
			this.getCounts().computeIfAbsent(predicted, p -> new TreeMap<>()).computeIfAbsent(
					expected, e -> new AtomicLong()).incrementAndGet();
		}
		
		public final Map<Comparable<?>, Double> computeF1s() {
			final Map<Object, AtomicLong> tps = new HashMap<>();
			final Map<Object, AtomicLong> fps = new HashMap<>();
			final Map<Object, AtomicLong> fns = new HashMap<>();
			final Collection<Comparable<?>> keys = new HashSet<>(this.getCounts().keySet());
			
			for (final Entry<Comparable<?>, Map<Comparable<?>, AtomicLong>> entry : this.getCounts().entrySet()) {
				final Object predicted = entry.getKey();
				
				keys.addAll(entry.getValue().keySet());
				
				for (final Map.Entry<?, AtomicLong> subentry : entry.getValue().entrySet()) {
					final Object expected = subentry.getKey();
					final long delta = subentry.getValue().get();
					
					if (predicted.equals(expected)) {
						increment(tps, predicted, delta);
					} else {
						increment(fps, predicted, delta);
						increment(fns, expected, delta);
					}
				}
			}
			
			{
				final Map<Comparable<?>, Double> result = new TreeMap<>();
				
				for (final Comparable<?> key : keys) {
					final double tp = tps.getOrDefault(key, ZERO).get();
					final double fp = fps.getOrDefault(key, ZERO).get();
					final double fn = fns.getOrDefault(key, ZERO).get();
					
					result.put(key, 2.0 * tp / (2.0 * tp + fp + fn));
				}
				
				return result;
			}
			
		}
		
		public final double computeMacroF1() {
			return computeMacroF1(this.computeF1s());
		}
		
		private static final long serialVersionUID = -3078169987830724986L;
		
		private static final AtomicLong ZERO = new AtomicLong();
		
		public static void increment(final Map<Object, AtomicLong> counts, final Object key, final long delta) {
			counts.computeIfAbsent(key, e -> new AtomicLong()).addAndGet(delta);
		}
		
		public static final double computeMacroF1(final Map<?, Double> f1s) {
			return f1s.values().stream().mapToDouble(Double::doubleValue).average().getAsDouble();
		}
		
	}
	
}
