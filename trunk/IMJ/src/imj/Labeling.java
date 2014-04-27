package imj;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;
import jgencode.primitivelists.IntList;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-01-25)
 */
public abstract class Labeling {
	
	private final Image image;
	
	private final Image result;
	
	private final int rowCount;
	
	private final int columnCount;
	
	private final int pixelCount;
	
	private final int lastRowIndex;
	
	private final int lastColumnIndex;
	
	protected Labeling(final Image image, final Image result) {
		this.image = image;
		this.rowCount = image.getRowCount();
		this.columnCount = image.getColumnCount();
		this.pixelCount = this.rowCount * this.columnCount;
		this.lastRowIndex = this.rowCount - 1;
		this.lastColumnIndex = this.columnCount - 1;
		this.result = result != null ? result : getMemoryManagementStrategy().newImage(image.getRowCount(), image.getColumnCount(), image.getChannelCount());
	}
	
	protected Labeling(final Image image) {
		this(image, null);
	}
	
	protected Labeling(final Image image, final int resultChannelCount) {
		this(image, getMemoryManagementStrategy().newImage(image.getRowCount(), image.getColumnCount(), resultChannelCount));
	}
	
	public final Image getImage() {
		return this.image;
	}
	
	public final Image getResult() {
		return this.result;
	}
	
	public final int getRowCount() {
		return this.rowCount;
	}
	
	public final int getColumnCount() {
		return this.columnCount;
	}
	
	public final int getPixelCount() {
		return this.pixelCount;
	}
	
	public final int getLastRowIndex() {
		return this.lastRowIndex;
	}
	
	public final int getLastColumnIndex() {
		return this.lastColumnIndex;
	}
	
	public final int north(final int pixel) {
		return pixel - this.getColumnCount();
	}
	
	public final int west(final int pixel) {
		return pixel - 1;
	}
	
	public final int east(final int pixel) {
		return pixel + 1;
	}
	
	public final int south(final int pixel) {
		return pixel + this.getColumnCount();
	}
	
	public final int getRowIndex(final int pixel) {
		return pixel / this.getColumnCount();
	}
	
	public final int getColumnIndex(final int pixel) {
		return pixel % this.getColumnCount();
	}
	
	public final boolean hasNorth(final int rowIndex) {
		return 0 < rowIndex;
	}
	
	public final boolean hasWest(final int columnIndex) {
		return 0 < columnIndex;
	}
	
	public final boolean hasEast(final int columnIndex) {
		return columnIndex < this.getLastColumnIndex();
	}
	
	public final boolean hasSouth(final int rowIndex) {
		return rowIndex < this.getLastRowIndex();
	}
	
	public final int getIndex(final int rowIndex, final int columnIndex) {
		return rowIndex * this.getColumnCount() + columnIndex;
	}
	
	/**
	 * @author codistmonk (creation 2013-01-26)
	 */
	public final class Neighborhood {
		
		private final IntList deltas;
		
		private final IntList neighbors;
		
		private int i;
		
		public Neighborhood(final int... deltas) {
			this.deltas = new IntList(deltas.length);
			this.neighbors = new IntList((deltas.length + 1) / 2);
			
			this.deltas.addAll(deltas);
		}
		
		public final void reset(final int pixel) {
			final int rowIndex = Labeling.this.getRowIndex(pixel);
			final int columnIndex = Labeling.this.getColumnIndex(pixel);
			final int n = this.deltas.size();
			
			this.neighbors.clear();
			
			for (int i = 0; i < n; i += 2) {
				final int r = rowIndex + this.deltas.get(i);
				
				if (0 <= r && r < Labeling.this.getRowCount()) {
					final int c = columnIndex + this.deltas.get(i + 1);
					
					if (0 <= c && c < Labeling.this.getColumnCount()) {
						this.neighbors.add(Labeling.this.getIndex(r, c));
					}
				}
			}
			
			assert rowIndex == 0 || rowIndex == Labeling.this.getLastRowIndex() ||
					columnIndex == 0 || columnIndex == Labeling.this.getLastColumnIndex() ||
					this.neighbors.size() == this.deltas.size() / 2;
			
			this.i = 0;
		}
		
		public final boolean hasNext() {
			return this.i < this.neighbors.size();
		}
		
		public final int getNextDeltaIndex() {
			return this.i++;
		}
		
		public final int getNext() {
			return this.get(this.getNextDeltaIndex());
		}
		
		public final int get(final int deltaIndex) {
			return this.neighbors.get(deltaIndex);
		}
		
	}
	
	private static MemoryManagementStrategy memoryManagementStrategy = MemoryManagementStrategy.PRIORITIZE_SPEED;
	
	public static final MemoryManagementStrategy getMemoryManagementStrategy() {
		return memoryManagementStrategy;
	}
	
	public static final void setMemoryManagementStrategy(final MemoryManagementStrategy memoryManagementStrategy) {
		Labeling.memoryManagementStrategy = memoryManagementStrategy;
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public static enum MemoryManagementStrategy {
		
		PRIORITIZE_SPEED {
			
			@Override
			public final ImageOfInts newImage(final int rowCount, final int columnCount, final int channelCount) {
				return new ImageOfInts(rowCount, columnCount, channelCount);
			}
			
		},
		PRIORITIZE_MEMORY {
			
			@Override
			public final LinearStorage newImage(final int rowCount, final int columnCount, final int channelCount) {
				return new LinearStorage(rowCount, columnCount, channelCount);
			}
			
		};
		
		public abstract Image.Abstract newImage(int rowCount, int columnCount, int channelCount);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-08)
	 */
	public static final class NeighborhoodShape {
		
		private NeighborhoodShape() {
			throw new IllegalInstantiationException();
		}
		
		public static final int[] CONNECTIVITY_4 = newDisk(1.0, Distance.CITYBLOCK);
		
		public static final int[] CONNECTIVITY_8 = newDisk(1.0, Distance.CHESSBOARD);
		
		public static final int[] newDisk(final double radius, final Distance distance) {
			final int r = (int) ceil(radius);
			final IntList resultBuilder = new IntList();
			
			for (int i = -r; i <= +r; ++i) {
				for (int j = -r; j <= +r; ++j) {
					if ((i != 0 || j != 0) && distance.distance(0, 0, j, i) <= radius) {
						resultBuilder.add(i);
						resultBuilder.add(j);
					}
				}
			}
			
			return resultBuilder.toArray();
		}
		
		public static final int[] newDisk(final double radius) {
			return newDisk(radius, Distance.EUCLIDEAN);
		}
		
		/**
		 * @author codistmonk (creation 2013-02-03)
		 */
		public static enum Distance {
			
			CITYBLOCK {
				
				@Override
				public final double distance(final int x1, final int y1, final int x2, final int y2) {
					return abs(x2 - x1) + abs(y2 - y1);
				}
				
			},
			CHESSBOARD {
				
				@Override
				public final double distance(final int x1, final int y1, final int x2, final int y2) {
					return max(abs(x2 - x1), abs(y2 - y1));
				}
				
			},
			EUCLIDEAN {
				
				@Override
				public final double distance(final int x1, final int y1, final int x2, final int y2) {
					return sqrt(square(x2 - x1) + square(y2 - y1));
				}
				
			}
			;
			
			public abstract double distance(int x1, int y1, int x2, int y2);
			
		}
		
	}
	
}
