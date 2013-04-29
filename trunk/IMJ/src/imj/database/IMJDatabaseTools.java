package imj.database;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static java.util.Arrays.copyOf;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.BKDatabase;
import imj.database.BKSearch.Metric;
import imj.database.Sampler.SampleProcessor;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

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
	
	public static final void updateDatabase(final String imageId, final int lod,
			final int tileRowCount, final int tileColumnCount,
			final int verticalTileStride, final int horizontalTileStride,
			final Class<? extends Sampler> samplerFactory, final Channel[] channels,
			final Map<String, RegionOfInterest> classes, final TileDatabase<Sample> database) {
		final TicToc timer = new TicToc();
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
		
		loadRegions(lod, imageRowCount, imageColumnCount, annotations, classes);
		
		final SampleProcessor processor = new Sample.ClassSetter(imageColumnCount, tileRowCount, tileColumnCount,
				verticalTileStride, horizontalTileStride, classes, database);
		final Sampler sampler;
		
		try {
			sampler = samplerFactory.getConstructor(Image.class, Channel[].class, int.class, SampleProcessor.class)
					.newInstance(image, channels, tileRowCount * tileColumnCount, processor);
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		timer.tic();
		forEachPixelInEachTile(image, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride, sampler);
		gc();
		debugPrint("time:", timer.toc());
	}
	
	public static final void loadRegions(final int lod, final int imageRowCount,
			final int imageColumnCount, final Annotations annotations,
			final Map<String, RegionOfInterest> classes) {
		final TicToc timer = new TicToc();
		
		debugPrint("Loading regions...", new Date(timer.tic()));
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			debugPrint("Loading", annotation.getUserObject());
			final RegionOfInterest mask = RegionOfInterest.newInstance(imageRowCount, imageColumnCount);
			ShowActions.UseAnnotationAsROI.set(mask, lod, annotation.getRegions());
			classes.put(annotation.getUserObject().toString(), mask);
			gc();
		}
		
		debugPrint("Loading regions done", "time:", timer.toc());
	}
	
	public static final Collection<Collection<String>> extractMonoclassGroups(final TileDatabase<Sample> database) {
		final Collection<Collection<String>> result = new HashSet<Collection<String>>();
		
		for (final Map.Entry<byte[], Sample> entry : database) {
			if (entry.getValue().getClasses().size() == 1) {
				result.add(entry.getValue().getClasses());
			}
		}
		
		return result;
	}
	
	public static final BKDatabase<Sample> newBKDatabase(final TileDatabase<Sample> tileDatabase,
			final Collection<Collection<String>> groups) {
		final int entryCount = tileDatabase.getEntryCount();
		final Sample[] samples = new Sample[entryCount];
		int i = 0;
		
		for (final Map.Entry<byte[], Sample> entry : tileDatabase) {
			if (groups.contains(entry.getValue().getClasses())) {
				samples[i++] = entry.getValue();
			}
		}
		
		return new BKDatabase<Sample>(copyOf(samples, i), Sample.EuclideanMetric.INSTANCE, Sample.KeyComparator.INSTANCE);
	}
	
	public static final BKDatabase<Sample> newBKDatabase(final TileDatabase<Sample> tileDatabase) {
		final TicToc timer = new TicToc();
		
		debugPrint("Creating bk-database...");
		timer.tic();
		final BKDatabase<Sample> result = newBKDatabase(tileDatabase, extractMonoclassGroups(tileDatabase));
		debugPrint("Creating bk-database done", "time:", timer.toc());
		
		return result;
	}
	
	public static final long square(final int x) {
		return (long) x * x;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class EuclideanMetric implements Metric<byte[]> {
		
		@Override
		public final long getDistance(final byte[] sample0, final byte[] sample1) {
			long result = 0L;
			final int n = sample0.length;
			
			for (int i = 0; i < n; ++i) {
				result += square(sample1[i] - sample0[i]);
			}
			
			return (long) ceil(sqrt(result));
		}
		
		public static final EuclideanMetric INSTANCE = new EuclideanMetric();
		
	}
	
}
