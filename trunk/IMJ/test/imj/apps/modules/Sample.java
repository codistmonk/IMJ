package imj.apps.modules;

import imj.apps.modules.BKSearch.Metric;
import imj.apps.modules.TileDatabase.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

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
