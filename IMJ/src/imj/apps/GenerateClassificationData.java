package imj.apps;

import static imj.IMJTools.loadAndTryToCache;
import static imj.apps.modules.ShowActions.baseName;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.loadRegions;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newRGBSampler;
import static imj.database.IMJDatabaseTools.simplifyArbitrarily;
import static imj.database.IMJDatabaseTools.updateNegativeGroups;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
		
		final Map<String, ConfusionTable[]> allConfusionTables = new HashMap<String, ConfusionTable[]>();
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
						
						loadTrainingSet(configuration, trainingSet, sampleDatabase);
						
						System.out.println("Loading training set done, time: " + timer.toc());
						
						if (0 < maximumGroupSize) {
							simplifyArbitrarily(sampleDatabase, maximumGroupSize);
						}
						
						checkDatabase(sampleDatabase);
						
						System.out.println("Processing image... " + new Date(timer.tic()));
						
						final Map<String, ConfusionTable> confusionTables = computeConfusionTables(
								configuration, testImageId, sampleDatabase);
						
						synchronizedUpdate(allConfusionTables, n, i, confusionTables);
						
						System.out.println("Processing image done, time: " + timer.toc());
						System.out.println("confusionTables: " + confusionTables);
					}
					
				}));
			}
			
			for (final Future<?> task : tasks) {
				task.get();
			}
		} finally {
			executor.shutdown();
		}
		
		for (final Map.Entry<String, ConfusionTable[]> entry : allConfusionTables.entrySet()) {
			final Statistics fpr = new Statistics();
			final Statistics tpr = new Statistics();
			
			for (final ConfusionTable table : entry.getValue()) {
				if (0L < table.getActualNegative() && 0L < table.getActualPositive()) {
					fpr.addValue(table.getFalsePositiveRate());
					tpr.addValue(table.getTruePositiveRate());
				}
			}
			
			System.out.println("class: " + entry.getKey() + ", FPR: " + format(fpr) + ", TPR: " + format(tpr));
		}
		
		{
			System.out.println("Saving confusion tables... " + new Date(timer.tic()));
			
			final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("confusion" + configuration.getSuffix()));
			
			try {
				oos.writeObject(allConfusionTables);
				
				System.out.println("Saving confusion tables done, time: " + timer.toc());
			} finally {
				oos.close();
			}
		}
	}
	
	public static final void synchronizedUpdate(
			final Map<String, ConfusionTable[]> allConfusionTables,
			final int testCount, final int testIndex, final Map<String, ConfusionTable> confusionTables) {
		synchronized (allConfusionTables) {
			for (final Map.Entry<String, ConfusionTable> entry : confusionTables.entrySet()) {
				ConfusionTable[] tables = allConfusionTables.get(entry.getKey());
				
				if (tables == null) {
					tables = new ConfusionTable[testCount];
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
	
	public static final Map<String, ConfusionTable> computeConfusionTables(
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
		final Map<String, ConfusionTable> result = new HashMap<String, ConfusionTable>();
		
		loadRegions(testImageId, configuration.getLod(), imageRowCount, imageColumnCount, annotations, classes);
		
		for (final String key : classes.keySet()) {
			result.put(key, new ConfusionTable());
		}
		
		configuration.getSegmenter().process(image, new Sampler(image, quantizer, channels, collector) {
			
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
				
				for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
					final String key = entry.getKey();
					final RegionOfInterest groundTruth = classes.get(key);
					final ConfusionTable confusionTable = result.get(key);
					
					if (sample.getClasses().contains(key)) {
						// sample is positive
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									confusionTable.incrementTruePositive(1L);
								} else {
									confusionTable.incrementFalsePositive(1L);
								}
							}
							
						});
					} else {
						// sample is negative
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									confusionTable.incrementFalseNegative(1L);
								} else {
									confusionTable.incrementTrueNegative(1L);
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
			final String[] trainingSet, final PatchDatabase<Sample> sampleDatabase) {
		try {
			for (final String trainingImageId : trainingSet) {
				final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
						baseName(trainingImageId) + configuration.getSuffix()));
				
				try {
					@SuppressWarnings("unchecked")
					final Map<Object, Object> database = (Map<Object, Object>) ois.readObject();
					@SuppressWarnings("unchecked")
					final PatchDatabase<Sample> samples = (PatchDatabase<Sample>) database.get("samples");
					
					for (final Map.Entry<byte[], Sample> entry : samples) {
						final Sample sample = entry.getValue();
						final Sample newSample = sampleDatabase.add(entry.getKey());
						
						newSample.getClasses().addAll(sample.getClasses());
						newSample.incrementCount(sample.getCount());
					}
				} finally {
					ois.close();
				}
			}
			
			updateNegativeGroups(sampleDatabase);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-06-04)
	 */
	public static final class ConfusionTable implements Serializable {
		
		private long truePositive;
		
		private long falsePositive;
		
		private long trueNegative;
		
		private long falseNegative;
		
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
		
		public final void incrementTruePositive(final long delta) {
			this.truePositive += delta;
		}
		
		public final void incrementFalsePositive(final long delta) {
			this.falsePositive += delta;
		}
		
		public final void incrementTrueNegative(final long delta) {
			this.trueNegative += delta;
		}
		
		public final void incrementFalseNegative(final long delta) {
			this.falseNegative += delta;
		}
		
		@Override
		public final String toString() {
			return "{TP: " + this.getTruePositive() + ", FP: " + this.getFalsePositive() +
					", TN: " + this.getTrueNegative() + ", FN: " + this.getFalseNegative() + "}";
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -2815956297294457592L;
		
	}
	
}
