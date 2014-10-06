package imj2.draft;

import static imj2.tools.IMJTools.blue8;
import static imj2.tools.IMJTools.green8;
import static imj2.tools.IMJTools.red8;
import static imj2.topology.Manifold.opposite;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import imj2.core.ConcreteImage2D;
import imj2.core.Image;
import imj2.core.Image2D;
import imj2.core.Image2D.MonopatchProcess;
import imj2.core.LinearBooleanImage;
import imj2.core.LinearIntImage;
import imj2.core.SubsampledImage2D;
import imj2.tools.IMJTools;
import imj2.tools.LociBackedImage;
import imj2.topology.Manifold;
import imj2.topology.Manifold.DartProcessor;
import imj2.topology.Manifold.Traversor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.sourceforge.aprog.swing.SwingTools;
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
				
				final Partition2D segmentation = new Partition2D(imageWidth, imageHeight);
				
				segmentation.makeGrid(32, 3);
				
				{
					final BufferedImage awtImage = IMJTools.awtImage(image);
					
					draw(segmentation, awtImage, Color.RED);
					
					SwingTools.show(awtImage, image.getId(), false);
				}
			} catch (final NullPointerException | IndexOutOfBoundsException exception) {
				exception.printStackTrace();
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

	public static void draw(final Partition2D segmentation,
			final BufferedImage awtImage, final Color color) {
		final Graphics2D graphics = awtImage.createGraphics();
		
		graphics.setColor(color);
		
		segmentation.forEach(Traversor.EDGE, dart -> {
			final Point2D edgeBegin = segmentation.getVertex(dart);
			final Point2D edgeEnd = segmentation.getVertex(opposite(dart));
			final int x1 = (int) edgeBegin.getX();
			final int y1 = (int) edgeBegin.getY();
			final int x2 = (int) edgeEnd.getX();
			final int y2 = (int) edgeEnd.getY();
			
			graphics.setColor(color);
			graphics.drawLine(x1, y1, x2, y2);
			graphics.setColor(Color.BLUE);
			graphics.drawOval(x1 - 2, y1 - 2, 5, 5);
			graphics.drawOval(x2 - 2, y2 - 2, 5, 5);
			
			return true;
		});
		
		graphics.dispose();
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
	 * @author codistmonk (creation 2014-10-06)
	 */
	public static final class Partition2D implements Serializable {
		
		private final Manifold manifold;
		
		private final List<Point2D> vertices;
		
		public Partition2D(final int width, final int height) {
			this.manifold = new Manifold();
			this.vertices = new ArrayList<>();
			
			final int left = this.manifold.newEdge();
			this.vertices.add(new Point2D.Float(0F, 0F));
			final int bottom = this.manifold.newEdge();
			this.vertices.add(new Point2D.Float(0F, height - 1F));
			final int right = this.manifold.newEdge();
			this.vertices.add(new Point2D.Float(width - 1F, height - 1F));
			final int top = this.manifold.newEdge();
			this.vertices.add(new Point2D.Float(width - 1F, 0F));
			
			this.manifold.setNext(left, bottom);
			this.manifold.setNext(bottom, right);
			this.manifold.setNext(right, top);
			this.manifold.setNext(top, left);
			this.manifold.setNext(opposite(left), opposite(top));
			this.manifold.setNext(opposite(top), opposite(right));
			this.manifold.setNext(opposite(right), opposite(bottom));
			this.manifold.setNext(opposite(bottom), opposite(left));
		}
		
		public final int getLeftDart() {
			return 0;
		}
		
		public final int getBottomDart() {
			return 2;
		}
		
		public final int getRightDart() {
			return 4;
		}
		
		public final int getTopDart() {
			return 6;
		}
		
		public final Point2D getVertex(final int dart) {
			if ((dart & 1) == 0) {
				return this.vertices.get(dart / 2);
			}
			
			return this.getVertex(this.manifold.getNext(opposite(dart)));
		}
		
		public final int cut(final int dart) {
			final Point2D edgeBegin = this.getVertex(dart);
			final Point2D edgeEnd = this.getVertex(opposite(dart));
			final int result = this.manifold.cutEdge(dart);
			
			this.vertices.add(midpoint(edgeBegin, edgeEnd));
			
			return result;
		}
		
		public final int splitByCuttingBoth(final int dart1, final int dart2) {
			this.cut(dart2);
			
			return this.splitByCuttingFirst(dart1, dart2);
		}
		
		public final int splitByCuttingFirst(final int dart1, final int dart2) {
			final Point2D vertex = this.getVertex(this.cut(dart1));
			
			final int result = this.manifold.cutFace(dart1, dart2);
			this.vertices.add(vertex);
			
			return result;
		}
		
		public final void makeGrid(final int step, final int edgeDivisions) {
			if (this.manifold.getNext(this.manifold.getNext(this.getLeftDart())) != this.getRightDart()) {
				throw new IllegalStateException();
			}
			
			final Point2D bottomRight = this.getVertex(this.getRightDart());
			final int imageWidth = (int) (bottomRight.getX() + 1.0);
			final int imageHeight = (int) (bottomRight.getY() + 1.0);
			final int verticalDivisions = (imageHeight + step - 1) / step;
			final List<Integer> horizontalStrips = new ArrayList<>(verticalDivisions);
			
			if (step < imageHeight) {
				int y = (verticalDivisions - 1) * step;
				int newDart = this.splitByCuttingBoth(this.getLeftDart(), this.getRightDart());
				
				horizontalStrips.add(0, newDart);
				
				setY(this.getVertex(newDart), y);
				setY(this.getVertex(opposite(newDart)), y);
				y -= step;
				
				for (int i = 2; i < verticalDivisions; ++i) {
					newDart = this.splitByCuttingBoth(this.getLeftDart(), this.manifold.getNext(newDart));
					
					horizontalStrips.add(0, newDart);
					
					setY(this.getVertex(newDart), y);
					setY(this.getVertex(opposite(newDart)), y);
					y -= step;
				}
			}
			
			horizontalStrips.add(this.getBottomDart());
			
			final int horizontalDivisions = (imageWidth + step - 1) / step;
			
			if (1 < horizontalDivisions) {
				int x = (horizontalDivisions - 1) * step;
				
				for (int i = 1; i < horizontalDivisions; ++i) {
					boolean firstStrip = true;
					
					for (final Integer stripBottom : horizontalStrips) {
						final int stripTop = this.manifold.getNext(this.manifold.getNext(stripBottom));
						
						if (firstStrip) {
							this.cut(stripTop);
							firstStrip = false;
						}
						
						final int newDart = this.splitByCuttingFirst(stripBottom, stripTop);
						
						setX(this.getVertex(newDart), x);
						setX(this.getVertex(opposite(newDart)), x);
					}
					
					x -= step;
				}
			}
			
			this.manifold.forEach(Traversor.EDGE, dart -> {
				for (int i = 1; i < edgeDivisions; ++i) {
					// TODO adjust vertex location
					this.cut(dart);
				}
				
				return true;
			});
		}
		
		public final void forEach(final Traversor traversor, final DartProcessor processor) {
			this.manifold.forEach(traversor, processor);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 4651220208345159029L;
		
		public static final Point2D midpoint(final Point2D p1, final Point2D p2) {
			return new Point2D.Float(middle(p1.getX(), p2.getX()), middle(p1.getY(), p2.getY()));
		}
		
		public static final float middle(final double a, final double b) {
			return (float) ((a + b) / 2.0);
		}
		
		public static final void setX(final Point2D point, final double x) {
			point.setLocation(x, point.getY());
		}
		
		public static final void setY(final Point2D point, final double y) {
			point.setLocation(point.getX(), y);
		}
		
	}
	
}
