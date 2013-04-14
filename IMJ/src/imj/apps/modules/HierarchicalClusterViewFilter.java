package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.clustering.HierarchicalClusterer.relabel;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.clustering.Distance;
import imj.clustering.HierarchicalClusterer;
import imj.clustering.HierarchicalClusterer.InterNode;
import imj.clustering.HierarchicalClusterer.Node;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;

import javax.swing.JComponent;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class HierarchicalClusterViewFilter extends ViewFilter {
	
	private Channel[] channels;
	
	private String scope;
	
	private Image source;
	
	private int verticalTileCount;
	
	private int horizontalTileCount;
	
	private String sampling;
	
	private int channelBinningBitCount;
	
	private String clustering;
	
	private int clusterCount;
	
	private double diameter;
	
	private HierarchicalClusterer clusterer;
	
	private int[] clusters;
	
	public HierarchicalClusterViewFilter(final Context context) {
		super(context);
		
		this.getParameters().clear();
		
		this.getParameters().put("channels", "brightness");
		this.getParameters().put("tiling", "16");
		this.getParameters().put("scope", "viewport");
		this.getParameters().put("sampling", "tile");
		this.getParameters().put("clustering", "count 2");
	}
	
	@Override
	protected final void sourceImageChanged() {
		this.updateParameters();
		this.updateClusters();
		this.updateSelectedClusters();
	}
	
	@Override
	protected final void doInitialize() {
		if (this.updateParameters()) {
			this.updateClusters();
		}
		
		this.updateSelectedClusters();
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return HierarchicalClusterViewFilter.this.getClusterColor(index);
			}
			
		};
	}
	
	final int getClusterColor(final int pixel) {
		return 0 < this.clusterCount ? 0xFF000000 | (0x00FFFFFF * (1 + this.getClusterIndex(pixel)) / this.clusterCount) : 0;
	}
	
	private final boolean updateParameters() {
		final Channel[] oldChannels = this.channels;
		final String[] channelAsStrings = this.getParameters().get("channels").trim().split("\\s+");
		final int channelCount = channelAsStrings.length;
		this.channels = new Channel[channelCount];
		
		for (int i = 0; i < channelCount; ++i) {
			this.channels[i] = ViewFilter.parseChannel(channelAsStrings[i].toUpperCase(ENGLISH));
		}
		
		final int oldVerticalTileCount = this.verticalTileCount;
		final String[] tiling = this.getParameters().get("tiling").split("\\s+");
		this.verticalTileCount = parseInt(tiling[0]);
		this.horizontalTileCount = this.verticalTileCount;
		
		this.scope = this.getParameters().get("scope").trim().toUpperCase(ENGLISH);
		
		final String oldSampling = this.sampling;
		final int oldChannelBinningBitCount = this.channelBinningBitCount;
		final String[] sampling = this.getParameters().get("sampling").split("\\s+");
		this.sampling = sampling[0].toUpperCase(ENGLISH);
		
		if ("HISTOGRAM".equals(this.sampling)) {
			this.channelBinningBitCount = parseInt(sampling[1]);
		}
		
		final String[] clustering = this.getParameters().get("clustering").split("\\s+");
		this.clustering = clustering[0].trim().toUpperCase(ENGLISH);
		
		if ("COUNT".equals(this.clustering)) {
			this.clusterCount = parseInt(clustering[1]);
		} else if ("DIAMETER".equals(this.clustering)) {
			this.diameter = parseDouble(clustering[1]);
		}
		
		this.updateSource();
		
		return !Arrays.equals(oldChannels, this.channels) ||
				oldVerticalTileCount != this.verticalTileCount ||
				!Tools.equals(oldSampling, this.sampling) || oldChannelBinningBitCount != this.channelBinningBitCount;
	}
	
	private final void updateSource() {
		this.source = this.getImage().getSource();
		
		if (this.source != null && "VIEWPORT".equals(this.scope)) {
			final JComponent imageView = this.getContext().get("imageView");
			this.source = new Viewport(this.source, imageView.getVisibleRect());
			final int sourceRowCount = this.source.getRowCount();
			final int sourceColumnCount = this.source.getColumnCount();
			this.horizontalTileCount = sourceColumnCount / (sourceRowCount / this.verticalTileCount);
		}
	}
	
	private final void updateClusters() {
		if (this.source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			this.clusterer = new HierarchicalClusterer(Distance.Predefined.EUCLIDEAN);
			final PixelProcessor collector;
			final int sourceRowCount = this.source.getRowCount();
			final int sourceColumnCount = this.source.getColumnCount();
			final int tileRowCount = sourceRowCount / this.verticalTileCount;
			final int tileColumnCount = sourceColumnCount / this.horizontalTileCount;
			final int tileSize = tileRowCount * tileColumnCount;
			final int channelCount = this.channels.length;
			final long[] processed = { 0L };
			
			if ("TILE".equals(this.sampling)) {
				final double[] sample = new double[tileSize * channelCount];
				collector = new PixelProcessor() {
					
					private int i;
					
					@Override
					public final void process(final int pixel) {
						++processed[0];
						this.i = HierarchicalClusterViewFilter.this.collectChannelValues(pixel, this.i, sample);
						
						if (this.i == tileSize * channelCount) {
							HierarchicalClusterViewFilter.this.clusterer.addSample(sample.clone());
							this.i = 0;
							fill(sample, 0.0);
						}
					}
					
				};
			} else if ("HISTOGRAM".equals(this.sampling)) {
				final double[] sample = new double[1 << (this.channelBinningBitCount * channelCount)];
				collector = new PixelProcessor() {
					
					private int i;
					
					@Override
					public final void process(final int pixel) {
						++processed[0];
						HierarchicalClusterViewFilter.this.updateHistogram(pixel, sample);
						
						if (++this.i == tileSize) {
							HierarchicalClusterViewFilter.this.clusterer.addSample(sample.clone());
							this.i = 0;
							fill(sample, 0.0);
						}
					}
					
				};
			} else {
				throw new IllegalArgumentException();
			}
			
			forEachPixelInEachTile(this.source, this.verticalTileCount, this.horizontalTileCount, collector);
			
			debugPrint(processed[0], "/", this.source.getRowCount() * this.source.getColumnCount());
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint("Clustering...", "(" + new Date(timer.tic()) + ")");
			
			this.clusterer.finish();
			
			debugPrint("Clustering done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint(this.clusterer.getDiameterStatistics().getMinimum(), this.clusterer.getDiameterStatistics().getMean(), this.clusterer.getDiameterStatistics().getMaximum());
		}
	}
	
	final int collectChannelValues(final int pixel, final int i, final double[] sample) {
		final Image image = this.source;
		int j = i;
		
		for (final Channel channel : this.channels) {
			if (Channel.Primitive.ROW == channel) {
				sample[j++] = pixel / image.getColumnCount();
			} else if (Channel.Primitive.COLUMN == channel) {
				sample[j++] = pixel % image.getColumnCount();
			} else {
				sample[j++] = channel.getValue(image.getValue(pixel));
			}
		}
		
		return j;
	}
	
	final void updateHistogram(final int pixel, final double[] histogram) {
		++histogram[this.getColorIndex(pixel, this.source.getValue(pixel))];
	}
	
	private final int getClusterIndex(final int pixel) {
		final int sourceRowCount = this.source.getRowCount();
		final int sourceColumnCount = this.source.getColumnCount();
		final int pixelRowIndex;
		final int pixelColumnIndex;
		final Viewport viewport = cast(Viewport.class, this.source);
		
		if (viewport != null) {
			final int imageColumnCount = this.getImage().getColumnCount();
			final int pixelRowIndexInImage = pixel / imageColumnCount;
			final int pixelColumnIndexInImage = pixel % imageColumnCount;
			
			if (!viewport.getViewport().contains(pixelColumnIndexInImage, pixelRowIndexInImage)) {
				return 0;
			}
			
			pixelRowIndex = viewport.convertRowIndex(pixelRowIndexInImage);
			pixelColumnIndex = viewport.convertColumnIndex(pixelColumnIndexInImage);
		} else {
			pixelRowIndex = pixel / sourceColumnCount;
			pixelColumnIndex = pixel % sourceColumnCount;
		}
		
		final int tileRowCount = sourceRowCount / this.verticalTileCount;
		final int tileColumnCount = sourceColumnCount / this.horizontalTileCount;
		final int tileRowIndex = min(this.verticalTileCount - 1, pixelRowIndex / tileRowCount);
		final int tileColumnIndex = min(this.horizontalTileCount - 1, pixelColumnIndex / tileColumnCount);
		
		return this.clusters[tileRowIndex * this.horizontalTileCount + tileColumnIndex];
	}
	
	private final int getColorIndex(final int pixelIndex, final int pixelRawValue) {
		int result = 0;
		final int n = 1 << this.channelBinningBitCount;
		final int shift = 8 - this.channelBinningBitCount;
		
		for (final Channel channel : this.channels) {
			final int channelValue;
			
			if (Channel.Primitive.ROW == channel) {
				channelValue = (1 + pixelIndex / this.source.getColumnCount()) * 255 / this.source.getRowCount();
			} else if (Channel.Primitive.COLUMN == channel) {
				channelValue = (1 + pixelIndex % this.source.getColumnCount()) * 255 / this.source.getColumnCount();
			} else {
				channelValue = channel.getValue(pixelRawValue);
			}
			
			result = result * n + (channelValue >> shift);
		}
		
		return result;
	}
	
	private final void updateSelectedClusters() {
		if (this.clusterer == null) {
			return;
		}
		
		final TicToc timer = new TicToc();
		
		debugPrint("Extracting data...", "(" + new Date(timer.tic()) + ")");
		
		if ("COUNT".equals(this.clustering)) {
			final PriorityQueue<Node> nodes = new PriorityQueue<Node>(11, new Comparator<Node>() {
				
				@Override
				public final int compare(final Node n1, final Node n2) {
					return Double.compare(n2.getDiameter(), n1.getDiameter());
				}
				
			});
			
			nodes.add(this.clusterer.getLastNode());
			
			while (nodes.size() < this.clusterCount) {
				final InterNode interNode = cast(InterNode.class, nodes.peek());
				
				if (interNode == null) {
					break;
				}
				
				nodes.poll();
				nodes.add(interNode.getChild0());
				nodes.add(interNode.getChild1());
			}
			
			this.clusters = new int[this.clusterer.getSampleCount()];
			
			for (final Node node : nodes) {
				HierarchicalClusterer.label(node, this.clusters);
			}
		} else if ("DIAMETER".equals(this.clustering)) {
			this.clusters = this.clusterer.getClusters(this.diameter);
		}
		
		this.clusterCount = relabel(this.clusters);
		
		debugPrint("Extracting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
		
		debugPrint("clusterCount:", this.clusterCount);
	}
	
	/**
	 * @author codistmonk (creation 2013-04-11)
	 */
	public static final class Viewport implements Image {
		
		private final Image source;
		
		private final Rectangle viewport;
		
		public Viewport(final Image source, final Rectangle viewport) {
			this.source = source;
			this.viewport = viewport;
		}
		
		public final Image getSource() {
			return this.source;
		}
		
		public final Rectangle getViewport() {
			return this.viewport;
		}
		
		public final int convertRowIndex(final int rowIndexInSource) {
			return rowIndexInSource - this.getViewport().y;
		}
		
		public final int convertColumnIndex(final int columnIndexInSource) {
			return columnIndexInSource - this.getViewport().x;
		}
		
		public final int convertIndex(final int indexInSource) {
			final int sourceColumnCount = this.getSource().getColumnCount();
			final int rowIndexInSource = indexInSource / sourceColumnCount;
			final int columnIndexInSource = indexInSource % sourceColumnCount;
			
			return this.convertRowIndex(rowIndexInSource) * this.getColumnCount() + this.convertColumnIndex(columnIndexInSource);
		}
		
		@Override
		public final int getPixelCount() {
			return this.getSource().getPixelCount();
		}
		
		@Override
		public final int getChannelCount() {
			return this.getSource().getChannelCount();
		}
		
		@Override
		public final int getRowCount() {
			return this.getViewport().height;
		}
		
		@Override
		public final int getColumnCount() {
			return this.getViewport().width;
		}
		
		@Override
		public final int getValue(final int rowIndex, final int columnIndex) {
			return this.getSource().getValue(this.getViewport().y + rowIndex, this.getViewport().x + columnIndex);
		}
		
		@Override
		public final int setValue(final int rowIndex, final int columnIndex, final int value) {
			return this.getSource().setValue(this.getViewport().y + rowIndex, this.getViewport().x + columnIndex, value);
		}
		
		@Override
		public final int getValue(final int index) {
			final int columnCount = this.getColumnCount();
			final int rowIndex = index / columnCount;
			final int columnIndex = index % columnCount;
			
			return this.getValue(rowIndex, columnIndex);
		}
		
		@Override
		public final int setValue(final int index, final int value) {
			final int columnCount = this.getColumnCount();
			final int rowIndex = index / columnCount;
			final int columnIndex = index % columnCount;
			
			return this.setValue(rowIndex, columnIndex, value);
		}
		
	}
	
}
