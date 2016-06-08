package imj3.draft;

import static imj3.draft.SVG2Bin.*;
import static imj3.draft.SVGTools.*;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.*;

import imj3.core.Image2D;
import imj3.tools.IMJTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2015-06-09)
 */
public final class SVG2PNG {
	
	private SVG2PNG() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		final int lod = arguments.get("lod", 4)[0];
		final int stride = arguments.get("stride", 1)[0];
		final String svgPath = arguments.get("svg", baseName(imagePath) + ".svg");
		final File svgFile = new File(svgPath);
		final File classesFile = new File(arguments.get("classes", new File(svgFile.getParentFile(), "classes.xml").getPath()));
		final Document svg = readXML(svgFile);
		final Element svgRoot = svg.getDocumentElement();
		
		String classIds = arguments.get("classIds", "");
		
		if (classIds.isEmpty()) {
			final Document classes = readXML(classesFile);
			classIds = join(",", getNodes(classes, "//class").stream().map(node -> ((Element) node).getAttribute("id")).toArray());
		}
		
		final String[] classIdsArray = classIds.split(",");
		
		int w = arguments.get("width", 0)[0];
		int h = arguments.get("height", w)[0];
		double scale = pow(2.0, -lod);
		
		if (w == 0 || h == 0) {
			if (!imagePath.isEmpty()) {
				try {
					final Image2D image = IMJTools.read(imagePath, lod);
					
					debugPrint("LOD:", lod, "imageWidth:", image.getWidth(), "imageWidth:", image.getHeight(), "imageChannels:", image.getChannels());
					
					w = resize(image.getWidth(), stride);
					h = resize(image.getHeight(), stride);
					scale *= (double) max(w, h) / max(image.getWidth(), image.getHeight());
				} catch (final Exception exception) {
					throw unchecked(exception);
				}
			} else {
				final Rectangle bounds = new Rectangle(getInt(svgRoot, "width", 0), getInt(svgRoot, "height", 0));
				
				if (w == 0 || h == 0) {
					for (final Node regionNode : getNodes(svg, "//path|//polygon")) {
						bounds.add(newRegion((Element) regionNode).getBounds());
					}
					
					++bounds.width;
					++bounds.height;
				}
				
				w = resize(bounds.width >> lod, stride);
				h = resize(bounds.height >> lod, stride);
				scale = (double) max(w, h) / max(bounds.width, bounds.height);
			}
		}
		
		final int clearColor = arguments.get("clear", -1)[0];
		final Canvas canvas = new Canvas().setFormat(w, h, BufferedImage.TYPE_3BYTE_BGR).clear(new Color(clearColor));
		final String outputPath = arguments.get("output", baseName(svgPath) + "_groundtruth.png");
		final Graphics2D graphics = canvas.getGraphics();
		
		debugPrint("classIds:", classIds);
		debugPrint("stride:", stride, "scale:", scale, "w:", w, "h:", h);
		
		draw(svg, scale, classIdsArray, graphics);
		
		final File outputFile = new File(outputPath);
		
		try {
			debugPrint("Writing", outputFile);
			ImageIO.write(canvas.getImage(), "png", outputFile);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final void draw(final Document svg, double scale, final String[] classIdsArray, final Graphics2D graphics) {
		for (final Node regionNode : getNodes(svg, "//path|//polygon")) {
			final Element regionElement = (Element) regionNode;
			final int label = indexOf(regionElement.getAttribute("imj:classId"), classIdsArray);
			
			if (label < 0) {
				continue;
			}
			
			final Area region = newRegion(regionElement);
			
			region.transform(AffineTransform.getScaleInstance(scale, scale));
			
			graphics.setColor(new Color(label));
			graphics.fill(region);
		}
	}
	
	public static int resize(final int size, final int stride) {
		return (size - stride / 2 + stride - 1) / stride;
	}
	
	public static final int getInt(final Node node, final String attributeName, final int defaultValue) {
		final String attributeValue = ((Element) node).getAttribute(attributeName);
		
		return attributeValue.isEmpty() ? defaultValue : Long.decode(attributeValue).intValue();
	}
	
}
