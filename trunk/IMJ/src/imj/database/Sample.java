package imj.database;

import static net.sourceforge.aprog.tools.Tools.debugPrint;
import imj.apps.modules.RegionOfInterest;
import imj.database.BKSearch.Metric;
import imj.database.IMJDatabaseTools.ChessboardMetric;
import imj.database.IMJDatabaseTools.CityblockMetric;
import imj.database.IMJDatabaseTools.EuclideanMetric;
import imj.database.Sampler.SampleProcessor;
import imj.database.PatchDatabase.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;

import net.sourceforge.aprog.tools.Tools;

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
	
	public final void setKey(final byte[] key) {
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
	
	public static final void processTile(final Sampler sampler, final int tileRowIndex, final int tileColumnIndex,
			final int tileRowCount, final int tileColumnCount) {
		final int imageColumnCount = sampler.getImage().getColumnCount();
		final int nextTileRowIndex = tileRowIndex + tileRowCount;
		final int nextTileColumnIndex = tileColumnIndex + tileColumnCount;
		
		for (int y = tileRowIndex; y < nextTileRowIndex; ++y) {
			for (int x = tileColumnIndex; x < nextTileColumnIndex; ++x) {
				sampler.process(y * imageColumnCount + x);
			}
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-29)
	 */
	public static final class Collector implements SampleProcessor {
		
		private final Sample sample = new Sample();
		
		public final Sample getSample() {
			return this.sample;
		}
		
		@Override
		public final void process(final byte[] sample) {
			this.getSample().setKey(sample);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-26)
	 */
	public static final class ClassSetter implements SampleProcessor {
		
		private final int tileColumnCount;
		
		private final Map<String, RegionOfInterest> classes;
		
		private final PatchDatabase<Sample> database;
		
		private final int horizontalTileStride;
		
		private final int tileRowCount;
		
		private final int verticalTileStride;
		
		private final int imageColumnCount;
		
		private int tileRowIndex;
		
		private int tileColumnIndex;
		
		public ClassSetter(final int imageColumnCount, final int tileRowCount, final int tileColumnCount,
				final int verticalTileStride, final int horizontalTileStride,
				final Map<String, RegionOfInterest> classes, final PatchDatabase<Sample> database) {
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
			
			if (this.imageColumnCount < this.tileColumnIndex + this.tileColumnCount) {
				this.tileColumnIndex = 0;
				this.tileRowIndex += this.verticalTileStride;
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-04-30)
	 */
	public static final class SampleMetric implements Metric<Sample> {
		
		private final Metric<byte[]> keyMetric;
		
		public SampleMetric(final Metric<byte[]> keyMetric) {
			this.keyMetric = keyMetric;
		}
		
		@Override
		public final long getDistance(final Sample object0, final Sample object1) {
			return this.keyMetric.getDistance(object0.getKey(), object1.getKey());
		}
		
		public static final SampleMetric EUCLIDEAN = new SampleMetric(EuclideanMetric.INSTANCE);
		
		public static final SampleMetric CITYBLOCK = new SampleMetric(CityblockMetric.INSTANCE);
		
		public static final SampleMetric CHESSBOARD = new SampleMetric(ChessboardMetric.INSTANCE);
		
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
