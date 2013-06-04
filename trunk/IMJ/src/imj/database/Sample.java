package imj.database;

import imj.ByteList;
import imj.apps.modules.RegionOfInterest;
import imj.database.BKSearch.Metric;
import imj.database.IMJDatabaseTools.ChessboardMetric;
import imj.database.IMJDatabaseTools.CityblockMetric;
import imj.database.IMJDatabaseTools.EuclideanMetric;
import imj.database.PatchDatabase.Value;
import imj.database.Sampler.SampleProcessor;

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
	public final void incrementCount(final int increment) {
		this.count += increment;
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
		
		sampler.finishPatch();
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
		public final void processPixel(final int pixel, final int pixelValue) {
			// NOP
		}
		
		@Override
		public final void processSample(final ByteList sample) {
			this.getSample().setKey(sample.toArray());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-26)
	 */
	public static final class ClassSetter implements SampleProcessor {
		
		private final Map<String, RegionOfInterest> classes;
		
		private final PatchDatabase<Sample> database;
		
		private final Collection<String> group;
		
		public ClassSetter(final Map<String, RegionOfInterest> classes, final PatchDatabase<Sample> database) {
			this.classes = classes;
			this.database = database;
			this.group = new HashSet<String>();
		}
		
		@Override
		public final void processPixel(final int pixel, final int pixelValue) {
			for (final Map.Entry<String, RegionOfInterest> entry : this.classes.entrySet()) {
				if (entry.getValue().get(pixel)) {
					this.group.add(entry.getKey());
				}
			}
		}
		
		@Override
		public final void processSample(final ByteList key) {
			this.database.add(key.toArray()).getClasses().addAll(this.group);
			this.group.clear();
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
