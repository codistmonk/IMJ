package imj.apps.modules;

import static imj.IMJTools.forEachPixelInEachTile;
import static imj.apps.modules.ViewFilter.Channel.Primitive.BLUE;
import static imj.apps.modules.ViewFilter.Channel.Primitive.GREEN;
import static imj.apps.modules.ViewFilter.Channel.Primitive.RED;
import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static net.sourceforge.aprog.tools.Tools.usedMemory;
import imj.IMJTools.PixelProcessor;
import imj.Image;
import imj.clustering.Distance;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class HistogramDatabaseClusterViewFilter extends ViewFilter {
	
	private final Channel[] channels;
	
	private String databaseFile;
	
	private HistogramDatabaseClusterer clusterer;
	
	private final Map<String, Integer> classes;
	
	private Image source;
	
	private int verticalTileCount;
	
	private int horizontalTileCount;
	
	private int channelBinningBitCount;
	
	private int[] clusters;
	
	private int clusterCount;
	
	public HistogramDatabaseClusterViewFilter(final Context context) {
		super(context);
		this.channels = array(RED, GREEN, BLUE);
		this.classes = new HashMap<String, Integer>();
		
		this.getParameters().clear();
		
		this.getParameters().put("tiling", "16");
//		this.getParameters().put("shift", "3");
		this.getParameters().put("database", "db.jo");
		this.getParameters().put("references", "*");
	}
	
	@Override
	protected final void sourceImageChanged() {
		this.updateParameters();
		debugPrint();
		this.updateClusters();
	}
	
	@Override
	protected final void doInitialize() {
		if (this.updateParameters()) {
			debugPrint();
			this.updateClusters();
		}
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				return HistogramDatabaseClusterViewFilter.this.getClusterColor(index);
			}
			
		};
	}
	
	final int getClusterColor(final int pixel) {
		return 0 < this.clusterCount ? 0xFF000000 | (0x00FFFFFF * (1 + this.getClusterIndex(pixel)) / this.clusterCount) : 0;
	}
	
	private final boolean updateParameters() {
		final int oldVerticalTileCount = this.verticalTileCount;
		final String[] tiling = this.getParameters().get("tiling").split("\\s+");
		this.verticalTileCount = parseInt(tiling[0]);
		this.horizontalTileCount = this.verticalTileCount;
		
		this.updateSource();
		
		final int oldChannelBinningBitCount = this.channelBinningBitCount;
		final String oldDatabaseFile = this.databaseFile;
		
		this.databaseFile = this.getParameters().get("database");
		
		if (!Tools.equals(oldDatabaseFile, this.databaseFile)) {
			debugPrint(oldDatabaseFile, this.databaseFile);
			
			try {
				final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.databaseFile));
				
				try {
					this.clusterer = new HistogramDatabaseClusterer((Map<Object, Object>) ois.readObject());
					final String[] classes = this.clusterer.getClasses();
					final int n = classes.length;
					this.clusterCount = n;
					
					this.classes.clear();
					this.classes.put(null, 0);
					
					for (int i = 0; i < n; ++i) {
						this.classes.put(classes[i], i + 1);
					}
					
					this.channelBinningBitCount = 8 - this.clusterer.getShift();
				} finally {
					ois.close();
				}
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		return oldVerticalTileCount != this.verticalTileCount ||
				oldChannelBinningBitCount != this.channelBinningBitCount ||
				!Tools.equals(oldDatabaseFile, this.databaseFile);
	}
	
	private final void updateSource() {
		this.source = this.getImage().getSource();
		
		if (this.source != null) {
			final int sourceRowCount = this.source.getRowCount();
			final int sourceColumnCount = this.source.getColumnCount();
			this.horizontalTileCount = sourceColumnCount / (sourceRowCount / this.verticalTileCount);
		}
	}
	
	private final void updateClusters() {
		if (this.source != null) {
			final TicToc timer = new TicToc();
			
			debugPrint("Collecting data...", "(" + new Date(timer.tic()) + ")");
			
			final PixelProcessor collector;
			final int sourceRowCount = this.source.getRowCount();
			final int sourceColumnCount = this.source.getColumnCount();
			final int tileRowCount = sourceRowCount / this.verticalTileCount;
			final int tileColumnCount = sourceColumnCount / this.horizontalTileCount;
			final int tileSize = tileRowCount * tileColumnCount;
			final int tileCount = (sourceRowCount / tileRowCount) * (sourceColumnCount / tileColumnCount);
			this.clusters = new int[tileCount];
			final int channelCount = this.channels.length;
			final long[] processed = { 0L };
			
			{
				final int colorCount = 1 << (this.channelBinningBitCount * channelCount);
				final float[] sample = new float[colorCount];
				collector = new PixelProcessor() {
					
					private int i;
					
					private int sampleIndex;
					
					@Override
					public final void process(final int pixel) {
						++processed[0];
						HistogramDatabaseClusterViewFilter.this.updateHistogram(pixel, sample);
						
						if (++this.i == tileSize) {
							for (int j = 0; j < colorCount; ++j) {
								sample[j] /= tileSize;
							}
							
							HistogramDatabaseClusterViewFilter.this.classify(sample, this.sampleIndex++);
							this.i = 0;
							fill(sample, 0);
						}
					}
					
				};
			}
			
			forEachPixelInEachTile(this.source, this.verticalTileCount, this.horizontalTileCount, collector);
			
			debugPrint(processed[0], "/", this.source.getRowCount() * this.source.getColumnCount());
			
			debugPrint(Arrays.toString(this.clusterer.getClasses()));
			debugPrint(Arrays.toString(this.clusters));
			
			debugPrint("Collecting data done", "(time:", timer.toc(), "memory:", usedMemory() + ")");
		}
	}
	
	final void classify(final float[] sample, final int sampleIndex) {
		final int sampleLOD = this.getContext().get("lod");
		final String sampleClass = this.clusterer.getCluster(sample, sampleLOD);
		
		this.clusters[sampleIndex] = this.classes.get(sampleClass);
	}
	
	final void updateHistogram(final int pixel, final float[] histogram) {
		++histogram[this.getColorIndex(pixel, this.source.getValue(pixel))];
	}
	
	private final int getClusterIndex(final int pixel) {
		final int sourceRowCount = this.source.getRowCount();
		final int sourceColumnCount = this.source.getColumnCount();
		final int pixelRowIndex = pixel / sourceColumnCount;
		final int pixelColumnIndex = pixel % sourceColumnCount;
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
	
	/**
	 * @author codistmonk (creation 2013-04-11)
	 */
	public static final class HistogramDatabaseClusterer {
		
		private final Map<Object, Object> database;
		
		private final Map<String, double[]> classRadii;
		
		public HistogramDatabaseClusterer(final Map<Object, Object> database) {
			this.database = database;
			this.classRadii = new LinkedHashMap<String, double[]>();
			final List<Object[]> table = (List<Object[]>) database.get("table");
			final int n = table.size();
			
			for (int i = 0; i < n; ++i) {
				final Object[] rowI = table.get(i);
				final String classI = (String) rowI[0];
				final int lodI = (Integer) rowI[2];
				final float[] referenceI = (float[]) rowI[3];
				double[] classRadius = this.classRadii.get(classI);
				
				if (classRadius == null) {
					classRadius = new double[lodI + 1];
				} else if (classRadius.length <= lodI) {
					classRadius = copyOf(classRadius, lodI + 1);
				}
				
				for (int j = i + 1; j < n; ++j) {
					final Object[] rowJ = table.get(j);
					final String classJ = (String) rowJ[0];
					final int lodJ = (Integer) rowJ[2];
					
					if (classI.equals(classJ) && lodI == lodJ) {
						final float[] referenceJ = (float[]) rowJ[3];
						final double distance = this.getDistance(referenceI, referenceJ) / 2.0;
						
						if (classRadius[lodI] < distance) {
							classRadius[lodI] = distance;
						}
					}
				}
				
				this.classRadii.put(classI, classRadius);
			}
			
			for (final Map.Entry<String, double[]> entry : this.classRadii.entrySet()) {
				debugPrint(entry.getKey(), Arrays.toString(entry.getValue()));
			}
		}
		
		public final int getShift() {
			return (Integer) this.database.get("shift");
		}
		
		public final String[] getClasses() {
			return this.classRadii.keySet().toArray(new String[this.classRadii.size()]);
		}
		
		public final String getCluster(final float[] sample, final int sampleLOD) {
			double closestDistance = Double.POSITIVE_INFINITY;
			String closestClass = null;
			final List<Object[]> table = (List<Object[]>) this.database.get("table");
			
			for (final Object[] row : table) {
				final int referenceLOD = (Integer) row[2];
				
				if (sampleLOD == referenceLOD) {
					final String referenceClass = (String) row[0];
					final double classRadius = this.classRadii.get(referenceClass)[sampleLOD];
					final float[] reference = (float[]) row[3];
					
					assert sample.length == reference.length;
					
					final double distance = this.getDistance(sample, reference);
					
					if (distance <= classRadius && distance < closestDistance) {
						closestDistance = distance;
						closestClass = referenceClass;
					}
				}
			}
			
			return closestClass;
		}
		
		private final double getDistance(final float[] sample1, final float[] sample2) {
			return Distance.Predefined.EUCLIDEAN.getDistance(sample1, sample2);
		}
		
	}
	
}
