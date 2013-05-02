package imj.database;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.assertEquals;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.PatchDatabase.Value;
import imj.database.Sampler.SampleProcessor;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public class PatchDatabaseTest1 {
	
	@Test
	public final void test() {
//		final Image image = ImageWrangler.INSTANCE.load("test/imj/12003.jpg");
//		final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/16088-4.png");
		final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/45657.svs", 4);
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		final int tileRowCount = 3;
		final int tileColumnCount = tileRowCount;
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final int tilePixelCount = tileRowCount * tileColumnCount;
		final Channel[] channels = { RED, GREEN, BLUE };
		final PatchDatabase<?> database = new PatchDatabase<Value>();
		final int verticalTileStride = tileRowCount;
		final int horizontalTileStride = verticalTileStride;
		
		debugPrint(verticalTileCount, horizontalTileCount);
		
		final SampleProcessor processor = new SampleProcessor() {
			
			@Override
			public final void process(final byte[] key) {
				database.add(key);
			}
			
		};
		
		final TicToc timer = new TicToc();
		timer.tic();
		forEachPixelInEachTile(image, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				new ColorSignatureSampler(image, null, channels, tilePixelCount, processor));
		debugPrint("time:", timer.toc());
		
		debugPrint(database.getEntryCount());
		
		timer.tic();
		
		final int[] dictionaryEntryCount = { 0 };
		final int[] dictionarySampleCount = { 0 };
		
		for (final Map.Entry<byte[], ? extends Value> entry : database) {
			if (dictionaryEntryCount[0] % 100000 == 0) {
				System.out.print(dictionaryEntryCount[0] + "/" + database.getEntryCount() + "\r");
			}
			assertNotNull(entry.getValue());
			++dictionaryEntryCount[0];
			dictionarySampleCount[0] += entry.getValue().getCount();
		}
		
		debugPrint("time:", timer.toc());
		
		assertEquals(database.getEntryCount(), dictionaryEntryCount[0]);
		// k * verticalTileStride + tileRowCount <= imageRowCount
		// k * verticalTileStride <= imageRowCount - tileRowCount
		// k <= (imageRowCount - tileRowCount) / verticalTileStride
		// k = (imageRowCount - tileRowCount) / verticalTileStride
		assertEquals(((imageRowCount + verticalTileStride - tileRowCount) / verticalTileStride) *
				((imageColumnCount + horizontalTileStride - tileColumnCount) / horizontalTileStride), dictionarySampleCount[0]);
		
		debugPrint();
	}
	
	public static final long countBytes(final Serializable object) {
		final long[] result = { 0L };
		
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(new OutputStream() {
				
				@Override
				public final void write(final int b) throws IOException {
					++result[0];
				}
				
			});
			
			oos.writeObject(object);
			oos.flush();
			oos.close();
		} catch (final IOException exception) {
			throw unchecked(exception);
		}
		
		return result[0];
	}
	
}
