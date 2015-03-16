package imj3.draft.processing;

import static imj3.draft.machinelearning.Datum.Default.datum;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.array;
import static net.sourceforge.aprog.tools.Tools.baseName;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.join;
import static net.sourceforge.aprog.tools.Tools.last;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj2.draft.AutoCloseableImageWriter;
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
import imj3.tools.XMLSerializable;
import imj3.tools.CommonSwingTools.NestedList;
import imj3.tools.CommonSwingTools.PropertyGetter;
import imj3.tools.CommonSwingTools.PropertyOrdering;
import imj3.tools.CommonSwingTools.PropertySetter;
import imj3.tools.CommonSwingTools.StringGetter;
import imj3.tools.IMJTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
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

import javax.imageio.ImageIO;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author codistmonk (creation 2015-02-16)
 */
@PropertyOrdering({ "classes", "training", "algorithms" })
public final class Pipeline implements XMLSerializable {
	
	private List<TrainingField> trainingFields;
	
	private List<Algorithm> algorithms;
	
	private List<ClassDescription> classDescriptions;
	
	private Result trainingResult;
	
	private Result classificationResult;
	
	private ComputationStatus computationStatus;
	
	@Override
	public final Element toXML(final Document document, final Map<Object, Integer> ids) {
		final Element result = XMLSerializable.super.toXML(document, ids);
		final Node trainingFieldsNode = result.appendChild(document.createElement("trainingFields"));
		
		for (final TrainingField trainingField : this.getTrainingFields()) {
			trainingFieldsNode.appendChild(trainingField.toXML(document, ids));
		}
		
		final Node algorithmsNode = result.appendChild(document.createElement("algorithms"));
		
		for (final Algorithm algorithm : this.getAlgorithms()) {
			algorithmsNode.appendChild(algorithm.toXML(document, ids));
		}
		
		final Node classDescriptionsNode = result.appendChild(document.createElement("classDescriptions"));
		
		for (final ClassDescription classDescription : this.getClassDescriptions()) {
			classDescriptionsNode.appendChild(classDescription.toXML(document, ids));
		}
		
		result.appendChild(XMLSerializable.newElement("trainingResult", this.getTrainingResult(), document, ids));
		result.appendChild(XMLSerializable.newElement("classificationResult", this.getClassificationResult(), document, ids));
		
		return result;
	}
	
	@Override
	public final Pipeline fromXML(final Element xml, final Map<Integer, Object> objects) {
		XMLSerializable.super.fromXML(xml, objects);
		
		for (final Node node : XMLTools.getNodes(xml, "trainingFields/*")) {
			this.getTrainingFields().add(XMLSerializable.objectFromXML((Element) node, objects));
		}
		
		for (final Node node : XMLTools.getNodes(xml, "algorithms/*")) {
			this.getAlgorithms().add(XMLSerializable.objectFromXML((Element) node, objects));
		}
		
		for (final Node node : XMLTools.getNodes(xml, "classDescriptions/*")) {
			this.getClassDescriptions().add(XMLSerializable.objectFromXML((Element) node, objects));
		}
		
		this.trainingResult = XMLSerializable.objectFromXML((Element) XMLTools.getNode(xml, "trainingResult").getFirstChild(), objects);
		this.classificationResult = XMLSerializable.objectFromXML((Element) XMLTools.getNode(xml, "classificationResult").getFirstChild(), objects);
		
		return this;
	}

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
	
	public final Result getTrainingResult() {
		if (this.trainingResult == null) {
			this.trainingResult = new Result();
		}
		
		return this.trainingResult;
	}
	
	public final Result getClassificationResult() {
		if (this.classificationResult == null) {
			this.classificationResult = new Result();
		}
		
		return this.classificationResult;
	}
	
	public final Map<Integer, Map<Integer, AtomicLong>> getTrainingConfusionMatrix() {
		return this.getTrainingResult().getConfusionMatrix();
	}
	
	public final Map<Integer, Map<Integer, AtomicLong>> getClassificationConfusionMatrix() {
		return this.getClassificationResult().getConfusionMatrix();
	}
	
