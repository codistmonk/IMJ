package imj.apps.modules;

import static imj.Labeling.NeighborhoodShape.CONNECTIVITY_4;
import static imj.apps.modules.HistogramPanel.ValueScale.parseValueScale;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.sort;
import static net.sourceforge.aprog.tools.Tools.DEBUG_STACK_OFFSET;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.Image;
import imj.ImageOfInts;
import imj.MorphologicalOperations;
import imj.RegionalMinima;
import imj.Watershed;
import imj.apps.modules.HistogramPanel.ValueScale;

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
	
	private int hMin;
	
	private ValueScale valueScale;
	
	private Image histogram;
	
	private Image clusters;
	
	private final int[] clusterSizes;
	
	private final Integer[] indices;
	
	private final boolean[] selectedClusters;
	
	public HistogramClusterViewFilter(final Context context) {
		super(context);
		this.histogram = new ImageOfInts(1, 256, 1);
		final int maximumClusterCount = 256 * 256;
		this.clusterSizes = new int[maximumClusterCount];
		this.indices = new Integer[maximumClusterCount];
		this.selectedClusters = new boolean[maximumClusterCount];
		
		for (int i = 0; i < maximumClusterCount; ++i) {
			this.indices[i] = i;
		}
		
		this.getParameters().clear();
		
		this.getParameters().put("channels", "brightness");
		this.getParameters().put("valueScale", "linear");
		this.getParameters().put("hMin", "2");
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
		final int oldHMin = this.hMin;
		final ValueScale oldValueScale = this.valueScale;
		
		this.updateChannels();
		
		this.hMin = parseInt(this.getParameters().get("hMin").trim());
		this.valueScale = parseValueScale(this.getParameters().get("valueScale"));
		
		if (!Arrays.equals(oldChannels, this.channels) || oldHMin != this.hMin ||
				oldValueScale == null || !oldValueScale.getClass().equals(this.valueScale.getClass())) {
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
		return this.selectedClusters[this.clusters.getValue(this.getColorIndex(value))];
	}
	
	private final void updateChannels() {
		final String[] channelAsStrings = this.getParameters().get("channels").trim().split("\\s+");
		final int channelCount = channelAsStrings.length;
		this.channels = new Channel[channelCount];
		
		for (int i = 0; i < channelCount; ++i) {
			this.channels[i] = ViewFilter.parseChannel(channelAsStrings[i].toUpperCase(Locale.ENGLISH));
		}
		
		switch (channelCount) {
		case 1:
			if (this.histogram.getRowCount() != 1) {
				this.histogram = new ImageOfInts(1, 256, 1);
			}
			
			break;
		case 2:
			if (this.histogram.getRowCount() != 256) {
				this.histogram = new ImageOfInts(256, 256, 1);
			}
			
			break;
		default:
			throw new IllegalArgumentException("Invalid channel count: " + channelCount);
		}
	}
	
	private final void updateClusters() {
		final Image source = this.getImage().getSource();
		
		if (source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			this.updateHistogram(source);
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			debugPrint("Applying watershed...", "(" + new Date(timer.tic()) + ")");
			
			final Image hMinima = MorphologicalOperations.hMinima4(this.histogram, this.hMin);
			this.clusters = new RegionalMinima(hMinima, CONNECTIVITY_4).getResult();
			final int histogramSize = this.histogram.getRowCount() * this.histogram.getColumnCount();
			
			for (int pixel = 0; pixel < histogramSize; ++pixel) {
				final int value = hMinima.getValue(pixel);
				
				if (255 < value) {
					hMinima.setValue(pixel, 255);
				}
			}
			
			this.clusters = new Watershed(hMinima, this.clusters, CONNECTIVITY_4).getResult();
			
			debugPrint("Applying watershed done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
			
			Arrays.fill(this.clusterSizes, 0);
			int clusterCount = 0;
			
			for (int i = 0; i < histogramSize; ++i) {
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
			
//			debugPrintHistogram(this.clusters);
//			debugPrint(Arrays.toString(Arrays.copyOf(this.clusterSizes, 10)));
//			debugPrint(Arrays.toString(Arrays.copyOf(this.indices, 10)));
		}
	}

	private final void updateHistogram(final Image source) {
		fill(this.histogram, 0);
		
		final int sourceRowCount = source.getRowCount();
		final int sourceColumnCount = source.getColumnCount();
		final int pixelCount = sourceRowCount * sourceColumnCount;
		int maximum = 0;
		RegionOfInterest roi = Sieve.getROI(this.getContext());
		
		if (roi != null && (roi.getRowCount() != sourceRowCount || roi.getColumnCount() != sourceColumnCount)) {
			roi = null;
		}
		
		final FilteredImage f = cast(FilteredImage.class, source);
		
		if (f != null) {
			final int tileRowCount = 512;
			final int tileColumnCount = 512;
			final int lastTileRowIndex = sourceRowCount / tileRowCount;
			final int lastTileColumnIndex = sourceColumnCount / tileColumnCount;
			
			for (int tileRowIndex = 0; tileRowIndex <= lastTileRowIndex; ++tileRowIndex) {
				System.out.print(tileRowIndex + "/" + lastTileRowIndex + "\r");
				
				for (int tileColumnIndex = 0; tileColumnIndex <= lastTileColumnIndex; ++tileColumnIndex) {
					for (int rowIndexInTile = 0, rowIndex = tileRowIndex * tileRowCount; rowIndexInTile < tileRowCount && rowIndex < sourceRowCount; ++rowIndexInTile, ++rowIndex) {
						for (int columnIndexInTile = 0, columnIndex = tileColumnIndex * tileColumnCount; columnIndexInTile < tileColumnCount && columnIndex < sourceColumnCount; ++columnIndexInTile, ++columnIndex) {
							if (roi != null && roi.get(rowIndex, columnIndex)) {
								final int colorIndex = this.getColorIndex(source.getValue(rowIndex, columnIndex));
								final int count = this.histogram.getValue(colorIndex) + 1;
								
								this.histogram.setValue(colorIndex, count);
								
								if (maximum < count) {
									maximum = count;
								}
							}
						}
					}
				}
			}
		} else {
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				if (pixel % sourceColumnCount == 0) {
					System.out.print(pixel + "/" + pixelCount + "\r");
				}
				
				if (roi != null && roi.get(pixel)) {
					final int colorIndex = this.getColorIndex(source.getValue(pixel));
					final int count = this.histogram.getValue(colorIndex) + 1;
					
					this.histogram.setValue(colorIndex, count);
					
					if (maximum < count) {
						maximum = count;
					}
				}
			}
		}
		
		this.valueScale.setBounds(0, maximum, 0, 255);
		
		final int histogramSize = this.histogram.getRowCount() * this.histogram.getColumnCount();
		
		for (int i = 0; i < histogramSize; ++i) {
//			debugPrint(i, this.histogram.getValue(i), this.valueScale.getDisplayValue(this.histogram.getValue(i)));
			this.histogram.setValue(i, 255 - this.valueScale.getDisplayValue(this.histogram.getValue(i)));
		}
	}
	
	private final int getColorIndex(final int pixelRawValue) {
		int result = 0;
		
		for (final Channel channel : this.channels) {
			result = result * 256 + channel.getValue(pixelRawValue);
		}
		
		return result;
	}
	
	private final void updateSelectedClusters() {
		Arrays.fill(this.selectedClusters, false);
		
		for (final int cluster : new CommandLineArgumentsParser("clusters", this.getParameters().get("clusters")).get("clusters")) {
			this.selectedClusters[this.indices[cluster]] = true;
		}
	}
	
	public static final void debugPrintHistogram(final Image histogram) {
		System.out.println(debug(DEBUG_STACK_OFFSET + 1));
		
		for (int i = 0; i < 256; ++i) {
			System.out.print(histogram.getValue(i));
			System.out.print(" ");
		}
		
		System.out.println();
	}
	
}
