package imj.apps.modules;

import static imj.apps.modules.TileDatabaseTest2.checkDatabase;
import static imj.apps.modules.TileDatabaseTest2.updateDatabase;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.TileDatabaseTest2.TileData;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest3 {
	
	@Test
	public final void test() {
		final String[] imageIds = {
				"../Libraries/images/45656.svs",
				"../Libraries/images/45657.svs",
				"../Libraries/images/45659.svs",
				"../Libraries/images/45660.svs" };
		final int lod = 4;
		final TileDatabase<TileData> database = new TileDatabase<TileData>(TileData.class);
		
		for (final String imageId : imageIds) {
			debugPrint("imageId:", imageId);
			
			final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			
			gc();
			
			final int tileRowCount = 3;
			final int tileColumnCount = tileRowCount;
			final int verticalTileStride = 1;
			final int horizontalTileStride = verticalTileStride;
			final int imageRowCount = image.getRowCount();
			final int imageColumnCount = image.getColumnCount();
			final int verticalTileCount = imageRowCount / tileRowCount;
			final int horizontalTileCount = imageColumnCount / tileColumnCount;
			final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
			
			debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
			debugPrint("verticalTileCount:", verticalTileCount, "horizontalTileCount:", horizontalTileCount);
			
			updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
					LinearSampler.class, classes, database);
			gc();
			checkDatabase(classes, database);
			gc();
			
			debugPrint();
		}
	}
	
}
