package imj.apps.modules;

import static imj.IMJTools.argb;
import imj.IMJTools.StatisticsSelector;
import imj.Image;
import imj.IntList;
import imj.apps.modules.ViewFilter.Channel;
import net.sourceforge.aprog.tools.MathTools.Statistics;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class FilteredImage extends Image.Abstract {
	
	private final Image source;
	
	private ViewFilter filter;
	
	public FilteredImage(final Image source) {
		super(source.getRowCount(), source.getColumnCount(), source.getChannelCount());
		this.source = source;
	}
	
	public final Image getSource() {
		return this.source;
	}
	
	public final ViewFilter getFilter() {
		return this.filter;
	}
	
	public final void setFilter(final ViewFilter filter) {
		this.filter = filter;
	}
	
	@Override
	public final int getValue(final int index) {
		if (this.getFilter() == null) {
			final int value = this.getSource().getValue(index);
			
			return 1 < this.getChannelCount() ? value : argb(255, value, value, value);
		}
		
		return this.getFilter().getNewValue(index, this.getSource().getValue(index));
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final float getFloatValue(final int index) {
		return this.getValue(index);
	}
	
	@Override
	public final float setFloatValue(final int index, float value) {
		return this.setValue(index, (int) value);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract interface Filter {
		
		public abstract int getNewValue(int index, int oldValue);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract interface ChannelFilter {
		
		public abstract int getNewValue(int index, int oldValue, Channel channel);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static abstract class StructuringElementFilter implements ChannelFilter {
		
		private final int[] deltas;
		
		private Image image;
		
		private int size;
		
		protected StructuringElementFilter(final int[] deltas) {
			if ((deltas.length & 1) != 0) {
				throw new IllegalArgumentException("deltas must have an odd number of elements");
			}
			
			this.deltas = deltas;
		}
		
		public final Image getImage() {
			return this.image;
		}
		
		public final int getSize() {
			return this.size;
		}
		
		public final int getMaximumSize() {
			return this.deltas.length / 2;
		}
		
		public final void setImage(final Image image) {
			this.image = image;
		}
		
		@Override
		public final int getNewValue(final int index, final int oldValue, final Channel channel) {
			final int oldChannelValue = channel.getValue(oldValue);
			
			this.reset(index, oldChannelValue);
			
			final int rowCount = this.getImage().getRowCount();
			final int columnCount = this.getImage().getColumnCount();
			final int rowIndex = index / columnCount;
			final int columnIndex = index % columnCount;
			final int n = this.deltas.length;
			this.size = 0;
			
			for (int i = 0; i < n; i += 2) {
				final int y = rowIndex + this.deltas[i + 0];
				
				if (0 <= y && y < rowCount) {
					final int x = columnIndex + this.deltas[i + 1];
					
					if (0 <= x && x < columnCount) {
						++this.size;
						final int neighborIndex = y * columnCount + x;
						final int neighborChannelValue = channel.getValue(this.getImage().getValue(neighborIndex));
						this.processNeighbor(index, oldChannelValue, neighborIndex, neighborChannelValue);
					} else {
						this.neighborIgnored();
					}
				} else {
					this.neighborIgnored();
				}
			}
			
			return this.getResult(index, oldChannelValue);
		}
		
		protected abstract void reset(int index, int oldChannelValue);
		
		protected void neighborIgnored() {
			// NOP
		}
		
		protected abstract void processNeighbor(int index, int oldChannelValue, int neighborIndex, int neighborChannelValue);
		
		protected abstract int getResult(int index, int oldChannelValue);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class RankFilter extends StructuringElementFilter {
		
		private final int rank;
		
		private final IntList values;
		
		public RankFilter(final int[] deltas, final int rank) {
			super(deltas);
			this.rank = rank;
			this.values = new IntList(this.getMaximumSize());
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			this.values.clear();
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			this.values.add(neighborValue);
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			final int n = this.values.size();
			
			this.values.sort();
			
			return this.values.get((this.rank + n) % n);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class StatisticsFilter extends StructuringElementFilter {
		
		private final StatisticsSelector feature;
		
		private final Statistics statistics;
		
		public StatisticsFilter(final int[] deltas, final StatisticsSelector feature) {
			super(deltas);
			this.feature = feature;
			this.statistics = new Statistics();
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			this.statistics.reset();
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			this.statistics.addValue(neighborValue);
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			return (int) this.feature.getValue(this.statistics);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class LinearFilter extends StructuringElementFilter {
		
		private final double[] coefficients;
		
		private double result;
		
		private int i;
		
		public LinearFilter(final int[] deltas, final double[] coefficients) {
			super(deltas);
			this.coefficients = coefficients;
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			this.result = 0.0;
			this.i = 0;
		}
		
		@Override
		protected final void neighborIgnored() {
			++this.i;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			this.result += this.coefficients[this.i++] * neighborValue;
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			return (int) this.result;
		}
		
	}
	
}
