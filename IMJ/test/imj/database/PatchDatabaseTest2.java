package imj.database;

import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static org.junit.Assert.assertEquals;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.RegionOfInterest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class PatchDatabaseTest2 {
	
	@Test
	public final void test() {
		final String imageId = "../Libraries/images/45656.svs";
		final int lod = 3;
		final PatchDatabase<Sample> database = new PatchDatabase<Sample>(Sample.class);
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
		final Quantizer quantizer = new BinningQuantizer();
		final Segmenter segmenter = new TileSegmenter(tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride);
		
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		debugPrint("verticalTileCount:", verticalTileCount, "horizontalTileCount:", horizontalTileCount);
		
		quantizer.initialize(image, null, RGB, 0);
		
		updateDatabase(imageId, lod, segmenter, LinearSampler.class, RGB, quantizer, classes, database);
		
		final int databaseSampleCount = checkDatabase(database).getDatabaseSampleCount();
		
		// k * verticalTileStride + tileRowCount <= imageRowCount
		// k * verticalTileStride <= imageRowCount - tileRowCount
		// k <= (imageRowCount - tileRowCount) / verticalTileStride
		// k = (imageRowCount - tileRowCount) / verticalTileStride
		assertEquals(((imageRowCount + verticalTileStride - tileRowCount) / verticalTileStride) *
				((imageColumnCount + horizontalTileStride - tileColumnCount) / horizontalTileStride), databaseSampleCount);
		
		debugPrint();
	}
	
}
