package imj.database;

import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static imj.database.Sample.processTile;
import static imj.database.TileDatabaseTest2.checkDatabase;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.RegionOfInterest;
import imj.database.BKSearch.BKDatabase;
import imj.database.Sampler.SampleProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class TileDatabaseTest4 {
	
	@Test
	public final void test() {
		final String[] imageIds = {
				"../Libraries/images/45656.svs",
//				"../Libraries/images/45657.svs",
//				"../Libraries/images/45659.svs",
//				"../Libraries/images/45660.svs"
		};
		final int lod = 5;
		final int tileRowCount = 3;
		final int tileColumnCount = 3;
		final int verticalTileStride = tileRowCount;
		final int horizontalTileStride = verticalTileStride;
		final TileDatabase<Sample> tileDatabase = new TileDatabase<Sample>(Sample.class);
		
		for (final String imageId : imageIds) {
			debugPrint("imageId:", imageId);
			
			final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			
			debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
			
			gc();
			
			final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
			
			updateDatabase(imageId, lod, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
					LinearSampler.class, RGB, classes, tileDatabase);
			gc();
			checkDatabase(classes, tileDatabase);
			gc();
		}
		
		final BKDatabase<Sample> bkDatabase = newBKDatabase(tileDatabase);
		gc();
		
		debugPrint(bkDatabase.getValues().length);
		
		{
//			final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/16088.svs", lod);
			final Image image = ImageWrangler.INSTANCE.load(imageIds[0], lod);
			final Sample.Collector collector = new Sample.Collector();
			final Sampler sampler = new LinearSampler(image, RGB, tileRowCount * tileColumnCount, collector);
			
			final BufferedImage labels = new BufferedImage(image.getColumnCount(), image.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
			final Graphics2D g = labels.createGraphics();
			final int imageRowCount = image.getRowCount();
			final int imageColumnCount = image.getColumnCount();
			
			for (int y = 0; y < imageRowCount; y += verticalTileStride) {
				System.out.print(y + "/" + imageRowCount + " " + Tools.usedMemory() + "\r");
				
				for (int x = 0; x < imageColumnCount; x += horizontalTileStride) {
					if (y + tileRowCount <= imageRowCount && x + tileColumnCount <= imageColumnCount) {
						processTile(sampler, y, x, tileRowCount, tileColumnCount);
						final Sample sample = tileDatabase.get(collector.getSample().getKey());
//						final Sample sample = bkDatabase.findClosest(collector.getSample());
						
						g.setColor(new Color(generateColor(sample)));
						g.fillRect(x, y, tileColumnCount, tileRowCount);
					}
				}
			}
			
			g.dispose();
			
			SwingTools.show(labels, "Labels", true);
		}
	}
	
	public static final int generateColor(final Sample sample) {
		return sample == null || sample.getClasses().size() != 1 ? 0 : sample.getClasses().iterator().next().hashCode() | 0xFF000000;
	}
	
}
