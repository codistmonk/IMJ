package imj.database;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ShowActions.baseName;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ShowActions;
import imj.apps.modules.ViewFilter;
import imj.apps.modules.Annotations.Annotation;
import imj.apps.modules.ShowActions.UseAnnotationAsROI;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.Metric;
import imj.database.BKSearchTest.EuclideanMetric;
import imj.database.Sampler.SampleProcessor;
import imj.database.TileDatabase.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-04-25)
 */
public final class Sample implements Value {
	
	private byte[] key;
	
	private final Collection<String> classes;
	
	private int count;
	
	public Sample() {
		this.classes = new HashSet<String>();
		this.count = 1;
	}
	
	public final byte[] getKey() {
		return this.key;
	}
	
	public final void setSample(final byte[] key) {
		this.key = key;
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
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -2244166134966161936L;
	
	public static final void updateDatabase(final String imageId, final int lod,
			final int tileRowCount, final int tileColumnCount,
			final int verticalTileStride, final int horizontalTileStride,
			final Class<? extends Sampler> samplerFactory,
			final Map<String, RegionOfInterest> classes, final TileDatabase<Sample> database) {
		final TicToc timer = new TicToc();
		final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Channel[] channels = { RED, GREEN, BLUE };
		final Annotations annotations = Annotations.fromXML(baseName(imageId) + ".xml");
		
		loadRegions(lod, imageRowCount, imageColumnCount, annotations, classes);
		
		final SampleProcessor processor = new Collector(imageColumnCount, tileRowCount, tileColumnCount,
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
	
	/**
	 * @author codistmonk (creation 2013-04-26)
	 */
	public static final class Collector implements SampleProcessor {
		
		private final int tileColumnCount;
		
		private final Map<String, RegionOfInterest> classes;
		
		private final TileDatabase<Sample> database;
		
		private final int horizontalTileStride;
		
		private final int tileRowCount;
		
		private final int verticalTileStride;
		
		private final int imageColumnCount;
		
		private int tileRowIndex;
		
		private int tileColumnIndex;
		
		public Collector(final int imageColumnCount, final int tileRowCount, final int tileColumnCount,
				final int verticalTileStride, final int horizontalTileStride,
				final Map<String, RegionOfInterest> classes, final TileDatabase<Sample> database) {
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
			final Sample sample = this.database.add(key);
			
			sample.setSample(key);
			
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
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class EuclideanMetric implements Metric<Sample> {
		
		@Override
		public final long getDistance(final Sample sample0, final Sample sample1) {
			return BKSearchTest.EuclideanMetric.INSTANCE.getDistance(sample0.getKey(), sample1.getKey());
		}
		
		public static final EuclideanMetric INSTANCE = new EuclideanMetric();
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-28)
	 */
	public static final class KeyComparator implements Comparator<Sample> {
		
		@Override
		public final int compare(final Sample sample0, final Sample sample1) {
			return ByteArrayComparator.INSTANCE.compare(sample0.getKey(), sample1.getKey());
		}
		
		public static final KeyComparator INSTANCE = new KeyComparator();
		
	}
	
}
