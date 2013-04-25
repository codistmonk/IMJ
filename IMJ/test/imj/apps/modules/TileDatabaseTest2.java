package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static junit.framework.Assert.assertNotNull;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
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
		final String imageId = "../Libraries/images/45657.svs";
		final int lod = 4;
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		final String annotationsId = baseName(imageId) + ".xml";
		final Annotations annotations = Annotations.fromXML(annotationsId);
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		final TicToc timer = new TicToc();
		
		debugPrint("imageRowCount:", imageRowCount, "imageColumnCount:", imageColumnCount);
		
		debugPrint("Loading regions...", new Date(timer.tic()));
		
		for (final Annotation annotation : annotations.getAnnotations()) {
			debugPrint("Loading", annotation.getUserObject());
			final RegionOfInterest mask = RegionOfInterest.newInstance(imageRowCount, imageColumnCount);
			ShowActions.UseAnnotationAsROI.set(mask, lod, annotation.getRegions());
			classes.put(annotation.getUserObject().toString(), mask);
		}
		
		debugPrint("Loading regions done", "time:", timer.toc());
		
		final int tileRowCount = 3;
		final int tileColumnCount = tileRowCount;
		final int verticalTileCount = imageRowCount / tileRowCount;
		final int horizontalTileCount = imageColumnCount / tileColumnCount;
		final int tilePixelCount = tileRowCount * tileColumnCount;
		final Channel[] channels = { RED, GREEN, BLUE };
		final TileDatabase database = new TileDatabase(TileData.class);
		final int verticalTileStride = tileRowCount;
		final int horizontalTileStride = verticalTileStride;
		
		debugPrint(verticalTileCount, horizontalTileCount);
		
		final SampleProcessor processor = new SampleProcessor() {
			
			private int tileRowIndex;
			
			private int tileColumnIndex;
			
			@Override
			public final void process(final byte[] key) {
				final TileData sample = database.add(key);
				
				for (int rowIndex = this.tileRowIndex; rowIndex < this.tileRowIndex + tileRowCount; ++rowIndex) {
					for (int columnIndex = this.tileColumnIndex; columnIndex < this.tileColumnIndex + tileColumnCount; ++columnIndex) {
						for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
							if (entry.getValue().get(rowIndex, columnIndex)) {
								sample.getClasses().add(entry.getKey());
							}
						}
					}
				}
				
				this.tileColumnIndex += horizontalTileStride;
				
				if (imageColumnCount <= this.tileColumnIndex + tileColumnCount) {
					this.tileColumnIndex = 0;
					this.tileRowIndex += verticalTileStride;
				}
			}
			
		};
		
		timer.tic();
		forEachPixelInEachTile(image, tileRowCount, tileColumnCount, verticalTileStride, horizontalTileStride,
				new CompactHistogramSampler(image, channels, tilePixelCount, processor));
		debugPrint("time:", timer.toc());
		
		debugPrint(database.getEntryCount());
		
		timer.tic();
		
		int dictionaryEntryCount = 0;
		int dictionarySampleCount = 0;
		final Map<String, AtomicInteger> classCounts = new HashMap<String, AtomicInteger>();
		final Map<Collection<String>, AtomicInteger> groups = new HashMap<Collection<String>, AtomicInteger>();
		
		for (final String key : classes.keySet()) {
			classCounts.put(key, new AtomicInteger());
		}
		
		for (final Map.Entry<byte[], ? extends Value> entry : database) {
			if (dictionaryEntryCount % 100000 == 0) {
				System.out.print(dictionaryEntryCount + "/" + database.getEntryCount() + "\r");
			}
			assertNotNull(entry.getValue());
			++dictionaryEntryCount;
			dictionarySampleCount += entry.getValue().getCount();
			
			final TileData tileData = (TileData) entry.getValue();
			
			count(groups, tileData.getClasses());
			
			for (final String classId : tileData.getClasses()) {
				classCounts.get(classId).incrementAndGet();
			}
		}
		
		debugPrint("time:", timer.toc());
		
		debugPrint(classCounts);
		debugPrint(groups.size(), groups);
		
		assertEquals(database.getEntryCount(), dictionaryEntryCount);
		// k * verticalTileStride + tileRowCount <= imageRowCount
		// k * verticalTileStride <= imageRowCount - tileRowCount
		// k <= (imageRowCount - tileRowCount) / verticalTileStride
		// k = (imageRowCount - tileRowCount) / verticalTileStride
		assertEquals(((imageRowCount + verticalTileStride - tileRowCount) / verticalTileStride) *
				((imageColumnCount + horizontalTileStride - tileColumnCount) / horizontalTileStride), dictionarySampleCount);
		
		debugPrint();
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
