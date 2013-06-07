package imj.database;

import static imj.IMJTools.square;
import static imj.IMJTools.unsigned;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.groupSamples;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newRGBSampler;
import static imj.database.IMJDatabaseTools.simplifyArbitrarily;
import static imj.database.IMJDatabaseTools.updateDatabase;
import static imj.database.IMJDatabaseTools.updateNegativeGroups;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj.Image;
import imj.ImageWrangler;
import imj.IntList;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.BKDatabase;
import imj.database.BKSearch.Metric;
import imj.database.IMJDatabaseTools.EuclideanMetric;
import imj.database.IMJDatabaseTools.NoIdentityMetric;
import imj.database.Sample.SampleMetric;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-19)
 */
public final class PatchDatabaseTest4 {
	
	@Test
	public final void test() {
		final String[] imageIds = {
				"../Libraries/images/45656.svs",
				"../Libraries/images/45657.svs",
				"../Libraries/images/45659.svs",
				"../Libraries/images/45660.svs",
				"../Libraries/images/45662.svs",
				"../Libraries/images/45668.svs",
				"../Libraries/images/45683.svs"
		};
		final int nonTrainingIndex = 3;
		final Quantizer[] quantizers = new Quantizer[imageIds.length];
		final int quantizationLevel = 2;
		final int lod = 4;
		final int tileRowCount = 32;
		final int tileColumnCount = tileRowCount;
		
		final int trainingVerticalTileStride = tileRowCount;
		final int trainingHorizontalTileStride = trainingVerticalTileStride;
		final int testVerticalTileStride = tileRowCount;
		final int testHorizontalTileStride = testVerticalTileStride;
		final PatchDatabase<Sample> patchDatabase = new PatchDatabase<Sample>(Sample.class);
		final Class<? extends Sampler> samplerFactory = SparseHistogramSampler.class;
//		final Class<? extends Sampler> samplerFactory = PatterngramSampler.class;
//		final Class<? extends Sampler> samplerFactory = ColorSignatureSampler.class;
//		final Class<? extends Sampler> samplerFactory = StatisticsSampler.class;
//		final Class<? extends Sampler> samplerFactory = LinearSampler.class;
		final Channel[] channels = RGB;
//		final Segmenter trainingSegmenter = new TileSegmenter(tileRowCount, tileColumnCount,
//				trainingVerticalTileStride, trainingHorizontalTileStride);
//		final Segmenter testSegmenter = new TileSegmenter(tileRowCount, tileColumnCount,
//				testVerticalTileStride, testHorizontalTileStride);
		final Segmenter trainingSegmenter = new SeamGridSegmenter(tileRowCount);
		final Segmenter testSegmenter = trainingSegmenter;
		
		for (int i = 0; i < imageIds.length; ++i) {
			if (nonTrainingIndex == i) {
				continue;
			}
			
			final String imageId = imageIds[i];
			
			debugPrint("imageId:", imageId);
			
			final Image image = ImageWrangler.INSTANCE.load(imageId, lod);
			gc();
			
			debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
			
			quantizers[i] = new BinningQuantizer();
			quantizers[i].initialize(image, null, channels, quantizationLevel);
			
			final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
			
			updateDatabase(imageId, lod, trainingSegmenter, samplerFactory, channels, quantizers[i], classes, patchDatabase);
			gc();
			
			checkDatabase(patchDatabase);
			gc();
		}
		
		if (true) {
			updateNegativeGroups(patchDatabase);
		}
		
		if (true) {
			simplifyArbitrarily(patchDatabase, 10000);
		}
		
		checkDatabase(patchDatabase);
		
		if (false) {
			printIntragroupDistanceStatistics(patchDatabase);
		}
		
		if (true) {
//			final Image image = ImageWrangler.INSTANCE.load("../Libraries/images/40267.svs", lod);
			final Image image = ImageWrangler.INSTANCE.load(imageIds[nonTrainingIndex], lod);
			final Quantizer quantizer = new BinningQuantizer();
			
			quantizer.initialize(image, null, channels, quantizationLevel);
			
			final Sample.Collector collector = new Sample.Collector();
			final Sampler sampler = newRGBSampler(samplerFactory, image, quantizer, collector);
			final BKDatabase<Sample> bkDatabase = newBKDatabase(patchDatabase, getPreferredMetric(sampler));
			
			gc();
			
			debugPrint("bkDatabaseEntryCount:", bkDatabase.getValues().length);
			
			final BufferedImage labels = new BufferedImage(image.getColumnCount(), image.getRowCount(), BufferedImage.TYPE_3BYTE_BGR);
			final int imageColumnCount = image.getColumnCount();
			final Map<Collection<String>, Color> colors = new HashMap<Collection<String>, Color>();
			final int[] patchCount = { 0 };
			
			testSegmenter.process(image, new Sampler(image, quantizer, channels, collector) {
				
				private final IntList pixels = new IntList();
				
				@Override
				public final void process(final int pixel) {
					sampler.process(pixel);
					
					this.pixels.add(pixel);
				}
				
				@Override
				public final void finishPatch() {
					++patchCount[0];
					
					sampler.finishPatch();
					
					final Sample sample = bkDatabase.findClosest(collector.getSample());
					final Color color = new Color(generateColor(sample));
					
					this.pixels.forEach(new IntList.Processor() {
						
						@Override
						public final void process(final int pixel) {
							final int x = pixel % imageColumnCount;
							final int y = pixel / imageColumnCount;
							
							labels.setRGB(x, y, ((x | y) & 1) == 0 ? color.getRGB() : image.getValue(pixel));
						}
						
					});
					
					this.pixels.clear();
					
					colors.put(sample != null ? sample.getClasses() : Collections.EMPTY_SET, color);
				}
				
			});
			
			debugPrint("patchCount:", patchCount[0]);
			
			for (final Map.Entry<Collection<String>, Color> entry : colors.entrySet()) {
				debugPrint(entry);
			}
			
			checkDatabase(patchDatabase);
			gc();
			
			SwingTools.show(labels, "Labels", true);
		}
	}
	