	public final long getTrainingMilliseconds() {
		return this.getTrainingResult().getMilliseconds();
	}
	
	public final long getClassificationMilliseconds() {
		return this.getClassificationResult().getMilliseconds();
	}
	
	public final synchronized ComputationStatus getComputationStatus() {
		if (this.computationStatus == null) {
			this.computationStatus = ComputationStatus.IDLE;
		}
		
		return this.computationStatus;
	}
	
	public final synchronized void setComputationStatus(final ComputationStatus computationStatus) {
		this.computationStatus = computationStatus;
	}
	
	public final void checkComputing() {
		if (!ComputationStatus.COMPUTING.equals(this.getComputationStatus())) {
			throw new RuntimeException("Computation canceled");
		}
	}
	
	public final Pipeline train(final String groundTruthName) {
		this.setComputationStatus(ComputationStatus.COMPUTING);
		
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
			Tools.debugPrint(bestScore, bestConfusionMatrix);
			Tools.debugPrint(parameterTrainings.stream().map(ParameterTraining::getCurrentIndex).toArray());
			
			parameterTrainings.forEach(ParameterTraining::set);
			
			train1(groundTruthName);
			
			final double score = f1(this.getTrainingConfusionMatrix());
			
			if (bestScore < score) {
				bestScore = score;
				bestConfusionMatrix = new HashMap<>(this.getTrainingConfusionMatrix());
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
		
		this.getTrainingResult().setMilliseconds(timer.toc());
		
		Tools.debugPrint("Training done in", this.getTrainingMilliseconds(), "ms");
		
		this.setComputationStatus(ComputationStatus.IDLE);
		
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
			
			final Image2D image = IMJTools.read(f.getImagePath(), 0);
			final Image2D labels = IMJTools.read(getGroundTruthPathFromImagePath(f.getImagePath(), groundTruthName), 0);
			
			out[0].add(new ConcreteTrainingField(image, labels, f.getBounds()));
		});
		
