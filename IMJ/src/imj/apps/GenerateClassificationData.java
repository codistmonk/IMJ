package imj.apps;

import static imj.apps.modules.ShowActions.baseName;
import static imj.database.IMJDatabaseTools.RGB;
import static imj.database.IMJDatabaseTools.checkDatabase;
import static imj.database.IMJDatabaseTools.getPreferredMetric;
import static imj.database.IMJDatabaseTools.loadRegions;
import static imj.database.IMJDatabaseTools.newBKDatabase;
import static imj.database.IMJDatabaseTools.newRGBSampler;
import static imj.database.IMJDatabaseTools.updateNegativeGroups;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.gc;

import imj.Image;
import imj.ImageWrangler;
import imj.IntList;
import imj.apps.GenerateSampleDatabase.Configuration;
import imj.apps.modules.Annotations;
import imj.apps.modules.RegionOfInterest;
import imj.apps.modules.ViewFilter.Channel;
import imj.database.BKSearch.BKDatabase;
import imj.database.BinningQuantizer;
import imj.database.PatchDatabase;
import imj.database.Quantizer;
import imj.database.Sample;
import imj.database.Sampler;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.TicToc;

/**
 * @author codistmonk (creation 2013-06-04)
 */
public final class GenerateClassificationData {
	
	private GenerateClassificationData() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws Exception 
	 */
	public static final void main(final String[] commandLineArguments) throws Exception {
		final Configuration configuration = new Configuration(commandLineArguments);
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final TicToc timer = new TicToc();
		final String[] trainingSet = arguments.get("using", "").split(",");
		final String testImageId = arguments.get("on", "");
		final PatchDatabase<Sample> sampleDatabase = new PatchDatabase<Sample>(Sample.class);
		
		System.out.println("Loading training set... " + new Date(timer.tic()));
		
		for (final String trainingImageId : trainingSet) {
			final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					trainingImageId + configuration.getSuffix()));
			
			try {
				@SuppressWarnings("unchecked")
				final Map<Object, Object> database = (Map<Object, Object>) ois.readObject();
				@SuppressWarnings("unchecked")
				final PatchDatabase<Sample> samples = (PatchDatabase<Sample>) database.get("samples");
				
				for (final Map.Entry<byte[], Sample> entry : samples) {
					final Sample sample = entry.getValue();
					final Sample newSample = sampleDatabase.add(entry.getKey());
					
					newSample.getClasses().addAll(sample.getClasses());
					newSample.incrementCount(sample.getCount());
				}
			} finally {
				ois.close();
			}
		}
		
		updateNegativeGroups(sampleDatabase);
		
		System.out.println("Loading training set done, time: " + timer.toc());
		
		checkDatabase(sampleDatabase);
		
		System.out.println("Processing image... " + new Date(timer.tic()));
		
		final Image image = ImageWrangler.INSTANCE.load(testImageId, configuration.getLod());
		final Quantizer quantizer = new BinningQuantizer();
		final Channel[] channels = RGB;
		
		quantizer.initialize(image, null, channels, configuration.getQuantizationLevel());
		
		final Sample.Collector collector = new Sample.Collector();
		final Sampler sampler = newRGBSampler(configuration.getSamplerClass(), image, quantizer, collector);
		final BKDatabase<Sample> bkDatabase = newBKDatabase(sampleDatabase, getPreferredMetric(sampler));
		
		gc();
		
		debugPrint("imageRowCount:", image.getRowCount(), "imageColumnCount:", image.getColumnCount());
		final int imageRowCount = image.getRowCount();
		final int imageColumnCount = image.getColumnCount();
		final Annotations annotations = Annotations.fromXML(baseName(testImageId) + ".xml");
		final Map<String, RegionOfInterest> classes = new HashMap<String, RegionOfInterest>();
		final Map<String, ConfusionTable> confusionTables = new HashMap<String, ConfusionTable>();
		
		loadRegions(testImageId, configuration.getLod(), imageRowCount, imageColumnCount, annotations, classes);
		
		for (final String key : classes.keySet()) {
			confusionTables.put(key, new ConfusionTable());
		}
		
		configuration.getSegmenter().process(image, new Sampler(image, quantizer, channels, collector) {
			
			private final IntList pixels = new IntList();
			
			@Override
			public final void process(final int pixel) {
				sampler.process(pixel);
				
				this.pixels.add(pixel);
			}
			
			@Override
			public final void finishPatch() {
				sampler.finishPatch();
				
				final Sample sample = bkDatabase.findClosest(collector.getSample());
				
				for (final Map.Entry<String, RegionOfInterest> entry : classes.entrySet()) {
					final String key = entry.getKey();
					final RegionOfInterest groundTruth = classes.get(key);
					final ConfusionTable confusionTable = confusionTables.get(key);
					
					if (sample.getClasses().contains(key)) {
						// sample is positive
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									confusionTable.incrementTruePositive(1L);
								} else {
									confusionTable.incrementFalsePositive(1L);
								}
							}
							
						});
					} else {
						// sample is negative
						
						this.pixels.forEach(new IntList.Processor() {
							
							@Override
							public final void process(final int pixel) {
								if (groundTruth.get(pixel)) {
									confusionTable.incrementFalseNegative(1L);
								} else {
									confusionTable.incrementTrueNegative(1L);
								}
							}
							
						});
					}
				}
				
				this.pixels.clear();
			}
			
		});
		
		System.out.println("Processing image done, time: " + timer.toc());
		System.out.println("confusionTables: " + confusionTables);
	}
	
	/**
	 * @author codistmonk (creation 2013-06-04)
	 */
	public static final class ConfusionTable {
		
		private long truePositive;
		
		private long falsePositive;
		
		private long trueNegative;
		
		private long falseNegative;
		
		public final long getTruePositive() {
			return this.truePositive;
		}
		
		public final long getFalsePositive() {
			return this.falsePositive;
		}
		
		public final long getTrueNegative() {
			return this.trueNegative;
		}
		
		public final long getFalseNegative() {
			return this.falseNegative;
		}
		
		public final void incrementTruePositive(final long delta) {
			this.truePositive += delta;
		}
		
		public final void incrementFalsePositive(final long delta) {
			this.falsePositive += delta;
		}
		
		public final void incrementTrueNegative(final long delta) {
			this.trueNegative += delta;
		}
		
		public final void incrementFalseNegative(final long delta) {
			this.falseNegative += delta;
		}
		
		@Override
		public final String toString() {
			return "{TP: " + this.getTruePositive() + ", FP: " + this.getFalsePositive() +
					", TN: " + this.getTrueNegative() + ", FN: " + this.getFalseNegative() + "}";
		}
		
	}
	
}
