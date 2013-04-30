package imj.database;

import static imj.IMJTools.square;
import static imj.IMJTools.unsigned;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static imj.database.Sample.processTile;
import static imj.database.TileDatabaseTest2.checkDatabase;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.Image;
import imj.ImageWrangler;
import imj.apps.modules.RegionOfInterest;
import imj.database.BKSearch.BKDatabase;
import imj.database.IMJDatabaseTools.EuclideanMetric;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.SystemProperties;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.AfterClass;
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
		final int tileRowCount = 2;
		final int tileColumnCount = tileRowCount;
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
//					SparseHistogramSampler.class, RGB, classes, tileDatabase);
			gc();
			checkDatabase(classes, tileDatabase);
			gc();
		}
		
		{
			final Map<Collection<String>, List<byte[]>> groups = new HashMap<Collection<String>, List<byte[]>>();
			final TicToc timer = new TicToc();
			
			for (final Map.Entry<byte[], Sample> entry : tileDatabase) {
				put(groups, entry.getValue().getClasses(), entry.getKey());
			}
			
			debugPrint("groupCount:", groups.size());
			
			for (final Map.Entry<Collection<String>, List<byte[]>> entry : groups.entrySet()) {
				final List<byte[]> samples = entry.getValue();
				final int sampleCount = samples.size();
				
				debugPrint("group:", entry.getKey(), "sampleCount:", samples.size());
				
				if (sampleCount <= 1) {
					continue;
				}
				
				final Statistics statistics = new Statistics();
				timer.tic();
				
				if (true) {
					parallelForEachDifferentPair(sampleCount, new ActionIJ() {
						
						@Override
						public final void perform(final int i, final int j) {
							final byte[] sampleI = samples.get(i);
							final byte[] sampleJ = samples.get(j);
							
							statistics.addValue(EuclideanMetric.INSTANCE.getDistance(sampleI, sampleJ));
						}
						
					});
				} else {
					for (int i = 0; i < sampleCount; ++i) {
						final byte[] sampleI = samples.get(i);
						
						for (int j = i + 1; j < sampleCount; ++j) {
							final byte[] sampleJ = samples.get(j);
							
							statistics.addValue(EuclideanMetric.INSTANCE.getDistance(sampleI, sampleJ));
						}
					}
				}
				
				debugPrint("meanDistance:", statistics.getMean(),
						"minDistance:", statistics.getMinimum(), "maxDistance:", statistics.getMaximum(),
						"variance:", statistics.getVariance());
				debugPrint("time:", timer.toc());
			}
		}
		
		if (false) {
			final BKDatabase<Sample> bkDatabase = newBKDatabase(tileDatabase);
			gc();
			
			debugPrint(bkDatabase.getValues().length);
			
//			final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/16088.svs", lod);
			final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/45656.svs", lod);
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
//						final Sample sample = tileDatabase.get(collector.getSample().getKey());
						final Sample sample = bkDatabase.findClosest(collector.getSample());
						
						g.setColor(new Color(generateColor(sample)));
						g.fillRect(x, y, tileColumnCount, tileRowCount);
					}
				}
			}
			
			g.dispose();
			
			SwingTools.show(labels, "Labels", true);
		}
	}
	
	@AfterClass
	public static final void afterClass() {
		executor.shutdown();
	}
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(SystemProperties.getAvailableProcessorCount());
	
	public static final void parallelForEachDifferentPair(final int n, final ActionIJ action) {
		final Collection<Future<?>> tasks = new ArrayList<Future<?>>(n);
		
		for (int i0 = 0; i0 < n; ++i0) {
			final int i = i0;
			
			tasks.add(executor.submit(new Runnable() {
				
				@Override
				public final void run() {
					for (int j = i + 1; j < n; ++j) {
						action.perform(i, j);
					}
				}
				
			}));
		}
		
		try {
			for (final Future<?> task : tasks) {
					task.get();
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final <K, V> void put(final Map<K, List<V>> map, final K key, final V value) {
		List<V> values = map.get(key);
		
		if (values == null) {
			values = new ArrayList<V>();
			map.put(key, values);
		}
		
		values.add(value);
	}
	
	public static final int generateColor(final Sample sample) {
		return sample == null || sample.getClasses().size() != 1 ? 0 : sample.getClasses().iterator().next().hashCode() | 0xFF000000;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-30)
	 */
	public static final class ByteArrayStatistics {
		
		private int[] minima;
		
		private int[] maxima;
		
		private long[] sums;
		
		private long[] sumsOfSquares;
		
		private long count;
		
		public final void reset() {
			if (this.getSums() != null) {
				fill(this.getMinima(), 256);
				fill(this.getMaxima(), -256);
				fill(this.getSums(), 0L);
				fill(this.getSumsOfSquares(), 0L);
				this.count = 0L;
			}
		}
		
		public final void addSigned(final byte... bytes) {
			final int n = this.checkLength(bytes);
			
			for (int i = 0; i < n; ++i) {
				this.addValue(i, bytes[i]);
			}
			
			++this.count;
		}
		
		public final void addUnsigned(final byte... bytes) {
			final int n = this.checkLength(bytes);
			
			for (int i = 0; i < n; ++i) {
				final int b = unsigned(bytes[i]);
				
				this.addValue(i, b);
			}
			
			++this.count;
		}
		
		public final int[] getMinima() {
			return this.minima;
		}
		
		public final int[] getMaxima() {
			return this.maxima;
		}
		
		public final long[] getSums() {
			return this.sums;
		}
		
		public final long[] getSumsOfSquares() {
			return this.sumsOfSquares;
		}
		
		public final long getCount() {
			return this.count;
		}
		
		public final double[] getMean(final double[] result) {
			final int n = this.getSums().length;
			final double[] actualResult = result != null ? result : new double[n];
			
			for (int i = 0; i < n; ++i) {
				actualResult[i] = this.getSums()[i] / this.getCount();
			}
			
			return actualResult;
		}
		
		private final void addValue(final int i, final int b) {
			if (b < this.getMinima()[i]) {
				this.getMinima()[i] = b;
			}
			
			if (this.getMaxima()[i] < b) {
				this.getMaxima()[i] = b;
			}
			
			this.getSums()[i] += b;
			this.getSumsOfSquares()[i] += square(b);
		}
		
		private final int checkLength(final byte... bytes) {
			final int n = bytes.length;
			
			if (this.getSums() == null) {
				this.minima = new int[n];
				this.maxima = new int[n];
				this.sums = new long[n];
				this.sumsOfSquares = new long[n];
			} else if (n != this.getSums().length) {
				throw new IllegalArgumentException();
			}
			
			return n;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-30)
	 */
	public static abstract interface ActionIJ {
		
		public abstract void perform(int i, int j);
		
	}
	
}