	public static final void printIntragroupDistanceStatistics(final PatchDatabase<Sample> tileDatabase) {
		final TicToc timer = new TicToc();
		final Map<Collection<String>, List<byte[]>> groups = groupSamples(tileDatabase);
		
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
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(SystemProperties.getAvailableProcessorCount());
	
	@AfterClass
	public static final void afterClass() {
		executor.shutdown();
	}
	
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
	
	public static final void diffuseClasses(final PatchDatabase<Sample> database, final int iterationCount) {
		final TicToc timer = new TicToc();
		
		debugPrint(new Date(timer.tic()));
		
		final Metric<Sample> noIdMetric = new NoIdentityMetric<Sample>(SampleMetric.EUCLIDEAN);
		final BKDatabase<Sample> bkDatabase = new BKDatabase<Sample>(collectAllSamples(database), noIdMetric, Sample.KeyComparator.INSTANCE);
		
		for (int i = 0; i < iterationCount; ++i) {
			int progress = 0;
			
			for (final Map.Entry<byte[], Sample> entry : database) {
				if (((progress++) % 100) == 0) {
					System.out.print(progress + "/" + database.getEntryCount() + "\r");
				}
				
				final Sample sample = entry.getValue();
				final Sample closest = bkDatabase.findClosest(sample);
				
				assert sample != closest;
				
				sample.getClasses().addAll(closest.getClasses());
				closest.getClasses().addAll(sample.getClasses());
			}
		}
		
		debugPrint("time:", timer.toc());
	}
	
	public static final Sample[] collectAllSamples(final PatchDatabase<Sample> database) {
		final Sample[] result = new Sample[database.getEntryCount()];
		int i = 0;
		
		for (final Map.Entry<?, Sample> entry : database) {
			result[i++] = entry.getValue();
		}
		
		return result;
	}
	
	public static final int generateColor(final Sample sample) {
//		return sample == null || sample.getClasses().size() != 1 ? 0 : sample.getClasses().iterator().next().hashCode() | 0xFF000000;
		return sample == null ? 0 : sample.getClasses().hashCode() | 0xFF000000;
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
