package imj3.draft;

import static imj3.draft.SVG2Bin.*;
import static java.lang.Math.pow;
import static multij.tools.Tools.*;
import static multij.xml.XMLTools.*;

import imj3.core.Image2D;
import imj3.tools.IMJTools;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
		final String svgPath = arguments.get("svg", baseName(imagePath) + ".svg");
		final File svgFile = new File(svgPath);
		final File classesFile = new File(arguments.get("classes", new File(svgFile.getParentFile(), "classes.xml").getPath()));
		final Document svg = readXML(svgFile);
		final Document classes = readXML(classesFile);
		final String[] classIds = arguments.get("classIds", join(",",
				getNodes(classes, "//class").stream().map(node -> ((Element) node).getAttribute("id")).toArray())).split(",");
		final Image2D image = IMJTools.read(imagePath, lod);
		final Canvas canvas = new Canvas().setFormat(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		
		debugPrint("LOD:", lod, "imageWidth:", image.getWidth(), "imageWidth:", image.getHeight(), "imageChannels:", image.getChannels());
		debugPrint("classIds", Arrays.toString(classIds));
		
		for (final Node regionNode : getNodes(svg, "//path")) {
			final Element regionElement = (Element) regionNode;
			final int label = indexOf(regionElement.getAttribute("imj:classId"), classIds);
			
			if (label < 0) {
				continue;
			}
			
			final Area region = newRegion(regionElement);
			final double scale = pow(2.0, -lod);
			
			region.transform(AffineTransform.getScaleInstance(scale, scale));
			
			canvas.getGraphics().setColor(new Color(label));
			canvas.getGraphics().fill(region);
		}
		
		final File outputFile = new File(baseName(svgPath) + "_groundtruth.png");
		
		try {
			debugPrint(outputFile);
			ImageIO.write(canvas.getImage(), "png", outputFile);
		} catch (final IOException exception) {
			exception.printStackTrace();
		}
	}
	
}
