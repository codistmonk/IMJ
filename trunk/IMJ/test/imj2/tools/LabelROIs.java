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
import imj2.core.SubsampledImage2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

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
			try {
				final Image2D image = new SubsampledImage2D(new LociBackedImage(((Element) node).getAttribute("image"), 4));
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				
				Tools.debugPrint(image.getId(), imageWidth, imageHeight);
				
				final Image2D gradientImage = computeGradient(image);
				
				Tools.debugPrint(gradientImage.getWidth(), gradientImage.getHeight());
				
				final SegmentationGrid segmentationGrid = new SegmentationGrid(gradientImage, 12);
			} catch (final Exception exception) {
				Tools.debugError(exception);
			}
		});
		XMLTools.getNodes(argumentsDocument, "arguments/test").forEach(node -> {
			try {
				final Image2D image = new SubsampledImage2D(new LociBackedImage(((Element) node).getAttribute("image"), 4)).getLODImage(2);
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				
				Tools.debugPrint(image.getId(), imageWidth, imageHeight);
				
				final Image2D gradientImage = computeGradient(image);
				
				Tools.debugPrint(gradientImage.getWidth(), gradientImage.getHeight());
				
				final Image2D segmentationMask = computeSegmentationMask(image, gradientImage, 12);
				
				ImageIO.write(IMJTools.awtImage(image), "png", new File("image.png"));
				ImageIO.write(IMJTools.awtImage(segmentationMask), "png", new File("segmentation.png"));
				ImageIO.write(newSegmentationVisualization(image, segmentationMask), "png", new File("segmented.png"));
			} catch (final Exception exception) {
				Tools.debugError(exception);
			}
		});
	}
	
	public static final BufferedImage newSegmentationVisualization(final Image2D image, final Image2D segmentationMask) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final BufferedImage result = IMJTools.awtImage(image);
		
		{
			final Graphics2D graphics = result.createGraphics();
			
			graphics.setColor(Color.BLACK);
			
			segmentationMask.forEachPixelInBox(0, 0, imageWidth, imageHeight, new MonopatchProcess() {
				
				@Override
				public final void pixel(final int x, final int y) {
					if (segmentationMask.getPixelValue(x, y) != 0) {
						graphics.fillRect(x, y, 2, 2);
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 7131721130964203008L;
				
			});
			
			graphics.dispose();
		}
		
		return result;
	}
	
	public static final Image2D computeSegmentationMask(final Image2D image, final Image2D gradientImage, final int stripeSize) {
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final Image2D result = newConcreteC1U1Image2D(image.getId() + "_segmentation", imageWidth, imageHeight);
		
		for (int stripeX = 0; stripeX < imageWidth; stripeX += stripeSize) {
			int x = min(imageWidth - 1, stripeX + stripeSize / 2);
			
			for (int y = 0; y < imageHeight; ++y) {
				int gradient = gradientImage.getPixelValue(x, y);
				final int leftVariation = stripeX < x - 1 ? gradientImage.getPixelValue(x - 1, y) - gradient : 0;
				final int rightVariation = x + 1 < min(imageWidth, stripeX + stripeSize) ? gradientImage.getPixelValue(x + 1, y) - gradient : 0;
				
				if (max(0, leftVariation) < rightVariation) {
					++x;
				} else if (max(0, rightVariation) < leftVariation) {
					--x;
				}
				
				result.setPixelValue(x, y, 1);
			}
		}
		
		for (int stripeY = 0; stripeY < imageHeight; stripeY += stripeSize) {
			int y = min(imageHeight - 1, stripeY + stripeSize / 2);
			
			for (int x = 0; x < imageWidth; ++x) {
				int gradient = gradientImage.getPixelValue(x, y);
				final int topVariation = stripeY < y - 1 ? gradientImage.getPixelValue(x, y - 1) - gradient : 0;
				final int bottomVariation = y + 1 < min(imageHeight, stripeY + stripeSize) ? gradientImage.getPixelValue(x, y + 1) - gradient : 0;
				
				if (max(0, topVariation) < bottomVariation) {
					++y;
				} else if (max(0, bottomVariation) < topVariation) {
					--y;
				}
				
				result.setPixelValue(x, y, 1);
			}
		}
		
		return result;
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
