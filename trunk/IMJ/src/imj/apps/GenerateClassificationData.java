package imj.apps;

import static imj.IMJTools.loadAndTryToCache;
import static imj.apps.modules.ShowActions.baseName;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.loadRegions;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newRGBSampler;
import static imj.database.IMJDatabaseTools.reduceArbitrarily;
import static imj.database.IMJDatabaseTools.updateNegativeGroups;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.readObject;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.writeObject;

import imj.Image;
import imj.IntList;
import imj.apps.GenerateSampleDatabase.Configuration;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.BKDatabase;
import imj.database.BinningQuantizer;
import imj.database.PatchDatabase;
import imj.database.Quantizer;
import imj.database.Sample;
import imj.database.Sampler;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-06-04)
 */
public final class GenerateClassificationData {
	
	private GenerateClassificationData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * {@value}.
	 */
	public static final String EXCLUDED = "Edges & Artifacts to be excluded";
	
	private static final Map<String, SynchronizedWeakLoader> cache = new HashMap<String, SynchronizedWeakLoader>();
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final Configuration configuration = new Configuration(commandLineArguments);
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final int threadCount = arguments.get("threads", 3)[0];
		final int maximumGroupSize = arguments.get("maximumGroupSize", 0)[0];
		final TicToc timer = new TicToc();
		final String[] trainingSet0 = arguments.get("using", "").split(",");
		final String testImageId0 = arguments.get("on", "");
		final String[][] trainingSets;
		final String[] testImageIds;
		final boolean checkDatabase = arguments.get("checkDatabase", 1)[0] != 0;
		final boolean processImages = arguments.get("processImages", 1)[0] != 0;
		
		if ("".equals(testImageId0)) {
			final int n = trainingSet0.length;
			trainingSets = new String[n][];
			testImageIds = new String[n];
			
			for (int i = 0; i < n; ++i) {
				trainingSets[i] = remove(i, trainingSet0);
				testImageIds[i] = trainingSet0[i];
			}
		} else {
			trainingSets = new String[][] { trainingSet0 };
			testImageIds = new String[] { testImageId0 };
		}
		
		final Map<String, ExtendedConfusionTable[]> allConfusionTables = new HashMap<String, ExtendedConfusionTable[]>();
		final int n = trainingSets.length;
		final Collection<Future<?>> tasks = new ArrayList<Future<?>>();
		final ExecutorService executor = newFixedThreadPool(threadCount);
		
