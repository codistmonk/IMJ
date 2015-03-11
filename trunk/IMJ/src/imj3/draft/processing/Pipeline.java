package imj3.draft.processing;

import static imj3.draft.machinelearning.Datum.Default.datum;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.join;
import static net.sourceforge.aprog.tools.Tools.last;
import static net.sourceforge.aprog.tools.Tools.unchecked;

import imj3.core.Image2D;
import imj3.draft.machinelearning.BufferedDataSource;
import imj3.draft.machinelearning.Classifier;
import imj3.draft.machinelearning.DataSource;
import imj3.draft.machinelearning.Datum;
import imj3.draft.machinelearning.FilteredCompositeDataSource;
import imj3.draft.machinelearning.Measure;
import imj3.draft.machinelearning.MedianCutClustering;
import imj3.draft.machinelearning.NearestNeighborClassifier;
import imj3.draft.machinelearning.Measure.Predefined;
import imj3.draft.processing.Image2DSource.PatchIterator;
import imj3.tools.AwtImage2D;
import imj3.tools.CommonTools;
import imj3.tools.CommonSwingTools.NestedList;
import imj3.tools.CommonSwingTools.PropertyGetter;
import imj3.tools.CommonSwingTools.PropertyOrdering;
import imj3.tools.CommonSwingTools.PropertySetter;
import imj3.tools.CommonSwingTools.StringGetter;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-16)
 */
@PropertyOrdering({ "classes", "training", "algorithms" })
public final class Pipeline implements Serializable {
	
	private List<Pipeline.TrainingField> trainingFields;
	
	private List<Algorithm> algorithms;
	
	private List<Pipeline.ClassDescription> classDescriptions;
	
	private long trainingMilliseconds;
	
	private Map<Integer, Map<Integer, AtomicLong>> trainingConfusionMatrix;
	
	private long classificationMilliseconds;
	
	private Map<Integer, Map<Integer, AtomicLong>> classificationConfusionMatrix;
	
	@NestedList(name="classes", element="class", elementClass=Pipeline.ClassDescription.class)
	public final List<Pipeline.ClassDescription> getClassDescriptions() {
		if (this.classDescriptions == null) {
			this.classDescriptions = new ArrayList<>();
		}
		
		return this.classDescriptions;
	}
	
	@NestedList(name="training", element="training field", elementClass=Pipeline.TrainingField.class)
	public final List<Pipeline.TrainingField> getTrainingFields() {
		if (this.trainingFields == null) {
			this.trainingFields = new ArrayList<>();
		}
		
		return this.trainingFields;
	}
	
	@NestedList(name="algorithms", element="algorithm", elementClass=Algorithm.class)
	public final List<Algorithm> getAlgorithms() {
		if (this.algorithms == null) {
			this.algorithms = new ArrayList<>();
		}
		
		return this.algorithms;
	}
	
	public final Map<Integer, Map<Integer, AtomicLong>> getTrainingConfusionMatrix() {
		if (this.trainingConfusionMatrix == null) {
			this.trainingConfusionMatrix = new HashMap<>();
		}
		
		return this.trainingConfusionMatrix;
	}
	
	public final Map<Integer, Map<Integer, AtomicLong>> getClassificationConfusionMatrix() {
		if (this.classificationConfusionMatrix == null) {
			this.classificationConfusionMatrix = new HashMap<>();
		}
		
		return this.classificationConfusionMatrix;
	}
	
	public final long getTrainingMilliseconds() {
		return this.trainingMilliseconds;
	}
	
	public final long getClassificationMilliseconds() {
		return this.classificationMilliseconds;
	}
	
