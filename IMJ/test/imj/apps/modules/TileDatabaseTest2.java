package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static org.junit.Assert.assertEquals;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.Sampler.SampleProcessor;
import imj.apps.modules.TileDatabase.Value;
import imj.apps.modules.ViewFilter.Channel;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest2 {
	
	@Test
	public final void test1() {
		final String imageId = "../Libraries/images/45660.svs";
		final int lod = 4;
		final TileDatabase database = new TileDatabase(TileData.class);
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		gc();
		final int tileRowCount = 3;
		final int tileColumnCount = tileRowCount;
		final int verticalTileStride = 2;
		final int horizontalTileStride = verticalTileStride;
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		debugPrint("verticalTileCount:", verticalTileCount, "horizontalTileCount:", horizontalTileCount);
		
		updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride, classes, database);
		
		final int databaseSampleCount = checkDatabase(classes, database);
		
		// k * verticalTileStride + tileRowCount <= imageRowCount
		// k * verticalTileStride <= imageRowCount - tileRowCount
		// k <= (imageRowCount - tileRowCount) / verticalTileStride
		// k = (imageRowCount - tileRowCount) / verticalTileStride
		assertEquals(((imageRowCount + verticalTileStride - tileRowCount) / verticalTileStride) *
				((imageColumnCount + horizontalTileStride - tileColumnCount) / horizontalTileStride), databaseSampleCount);
		
		debugPrint();
	}
	
	public static final int checkDatabase(final Map<String, RegionOfInterest> classes,
			final TileDatabase database) {
		final TicToc timer = new TicToc();
		
		debugPrint("Checking database...", new Date(timer.tic()));
		
		int databaseEntryCount = 0;
		int databaseSampleCount = 0;
		final Map<String, AtomicInteger> classCounts = new HashMap<String, AtomicInteger>();
		final Map<Collection<String>, AtomicInteger> groups = new HashMap<Collection<String>, AtomicInteger>();
		
		for (final String key : classes.keySet()) {
			classCounts.put(key, new AtomicInteger());
		}
		
		for (final Map.Entry<byte[], ? extends Value> entry : database) {
			if (databaseEntryCount % 100000 == 0) {
				System.out.print(databaseEntryCount + "/" + database.getEntryCount() + "\r");
			}
			assertNotNull(entry.getValue());
			++databaseEntryCount;
			databaseSampleCount += entry.getValue().getCount();
			
			final TileData tileData = (TileData) entry.getValue();
			
			count(groups, tileData.getClasses());
			
			for (final String classId : tileData.getClasses()) {
				classCounts.get(classId).incrementAndGet();
			}
		}
		
		debugPrint("Checking database done", "time:", timer.toc());
		
		debugPrint("classCounts", classCounts);
		debugPrint("groupCount:", groups.size());
		debugPrint("sampleCount:", databaseSampleCount);
		
		for (final Map.Entry<Collection<String>, AtomicInteger> entry : groups.entrySet()) {
			debugPrint(entry);
		}
		
		assertEquals(database.getEntryCount(), databaseEntryCount);
		
		return databaseSampleCount;
	}

	public static final void updateDatabase(final String imageId, final int lod,
			final int tileRowCount, final int tileColumnCount,
			final int verticalTileStride, final int horizontalTileStride,
			final Map<String, RegionOfInterest> classes, final TileDatabase database) {
		final TicToc timer = new TicToc();
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Channel[] channels = { RED, GREEN, BLUE };
		final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
		
		loadRegions(lod, imageRowCount, imageColumnCount, annotations, classes);
		
		final SampleProcessor processor = new Collector(imageColumnCount, tileRowCount, tileColumnCount,
				verticalTileStride, horizontalTileStride, classes, database);
		
		timer.tic();
		forEachPixelInEachTile(image, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				new CompactHistogramSampler(image, channels, tileRowCount * tileColumnCount, processor));
		gc();
		debugPrint("time:", timer.toc());
		
		debugPrint("entryCount:", database.getEntryCount());
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
	
	public static final <K> void count(final Map<K, AtomicInteger> map, final K key) {
		final AtomicInteger counter = map.get(key);
		
		if (counter == null) {
			map.put(key, new AtomicInteger(1));
		} else {
			counter.incrementAndGet();
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-04-26)
	 */
	public static final class Collector implements SampleProcessor {
		
		private final int tileColumnCount;
		
		private final Map<String, RegionOfInterest> classes;
		
		private final TileDatabase database;
		
		private final int horizontalTileStride;
		
		private final int tileRowCount;
		
		private final int verticalTileStride;
		
		private final int imageColumnCount;
		
		private int tileRowIndex;
		
		private int tileColumnIndex;
		
		public Collector(final int imageColumnCount, final int tileRowCount, final int tileColumnCount,
				final int verticalTileStride, final int horizontalTileStride,
				final Map<String, RegionOfInterest> classes, final TileDatabase database) {
			this.tileRowCount = tileRowCount;
			this.tileColumnCount = tileColumnCount;
			this.classes = classes;
			this.database = database;
			this.horizontalTileStride = horizontalTileStride;
			this.verticalTileStride = verticalTileStride;
			this.imageColumnCount = imageColumnCount;
		}
		
		@Override
		public final void process(final byte[] key) {
			final TileData sample = this.database.add(key);
			
			for (int rowIndex = this.tileRowIndex; rowIndex < this.tileRowIndex + this.tileRowCount; ++rowIndex) {
				for (int columnIndex = this.tileColumnIndex; columnIndex < this.tileColumnIndex + this.tileColumnCount; ++columnIndex) {
					for (final Map.Entry<String, RegionOfInterest> entry : this.classes.entrySet()) {
						if (entry.getValue().get(rowIndex, columnIndex)) {
							sample.getClasses().add(entry.getKey());
						}
					}
				}
			}
			
			this.tileColumnIndex += this.horizontalTileStride;
			
			if (this.imageColumnCount <= this.tileColumnIndex + this.tileColumnCount) {
				this.tileColumnIndex = 0;
				this.tileRowIndex += this.verticalTileStride;
			}
		}
	}

	/**
	 * @author codistmonk (creation 2013-04-25)
	 */
	public static final class TileData implements Value {
		
		private final Collection<String> classes;
		
		private int count;
		
		public TileData() {
			this.classes = new HashSet<String>();
			this.count = 1;
		}
		
		public final Collection<String> getClasses() {
			return this.classes;
		}
		
		@Override
		public final int getCount() {
			return this.count;
		}
		
		@Override
		public final void incrementCount() {
			++this.count;
		}
		
	}
	
}
