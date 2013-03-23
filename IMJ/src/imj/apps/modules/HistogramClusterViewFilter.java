package imj.apps.modules;

import static imj.Labeling.NeighborhoodShape.CONNECTIVITY_4;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.ImageOfInts;
import imj.MorphologicalOperations;
import imj.RegionalMinima;
import imj.Watershed;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class HistogramClusterViewFilter extends ViewFilter {
	
	private Channel[] channels;
	
	private Image histogram;
	
	private Image clusters;
	
	private final int[] clusterSizes;
	
	private final Integer[] indices;
	
	private final boolean[] selectedClusters;
	
	public HistogramClusterViewFilter(final Context context) {
		super(context);
		this.histogram = new ImageOfInts(1, 256, 1);
		this.clusterSizes = new int[256];
		this.indices = new Integer[256];
		this.selectedClusters = new boolean[256];
		
		for (int i = 0; i < 256; ++i) {
			this.indices[i] = i;
		}
		
		this.getParameters().clear();
		
		this.getParameters().put("channels", "brightness");
		this.getParameters().put("clusters", "1");
	}
	
	public static final void fill(final Image image, final int value) {
		final int pixelCount = image.getRowCount() * image.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			image.setValue(pixel, value);
		}
	}
	
	@Override
	protected final void sourceImageChanged() {
		this.updateChannels();
		this.updateClusters();
		this.updateSelectedClusters();
	}
	
	@Override
	protected final void doInitialize() {
		final Channel[] oldChannels = this.channels;
		
		this.updateChannels();
		
		if (!Arrays.equals(oldChannels, this.channels)) {
			this.updateClusters();
		}
		
		this.updateSelectedClusters();
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return HistogramClusterViewFilter.this.accept(oldValue) ? oldValue : 0;
			}
			
		};
	}
	
	final boolean accept(final int value) {
		return this.selectedClusters[this.clusters.getValue(this.channels[0].getValue(value))];
	}
	
	private final void updateChannels() {
		final String[] channelAsStrings = this.getParameters().get("channels").trim().split("\\s+");
		final int channelCount = channelAsStrings.length;
		this.channels = new Channel[channelCount];
		
		for (int i = 0; i < channelCount; ++i) {
			this.channels[i] = ViewFilter.parseChannel(channelAsStrings[i].toUpperCase(Locale.ENGLISH));
		}
	}
	
	private final void updateClusters() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			fill(this.histogram, 0);
			
			final int pixelCount = source.getRowCount() * source.getColumnCount();
			int maximum = 0;
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				final int pixelChannelValue = this.channels[0].getValue(source.getValue(pixel));
				final int count = this.histogram.getValue(pixelChannelValue) + 1;
				
				this.histogram.setValue(pixelChannelValue, count);
				
				if (maximum < count) {
					maximum = count;
				}
			}
			
			for (int i = 0; i < 256; ++i) {
				this.histogram.setValue(i, 255 - 255 * (1 + this.histogram.getValue(i)) / (1 + maximum));
			}
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint("Applying watershed...", "(" + new Date(timer.tic()) + ")");
			
			final Image hMinima = MorphologicalOperations.hMinima4(this.histogram, 2);
			this.clusters = new RegionalMinima(hMinima, CONNECTIVITY_4).getResult();
			
			for (int pixel = 0; pixel < 256; ++pixel) {
				final int value = hMinima.getValue(pixel);
				
				if (255 < value) {
					hMinima.setValue(pixel, 255);
				}
			}
			
			this.clusters = new Watershed(hMinima, this.clusters, CONNECTIVITY_4).getResult();
			
			debugPrint("Applying watershed done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			Arrays.fill(this.clusterSizes, 0);
			int clusterCount = 0;
			
			for (int i = 0; i < 256; ++i) {
				final int cluster = this.clusters.getValue(i);
				this.clusterSizes[cluster] += 255 - this.histogram.getValue(i);
				
				if (clusterCount < cluster) {
					clusterCount = cluster;
				}
			}
			
			debugPrint("clusterCount:", clusterCount);
			
			sort(this.indices, new Comparator<Integer>() {
				
				@Override
				public final int compare(final Integer i1, final Integer i2) {
					return clusterSizes[i2] - clusterSizes[i1];
				}
				
			});
		}
	}
	
	private final void updateSelectedClusters() {
		Arrays.fill(this.selectedClusters, false);
		
		for (final int cluster : new CommandLineArgumentsParser("clusters", this.getParameters().get("clusters")).get("clusters")) {
			this.selectedClusters[this.indices[cluster]] = true;
		}
	}
	
	public static final void debugPrintHistogram(final Image histogram) {
		debugPrint();
		
		for (int i = 0; i < 256; ++i) {
			System.out.print(histogram.getValue(i));
			System.out.print(" ");
		}
		
		System.out.println();
	}
	
}
