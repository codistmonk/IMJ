package imj.apps.modules;

import static imj.clustering.HierarchicalClusterer.relabel;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.fill;
import static java.util.Locale.ENGLISH;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.clustering.Distance;
import imj.clustering.HierarchicalClusterer;

import java.util.Arrays;
import java.util.Date;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class HierarchicalClusterViewFilter extends ViewFilter {
	
	private Channel[] channels;
	
	private int verticalTileCount;
	
	private int horizontalTileCount;
	
	private String sampling;
	
	private String clustering;
	
	private int clusterCount;
	
	private double diameter;
	
	private HierarchicalClusterer clusterer;
	
	private int[] clusters;
	
	public HierarchicalClusterViewFilter(final Context context) {
		super(context);
		
		this.getParameters().clear();
		
		this.getParameters().put("channels", "brightness");
		this.getParameters().put("tiling", "16 16");
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
				return HierarchicalClusterViewFilter.this.accept(oldValue) ? oldValue : 0;
			}
			
		};
	}
	
	final boolean accept(final int value) {
		return true;
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
		final int oldHorizontalTileCount = this.horizontalTileCount;
		final String[] tiling = this.getParameters().get("tiling").split("\\s+");
		this.verticalTileCount = parseInt(tiling[0]);
		this.horizontalTileCount = parseInt(tiling[1]);
		
		final String oldSampling = this.sampling;
		this.sampling = this.getParameters().get("sampling").trim().toUpperCase(ENGLISH);
		
		final String[] clustering = this.getParameters().get("clustering").split("\\s+");
		this.clustering = clustering[0].trim().toUpperCase(ENGLISH);
		
		if ("COUNT".equals(this.clustering)) {
			this.clusterCount = parseInt(clustering[1]);
		} else if ("DIAMETER".equals(this.clustering)) {
			this.diameter = parseDouble(clustering[1]);
		}
		
		return !Arrays.equals(oldChannels, this.channels) ||
				oldVerticalTileCount != this.verticalTileCount || oldHorizontalTileCount != this.horizontalTileCount ||
				!Tools.equals(oldSampling, this.sampling);
	}
	
	private final void updateClusters() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			this.clusterer = new HierarchicalClusterer(Distance.Predefined.EUCLIDEAN);
			final PixelProcessor collector;
			final int imageRowCount = source.getRowCount();
			final int imageColumnCount = source.getColumnCount();
			final int tileRowCount = imageRowCount / this.verticalTileCount;
			final int tileColumnCount = imageColumnCount / this.horizontalTileCount;
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
						this.i = HierarchicalClusterViewFilter.this.collectChannelValues(source.getValue(pixel), this.i, sample);
						
						if (this.i == tileSize) {
							HierarchicalClusterViewFilter.this.clusterer.addSample(sample.clone());
							this.i = 0;
							fill(sample, 0.0);
						}
					}
					
				};
			} else if ("HISTOGRAM".equals(this.sampling)) {
				final double[] sample = new double[1 << (8 * (channelCount - 1))];
				collector = new PixelProcessor() {
					
					private int i;
					
					@Override
					public final void process(final int pixel) {
						HierarchicalClusterViewFilter.this.updateHistogram(source.getValue(pixel), sample);
						
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
			
			forEachPixelInEachTile(source, this.verticalTileCount, this.horizontalTileCount, collector);
			
			debugPrint(processed[0], "/", source.getRowCount() * source.getColumnCount());
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint("Clustering...", "(" + new Date(timer.tic()) + ")");
			
			this.clusterer.finish();
			
			debugPrint("Clustering done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint(this.clusterer.getDiameterStatistics().getMinimum(), this.clusterer.getDiameterStatistics().getMean(), this.clusterer.getDiameterStatistics().getMaximum());
		}
	}
	
	final int collectChannelValues(final int pixelValue, final int i, final double[] sample) {
		int j = i;
		
		for (final Channel channel : this.channels) {
			sample[j++] = channel.getValue(pixelValue);
		}
		
		return j;
	}
	
	final void updateHistogram(final int pixelValue, final double[] histogram) {
		++histogram[this.getColorIndex(pixelValue)];
	}
	
	public static final void forEachPixelInEachTile(final Image image, final int verticalTileCount, final int horizontalTileCount, final PixelProcessor processor) {
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final int tileRowCount = imageRowCount / verticalTileCount;
		final int tileColumnCount = imageColumnCount / horizontalTileCount;
		
		for (int tileRowIndex = 0; tileRowIndex < verticalTileCount; ++tileRowIndex) {
			for (int tileColumnIndex = 0; tileColumnIndex < horizontalTileCount; ++tileColumnIndex) {
				for (int rowIndex = tileRowIndex, endRowIndex = tileRowIndex + tileRowCount; rowIndex < endRowIndex; ++rowIndex) {
					for (int columnIndex = tileColumnIndex, endColumnIndex = tileColumnIndex + tileColumnCount; columnIndex < endColumnIndex; ++columnIndex) {
						processor.process(rowIndex * imageColumnCount + columnIndex);
					}
				}
			}
		}
	}
	
	/**
	 * @author codistmonk (creation 2013-04-08)
	 */
	public static abstract interface PixelProcessor {
		
		public abstract void process(int pixel);
		
	}
	
	private final int getColorIndex(final int pixelRawValue) {
		int result = 0;
		
		for (final Channel channel : this.channels) {
			result = result * 256 + channel.getValue(pixelRawValue);
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
			debugPrint("TODO");
		} else if ("DIAMETER".equals(this.clustering)) {
			this.clusters = this.clusterer.getClusters(this.diameter);
			
			debugPrint("clusterCount:", relabel(this.clusters));
		}
		
		debugPrint("Extracting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
	}
	
}
