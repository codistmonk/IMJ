package imj3.draft;

import static java.lang.Integer.toHexString;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-04-22)
 */
public final class EvaluateClassification {
	
	private EvaluateClassification() {
		throw new IllegalInstantiationException();
	}
	
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File groundtruthFile = new File(arguments.get("groundtruth", ""));
		final File classificationFile = new File(arguments.get("classification", ""));
		final BufferedImage groundtruth = ImageIO.read(groundtruthFile);
		final BufferedImage classification = ImageIO.read(classificationFile);
		final int groundtruthWidth = groundtruth.getWidth();
		final int groundtruthHeight = groundtruth.getHeight();
		final int classificationWidth = classification.getWidth();
		final int classificationHeight = classification.getHeight();
		final ConfusionMatrix confusionMatrix = new ConfusionMatrix();
		
		for (int y = 0; y < classificationHeight; ++y) {
			final int groundtruthY = y * groundtruthHeight / classificationHeight;
			
			for (int x = 0; x < classificationWidth; ++x) {
				final int groundtruthX = x * groundtruthWidth / classificationWidth;
				
				confusionMatrix.count(toHexString(classification.getRGB(x, y)), toHexString(groundtruth.getRGB(groundtruthX, groundtruthY)));
			}
			
		}
		
		debugPrint(confusionMatrix.getCounts());
		debugPrint(confusionMatrix.computeF1s());
		debugPrint(confusionMatrix.computeMacroF1());
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
			this.getCounts().computeIfAbsent(predicted, p -> new TreeMap<>()).computeIfAbsent(expected, e -> new AtomicLong()).incrementAndGet();
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
						tps.computeIfAbsent(predicted, e -> new AtomicLong()).addAndGet(delta);
					} else {
						fps.computeIfAbsent(predicted, e -> new AtomicLong()).addAndGet(delta);
						fns.computeIfAbsent(expected, e -> new AtomicLong()).addAndGet(delta);
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
		
		public static final double computeMacroF1(final Map<?, Double> f1s) {
			return f1s.values().stream().mapToDouble(Double::doubleValue).average().getAsDouble();
		}
		
	}
	
}
