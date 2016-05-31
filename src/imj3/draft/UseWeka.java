package imj3.draft;

import static multij.tools.Tools.*;

import imj3.draft.EvaluateClassification.ConfusionMatrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ProtectedProperties;

/**
 * @author codistmonk (creation 2015-09-05)
 */
public final class UseWeka {
	
	private UseWeka() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String trainingBinPath = arguments.get("train", "");
		final String testBinPath = arguments.get("test", "");
		final int itemSize = arguments.get("itemSize", 2)[0];
		final int classCount = arguments.get("classCount", 2)[0];
		final File trainingBinFile = new File(trainingBinPath);
		final String datasetName = baseName(trainingBinFile.getName());
		final Instances trainingData = newDataset(datasetName, itemSize, classCount);
		
		{
			debugPrint("Initializing data...");
			
			trainingData.setClassIndex(0);
			
			final byte[] item = new byte[itemSize];
			final double[] values = new double[itemSize];
			
			try (final InputStream input = new FileInputStream(trainingBinFile)) {
				while (itemSize == input.read(item)) {
					for (int i = 0; i < itemSize; ++i) {
						values[i] = 0xFF & item[i];
					}
					
					trainingData.add(new DenseInstance(1.0, values.clone()));
				}
			} catch (final IOException exception) {
				throw unchecked(exception);
			}
			
			debugPrint("dataSize:", trainingData.numInstances());
		}
		
		{
			debugPrint("Building classifier...");
			
			final Classifier wekaClassifier = new RandomForest();
			
			try {
				wekaClassifier.buildClassifier(trainingData);
				debugPrint("Classifier ready:", wekaClassifier);
			} catch (final Exception exception) {
				throw unchecked(exception);
			}
			
			{
				debugPrint("Applying classifier...");
				
				final File testBinFile = new File(testBinPath);
				final byte[] item = new byte[itemSize];
				final double[] values = new double[itemSize];
				final ConfusionMatrix confusionMatrix = new ConfusionMatrix();
				
				try (final InputStream input = new FileInputStream(testBinFile)) {
					while (itemSize == input.read(item)) {
						for (int i = 0; i < itemSize; ++i) {
							values[i] = 0xFF & item[i];
						}
						
						final Instance instance = new DenseInstance(1.0, values);
						
						instance.setDataset(trainingData);
						
						final double expected = values[0];
						final double predicted = wekaClassifier.classifyInstance(instance);
						
						confusionMatrix.count(predicted, expected);
					}
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
				
				debugPrint("Confusion matrix");
				EvaluateClassification.writeCSV(confusionMatrix, "\t", System.out);
				debugPrint("F1s:", confusionMatrix.computeF1s());
				debugPrint("F1:", confusionMatrix.computeMacroF1());
			}
		}
	}

	public static Instances newDataset(final String datasetName,
			final int itemSize, final int classCount) {
		final FastVector attributes = new FastVector(itemSize);
		final FastVector classLabels = new FastVector(2);
		
		for (int i = 0; i < classCount; ++i) {
			classLabels.addElement("" + i);
		}
		
		attributes.addElement(new Attribute("class", classLabels, new ProtectedProperties(new Properties())));
		
		for (int i = 1; i < itemSize; ++i) {
			attributes.addElement(new Attribute("x" + i));
		}
		
		final Instances data = new Instances(datasetName, attributes, 0);
		return data;
	}

}
