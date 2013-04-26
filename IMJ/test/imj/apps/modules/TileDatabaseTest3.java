package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.TileDatabaseTest2.checkDatabase;
import static imj.apps.modules.TileDatabaseTest2.updateDatabase;
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
import imj.apps.modules.TileDatabaseTest2.TileData;
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
public class TileDatabaseTest3 {
	
	@Test
	public final void test1() {
		final String[] imageIds = {
				"../Libraries/images/45656.svs",
				"../Libraries/images/45657.svs",
				"../Libraries/images/45659.svs",
				"../Libraries/images/45660.svs" };
		final int lod = 4;
		final TileDatabase database = new TileDatabase(TileData.class);
		
		for (final String imageId : imageIds) {
			debugPrint("imageId:", imageId);
			
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
			
			debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
			debugPrint("verticalTileCount:", verticalTileCount, "horizontalTileCount:", horizontalTileCount);
			
			updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride, classes, database);
			gc();
			checkDatabase(classes, database);
			gc();
			
			debugPrint();
		}
	}
	
}