	public final Pipeline train(final String groundTruthName) {
		Tools.debugPrint("Starting training");
		
		final TicToc timer = new TicToc();
		final List<ParameterTraining> parameterTrainings = new ArrayList<>();
		
		for (final Algorithm algorithm : this.getAlgorithms()) {
			for (final Method method : algorithm.getClass().getMethods()) {
				final Trainable trainable = method.getAnnotation(Trainable.class);
				
				if (trainable != null) {
					ParameterTraining.addTo(parameterTrainings, algorithm, method);
				}
			}
		}
		
		final int n = parameterTrainings.size();
		
		Tools.debugPrint(n);
		
		Classifier[] bestClassifiers = null;
		int[] bestParameters = new int[n];
		Map<Integer, Map<Integer, AtomicLong>> bestConfusionMatrix = null;
		double bestScore = 0.0;
		
		optimize_parameters:
		while (true) {
			parameterTrainings.forEach(ParameterTraining::set);
			
			train1(groundTruthName);
			
			final double score = f1(this.getTrainingConfusionMatrix());
			
			if (bestScore < score) {
				bestScore = score;
				Tools.debugPrint(bestConfusionMatrix);
				bestConfusionMatrix = new HashMap<>(this.getTrainingConfusionMatrix());
				Tools.debugPrint(bestConfusionMatrix);
				bestParameters = parameterTrainings.stream().mapToInt(ParameterTraining::getCurrentIndex).toArray();
				bestClassifiers = this.getAlgorithms().stream().map(Algorithm::getClassifier).toArray(Classifier[]::new);
			}
			
			for (int i = n - 1; 0 <= i; --i) {
				final ParameterTraining parameterTraining = parameterTrainings.get(i);
				
				if (parameterTraining.hasNext()) {
					parameterTraining.next();
					break;
				}
				
				parameterTraining.setCurrentIndex(0);
				
				if (i == 0) {
					break optimize_parameters;
				}
			}
		}
		
		{
			for (int i = 0; i < n; ++i) {
				parameterTrainings.get(i).setCurrentIndex(bestParameters[i]);
			}
			
			this.getTrainingConfusionMatrix().clear();
			this.getTrainingConfusionMatrix().putAll(bestConfusionMatrix);
			
			{
				final int m = bestClassifiers.length;
				
				for (int i = 0; i < m; ++i) {
					this.getAlgorithms().get(i).setClassifier(bestClassifiers[i]);
				}
			}
		}
		
		this.trainingMilliseconds = timer.toc();
		
		Tools.debugPrint("Training done in", this.trainingMilliseconds, "ms");
		
		return this;
	}
	
	private final void train1(final String groundTruthName) {
		final SupervisedAlgorithm supervisedLast = this.getAlgorithms().isEmpty() ?
				null : cast(SupervisedAlgorithm.class, last(this.getAlgorithms()));
		@SuppressWarnings("unchecked")
		final List<ConcreteTrainingField>[] in = array(new ArrayList<>());
		@SuppressWarnings("unchecked")
		final List<ConcreteTrainingField>[] out = array(new ArrayList<>());
		
		this.getTrainingFields().forEach(f -> {
			Tools.debugPrint(f.getImagePath());
			
			final Image2D image = VisualAnalysis.read(f.getImagePath());
			final Image2D labels = VisualAnalysis.read(getGroundTruthPathFromImagePath(f.getImagePath(), groundTruthName));
			
			out[0].add(new ConcreteTrainingField(image, labels, f.getBounds()));
		});
		
		for (final Algorithm algorithm : this.getAlgorithms()) {
			CommonTools.swap(in, 0, out, 0);
			
			this.getTrainingConfusionMatrix().clear();
			
			final int patchSize = algorithm.getPatchSize();
			final int patchSparsity = algorithm.getPatchSparsity();
			final int stride = algorithm.getStride();
			final boolean usingXY = algorithm.isUsingXY();
			final FilteredCompositeDataSource unbufferedTrainingSet = new FilteredCompositeDataSource(c -> {
				return c.getPrototype().getValue()[0] != 0.0;
			});
			
			in[0].forEach(f -> {
				final Image2D image = f.getImage();
				final Image2D labels = algorithm instanceof SupervisedAlgorithm ? f.getLabels() : null;
				final Image2DRawSource source = new Image2DRawSource(image, labels,
						patchSize, patchSparsity, stride, usingXY);
				
				source.getBounds().setBounds(f.getBounds());
				
				unbufferedTrainingSet.add(source);
			});
			
			final DataSource trainingSet = BufferedDataSource.buffer(unbufferedTrainingSet);
			final Classifier classifier = algorithm.train(trainingSet).getClassifier();
			
			out[0].clear();
			
			final int n = trainingSet.getClassDimension();
			
			in[0].forEach(f -> {
				final Image2D image = f.getImage();
				final Image2D labels = f.getLabels();
				final Image2DRawSource source = new Image2DRawSource(image, labels,
						patchSize, patchSparsity, stride, usingXY);
				
				source.getBounds().setBounds(f.getBounds());
				
				final Image2D newImage = new DoubleImage2D(image.getId() + "_out",
						Patch2DSource.newSize(f.getBounds().x, f.getBounds().width, patchSize, stride),
						Patch2DSource.newSize(f.getBounds().y, f.getBounds().height, patchSize, stride), n);
				final Image2D newLabels = new UnsignedImage2D(labels.getId() + "_tmp",
						newImage.getWidth(), newImage.getHeight());
				final Datum classification = datum();
				int targetPixel = -1;
				
				for (final PatchIterator i = source.iterator(); i.hasNext();) {
					final int sourceX = i.getX();
					final int sourceY = i.getY();
					final int expectedLabel = (int) labels.getPixelValue(sourceX, sourceY);
					
					classifier.classify(i.next(), classification);
					newImage.setPixelValue(++targetPixel, classification.getPrototype().getValue());
					newLabels.setPixelValue(targetPixel, expectedLabel);
					
					if (algorithm == supervisedLast && expectedLabel != 0) {
						final int actualLabel = classification.getPrototype().getIndex();
						
						this.getTrainingConfusionMatrix().computeIfAbsent(
								expectedLabel, e -> new HashMap<>()).computeIfAbsent(
										actualLabel, a -> new AtomicLong()).incrementAndGet();
					}
				}
				
				out[0].add(new ConcreteTrainingField(newImage, newLabels));
			});
		}
	}
	
