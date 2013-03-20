package imj.apps.modules;

import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.RegionalMaxima.regionalMaxima26;
import static imj.Watershed.watershedTopDown26;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.ImageOfInts;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class ColorClusterViewFilter extends ViewFilter {
	
	private final DataCube counts;
	
	private final DataCube clusters;
	
	private int[] clusterSizes;
	
	private Integer[] indices;
	
	private int maximum;
	
	private double threshold;
	
	private final Set<Integer> userClusters;
	
	private final Set<Integer> actualClusters;
	
	public ColorClusterViewFilter(final Context context) {
		super(context);
		this.counts = new DataCube(256);
		this.clusters = new DataCube(256);
		this.userClusters = new HashSet<Integer>();
		this.actualClusters = new HashSet<Integer>();
		
		this.getParameters().clear();
		
		this.getParameters().put("threshold", "0.1");
		this.getParameters().put("clusters", "0:2");
	}
	
	@Override
	protected final void sourceImageChanged() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			this.counts.fill(0);
			
			final int pixelCount = source.getRowCount() * source.getColumnCount();
			this.maximum = Integer.MIN_VALUE;
			
			for (int  pixel = 0; pixel < pixelCount; ++pixel) {
				final int value = source.getValue(pixel);
				final int r = red(value);
				final int g = green(value);
				final int b = blue(value);
				final int newCount = this.counts.get(r, g, b) + 1;
				
				this.counts.set(r, g, b, newCount);
				
				if (this.maximum < newCount) {
					this.maximum = newCount;
				}
			}
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint("Applying watershed...", "(" + new Date(timer.tic()) + ")");
			
			this.clusters.fill(0);
			
			watershedTopDown26(this.counts.getData(), regionalMaxima26(this.counts.getData(), this.clusters.getData()));
			
			debugPrint("Applying watershed done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			int clusterCount = 0;
			
			for (final Image layer : this.clusters.getData()) {
				for (int pixel = 0; pixel < 256 * 256; ++pixel) {
					final int cluster = layer.getValue(pixel);
					
					if (clusterCount < cluster) {
						clusterCount = cluster;
					}
					
					if (cluster == 0) {
						throw new IllegalStateException();
					}
				}
			}
			
			debugPrint("clusterCount:", clusterCount);
			
			this.clusterSizes = new int[clusterCount + 1];
			
			for (int layerIndex = 0; layerIndex < 256; ++layerIndex) {
				for (int pixel = 0; pixel < 256 * 256; ++pixel) {
					final int cluster = this.clusters.getData()[layerIndex].getValue(pixel);
					final int colorCount = this.counts.getData()[layerIndex].getValue(pixel);
					
					this.clusterSizes[cluster] += colorCount;
				}
			}
			
			this.indices = new Integer[clusterCount + 1];
			
			for (int cluster = 0; cluster <= clusterCount; ++cluster) {
				this.indices[cluster] = cluster;
			}
			
			sort(this.indices, new Comparator<Integer>() {
				
				@Override
				public final int compare(final Integer i1, final Integer i2) {
					return clusterSizes[i2] - clusterSizes[i1];
				}
				
			});
			
			debugPrint(Arrays.toString(this.clusterSizes));
		}
	}
	
	@Override
	protected final void doInitialize() {
		this.threshold = Double.parseDouble(this.getParameters().get("threshold").trim());
		
		this.userClusters.clear();
		
		for (final int cluster : new CommandLineArgumentsParser("clusters", this.getParameters().get("clusters")).get("clusters")) {
			this.userClusters.add(cluster);
		}
		
		this.actualClusters.clear();
		
//		final TicToc timer = new TicToc();
		
//		debugPrint("Applying watershed...", "(" + new Date(timer.tic()) + ")");
//		
//		this.clusters.fill(0);
//		
//		watershedTopDown26(this.counts.getData(), regionalMaxima26(this.counts.getData(), this.clusters.getData()));
//		
//		debugPrint("Applying watershed done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
//		
//		int labelCount = 0;
//		
//		for (final Image layer : this.clusters.getData()) {
//			for (int pixel = 0; pixel < 256 * 256; ++pixel) {
//				final int label = layer.getValue(pixel);
//				
//				if (labelCount < label) {
//					labelCount = label;
//				}
//			}
//		}
//		
//		debugPrint("labelCount:", labelCount);
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return ColorClusterViewFilter.this.accept(oldValue) ? oldValue : 0;
			}
			
		};
	}
	
	final boolean accept(final int red, final int green, final int blue) {
		final int count = this.counts.get(red, green, blue);
		
//		return this.maximum * this.threshold <= count;
		
		if (this.actualClusters.isEmpty() && !this.userClusters.isEmpty()) {
			debugPrint(this.indices[0], this.clusterSizes[this.indices[0]]);
			debugPrint(this.clusters.get(red, green, blue), this.clusterSizes[this.clusters.get(red, green, blue)]);
			for (final Integer cluster : this.userClusters) {
				this.actualClusters.add(this.indices[cluster]);
			}
			debugPrint(this.actualClusters);
		}
		
		return this.actualClusters.contains(this.clusters.get(red, green, blue));
	}
	
	final boolean accept(final int value) {
		return this.accept(red(value), green(value), blue(value));
	}
	
	/**
	 * @author codistmonk (creation 2013-03-19)
	 */
	public static final class DataCube {
		
		private final int n;
		
		private final int nn;
		
		private final Image[] data;
		
		public DataCube(final int n) {
			this.n = n;
			this.nn = n * n;
			this.data = new Image[n];
			
			for (int i = 0; i < n; ++i) {
				this.data[i] = new ImageOfInts(n, n, 1);
			}
		}
		
		public final void fill(final int value) {
			for (final Image layer : this.data) {
				for (int i = 0; i < this.nn; ++i) {
					layer.setValue(i, value);
				}
			}
		}
		
		public final int getN() {
			return this.n;
		}
		
//		public final int get(final int index) {
//			return this.data[index];
//		}
//		
//		public final void set(final int index, final int value) {
//			this.data[index] = value;
//		}
		
		public final Image[] getData() {
			return this.data;
		}
		
		public final int get(final int x, final int y, final int z) {
//			return this.get(this.getIndex(x, y, z));
			return this.data[z].getValue(y, x);
		}
		
		public final void set(final int x, final int y, final int z, final int value) {
//			this.set(this.getIndex(x, y, z), value);
			this.data[z].setValue(y, x, value);
		}
		
		public final int getIndex(final int x, final int y, final int z) {
			return this.getN() * (this.getN() * z + y) + x;
		}
		
		public final int getX(final int index) {
			return index % this.getN();
		}
		
		public final int getY(final int index) {
			return (index / this.getN()) % this.nn;
		}
		
		public final int getZ(final int index) {
			return index / this.nn;
		}
		
	}
	
}
