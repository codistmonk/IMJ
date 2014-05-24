package imj.database;

import static imj.IMJTools.loadAndTryToCache;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.BRIGHTNESS;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.HUE;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.SATURATION;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static java.util.Collections.shuffle;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.list;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import imj.Image;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Annotations.Annotation.Region;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions.ExportView;
import imj.apps.modules.ShowActions.UseAnnotationAsROI;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.BKDatabase;
import imj.database.BKSearch.Metric;
import imj.database.PatchDatabase.Value;
import imj.database.PatterngramSampler.PatterngramMetric;
import imj.database.Sample.SampleMetric;
import imj.database.Sampler.SampleProcessor;
import imj.database.SparseHistogramSampler.SparseHistogramMetric;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-04-29)
 */
public final class IMJDatabaseTools {
	
	private IMJDatabaseTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final Channel[] RGB = { RED, GREEN, BLUE };
	
	public static final Channel[] HSB = { HUE, SATURATION, BRIGHTNESS };
	
	/**
	 * {@value}.
	 */
	public static final String EDGES_ARTIFACTS_TO_BE_EXCLUDED = "Edges & Artifacts to be excluded";
	
	public static final void updateNegativeGroups(final PatchDatabase<Sample> patchDatabase) {
		for (final Map.Entry<byte[], Sample> entry : patchDatabase) {
			final Collection<String> group = entry.getValue().getClasses();
			
			if (1 < group.size() && group.contains(EDGES_ARTIFACTS_TO_BE_EXCLUDED)) {
				group.clear();
			}
			
			if (group.isEmpty()) {
				group.add(EDGES_ARTIFACTS_TO_BE_EXCLUDED);
			}
		}
	}
	
	public static final <K, V> void put(final Map<K, List<V>> map, final K key, final V value) {
		List<V> values = map.get(key);
		
		if (values == null) {
			values = new ArrayList<V>();
			map.put(key, values);
		}
		
		values.add(value);
	}
	
	public static final Map<Collection<String>, List<byte[]>> groupSamples(final PatchDatabase<Sample> tileDatabase) {
		final Map<Collection<String>, List<byte[]>> result = new HashMap<Collection<String>, List<byte[]>>();
		
		for (final Map.Entry<byte[], Sample> entry : tileDatabase) {
			put(result, entry.getValue().getClasses(), entry.getKey());
		}
		
		return result;
	}
	
	public static final void reduceArbitrarily(final PatchDatabase<Sample> tileDatabase, final int maximumSize) {
		if (maximumSize <= 0 || tileDatabase.getEntryCount() <= maximumSize) {
			return;
		}
		
		final TicToc timer = new TicToc();
		
		debugPrint("Reducing " + tileDatabase.getEntryCount() + " to " + maximumSize, new Date(timer.tic()));
		
		final List<Map.Entry<byte[], Sample>> entries = list(tileDatabase);
		final int n = min(maximumSize, entries.size());
		
		shuffle(entries, new Random(entries.size()));
		
		tileDatabase.clear();
		
		for (int i = 0; i < n; ++i) {
			final Entry<byte[], Sample> entry = entries.get(i);
			
			tileDatabase.put(entry.getKey(), entry.getValue());
		}
		
//		final Map<Collection<String>, List<byte[]>> groups = groupSamples(tileDatabase);
//		
//		debugPrint("groupCount:", groups.size());
//		
//		for (final Map.Entry<Collection<String>, List<byte[]>> entry : groups.entrySet()) {
//			final List<byte[]> samples = entry.getValue();
//			final int excess = samples.size() - maximumGroupSize;
//			
//			if (0 < excess) {
//				debugPrint("Removing", excess, "elements from group", entry.getKey());
//				
//				Collections.shuffle(samples);
//				
//				for (int i = 0; i < excess; ++i) {
//					tileDatabase.remove(samples.get(i));
//				}
//			}
//		}
		
		debugPrint("Simplifying done", "time:", timer.toc());
	}
	
