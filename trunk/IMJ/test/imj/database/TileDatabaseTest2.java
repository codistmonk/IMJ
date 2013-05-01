package imj.database;

import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static org.junit.Assert.assertEquals;

import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.AdaptiveQuantizationViewFilter.AdaptiveQuantizer;
import imj.apps.modules.RegionOfInterest;
import imj.database.TileDatabase.Value;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest2 {
	
	@Test
	public final void test() {
		final String imageId = "../Libraries/images/45656.svs";
		final int lod = 2;
		final TileDatabase<Sample> database = new TileDatabase<Sample>(Sample.class);
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		gc();
		final int tileRowCount = 3;
		final int tileColumnCount = tileRowCount;
		final int verticalTileStride = tileRowCount;
		final int horizontalTileStride = verticalTileStride;
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		final AdaptiveQuantizer quantizer = new AdaptiveQuantizer();
		
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		debugPrint("verticalTileCount:", verticalTileCount, "horizontalTileCount:", horizontalTileCount);
		
		quantizer.initialize(image, null, RGB, 0);
		
		updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				LinearSampler.class, RGB, quantizer, classes, database);
		
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
			final TileDatabase<?> database) {
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
			
			final Sample tileData = (Sample) entry.getValue();
			
			count(groups, tileData.getClasses());
			
			for (final String classId : tileData.getClasses()) {
				classCounts.get(classId).incrementAndGet();
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
		
		assertEquals(database.getEntryCount(), databaseEntryCount);
		
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
	
}
