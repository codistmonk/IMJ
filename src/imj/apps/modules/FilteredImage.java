package imj.apps.modules;

import static imj.IMJTools.argb;
import static imj.Image.Abstract.index;
import static java.util.Arrays.fill;
import imj.Image;
import imj.ImageOfInts;
import imj.apps.modules.ViewFilter.Channel;

import java.awt.Point;

import jgencode.primitivelists.IntList;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class FilteredImage implements Image {
	
	private Image source;
	
	private Filter filter;
	
	private Image cache;
	
	private RegionOfInterest cacheValidity;
	
	private Point cacheLocation;
	
	private long timestamp;
	
	public FilteredImage(final Image source) {
		this.source = source;
		
		this.setCacheDimensions(DEFAULT_CACHE_ROW_COUNT, DEFAULT_CACHE_COLUMN_COUNT);
	}
	
	public final Image getSource() {
		return this.source;
	}
	
	public final void setSource(final Image source) {
		final Image oldSource = this.getSource();
		
		if (oldSource != source) {
			this.source = source;
			
			this.invalidateCache();
		} else if (source instanceof FilteredImage && this.timestamp < ((FilteredImage) source).timestamp) {
			this.invalidateCache();
		}
	}
	
	public final void invalidateCache() {
		if (this.cacheValidity != null) {
			this.cacheValidity.reset(false);
		}
		
		this.timestamp = System.nanoTime();
	}
	
	public final Filter getFilter() {
		return this.filter;
	}
	
	public final void setFilter(final Filter filter) {
		this.filter = filter;
	}
	
	@Override
	public final int getDimensionCount() {
		return this.getSource().getDimensionCount();
	}
	
	@Override
	public final int getDimension(final int dimensionIndex) {
		return this.getSource().getDimension(dimensionIndex);
	}
	
	@Override
	public final int getValue(final int index) {
		if (this.getFilter() == null) {
			final int value = this.getSource().getValue(index);
			
			return 1 < this.getChannelCount() ? value : argb(255, value, value, value);
		}
		
		if (this.cache != null) {
			final int columnCount = this.getColumnCount();
			final int rowIndex = index / columnCount;
			final int columnIndex = index % columnCount;
			final int cacheRowIndex = this.cacheLocation.y;
			final int rowIndexInCache = rowIndex - cacheRowIndex;
			final int cacheColumnIndex = this.cacheLocation.x;
			final int columnIndexInCache = columnIndex - cacheColumnIndex;
			final int cacheRowCount = this.cache.getRowCount();
			final int cacheColumnCount = this.cache.getColumnCount();
			
			if (0 <= rowIndexInCache && rowIndexInCache < cacheRowCount &&
					0 <= columnIndexInCache && columnIndexInCache < cacheColumnCount) {
				final int indexInCache = rowIndexInCache * cacheColumnCount + columnIndexInCache;
				
				if (!this.cacheValidity.get(indexInCache)) {
					this.cache.setValue(indexInCache, this.getFilter().getNewValue(index, this.getSource().getValue(index)));
					this.cacheValidity.set(indexInCache);
				}
				
				return this.cache.getValue(indexInCache);
			}
			
			final int newCacheRowIndex;
			final int newCacheColumnIndex;
			
			if (rowIndex < cacheRowIndex) {
				newCacheRowIndex = rowIndex;
			} else if (cacheRowCount <= rowIndexInCache) {
				newCacheRowIndex = rowIndex - cacheRowCount + 1;
			} else {
				newCacheRowIndex = cacheRowIndex;
			}
			
			if (columnIndex < cacheColumnIndex) {
				newCacheColumnIndex = columnIndex;
			} else if (cacheColumnCount <= columnIndexInCache) {
				newCacheColumnIndex = columnIndex - cacheColumnCount + 1;
			} else {
				newCacheColumnIndex = cacheColumnIndex;
			}
			
			this.cacheLocation.setLocation(newCacheColumnIndex, newCacheRowIndex);
			this.cacheValidity.reset(false);
		}
		
		return this.getFilter().getNewValue(index, this.getSource().getValue(index));
	}
	
	public final void setCacheDimensions(final int rowCount, final int columnCount) {
		if (rowCount <= 0 || columnCount <= 0) {
			this.cache = null;
			this.cacheValidity = null;
			this.cacheLocation = null;
			
			return;
		}
		
		if (this.cache != null && this.cache.getRowCount() == rowCount && this.cache.getColumnCount() == columnCount) {
			return;
		}
		
		this.cache = new ImageOfInts(rowCount, columnCount, 1);
		this.cacheValidity = new RegionOfInterest.UsingBitSet(rowCount, columnCount, false);
		this.cacheLocation = new Point();
	}
	
	@Override
	public final int getPixelCount() {
		return this.getSource().getPixelCount();
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public final int getChannelCount() {
		return this.getSource().getChannelCount();
	}
	
	@Override
	public final int getRowCount() {
		return this.getSource().getRowCount();
	}
	
	@Override
	public final int getColumnCount() {
		return this.getSource().getColumnCount();
	}
	
	@Override
	public final int getValue(final int rowIndex, final int columnIndex) {
		return this.getValue(index(this.getSource(), rowIndex, columnIndex));
	}
	
	@Override
	public final int setValue(final int rowIndex, final int columnIndex, final int value) {
		return this.setValue(index(this.getSource(), rowIndex, columnIndex), value);
	}
	
	/**
	 * {@value}.
	 */
	public static final int DEFAULT_CACHE_ROW_COUNT = 512;
	
	/**
	 * {@value}.
	 */
	public static final int DEFAULT_CACHE_COLUMN_COUNT = DEFAULT_CACHE_ROW_COUNT;
	
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
		
		private Channel channel;
		
		private int size;
		
		private final int[] oldRowIndex;
		
		private final int[] oldColumnIndex;
		
		private final int[] north;
		
		private final int[] northEast;
		
		private final int[] east;
		
		private final int[] southEast;
		
		private final int[] south;
		
		private final int[] southWest;
		
		private final int[] west;
		
		private final int[] northWest;
		
		protected StructuringElementFilter(final int[] deltas) {
			this.deltas = deltas;
			this.oldRowIndex = new int[3];
			this.oldColumnIndex = new int[3];
			final int n = deltas.length;
			
			fill(this.oldRowIndex, Integer.MIN_VALUE);
			fill(this.oldColumnIndex, Integer.MIN_VALUE);
			
			if (this.isOptimizedTraversalSupported()) {
				final IntList north = new IntList();
				final IntList northEast = new IntList();
				final IntList east = new IntList();
				final IntList southEast = new IntList();
				final IntList south = new IntList();
				final IntList southWest = new IntList();
				final IntList west = new IntList();
				final IntList northWest = new IntList();
				
				for (int i = 0; i < n; i += 2) {
					int deltaType = Direction.ALL;
					final int drI = deltas[i + 0];
					final int dcI = deltas[i + 1];
					
					for (int j = 0; j < n; j += 2) {
						final int drJ = deltas[j + 0];
						final int dcJ = deltas[j + 1];
						
						if (dcI == dcJ && drI - 1 == drJ) {
							deltaType &= ~Direction.NORTH;
						} else if (dcI + 1 == dcJ && drI - 1 == drJ) {
							deltaType &= ~Direction.NORTH_EAST;
						} else if (dcI + 1 == dcJ && drI == drJ) {
							deltaType &= ~Direction.EAST;
						} else if (dcI + 1 == dcJ && drI + 1 == drJ) {
							deltaType &= ~Direction.SOUTH_EAST;
						} else if (dcI == dcJ && drI + 1 == drJ) {
							deltaType &= ~Direction.SOUTH;
						} else if (dcI - 1 == dcJ && drI + 1 == drJ) {
							deltaType &= ~Direction.SOUTH_WEST;
						} else if (dcI - 1 == dcJ && drI == drJ) {
							deltaType &= ~Direction.WEST;
						} else if (dcI - 1 == dcJ && drI - 1 == drJ) {
							deltaType &= ~Direction.NORTH_WEST;
						}
					}
					
					if ((deltaType & Direction.NORTH) != 0) {
						north.add(i);
					}
					
					if ((deltaType & Direction.NORTH_EAST) != 0) {
						northEast.add(i);
					}
					
					if ((deltaType & Direction.EAST) != 0) {
						east.add(i);
					}
					
					if ((deltaType & Direction.SOUTH_EAST) != 0) {
						southEast.add(i);
					}
					
					if ((deltaType & Direction.SOUTH) != 0) {
						south.add(i);
					}
					
					if ((deltaType & Direction.SOUTH_WEST) != 0) {
						southWest.add(i);
					}
					
					if ((deltaType & Direction.WEST) != 0) {
						west.add(i);
					}
					
					if ((deltaType & Direction.NORTH_WEST) != 0) {
						northWest.add(i);
					}
				}
				
				this.north = north.toArray();
				this.northEast = northEast.toArray();
				this.east = east.toArray();
				this.southEast = southEast.toArray();
				this.south = south.toArray();
				this.southWest = southWest.toArray();
				this.west = west.toArray();
				this.northWest = northWest.toArray();
				
				assert this.north.length == this.south.length;
				assert this.northEast.length == this.southWest.length;
				assert this.east.length == this.west.length;
				assert this.southEast.length == this.northWest.length;
			} else {
				this.north = null;
				this.northEast = null;
				this.east = null;
				this.southEast = null;
				this.south = null;
				this.southWest = null;
				this.west = null;
				this.northWest = null;
			}
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
			this.channel = channel;
			final int rowCount = this.getImage().getRowCount();
			final int columnCount = this.getImage().getColumnCount();
			final int rowIndex = index / columnCount;
			final int columnIndex = index % columnCount;
			final int oldChannelValue = channel.getValue(oldValue);
			boolean optimizedProcessingPerformed = false;
			
			if (this.isOptimizedTraversalSupported()) {
				optimizedProcessingPerformed = true;
				final int dr = rowIndex - this.oldRowIndex[channel.getChannelIndex()];
				final int dc = columnIndex - this.oldColumnIndex[channel.getChannelIndex()];
				
				if (dc == 0 && dr == -1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.south, this.north);
				} else if (dc == 1 && dr == -1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.southWest, this.northEast);
				} else if (dc == 1 && dr == 0) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.west, this.east);
				} else if (dc == 1 && dr == 1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.northWest, this.southEast);
				} else if (dc == 0 && dr == 1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.north, this.south);
				} else if (dc == -1 && dr == 1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.northEast, this.southWest);
				} else if (dc == -1 && dr == 0) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.east, this.west);
				} else if (dc == -1 && dr == -1) {
					this.unprocessAndProcess(index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue,
							dr, dc, this.southEast, this.northWest);
				} else {
					optimizedProcessingPerformed = false;
				}
			}
			
			if (!optimizedProcessingPerformed) {
				this.reset(index, oldChannelValue);
				
				final int n = this.deltas.length;
				this.size = 0;
				
				for (int i = 0; i < n; i += 2) {
					this.processNeighbor(i, index, channel, rowCount, columnCount,
							rowIndex, columnIndex, oldChannelValue);
				}
			}
			
			this.oldRowIndex[channel.getChannelIndex()] = rowIndex;
			this.oldColumnIndex[channel.getChannelIndex()] = columnIndex;
			
			return this.getResult(index, oldChannelValue);
		}
		
		protected final Channel getChannel() {
			return this.channel;
		}
		
		/**
		 * Called in constructor.
		 * @return
		 */
		protected boolean isOptimizedTraversalSupported() {
			return false;
		}
		
		protected abstract void reset(int index, int oldChannelValue);
		
		protected void neighborIgnored() {
			// NOP
		}
		
		protected void unprocessNeighbor(int index, int oldChannelValue, int neighborIndex, int neighborChannelValue) {
			// NOP
		}
		
		protected abstract void processNeighbor(int index, int oldChannelValue, int neighborIndex, int neighborChannelValue);
		
		protected abstract int getResult(int index, int oldChannelValue);
		
		private final void unprocessAndProcess(final int index,
				final Channel channel, final int rowCount,
				final int columnCount, final int rowIndex,
				final int columnIndex, final int oldChannelValue,
				final int dr, final int dc,
				final int[] toUnprocess, final int[] toProcess) {
			assert toUnprocess.length == toProcess.length;
			
			for (final int i : toUnprocess) {
				this.unprocessNeighbor(i, index, channel, rowCount, columnCount, rowIndex, columnIndex, dr, dc, oldChannelValue);
			}
			
			for (final int i : toProcess) {
				this.processNeighbor(i, index, channel, rowCount, columnCount, rowIndex, columnIndex, oldChannelValue);
			}
		}
		
		private final void processNeighbor(final int i, final int index,
				final Channel channel, final int rowCount,
				final int columnCount, final int rowIndex,
				final int columnIndex, final int oldChannelValue) {
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
		
		private final void unprocessNeighbor(final int i, final int index, final Channel channel,
				final int rowCount, final int columnCount,
				final int rowIndex, final int columnIndex,
				final int dr, final int dc, final int oldChannelValue) {
			final int y = rowIndex + this.deltas[i + 0] - dr;
			
			if (0 <= y && y < rowCount) {
				final int x = columnIndex + this.deltas[i + 1] - dc;
				
				if (0 <= x && x < columnCount) {
					--this.size;
					final int neighborIndex = y * columnCount + x;
					final int neighborChannelValue = channel.getValue(this.getImage().getValue(neighborIndex));
					this.unprocessNeighbor(index, oldChannelValue, neighborIndex, neighborChannelValue);
				}
			}
		}
		
		/**
		 * @author codistmonk (creation 2013-04-15)
		 */
		public static final class Direction {
			
			private Direction() {
				throw new IllegalInstantiationException();
			}
			
			/**
			 * {@value}.
			 */
			public static final int NORTH = 1 << 0;
			
			/**
			 * {@value}.
			 */
			public static final int NORTH_EAST = 1 << 1;
			
			/**
			 * {@value}.
			 */
			public static final int EAST = 1 << 2;
			
			/**
			 * {@value}.
			 */
			public static final int SOUTH_EAST = 1 << 3;
			
			/**
			 * {@value}.
			 */
			public static final int SOUTH = 1 << 4;
			
			/**
			 * {@value}.
			 */
			public static final int SOUTH_WEST = 1 << 5;
			
			/**
			 * {@value}.
			 */
			public static final int WEST = 1 << 6;
			
			/**
			 * {@value}.
			 */
			public static final int NORTH_WEST = 1 << 7;
			
			/**
			 * {@value}.
			 */
			public static final int ALL = NORTH | NORTH_EAST | EAST | SOUTH_EAST | SOUTH | SOUTH_WEST | WEST | NORTH_WEST;
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class BinaryRankFilter extends StructuringElementFilter {
		
		private final int rank;
		
		private int accumulator;
		
		private int nonZero;
		
		public BinaryRankFilter(final int[] deltas, final int rank) {
			super(deltas);
			this.rank = rank;
			this.nonZero = 255;
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			this.accumulator = 0;
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue, final int neighborIndex, final int neighborValue) {
			if (0 != neighborValue) {
				++this.accumulator;
				this.nonZero = neighborValue;
			}
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			final int n = this.getSize();
			
			return (n - this.accumulator) <= ((this.rank + n) % n) ? this.nonZero : 0;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static final class IntRankFilter extends StructuringElementFilter {
		
		private final int rank;
		
		private final IntList values;
		
		public IntRankFilter(final int[] deltas, final int rank) {
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
		
		private final ChannelStatistics.Selector feature;
		
		private final ChannelStatistics[] statistics;
		
		public StatisticsFilter(final int[] deltas, final ChannelStatistics.Selector feature) {
			super(deltas);
			this.feature = feature;
			this.statistics = new ChannelStatistics[3];
			
			for (int i = 0; i < 3; ++i) {
				this.statistics[i] = new ChannelStatistics();
			}
		}
		
		@Override
		protected final boolean isOptimizedTraversalSupported() {
			return true;
		}
		
		@Override
		protected final void reset(final int index, final int oldValue) {
			this.statistics[this.getChannel().getChannelIndex()].reset();
		}
		
		@Override
		protected final void unprocessNeighbor(int index, int oldChannelValue,
				int neighborIndex, int neighborChannelValue) {
			this.statistics[this.getChannel().getChannelIndex()].removeValue(neighborChannelValue);
		}
		
		@Override
		protected final void processNeighbor(final int index, final int oldValue,
				final int neighborIndex, final int neighborValue) {
			this.statistics[this.getChannel().getChannelIndex()].addValue(neighborValue);
		}
		
		@Override
		protected final int getResult(final int index, final int oldValue) {
			return (int) this.feature.getValue(this.statistics[this.getChannel().getChannelIndex()]);
		}
		
		/**
		 * @author codistmonk (creation 2013-04-15)
		 */
		public static final class ChannelStatistics {
			
			private final int[] histogram;
			
			private long sum;
			
			private long sumOfSquares;
			
			private int count;
			
			public ChannelStatistics() {
				this.histogram = new int[256];
			}
			
			public final void reset() {
				fill(this.histogram, 0);
				this.sum = 0L;
				this.sumOfSquares = 0L;
				this.count = 0;
			}
			
			public final void removeValue(final int value) {
				--this.histogram[value];
				this.sum -= value;
				this.sumOfSquares -= square(value);
				--this.count;
			}
			
			public final void addValue(final int value) {
				++this.histogram[value];
				this.sum += value;
				this.sumOfSquares += square(value);
				++this.count;
			}
			
			public final long getSum() {
				return this.sum;
			}
			
			public final long getSumOfSquares() {
				return this.sumOfSquares;
			}
			
			public final int getCount() {
				return this.count;
			}
			
			public final int getMean() {
				return (int) (this.getCount() == 0 ? 0 : this.getSum() / this.getCount());
			}
			
			public final int getVariance() {
				final int mean = this.getMean();
				
				return (int) (this.getSumOfSquares() - 2 * mean * this.getSum() + this.getCount() * square(mean));
			}
			
			public final int getAmplitude() {
				return this.getCount() == 0 ? 0 : this.getMaximum() - this.getMinimum();
			}
			
			public final int getMinimum() {
				final int n = this.histogram.length;
				
				for (int i = 0; i < n; ++i) {
					if (0 < this.histogram[i]) {
						return i;
					}
				}
				
				return Integer.MAX_VALUE;
			}
			
			public final int getMaximum() {
				final int n = this.histogram.length;
				
				for (int i = n - 1; 0 <= i; --i) {
					if (0 < this.histogram[i]) {
						return i;
					}
				}
				
				return Integer.MIN_VALUE;
			}
			
			public static final long square(final int value) {
				return value * value;
			}
			
			/**
			 * @author codistmonk (creation 2013-04-15)
			 */
			public static enum Selector {
				
				MEAN {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getMean();
					}
					
				}, MINIMUM {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getMinimum();
					}
					
				}, MAXIMUM {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getMaximum();
					}
					
				}, AMPLITUDE {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getAmplitude();
					}
					
				}, VARIANCE {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getVariance();
					}
					
				}, SUM {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return (int) statistics.getSum();
					}
					
				}, SUM_OF_SQUARES {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return (int) statistics.getSumOfSquares();
					}
					
				}, COUNT {
					
					@Override
					public final int getValue(final ChannelStatistics statistics) {
						return statistics.getCount();
					}
					
				};
				
				public abstract int getValue(ChannelStatistics statistics);
				
			}
			
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