	public static final Sampler newRGBSampler(final Class<? extends Sampler> samplerFactory,
			final Image image, final Quantizer quantizer, final SampleProcessor processor) {
		try {
			return samplerFactory.getConstructor(Image.class, Quantizer.class, Channel[].class, SampleProcessor.class)
					.newInstance(image, quantizer, RGB, processor);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final Metric<Sample> getPreferredMetric(final Sampler sampler) {
		if (sampler instanceof SparseHistogramSampler) {
			return new SampleMetric(new SparseHistogramMetric(sampler.getChannels().length));
		} else if (sampler instanceof PatterngramSampler) {
			return new SampleMetric(new PatterngramMetric(sampler.getChannels().length));
		}
		
		return SampleMetric.CHESSBOARD;
	}
	
	public static final void updateDatabase(final String imageId, final int lod,
			final Segmenter segmenter,
			final Class<? extends Sampler> samplerFactory, final Channel[] channels,
			final Quantizer quantizer,
			final Map<String, RegionOfInterest> classes, final PatchDatabase<Sample> database) {
		final TicToc timer = new TicToc();
		final Image image = loadAndTryToCache(imageId, lod);
		debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
		
		loadRegions(imageId, lod, imageRowCount, imageColumnCount, annotations, classes);
		
		final SampleProcessor processor = new Sample.ClassSetter(classes, database);
		final Sampler sampler;
		
		try {
			sampler = samplerFactory.getConstructor(Image.class, Quantizer.class, Channel[].class, SampleProcessor.class)
					.newInstance(image, quantizer, channels, processor);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		timer.tic();
		segmenter.process(image, sampler);
		gc();
		debugPrint("time:", timer.toc());
	}
	
	public static final void loadRegions(final String imageId, final int lod, final int imageRowCount,
			final int imageColumnCount, final Annotations annotations,
			final Map<String, RegionOfInterest> classes) {
		final ExecutorService executor = newFixedThreadPool(2);
		final TicToc timer = new TicToc();
		
		debugPrint("Loading regions...", new Date(timer.tic()));
		
		final Collection<Region> allRegions = UseAnnotationAsROI.collectAllRegions(annotations);
		
		try {
			final Collection<Future<?>> tasks = new ArrayList<Future<?>>(annotations.getAnnotations().size());
			
			for (final Annotation annotation : annotations.getAnnotations()) {
				tasks.add(executor.submit(new Runnable() {
					
					@Override
					public final void run() {
						final String annotationName = "" + annotation.getUserObject();
						
						debugPrint("Loading", annotationName);
						
						final File file = new File(baseName(imageId) + ".lod" + lod + "." + annotationName + ".jo");
						RegionOfInterest.UsingBitSet mask = null;
						
						if (file.isFile()) {
							mask = readMask(imageRowCount, imageColumnCount, file);
						} 
						
						if (mask == null) {
							mask = createMask(lod, imageRowCount, imageColumnCount, allRegions, annotation, file);
						}
						
						synchronized (classes) {
							classes.put(annotationName.toString(), mask);
						}
						
						gc();
						
//						writePNG(mask, annotation.getUserObject().toString());
					}
					
				}));
			}
			
			try {
				for (final Future<?> task : tasks) {
					task.get();
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		} finally {
			executor.shutdown();
		}
		
		debugPrint("Loading regions done", "time:", timer.toc());
	}

	public static RegionOfInterest.UsingBitSet readMask(
			final int imageRowCount, final int imageColumnCount,
			final File file) {
		debugPrint("Reading data from", file);
		
		try {
			final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			
			try {
				final RegionOfInterest.UsingBitSet result = new RegionOfInterest.UsingBitSet(imageRowCount, imageColumnCount, ois);
				
				debugPrint("OK");
				
				return result;
			} finally {
				ois.close();
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		
		return null;
	}

	public static RegionOfInterest.UsingBitSet createMask(final int lod,
			final int imageRowCount, final int imageColumnCount,
			final Collection<Region> allRegions, final Annotation annotation,
			final File file) {
		RegionOfInterest.UsingBitSet mask;
		debugPrint("Creating mask");
		
		mask = new RegionOfInterest.UsingBitSet(imageRowCount, imageColumnCount);
		UseAnnotationAsROI.set(mask, lod, annotation.getRegions(), allRegions);
		
		try {
			debugPrint("Writing data to", file);
			
			final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			
			try {
				mask.writeDataTo(oos);
				
				debugPrint("OK");
			} finally {
				oos.close();
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
		return mask;
	}
	
	public static final void writePNG(final Image image, final String baseName) {
		try {
			ImageIO.write(ExportView.toBufferedImage(image, null), "png", new File(baseName + ".png"));
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final Collection<Collection<String>> extractMonoclassGroups(final PatchDatabase<Sample> database) {
		final Collection<Collection<String>> result = new HashSet<Collection<String>>();
		
		for (final Map.Entry<byte[], Sample> entry : database) {
			if (entry.getValue().getClasses().size() == 1) {
				result.add(entry.getValue().getClasses());
			}
		}
		
		return result;
	}
	
	public static final BKDatabase<Sample> newBKDatabase(final PatchDatabase<Sample> tileDatabase,
			final Collection<Collection<String>> groups, final Metric<Sample> metric) {
		final int entryCount = tileDatabase.getEntryCount();
		final Sample[] samples = new Sample[entryCount];
		int i = 0;
		
		for (final Map.Entry<byte[], Sample> entry : tileDatabase) {
			if (groups.contains(entry.getValue().getClasses())) {
				samples[i++] = entry.getValue();
			}
		}
		
		return new BKDatabase<Sample>(copyOf(samples, i), metric, Sample.KeyComparator.INSTANCE);
	}
	
	public static final BKDatabase<Sample> newBKDatabase(final PatchDatabase<Sample> tileDatabase, Metric<Sample> metric) {
		final TicToc timer = new TicToc();
		
		debugPrint("Creating bk-database...");
		timer.tic();
		final BKDatabase<Sample> result = newBKDatabase(tileDatabase, extractMonoclassGroups(tileDatabase), metric);
		debugPrint("Creating bk-database done", "time:", timer.toc());
		
		return result;
	}
	
	public static final long square(final int x) {
		return (long) x * x;
	}
	
	public static final DBInfo checkDatabase(final PatchDatabase<?> database) {
		boolean junitAvailable = false;
		
		try {
			Class.forName("org.junit.Assert");
			junitAvailable = true;
		} catch (final Exception exception) {
			ignore(exception);
		}
		
		final TicToc timer = new TicToc();
		final DBInfo result = new DBInfo();
		
		debugPrint("Checking database...", new Date(timer.tic()));
		
		for (final Map.Entry<byte[], ? extends Value> entry : database) {
			if (result.getDatabaseEntryCount() % 100000 == 0) {
				System.out.print(result.getDatabaseEntryCount() + "/" + database.getEntryCount() + "\r");
			}
			
			if (junitAvailable) {
				assertNotNull(entry.getValue());
			}
			
			result.incrementDatabaseEntryCount(1);
			result.incrementDatabaseSampleCount(entry.getValue().getCount());
			
			final Sample tileData = (Sample) entry.getValue();
			
			count(result.getGroups(), tileData.getClasses());
			
			for (final String classId : tileData.getClasses()) {
				count(result.getClassCounts(), classId);
			}
		}
		
		debugPrint("Checking database done", "time:", timer.toc());
		
		debugPrint("classCounts", result.getClassCounts());
		debugPrint("groupCount:", result.getGroups().size());
		debugPrint("entryCount:", database.getEntryCount());
		debugPrint("sampleCount:", result.getDatabaseSampleCount());
		
		for (final Map.Entry<Collection<String>, AtomicInteger> entry : result.getGroups().entrySet()) {
			debugPrint("group:", entry);
		}
		
		if (junitAvailable) {
			assertEquals(database.getEntryCount(), result.getDatabaseEntryCount());
		}
		
		return result;
	}
	
	public static final <K> void count(final Map<K, AtomicInteger> map, final K key) {
		final AtomicInteger counter = map.get(key);
		
		if (counter == null) {
			map.put(key, new AtomicInteger(1));
		} else {
			counter.incrementAndGet();
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class EuclideanMetric implements Metric<byte[]> {
		
		@Override
		public final long getDistance(final byte[] sample0, final byte[] sample1) {
			long result = 0L;
			final int n0 = sample0.length;
			final int n1 = sample1.length;
			final int n = max(n0, n1);
			
			for (int i = 0; i < n; ++i) {
				final byte s0 = i < n0 ? sample0[i] : 0;
				final byte s1 = i < n1 ? sample1[i] : 0;
				result += square(s1 - s0);
			}
			
			return (long) ceil(sqrt(result));
		}
		
		public static final EuclideanMetric INSTANCE = new EuclideanMetric();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class CityblockMetric implements Metric<byte[]> {
		
		@Override
		public final long getDistance(final byte[] sample0, final byte[] sample1) {
			long result = 0L;
			final int n0 = sample0.length;
			final int n1 = sample1.length;
			final int n = max(n0, n1);
			
			for (int i = 0; i < n; ++i) {
				final byte s0 = i < n0 ? sample0[i] : 0;
				final byte s1 = i < n1 ? sample1[i] : 0;
				
				result += abs(s1 - s0);
			}
			
			return result;
		}
		
		public static final CityblockMetric INSTANCE = new CityblockMetric();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class ChessboardMetric implements Metric<byte[]> {
		
		@Override
		public final long getDistance(final byte[] sample0, final byte[] sample1) {
			long result = 0L;
			final int n0 = sample0.length;
			final int n1 = sample1.length;
			final int n = max(n0, n1);
			
			for (int i = 0; i < n; ++i) {
				final byte s0 = i < n0 ? sample0[i] : 0;
				final byte s1 = i < n1 ? sample1[i] : 0;
				final long d = abs(s1 - s0);
				
				if (result < d) {
					result = d;
				}
			}
			
			return result;
		}
		
		public static final CityblockMetric INSTANCE = new CityblockMetric();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-30)
	 * 
	 * @param <T>
	 */
	public static final class NoIdentityMetric<T> implements Metric<T> {
		
		private final Metric<T> metric;
		
		public NoIdentityMetric(final Metric<T> metric) {
			this.metric = metric;
		}
		
		@Override
		public final long getDistance(final T object0, final T object1) {
			final long result = this.metric.getDistance(object0, object1);
			
			return result == 0L ? Long.MAX_VALUE : result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-06-10)
	 */
	public static final class DBInfo implements Serializable {
		
		private int databaseEntryCount = 0;
		
		private int databaseSampleCount = 0;
		
		private final Map<String, AtomicInteger> classCounts = new HashMap<String, AtomicInteger>();
		
		private final Map<Collection<String>, AtomicInteger> groups = new HashMap<Collection<String>, AtomicInteger>();
		
		public final int getDatabaseEntryCount() {
			return this.databaseEntryCount;
		}
		
		public final void incrementDatabaseEntryCount(final int delta) {
			this.databaseEntryCount += delta;
		}
		
		public final int getDatabaseSampleCount() {
			return this.databaseSampleCount;
		}
		
		public final void incrementDatabaseSampleCount(final int delta) {
			this.databaseSampleCount += delta;
		}
		
		public final Map<String, AtomicInteger> getClassCounts() {
			return this.classCounts;
		}
		
		public final Map<Collection<String>, AtomicInteger> getGroups() {
			return this.groups;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -8503746046488694996L;
		
	}
	
}
