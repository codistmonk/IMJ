package imj.database;

import static imj.IMJTools.loadAndTryToCache;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.BRIGHTNESS;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.HUE;
import static imj.apps.modules.ViewFilter.Channel.Synthetic.SATURATION;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.ignore;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.assertEquals;
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
import imj.database.Sampler.SampleProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
		final TicToc timer = new TicToc();
		
		debugPrint("Loading regions...", new Date(timer.tic()));
		
		final Collection<Region> allRegions = UseAnnotationAsROI.collectAllRegions(annotations);
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			final String annotationName = "" + annotation.getUserObject();
			
			debugPrint("Loading", annotationName);
			
			final File file = new File(baseName(imageId) + ".lod" + lod + "." + annotationName + ".jo");
			RegionOfInterest.UsingBitSet mask = null;
			
			if (file.isFile()) {
				debugPrint("Reading data from", file);
				
				try {
					final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
					
					try {
						mask = new RegionOfInterest.UsingBitSet(imageRowCount, imageColumnCount, ois);
						
						debugPrint("OK");
					} finally {
						ois.close();
					}
				} catch (final Exception exception) {
					exception.printStackTrace();
				}
			} 
			
			if (mask == null) {
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
			}
			
			classes.put(annotationName.toString(), mask);
			gc();
			
//			writePNG(mask, annotation.getUserObject().toString());
		}
		
		debugPrint("Loading regions done", "time:", timer.toc());
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
		
		debugPrint(samples.length, i);
		
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
	
	public static final int checkDatabase(final PatchDatabase<?> database) {
		boolean junitAvailable = false;
		
		try {
			Class.forName("org.junit.Assert");
			junitAvailable = true;
		} catch (final Exception exception) {
			ignore(exception);
		}
		
		final TicToc timer = new TicToc();
		
		debugPrint("Checking database...", new Date(timer.tic()));
		
		int databaseEntryCount = 0;
		int databaseSampleCount = 0;
		final Map<String, AtomicInteger> classCounts = new HashMap<String, AtomicInteger>();
		final Map<Collection<String>, AtomicInteger> groups = new HashMap<Collection<String>, AtomicInteger>();
		
		for (final Map.Entry<byte[], ? extends Value> entry : database) {
			if (databaseEntryCount % 100000 == 0) {
				System.out.print(databaseEntryCount + "/" + database.getEntryCount() + "\r");
			}
			
			if (junitAvailable) {
				assertNotNull(entry.getValue());
			}
			
			++databaseEntryCount;
			databaseSampleCount += entry.getValue().getCount();
			
			final Sample tileData = (Sample) entry.getValue();
			
			count(groups, tileData.getClasses());
			
			for (final String classId : tileData.getClasses()) {
				count(classCounts, classId);
			}
		}
		
		debugPrint("Checking database done", "time:", timer.toc());
		
		debugPrint("classCounts", classCounts);
		debugPrint("groupCount:", groups.size());
		debugPrint("entryCount:", database.getEntryCount());
		debugPrint("sampleCount:", databaseSampleCount);
		
		for (final Map.Entry<Collection<String>, AtomicInteger> entry : groups.entrySet()) {
			debugPrint(entry);
		}
		
		if (junitAvailable) {
			assertEquals(database.getEntryCount(), databaseEntryCount);
		}
		
		return databaseSampleCount;
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
	
}
