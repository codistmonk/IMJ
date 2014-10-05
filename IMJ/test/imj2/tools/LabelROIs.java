package imj2.tools;

import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import imj2.core.ConcreteImage2D;
import imj2.core.Image;
import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;
import imj2.core.LinearBooleanImage;
import imj2.core.LinearIntImage;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author codistmonk (creation 2014-10-05)
 */
public final class LabelROIs {
	
	private LabelROIs() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		IMJTools.toneDownBioFormatsLogger();
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String argumentsPath = arguments.get("arguments", "arguments.xml");
		final Document argumentsDocument = XMLTools.parse(Tools.getResourceAsStream(argumentsPath));
		
		XMLTools.getNodes(argumentsDocument, "arguments/reference").forEach(node -> {
				final Image2D image = new LociBackedImage(((Element) node).getAttribute("image"), 3);
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				
				Tools.debugPrint(image.getId(), imageWidth, imageHeight);
				
				final Image2D gradientImage = computeGradient(image);
				
				Tools.debugPrint(gradientImage.getWidth(), gradientImage.getHeight());
				
				final Image2D segmentationMask = newConcreteC1U1Image2D(image.getId() + "_segmentation", image.getWidth(), image.getHeight());
				final int s = 32;
				
				
		});
		XMLTools.getNodes(argumentsDocument, "arguments/test").forEach(
				node -> Tools.debugPrint(((Element) node).getAttribute("image")));
	}

	public static Image2D computeGradient(final Image2D image) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Image2D gradientImage = newConcreteC1S32Image2D(image.getId() + "_gradient", imageWidth, imageHeight);
		
		gradientImage.forEachPixelInBox(0, 0, imageWidth, imageHeight, new MonopatchProcess() {
			
			@Override
			public final void pixel(final int x, final int y) {
				int gradient = 0;
				final int pixelValue = image.getPixelValue(x, y);
				final int pixelRed = red8(pixelValue);
				final int pixelGreen = green8(pixelValue);
				final int pixelBlue = blue8(pixelValue);
				
				for (int yy = max(0, y - 1); yy < min(imageHeight, y + 1); ++yy) {
					for (int xx = max(0, x - 1); xx < min(imageWidth, x + 1); ++xx) {
						final int neighborValue = image.getPixelValue(xx, yy);
						
						gradient += abs(red8(neighborValue) - pixelRed)
								+ abs(green8(neighborValue) - pixelGreen)
								+ abs(blue8(neighborValue) - pixelBlue);
					}
				}
				
				gradientImage.setPixelValue(x, y, gradient);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 8218734373002672032L;
			
		});
		return gradientImage;
	}
	
	public static final ConcreteImage2D<LinearIntImage> newConcreteC1S32Image2D(final String id,
			final int imageWidth, final int imageHeight) {
		return new ConcreteImage2D<>(new LinearIntImage(id, (long) imageWidth * imageHeight,
				Image.PredefinedChannels.C1_S32), imageWidth, imageHeight);
	}
	
	public static final ConcreteImage2D<LinearBooleanImage> newConcreteC1U1Image2D(final String id,
			final int imageWidth, final int imageHeight) {
		return new ConcreteImage2D<>(new LinearBooleanImage(
				id, (long) imageWidth * imageHeight), imageWidth, imageHeight);
	}
	
	/**
	 * @author codistmonk (creation 2014-10-05)
	 */
	public static final class SegmentationGrid implements Serializable {
		
		private final List<Point2D> vertices;
		
		private final int columnCount;
		
		private final int rowCount;
		
		public SegmentationGrid(final Image2D image, final int segmentationSize) {
			this.vertices = new ArrayList<>();
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			this.columnCount = (int) ceil((double) imageWidth / segmentationSize);
			this.rowCount = (int) ceil((double) imageHeight / segmentationSize);
			
			for (int row = 0; row < this.rowCount; ++row) {
				final int y = min(imageHeight - 1, row * segmentationSize);
				
				for (int column = 0; column < this.columnCount; ++column) {
					final int x = min(imageWidth - 1, column * segmentationSize);
					
					this.vertices.add(new Point2D.Float(x, y));
				}
			}
		}
		
		public final int getColumnCount() {
			return this.columnCount;
		}
		
		public final int getRowCount() {
			return this.rowCount;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5230229435132305086L;
		
	}
	
}