		try {
			for (int i0 = 0; i0 < n; ++i0) {
				final int i = i0;
				
				tasks.add(executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						final String[] trainingSet = trainingSets[i];
						final String testImageId = testImageIds[i];
						final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
						
						System.out.println("trainingSet: " + Arrays.toString(trainingSet));
						System.out.println("testImageId: " + testImageId);
						
						System.out.println("Loading training set... " + new Date(timer.tic()));
						
						loadTrainingSet(configuration, trainingSet, sampleDatabase, maximumGroupSize);
						
						System.out.println("Loading training set done, time: " + timer.toc());
						
						if (checkDatabase) {
							 writeObject(checkDatabase(sampleDatabase),
									 testImageId + ".dbinfo" + configuration.getConfusionSuffix());
						}
						
						if (processImages) {
							System.out.println("Processing image... " + new Date(timer.tic()));
							
							final Map<String, ExtendedConfusionTable> confusionTables = computeConfusionTables(
									configuration, testImageId, sampleDatabase);
							
							synchronizedUpdate(allConfusionTables, n, i, confusionTables);
							
							System.out.println("Processing image done, time: " + timer.toc());
							System.out.println("confusionTables: " + confusionTables);
						}
					}
					
				}));
			}
			
			for (final Future<?> task : tasks) {
				task.get();
			}
		} finally {
			executor.shutdown();
		}
		
		if (!processImages) {
			return;
		}
		
		for (final Map.Entry<String, ExtendedConfusionTable[]> entry : allConfusionTables.entrySet()) {
			final Statistics fpr = new Statistics();
			final Statistics tpr = new Statistics();
			
			for (final ExtendedConfusionTable table : entry.getValue()) {
				if (0L < table.getActualNegative() && 0L < table.getActualPositive()) {
					fpr.addValue(table.getFalsePositiveRate());
					tpr.addValue(table.getTruePositiveRate());
				}
			}
			
			System.out.println("class: " + entry.getKey() + ", FPR: " + format(fpr) + ", TPR: " + format(tpr));
		}
		
		{
			System.out.println("Saving confusion tables... " + new Date(timer.tic()));
			
			writeObject((Serializable) allConfusionTables, "confusion" + configuration.getConfusionSuffix());
			
			System.out.println("Saving confusion tables done, time: " + timer.toc());
		}
	}
	
	public static final void synchronizedUpdate(
			final Map<String, ExtendedConfusionTable[]> allConfusionTables,
			final int testCount, final int testIndex, final Map<String, ExtendedConfusionTable> confusionTables) {
		synchronized (allConfusionTables) {
			for (final Map.Entry<String, ExtendedConfusionTable> entry : confusionTables.entrySet()) {
				ExtendedConfusionTable[] tables = allConfusionTables.get(entry.getKey());
				
				if (tables == null) {
					tables = new ExtendedConfusionTable[testCount];
					allConfusionTables.put(entry.getKey(), tables);
				}
				
				tables[testIndex] = entry.getValue();
			}
		}
	}
	
	public static final String format(final Statistics statistics) {
		return statistics.getMean() + "(" + Math.sqrt(statistics.getVariance()) + ")";
	}
	
	public static final <T> T[] remove(final int index, final T[] array) {
		try {
			final T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - 1);
			
			System.arraycopy(array, 0, result, 0, index);
			System.arraycopy(array, index + 1, result, index, array.length - index - 1);
			
			return result;
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Map<String, ExtendedConfusionTable> computeConfusionTables(
			final Configuration configuration, final String testImageId,
			final PatchDatabase<Sample> sampleDatabase) {
		final Image image = loadAndTryToCache(testImageId, configuration.getLod());
		final Quantizer quantizer = new BinningQuantizer();
		final Channel[] channels = RGB;
		
		quantizer.initialize(image, null, channels, configuration.getQuantizationLevel());
		
		final Sample.Collector collector = new Sample.Collector();
		final Sampler sampler = newRGBSampler(configuration.getSamplerClass(), image, quantizer, collector);
		final BKDatabase<Sample> bkDatabase = newBKDatabase(sampleDatabase, getPreferredMetric(sampler));
		
		gc();
		
		debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Annotations annotations = Annotations.fromXML(baseName(testImageId) + ".xml");
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		final Map<String, ExtendedConfusionTable> result = new HashMap<String, ExtendedConfusionTable>();
		
		loadRegions(testImageId, configuration.getLod(), imageRowCount, imageColumnCount, annotations, classes);
		
		{
			final TicToc timer = new TicToc();
			
			debugPrint("Resetting excluded regions mask...", new Date(timer.tic()));
			
			final RegionOfInterest excluded = classes.get(EXCLUDED);
			final int pixelCount = excluded.getPixelCount();
			
			excluded.reset(true);
			
			for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
				if (EXCLUDED.equals(entry.getKey())) {
					continue;
				}
				
				final RegionOfInterest roi = entry.getValue();
				
				for (int pixel = 0; pixel < pixelCount; ++pixel) {
					if (roi.get(pixel)) {
						excluded.set(pixel, false);
					}
				}
			}
			
			debugPrint("Resetting excluded regions mask done, time:", timer.toc());
		}
		
		for (final String key : classes.keySet()) {
			result.put(key, new ExtendedConfusionTable());
		}
		
		configuration.newSegmenter().process(image, new Sampler(image, quantizer, channels, collector) {
			
			private final IntList pixels = new IntList();
			
			@Override
			public final void process(final int pixel) {
				sampler.process(pixel);
				
				this.pixels.add(pixel);
			}
			
			@Override
			public final void finishPatch() {
				sampler.finishPatch();
				
				final Sample sample = bkDatabase.findClosest(collector.getSample());
				
				if (1 != sample.getClasses().size()) {
					throw new IllegalArgumentException("Invalid group: " + sample.getClasses());
				}
				
				final String predicted = sample.getClasses().iterator().next();
				
				for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
					final String actual = entry.getKey();
					final RegionOfInterest groundTruth = classes.get(actual);
					final ExtendedConfusionTable actualRow = result.get(actual);
					
					if (actual.equals(predicted)) {
						// sample is positive
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									actualRow.incrementTruePositive(predicted, 1L);
								} else {
									actualRow.incrementFalsePositive(null, 1L);
								}
							}
							
						});
					} else {
						// sample is negative
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									actualRow.incrementFalseNegative(predicted, 1L);
								} else {
									actualRow.incrementTrueNegative(null, 1L);
								}
							}
							
						});
					}
				}
				
				this.pixels.clear();
			}
			
		});
		
		return result;
	}
	
	public static final void loadTrainingSet(final Configuration configuration,
			final String[] trainingSet, final PatchDatabase<Sample> sampleDatabase, final int maximumGroupSize) {
		final TicToc timer = new TicToc();
		
		for (final String trainingImageId : trainingSet) {
			final String filePath = baseName(trainingImageId) + configuration.getDatabaseSuffix();
			
			System.out.println("Retrieving samples from " + filePath + "... " + new Date(timer.tic()));
			
			final Map<Object, Object> database = SynchronizedWeakLoader.getObject(cache, filePath);
			
			System.out.println("Retrieving samples from " + filePath + " done, time: " + timer.toc());
			
			System.out.println("Including samples from " + filePath + "... " + new Date(timer.tic()));
			
			@SuppressWarnings("unchecked")
			final PatchDatabase<Sample> samples = (PatchDatabase<Sample>) database.get("samples");
			
			if (0 < maximumGroupSize) {
				synchronized (samples) {
					reduceArbitrarily(samples, maximumGroupSize);
				}
			}
			
			for (final Map.Entry<byte[], Sample> entry : samples) {
				final Sample sample = entry.getValue();
				final Sample newSample = sampleDatabase.add(entry.getKey());
				
				newSample.getClasses().addAll(sample.getClasses());
				newSample.incrementCount(sample.getCount());
			}
			
			System.out.println("Including samples from " + filePath + " done, time: " + timer.toc());
		}
		
		updateNegativeGroups(sampleDatabase);
		
		if (0 < maximumGroupSize) {
			reduceArbitrarily(sampleDatabase, maximumGroupSize);
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-06-04)
	 */
	public static final class ExtendedConfusionTable implements Serializable {
		
		private final Map<String, AtomicLong> counts = new HashMap<String, AtomicLong>();
		
		private long truePositive;
		
		private long falsePositive;
		
		private long trueNegative;
		
		private long falseNegative;
		
		public final Map<String, AtomicLong> getCounts() {
			return this.counts;
		}
		
		public final long getCount(final String key) {
			final AtomicLong count;
			
			synchronized (this.getCounts()) {
				count = this.getCounts().get(key);
			}
			
			return count == null ? 0L : count.get();
		}
		
		public final void count(final String key, final long delta) {
			if (key == null) {
				return;
			}
			
			final AtomicLong count;
			
			synchronized (this.getCounts()) {
				count = this.getCounts().get(key);
				
				if (count == null) {
					this.getCounts().put(key, new AtomicLong(delta));
					
					return;
				}
			}
			
			count.addAndGet(delta);
		}
		
		public final long getActualPositive() {
			return this.getTruePositive() + this.getFalseNegative();
		}
		
		public final long getActualNegative() {
			return this.getFalsePositive() + this.getTrueNegative();
		}
		
		public final long getPositive() {
			return this.getTruePositive() + this.getFalsePositive();
		}
		
		public final long getNegative() {
			return this.getTrueNegative() + this.getFalseNegative();
		}
		
		public final double getTruePositiveRate() {
			return (double) this.getTruePositive() / this.getActualPositive();
		}
		
		public final double getFalsePositiveRate() {
			return (double) this.getFalsePositive() / this.getActualNegative();
		}
		
		public final long getTruePositive() {
			return this.truePositive;
		}
		
		public final long getFalsePositive() {
			return this.falsePositive;
		}
		
		public final long getTrueNegative() {
			return this.trueNegative;
		}
		
		public final long getFalseNegative() {
			return this.falseNegative;
		}
		
		public final void incrementTruePositive(final String key, final long delta) {
			this.count(key, delta);
			this.truePositive += delta;
		}
		
		public final void incrementFalsePositive(final String key, final long delta) {
			this.count(key, delta);
			this.falsePositive += delta;
		}
		
		public final void incrementTrueNegative(final String key, final long delta) {
			this.count(key, delta);
			this.trueNegative += delta;
		}
		
		public final void incrementFalseNegative(final String key, final long delta) {
			this.count(key, delta);
			this.falseNegative += delta;
		}
		
		@Override
		public final String toString() {
			return "{TP: " + this.getTruePositive() + ", FP: " + this.getFalsePositive() +
					", TN: " + this.getTrueNegative() + ", FN: " + this.getFalseNegative() +
					", counts: " + this.getCounts() + "}";
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -2815956297294457592L;
		
	}
	
	/**
	 * @author codistmonk (creation 2013-06-10)
	 */
	public static final class SynchronizedWeakLoader {
		
		private final String filePath;
		
		private SoftReference<Object> reference;
		
		public SynchronizedWeakLoader(final String filePath) {
			this.filePath = filePath;
			this.setNewReference(null);
		}
		
		public final synchronized <T> T getObject() {
			T result = (T) this.reference.get();
			
			if (result == null) {
				result = readObject(this.filePath);
				this.setNewReference(result);
			}
			
			return result;
		}
		
		private final void setNewReference(final Object referent) {
			this.reference = new SoftReference<Object>(referent);
		}
		
		public static final <T> T getObject(final Map<String, SynchronizedWeakLoader> cache, final String filePath) {
			SynchronizedWeakLoader loader = null;
			
			synchronized (cache) {
				loader = cache.get(filePath);
				
				if (loader == null) {
					loader = new SynchronizedWeakLoader(filePath);
					cache.put(filePath, loader);
				}
			}
			
			return loader.getObject();
		}
		
	}
	
}