		for (final Algorithm algorithm : this.getAlgorithms()) {
			this.checkComputing();
			
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
			
			Tools.debugPrint("Validating...");
			
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
	
	public final Pipeline classify(final Image2D image, final Image2D labels, final Image2D classification) {
		return this.classify(image, new Rectangle(image.getWidth(), image.getHeight()), labels, classification);
	}
	
	public final Pipeline classify(final Image2D image, final Rectangle imageBounds, final Image2D labels, final Image2D classification) {
		Tools.debugPrint("Starting classification in", imageBounds);
		
		this.setComputationStatus(ComputationStatus.COMPUTING);
		
		final TicToc timer = new TicToc();
		final int tileSize = 512;
		final int left = imageBounds.x / tileSize * tileSize;
		final int top = imageBounds.y / tileSize * tileSize;
		final int rightEnd = (imageBounds.x + imageBounds.width + tileSize - 1) / tileSize * tileSize;
		final int bottomEnd = (imageBounds.y + imageBounds.height + tileSize - 1) / tileSize * tileSize;
		final int width = image.getWidth();
		final int height = image.getHeight();
		final Rectangle tileBounds = new Rectangle(width, height);
		
		this.getClassificationConfusionMatrix().clear();
		
		for (int y = top; y < bottomEnd; y += tileSize) {
			Tools.debugPrint(y, "/", bottomEnd);
			
			final int h = min(tileSize, height - y);
			
			for (int x = left; x < rightEnd; x += tileSize) {
				this.checkComputing();
				
				final int w = min(tileSize, width - x);
				
				tileBounds.setBounds(x, y, w, h);
				
				this.classify1(image, tileBounds, labels, classification);
			}
		}
		
		this.getClassificationResult().setMilliseconds(timer.toc());
		
		Tools.debugPrint("Classification done in", this.getClassificationMilliseconds(), "ms");
		
		this.setComputationStatus(ComputationStatus.IDLE);
		
		return this;
	}
	
	private final void classify1(final Image2D image, final Rectangle imageBounds, final Image2D labels,
			final Image2D classification) {
		final Algorithm last = this.getAlgorithms().isEmpty() ? null : last(this.getAlgorithms());
		Image2D actualLabels = null;
		
		{
			Image2D tmp = image;
			
			for (final Algorithm algorithm : this.getAlgorithms()) {
				final int patchSize = algorithm.getPatchSize();
				final int patchSparsity = algorithm.getPatchSparsity();
				final int stride = algorithm.getStride();
				final Image2DRawSource unbufferedInputs = new Image2DRawSource(tmp, null,
						patchSize, patchSparsity, stride, algorithm.isUsingXY());
				
				if (tmp == image) {
					unbufferedInputs.getBounds().setBounds(imageBounds);
				}
				
				final Rectangle bounds = unbufferedInputs.getBounds();
				final DataSource inputs = unbufferedInputs;
				final Classifier classifier = algorithm.getClassifier();
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
			final Image2D expectedLabels = labels;
			final int left = imageBounds.x;
			final int top = imageBounds.y;
			final int right = left + imageBounds.width - 1;
			final int bottom = top + imageBounds.height - 1;
			final int dx = max(1, right - left);
			final int dy = max(1, bottom - top);
			final int r = actualLabels.getWidth() - 1;
			final int b = actualLabels.getHeight() - 1;
			
			for (int y = top; y <= bottom; ++y) {
				for (int x = left; x <= right; ++x) {
					final int actualLabel = (int) actualLabels.getPixelValue((x - left) * r / dx, (y - top) * b / dy);
					
					classification.setPixelValue(x, y, actualLabel);
					
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
	public abstract class Algorithm implements XMLSerializable {
		
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
		
		@Override
		public final Element toXML(final Document document, final Map<Object, Integer> ids) {
			final Element result = XMLSerializable.super.toXML(document, ids);
			
			result.setAttribute(ENCLOSING_INSTANCE_ID, ids.get(this.getPipeline()).toString());
			result.setAttribute("clusteringName", this.getClusteringName());
			result.appendChild(XMLSerializable.newElement("classifier", this.getClassifier(), document, ids));
			result.setAttribute("patchSize", this.getPatchSizeAsString());
			result.setAttribute("patchSizeRange", this.getPatchSizeRange());
			result.setAttribute("patchSparsity", this.getPatchSparsityAsString());
			result.setAttribute("patchSparsityRange", this.getPatchSparsityRange());
			result.setAttribute("stride", this.getStrideAsString());
			result.setAttribute("strideRange", this.getStrideRange());
			result.setAttribute("usingXY", this.getUsingXYAsString());
			result.setAttribute("usingXYRange", this.getUsingXYRange());
			
			return this.subclassToXML(document, ids, result);
		}
		
		protected abstract Element subclassToXML(Document document, Map<Object, Integer> ids, Element result);
		
		@Override
		public final Algorithm fromXML(final Element xml, final Map<Integer, Object> objects) {
			XMLSerializable.super.fromXML(xml, objects);
			
			this.setClusteringName(xml.getAttribute("clusteringName"));
			this.setClassifier(XMLSerializable.objectFromXML((Element) XMLTools.getNode(xml, "classifier").getFirstChild(), objects));
			this.setPatchSize(xml.getAttribute("patchSize"));
			this.setPatchSizeRange(xml.getAttribute("patchSizeRange"));
			this.setPatchSparsity(xml.getAttribute("patchSparsity"));
			this.setPatchSparsityRange(xml.getAttribute("patchSparsityRange"));
			this.setStride(xml.getAttribute("stride"));
			this.setStrideRange(xml.getAttribute("strideRange"));
			this.setUsingXY(xml.getAttribute("usingXY"));
			this.setUsingXYRange(xml.getAttribute("usingXYRange"));
			
			return this.subclassFromXML(xml, objects);
		}
		
		protected abstract Algorithm subclassFromXML(Element xml, Map<Integer, Object> objects);
		
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
		protected final Element subclassToXML(final Document document, final Map<Object, Integer> ids,
				final Element result) {
			result.setAttribute("classCount", this.getClassCountAsString());
			
			return result;
		}
		
		@Override
		protected final UnsupervisedAlgorithm subclassFromXML(final Element xml, final Map<Integer, Object> objects) {
			return this.setClassCount(xml.getAttribute("classCount"));
		}
		
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
		
		@Override
		protected final Element subclassToXML(final Document document,
				final Map<Object, Integer> ids, final Element result) {
			result.appendChild(XMLSerializable.newElement("prototypeCounts", this.getPrototypeCounts(), document, ids));
			result.appendChild(XMLSerializable.newElement("prototypeCountRanges", this.getPrototypeCountRanges(), document, ids));
			
			return result;
		}
		
		@Override
		protected final SupervisedAlgorithm subclassFromXML(final Element xml,
				final Map<Integer, Object> objects) {
			this.prototypeCounts = XMLSerializable.objectFromXML((Element) XMLTools.getNode(xml, "prototypeCounts").getFirstChild(), objects);
			this.prototypeCountRanges = XMLSerializable.objectFromXML((Element) XMLTools.getNode(xml, "prototypeCountRanges").getFirstChild(), objects);
			
			return this;
		}
		
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
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final File pipelineFile = new File(arguments.get("pipeline", ""));
		final String pipelinePath = pipelineFile.getPath();
		final String groundTruthName = arguments.get("groundtruth", "");
		final String inputPath = arguments.get("in", "");
		final int lod = arguments.get("lod", 0)[0];
		final String pipelineName = baseName(pipelineFile.getName());
		final File classificationFile = new File(arguments.get("out", getClassificationPathFromImagePath(inputPath, groundTruthName, pipelineName)));
		final Pipeline pipeline;
		
		if (pipelinePath.endsWith(".jo")) {
			pipeline = Tools.readObject(pipelinePath);
		} else if (pipelinePath.endsWith(".xml")) {
			pipeline = XMLSerializable.objectFromXML(pipelineFile);
		} else {
			throw new IllegalArgumentException();
		}
		
		final Image2D image = IMJTools.read(inputPath, lod);
		final int width = image.getWidth();
		final int height = image.getHeight();
		final Image2D result = new AwtImage2D(null, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
		
		pipeline.classify(image, null, result);
		
		{
			Tools.debugPrint("Writing", classificationFile);
			ImageIO.write((RenderedImage) result.toAwt(), "png", classificationFile);
		}
		
		{
			final String overlayedContoursFormat = "jpg";
			final File overlayedContoursFile = new File(baseName(inputPath) + "_overlayedcontours_" + groundTruthName + "_" + pipelineName + "." + overlayedContoursFormat);
			final Image2D overlayedCountours = new AwtImage2D(null, new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
			
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					final long label = result.getPixelValue(x, y);
					final long north = label != 0L && 0 < y ? result.getPixelValue(x, y - 1) : label;
					final long west = label != 0L && 0 < x ? result.getPixelValue(x - 1, y) : label;
					final long east = label != 0L && x + 1 < width ? result.getPixelValue(x + 1, y) : label;
					final long south = label != 0L && y + 1 < height ? result.getPixelValue(x, y + 1) : label;
					
					if (label == north && label == west && label == east && label == south) {
						overlayedCountours.setPixelValue(x, y, image.getPixelValue(x, y));
					} else {
						overlayedCountours.setPixelValue(x, y, label);
					}
				}
			}
			
			Tools.debugPrint("Writing", overlayedContoursFile);
			AutoCloseableImageWriter.write((RenderedImage) overlayedCountours.toAwt(),
					overlayedContoursFormat, 0.9F, overlayedContoursFile);
		}
	}
	
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
	
	public static final String getClassificationPathFromImagePath(final String imagePath, final String groundTruthName, final String pipelineName) {
		return baseName(imagePath) + "_classification_" + groundTruthName + "_" + pipelineName + ".png";
	}
	
	public static final <K, N extends Number> double f1(final Map<K, Map<K, N>> confusionMatrix) {
		double numerator = 0.0;
		double denominator = 0.0;
		
		for (final Map.Entry<K, Map<K, N>> entry : confusionMatrix.entrySet()) {
			final Object expectedKey = entry.getKey();
			double total = 0.0;
			
			for (final N count : entry.getValue().values()) {
				total += count.doubleValue();
			}
			
			if (total == 0.0) {
				total = 1.0;
			}
			
			for (final Map.Entry<K, N> subEntry : entry.getValue().entrySet()) {
				final Object actualKey = subEntry.getKey();
				final double count = subEntry.getValue().doubleValue();
				
				if (expectedKey.equals(actualKey)) {
					numerator += count / total;
				} else {
					denominator += count / total;
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
	public static final class ClassDescription implements XMLSerializable {
		
		private String name = "class";
		
		private int label = 0xFF000000;
		
		@Override
		public final Element toXML(final Document document, final Map<Object, Integer> ids) {
			final Element result = XMLSerializable.super.toXML(document, ids);
			
			result.setAttribute("name", this.getName());
			result.setAttribute("label", this.getLabelAsString());
			
			return result;
		}
		
		@Override
		public final ClassDescription fromXML(final Element xml, final Map<Integer, Object> objects) {
			XMLSerializable.super.fromXML(xml, objects);
			
			this.setName(xml.getAttribute("name"));
			this.setLabel(xml.getAttribute("label"));
			
			return this;
		}
		
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
	public static final class TrainingField implements XMLSerializable {
		
		private String imagePath = "";
		
		private final Rectangle bounds = new Rectangle();
		
		@Override
		public final Element toXML(final Document document, final Map<Object, Integer> ids) {
			final Element result = XMLSerializable.super.toXML(document, ids);
			
			result.setAttribute("imagePath", this.getImagePath());
			result.setAttribute("bounds", this.getBoundsAsString());
			
			return result;
		}
		
		@Override
		public final TrainingField fromXML(final Element xml, final Map<Integer, Object> objects) {
			XMLSerializable.super.fromXML(xml, objects);
			
			this.setImagePath(xml.getAttribute("imagePath"));
			this.setBounds(xml.getAttribute("bounds"));
			
			return this;
		}
		
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
		
		@SuppressWarnings("unchecked")
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
					@SuppressWarnings("unchecked")
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
	
	/**
	 * @author codistmonk (creation 2015-03-14)
	 */
	public static enum ComputationStatus {
		
		COMPUTING, CANCELED, IDLE;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-03-15)
	 */
	public static final class Result implements XMLSerializable {
		
		private long milliseconds;
		
		private Map<Integer, Map<Integer, AtomicLong>> confusionMatrix;
		
		@Override
		public final Element toXML(final Document document, final Map<Object, Integer> ids) {
			final Element result = XMLSerializable.super.toXML(document, ids);
			
			result.setAttribute("milliseconds", Long.toString(this.getMilliseconds()));
			result.appendChild(XMLSerializable.objectToXML(this.getConfusionMatrix(), document, ids));
			
			return result;
		}
		
		@Override
		public Result fromXML(final Element xml, final Map<Integer, Object> objects) {
			XMLSerializable.super.fromXML(xml, objects);
			
			this.setMilliseconds(Long.parseLong(xml.getAttribute("milliseconds")));
			this.confusionMatrix = XMLSerializable.objectFromXML((Element) xml.getFirstChild(), objects);
			
			return this;
		}
		
		public final long getMilliseconds() {
			return this.milliseconds;
		}
		
		public final void setMilliseconds(final long milliseconds) {
			this.milliseconds = milliseconds;
		}
		
		public final Map<Integer, Map<Integer, AtomicLong>> getConfusionMatrix() {
			if (this.confusionMatrix == null)  {
				this.confusionMatrix = new HashMap<>();
			}
			return this.confusionMatrix;
		}
		
		private static final long serialVersionUID = -7054191235319173687L;
		
	}
	
}
