package imj.apps.modules;

import static imj.apps.modules.TileDatabaseTest2.checkDatabase;
import static imj.apps.modules.TileDatabaseTest2.updateDatabase;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.BKSearch.BKDatabase;
import imj.apps.modules.BKSearchTest.EuclideanMetric;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest4 {
	
	@Test
	public final void test() {
		final String[] imageIds = {
//				"../Libraries/images/45656.svs",
//				"../Libraries/images/45657.svs",
//				"../Libraries/images/45659.svs",
				"../Libraries/images/45660.svs" };
		final int lod = 4;
		final TileDatabase<Sample> tileDatabase = new TileDatabase<Sample>(Sample.class);
		
		for (final String imageId : imageIds) {
			debugPrint("imageId:", imageId);
			
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
			
			updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
					LinearSampler.class, classes, tileDatabase);
			gc();
			checkDatabase(classes, tileDatabase);
			gc();
			
			final BKDatabase<Sample> bkDatabase = newBKDatabase(tileDatabase);
			gc();
		}
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
			samples[i++] = entry.getValue();
		}
		
		return new BKDatabase<Sample>(samples, Sample.EuclideanMetric.INSTANCE, Sample.KeyComparator.INSTANCE);
	}
	
	public static final BKDatabase<Sample> newBKDatabase(final TileDatabase<Sample> tileDatabase) {
		final TicToc timer = new TicToc();
		
		debugPrint("Creating bk-database...");
		timer.tic();
		final BKDatabase<Sample> result = newBKDatabase(tileDatabase, extractMonoclassGroups(tileDatabase));
		debugPrint("Creating bk-database done", "time:", timer.toc());
		
		return result;
	}
	
}