	public final Pipeline classify(final BufferedImage inputImage, final BufferedImage labels, final BufferedImage classification) {
		Tools.debugPrint("Starting classification");
		
		final TicToc timer = new TicToc();
		final Image2D image = new AwtImage2D(null, inputImage);
		final Algorithm last = this.getAlgorithms().isEmpty() ? null : last(this.getAlgorithms());
		Image2D actualLabels = null;
		
		// TODO process as tiles to reduce memory usage
		{
			Image2D tmp = image;
			
			this.getClassificationConfusionMatrix().clear();
			
			for (final Algorithm algorithm : this.getAlgorithms()) {
				final int patchSize = algorithm.getPatchSize();
				final int patchSparsity = algorithm.getPatchSparsity();
				final int stride = algorithm.getStride();
				final Image2DRawSource unbufferedInputs = new Image2DRawSource(tmp, null,
						patchSize, patchSparsity, stride, algorithm.isUsingXY());
				final Rectangle bounds = unbufferedInputs.getBounds();
				final DataSource inputs = unbufferedInputs;
				final Classifier classifier = algorithm.getClassifier();
				Tools.debugPrint(algorithm.hashCode(), patchSize, patchSparsity, stride);
				Tools.debugPrint(((NearestNeighborClassifier) classifier).getPrototypes().size());
				final int n = classifier.getClassDimension(inputs.getInputDimension());
				final Image2D newImage = new DoubleImage2D(image.getId() + "_out",
						Patch2DSource.newSize(bounds.x, bounds.width, patchSize, stride),
						Patch2DSource.newSize(bounds.y, bounds.height, patchSize, stride), n);
				
				if (algorithm == last) {
					actualLabels = new UnsignedImage2D(image.getId() + "_outLabels", newImage.getWidth(), newImage.getHeight());
				}
				
				final Datum c = datum();
				int targetPixel = -1;
				
				for (final Datum input : inputs) {
					classifier.classify(input, c);
					
					newImage.setPixelValue(++targetPixel, c.getPrototype().getValue());
					
					if (actualLabels != null) {
						actualLabels.setPixelValue(targetPixel, c.getPrototype().getIndex());
					}
				}
				
				tmp = newImage;
			}
		}
		
		if (actualLabels != null) {
			final Image2D expectedLabels =  labels == null ? null : new AwtImage2D(null, labels);
			final int right = classification.getWidth() - 1;
			final int bottom = classification.getHeight() - 1;
			final int r = actualLabels.getWidth() - 1;
			final int b = actualLabels.getHeight() - 1;
			
			for (int y = 0; y <= bottom; ++y) {
				for (int x = 0; x <= right; ++x) {
					final int actualLabel = (int) actualLabels.getPixelValue(x * r / right, y * b / bottom);
					
					classification.setRGB(x, y, actualLabel);
					
					if (expectedLabels != null) {
						final int expectedLabel = (int) expectedLabels.getPixelValue(x, y);
						
						if (expectedLabel != 0) {
							this.getClassificationConfusionMatrix().computeIfAbsent(
									expectedLabel, e -> new HashMap<>()).computeIfAbsent(
											actualLabel, a -> new AtomicLong()).incrementAndGet();
						}
					}
				}
			}
		}
		
		this.classificationMilliseconds = timer.toc();
		
		Tools.debugPrint("Classification done in", this.classificationMilliseconds, "ms");
		
		return this;
	}
	
	public final Map<String, Integer> getClassLabels() {
		final Map<String, Integer> result = new LinkedHashMap<>();
		
		this.getClassDescriptions().forEach(c -> result.put(c.getName(), c.getLabel()));
		
		return result;
	}
	
	@Override
	public final String toString() {
		return "Pipeline";
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	@PropertyOrdering({ "patchSize", "patchSizeRange", "patchSparsity", "patchSparsityRange", "stride", "strideRange", "usingXY", "usingXYRange", "classifier" })
	public abstract class Algorithm implements Serializable {
		
		private String clusteringName = MedianCutClustering.class.getName();
		
		private Classifier classifier;
		
		private int patchSize = 1;
		
		private String patchSizeRange;
		
		private int patchSparsity = 1;
		
		private String patchSparsityRange;
		
		private int stride = 1;
		
		private String strideRange;
		
		private boolean usingXY = false;
		
		private String usingXYRange;
		
		@PropertyGetter("clustering")
		public final String getClusteringName() {
			return this.clusteringName;
		}
		
		@PropertySetter("clustering")
		public final Algorithm setClusteringName(final String clusteringName) {
			this.clusteringName = clusteringName;
			
			return this;
		}
		
		public final int getPatchSize() {
			return this.patchSize;
		}
		
		public final Algorithm setPatchSize(final int patchSize) {
			this.patchSize = patchSize;
			
			return this;
		}
		
		@PropertyGetter("patchSize")
		public final String getPatchSizeAsString() {
			return Integer.toString(this.getPatchSize());
		}
		
		@PropertySetter("patchSize")
		@Trainable("patchSizeRange")
		public final Algorithm setPatchSize(final String patchSizeAsString) {
			return this.setPatchSize(Integer.parseInt(patchSizeAsString));
		}
		
		@PropertyGetter("patchSizeRange")
		public final String getPatchSizeRange() {
			if (this.patchSizeRange == null) {
				this.patchSizeRange = "1";
			}
			
			return this.patchSizeRange;
		}
		
		@PropertySetter("patchSizeRange")
		public final Algorithm setPatchSizeRange(final String patchSizeRange) {
			this.patchSizeRange = patchSizeRange;
			
			return this;
		}
		
		public final int getPatchSparsity() {
			return this.patchSparsity;
		}
		
		public final Algorithm setPatchSparsity(final int patchSparsity) {
			this.patchSparsity = patchSparsity;
			
			return this;
		}
		
		@PropertyGetter("patchSparsity")
		public final String getPatchSparsityAsString() {
			return Integer.toString(this.getPatchSparsity());
		}
		
		@PropertySetter("patchSparsity")
		@Trainable("patchSparsityRange")
		public final Algorithm setPatchSparsity(final String patchSparsityAsString) {
			return this.setPatchSparsity(Integer.parseInt(patchSparsityAsString));
		}
		
		@PropertyGetter("patchSparsityRange")
		public final String getPatchSparsityRange() {
			if (this.patchSparsityRange == null) {
				this.patchSparsityRange = "1";
			}
			
			return this.patchSparsityRange;
		}
		
		@PropertySetter("patchSparsityRange")
		public final Algorithm setPatchSparsityRange(final String patchSparsityRange) {
			this.patchSparsityRange = patchSparsityRange;
			
			return this;
		}
		
		public final int getStride() {
			return this.stride;
		}
		
		public final Algorithm setStride(final int stride) {
			this.stride = stride;
			
			return this;
		}
		
		@PropertyGetter("stride")
		public final String getStrideAsString() {
			return Integer.toString(this.getStride());
		}
		
		@PropertySetter("stride")
		@Trainable("strideRange")
		public final Algorithm setStride(final String strideAsString) {
			return this.setStride(Integer.parseInt(strideAsString));
		}
		
		@PropertyGetter("strideRange")
		public final String getStrideRange() {
			if (this.strideRange == null) {
				this.strideRange = "1";
			}
			
			return this.strideRange;
		}
		
		@PropertySetter("strideRange")
		public final Algorithm setStrideRange(final String strideRange) {
			this.strideRange = strideRange;
			
			return this;
		}
		
		public final boolean isUsingXY() {
			return this.usingXY;
		}
		
		public final Algorithm setUsingXY(final boolean usingXY) {
			this.usingXY = usingXY;
			
			return this;
		}
		
		@PropertyGetter("usingXY")
		public final String getUsingXYAsString() {
			return this.isUsingXY() ? "1" : "0";
		}
		
		@PropertySetter("usingXY")
		@Trainable("usingXYRange")
		public final Algorithm setUsingXY(final String usingXYAsString) {
			return this.setUsingXY(Integer.parseInt(usingXYAsString) != 0);
		}
		
		@PropertyGetter("usingXYRange")
		public final String getUsingXYRange() {
			if (this.usingXYRange == null) {
				this.usingXYRange = "0";
			}
			
			return this.usingXYRange;
		}
		
		@PropertySetter("usingXYRange")
		public final Algorithm setUsingXYRange(final String usingXYRange) {
			this.usingXYRange = usingXYRange;
			
			return this;
		}
		
		public final Classifier getClassifier() {
			return this.classifier;
		}
		
		public final Algorithm setClassifier(final Classifier classifier) {
			this.classifier = classifier;
			
			return this;
		}
		
		public final Pipeline getPipeline() {
			return Pipeline.this;
		}
		
		public abstract int getClassCount();
		
		public abstract Algorithm train(DataSource trainingSet);
		
		@Override
		public final String toString() {
			final String classifierName = this.getClusteringName();
			final String suffix = this instanceof UnsupervisedAlgorithm ? " (unsupervised: " + this.getClassCount() + ")" : "";
			
			return classifierName.substring(classifierName.lastIndexOf('.') + 1) + suffix;
		}
		
		private static final long serialVersionUID = 7689582280746561160L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-24)
	 */
	public final class UnsupervisedAlgorithm extends Algorithm {
		
		private int classCount;
		
		@Override
		public final int getClassCount() {
			return this.classCount;
		}
		
		public final UnsupervisedAlgorithm setClassCount(final int classCount) {
			this.classCount = classCount;
			
			return this;
		}
		
		@PropertyGetter("classCount")
		public final String getClassCountAsString() {
			return Integer.toString(this.getClassCount());
		}
		
		@PropertySetter("classCount")
		public final UnsupervisedAlgorithm setClassCount(final String classCountAsString) {
			return this.setClassCount(Integer.parseInt(classCountAsString));
		}
		
		@Override
		public final UnsupervisedAlgorithm train(final DataSource trainingSet) {
			return (UnsupervisedAlgorithm) this.setClassifier(new MedianCutClustering(
					Measure.Predefined.L2_ES, this.getClassCount()).cluster(trainingSet).updatePrototypeIndices());
		}
		
		private static final long serialVersionUID = 130550869712582710L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-24)
	 */
	@PropertyOrdering({ "prototypes", "prototypeRanges" })
	public final class SupervisedAlgorithm extends Algorithm {
		
		private Map<String, Integer> prototypeCounts;
		
		private Map<String, String> prototypeCountRanges;
		
		@Trainable("prototypeRanges")
		public final Map<String, Integer> getPrototypeCounts() {
			if (this.prototypeCounts == null) {
				this.prototypeCounts = new LinkedHashMap<>();
			}
			
			{
				final Collection<String> classes = new LinkedHashSet<>();
				
				for (final Pipeline.ClassDescription classDescription : this.getPipeline().getClassDescriptions()) {
					final String name = classDescription.getName();
					
					classes.add(name);
					
					if (!this.prototypeCounts.containsKey(name)) {
						this.prototypeCounts.put(name, 1);
					}
				}
				
				this.prototypeCounts.keySet().retainAll(classes);
			}
			
			return this.prototypeCounts;
		}
		
		@PropertyGetter("prototypes")
		public final String getPrototypeCountsAsString() {
			final String string = this.getPrototypeCounts().toString();
			
			return string.substring(1, string.length() - 1).replace(',', ';');
		}
		
		@PropertySetter("prototypes")
		public final SupervisedAlgorithm setPrototypeCounts(final String prototypeCountsAsString) {
			final Map<String, Integer> prototypeCounts = this.getPrototypeCounts();
			final Map<String, Integer> tmp = new HashMap<>();
			
			for (final String keyValue : prototypeCountsAsString.split(";")) {
				final String[] keyAndValue = keyValue.split("=");
				final String key = keyAndValue[0].trim();
				final int value = Integer.parseInt(keyAndValue[1].trim());
				
				if (!prototypeCounts.containsKey(key)) {
					throw new IllegalArgumentException();
				}
				
				tmp.put(key, value);
			}
			
			if (!tmp.keySet().containsAll(prototypeCounts.keySet())) {
				throw new IllegalArgumentException();
			}
			
			this.prototypeCounts.putAll(tmp);
			
			return this;
		}
		
		public final Map<String, String> getPrototypeCountRanges() {
			if (this.prototypeCountRanges == null) {
				this.prototypeCountRanges = new LinkedHashMap<>();
			}
			
			{
				final Collection<String> classes = new LinkedHashSet<>();
				
				for (final Pipeline.ClassDescription classDescription : this.getPipeline().getClassDescriptions()) {
					final String name = classDescription.getName();
					
					classes.add(name);
					
					if (!this.prototypeCountRanges.containsKey(name)) {
						this.prototypeCountRanges.put(name, "1");
					}
				}
				
				this.prototypeCountRanges.keySet().retainAll(classes);
			}
			
			return this.prototypeCountRanges;
		}
		
		@PropertyGetter("prototypeRanges")
		public final String getPrototypeCountRangesAsString() {
			return Tools.join("; ", this.getPrototypeCountRanges().entrySet().toArray());
		}
		
		@PropertySetter("prototypeRanges")
		public final SupervisedAlgorithm setPrototypeCountRanges(final String prototypeCountRangesAsString) {
			final Map<String, String> prototypeCountRanges = this.getPrototypeCountRanges();
			final Map<String, String> tmp = parseRangeMap(prototypeCountRangesAsString, prototypeCountRanges.keySet());
			
			if (!tmp.keySet().containsAll(prototypeCountRanges.keySet())) {
				throw new IllegalArgumentException();
			}
			
			this.prototypeCountRanges.putAll(tmp);
			
			return this;
		}
		
		@Override
		public final int getClassCount() {
			return this.getPipeline().getClassDescriptions().size();
		}
		
		@Override
		public final SupervisedAlgorithm train(final DataSource trainingSet) {
			final Predefined measure = Measure.Predefined.L2_ES;
			final NearestNeighborClassifier classifier = new NearestNeighborClassifier(measure);
			final Map<String, Integer> classLabels = this.getPipeline().getClassLabels();
			
			for (final Map.Entry<String, Integer> entry : this.getPrototypeCounts().entrySet()) {
				final int classLabel = classLabels.get(entry.getKey());
				final NearestNeighborClassifier subClassifier = new MedianCutClustering(
						measure, entry.getValue()).cluster(new FilteredCompositeDataSource(
								c -> classLabel == (int) c.getPrototype().getValue()[0]).add(trainingSet));
				
				for (final Datum prototype : subClassifier.getPrototypes()) {
					classifier.getPrototypes().add(prototype.setIndex(classLabel));
				}
			}
			
			return (SupervisedAlgorithm) this.setClassifier(classifier);
		}
		
		private static final long serialVersionUID = 6887222324834498847L;
		
	}
	
	private static final long serialVersionUID = -4539259556658072410L;
	
	public static final Map<String, String> parseRangeMap(final String mapAsString, final Collection<String> keys) {
		final Map<String, String> tmp = new HashMap<>();
		
		for (final String keyValue : mapAsString.split(";")) {
			final String[] keyAndValue = keyValue.split("=");
			final String key = keyAndValue[0].trim();
			final String value = keyAndValue[1].trim();
			
			if (!keys.contains(key)) {
				throw new IllegalArgumentException();
			}
			
			tmp.put(key, value);
		}
		
		return tmp;
	}
	
	public static final String getGroundTruthPathFromImagePath(final String imagePath, final String groundTruthName) {
		return baseName(imagePath) + "_groundtruth_" + groundTruthName + ".png";
	}
	
	public static final <K, N extends Number> double f1(final Map<K, Map<K, N>> confusionMatrix) {
		double numerator = 0.0;
		double denominator = 0.0;
		
		for (final Map.Entry<K, Map<K, N>> entry : confusionMatrix.entrySet()) {
			final Object expectedKey = entry.getKey();
			
			for (final Map.Entry<K, N> subEntry : entry.getValue().entrySet()) {
				final Object actualKey = subEntry.getKey();
				final double count = subEntry.getValue().doubleValue();
				
				if (expectedKey.equals(actualKey)) {
					numerator += count;
				} else {
					denominator += count;
				}
			}
		}
		
		denominator += (numerator *= 2.0);
		
		return denominator == 0.0 ? 0.0 : numerator / denominator;
	}
	
	/**
	 * @author codistmonk (creation 2015-03-09)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	public static abstract @interface Trainable {
		
		public abstract String value();
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-16)
	 */
	@PropertyOrdering({ "name", "label" })
	public static final class ClassDescription implements Serializable {
		
		private String name = "class";
		
		private int label = 0xFF000000;
		
		@StringGetter
		@PropertyGetter("name")
		public final String getName() {
			return this.name;
		}
		
		@PropertySetter("name")
		public final Pipeline.ClassDescription setName(final String name) {
			this.name = name;
			
			return this;
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final Pipeline.ClassDescription setLabel(final int label) {
			this.label = label;
			
			return this;
		}
		
		@PropertyGetter("label")
		public final String getLabelAsString() {
			return "#" + Integer.toHexString(this.getLabel()).toUpperCase(Locale.ENGLISH);
		}
		
		@PropertySetter("label")
		public final Pipeline.ClassDescription setLabel(final String labelAsString) {
			return this.setLabel((int) Long.parseLong(labelAsString.substring(1), 16));
		}
		
		private static final long serialVersionUID = 4974707407567297906L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-17)
	 */
	@PropertyOrdering({ "image", "bounds" })
	public static final class TrainingField implements Serializable {
		
		private String imagePath = "";
		
		private final Rectangle bounds = new Rectangle();
		
		@PropertyGetter("image")
		public final String getImagePath() {
			return this.imagePath;
		}
		
		@PropertySetter("image")
		public final Pipeline.TrainingField setImagePath(final String imagePath) {
			this.imagePath = imagePath;
			
			return this;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		@PropertyGetter("bounds")
		public final String getBoundsAsString() {
			return join(",", this.getBounds().x, this.getBounds().y, this.getBounds().width, this.getBounds().height);
		}
		
		@PropertySetter("bounds")
		public final Pipeline.TrainingField setBounds(final String boundsAsString) {
			final int[] bounds = Arrays.stream(boundsAsString.split(",")).mapToInt(Integer::parseInt).toArray();
			
			this.getBounds().setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			
			return this;
		}
		
		@Override
		public final String toString() {
			return new File(this.getImagePath()).getName() + "[" + this.getBoundsAsString() + "]";
		}
		
		private static final long serialVersionUID = 847822079141878928L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-27)
	 */
	static final class ConcreteTrainingField implements Serializable {
		
		private final Image2D image;
		
		private final Image2D labels;
		
		private final Rectangle bounds;
		
		public ConcreteTrainingField(final Image2D image, final Image2D labels) {
			this(image, labels, new Rectangle(image.getWidth(), image.getHeight()));
		}
		
		public ConcreteTrainingField(final Image2D image,
				final Image2D labels, final Rectangle bounds) {
			this.image = image;
			this.labels = labels;
			this.bounds = bounds;
		}
		
		public final Image2D getImage() {
			return this.image;
		}
		
		public final Image2D getLabels() {
			return this.labels;
		}
		
		public final Rectangle getBounds() {
			return this.bounds;
		}
		
		private static final long serialVersionUID = 1918328132237430637L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-09)
	 */
	static final class ParameterTraining implements Serializable {
		
		private final Object object;
		
		private final Method accessor;
		
		private final Object key;
		
		private final int[] candidates;
		
		private int currentIndex;
		
		public ParameterTraining(final Object object, final Method accessor, final Object key, final int[] candidates) {
			this.object = object;
			this.accessor = accessor;
			this.key = key;
			this.candidates = candidates;
		}
		
		public final int getCandidateCount() {
			return this.candidates.length;
		}
		
		public final boolean hasNext() {
			return this.getCurrentIndex() + 1 < this.getCandidateCount();
		}
		
		public final int getCurrentIndex() {
			return this.currentIndex;
		}
		
		public final void setCurrentIndex(final int currentIndex) {
			this.currentIndex = currentIndex;
			
			this.set();
		}
		
		public final void next() {
			this.setCurrentIndex(this.getCurrentIndex() + 1);
		}
		
		public final void set() {
			try {
				if (this.key == null) {
					this.accessor.invoke(this.object, Integer.toString(this.candidates[this.currentIndex]));
				} else {
					((Map<Object, Object>) this.accessor.invoke(this.object)).put(this.key, this.candidates[this.currentIndex]);
				}
			} catch (final Exception exception) {
				throw Tools.unchecked(exception);
			}
		}
		
		@Override
		public final String toString() {
			return Arrays.toString(this.candidates) + "@" + this.currentIndex;
		}
		
		private static final long serialVersionUID = 1071010309738220354L;
		
		public static final int[] getCandidates(final Object object, final Method setter) {
			try {
				final Method trainer = getTrainer(object, setter.getAnnotation(Trainable.class));
				
				return new CommandLineArgumentsParser("range",
						trainer.invoke(object).toString()).get("range", 1);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
		}
		
		public static final Method getTrainer(final Object object, final Trainable trainable) {
			for (final Method method : object.getClass().getMethods()) {
				final PropertyGetter getter = method.getAnnotation(PropertyGetter.class);
				
				if (getter != null && getter.value().equals(trainable.value())) {
					return method;
				}
			}
			
			return null;
		}
		
		public static final void addTo(final List<ParameterTraining> parameterTrainings, final Object object, final Method accessor) {
			if (accessor.getParameterCount() == 1) {
				parameterTrainings.add(new ParameterTraining(object, accessor, null, getCandidates(object, accessor)));
			} else if (accessor.getParameterCount() == 0 && Map.class.isAssignableFrom(accessor.getReturnType())) {
				try {
					final Map<String, ?> map = (Map<String, ?>) accessor.invoke(object);
					final Method trainer = getTrainer(object, accessor.getAnnotation(Trainable.class));
					final Map<?, String> trainerMap = parseRangeMap(trainer.invoke(object).toString(), map.keySet());
					
					for (final Object key : map.keySet()) {
						parameterTrainings.add(new ParameterTraining(object, accessor, key,
								new CommandLineArgumentsParser("range", trainerMap.get(key)).get("range", 1)));
					}
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			} else {
				Tools.debugError(accessor);
				Tools.debugError(accessor.getParameterCount());
				throw new IllegalArgumentException();
			}
		}
		
	}
	
}